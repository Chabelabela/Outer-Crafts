package org.chabelabela.outer_crafts.world.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.physics.CelestialBody;
import org.chabelabela.outer_crafts.registry.CelestialBodyRegistry;
import org.chabelabela.outer_crafts.world.dimension.OuterCraftsDimensions;

import java.util.Map;

/**
 * Brittle Hollow procedural collapse (3.4).
 *
 * <p>Throughout the loop, chunks of Brittle Hollow's outer crust break loose and
 * fall into its central black hole. We pick a random surface position, scoop a
 * small cube of blocks, and replace each with a {@link FallingBlockEntity} whose
 * deltaMovement points <i>toward the planet centre</i> rather than -Y.
 *
 * <p>This relies on the gravity mixin already being scoped to
 * {@link FallingBlockEntity} (see {@code EntityMoveMixin}) so the falling blocks
 * accelerate radially, not straight down.
 *
 * <p>The cadence ramps up later in the loop (more collapses near the supernova).
 */
@EventBusSubscriber(modid = OuterCrafts.MODID)
public final class BrittleHollowCollapseManager {

    private BrittleHollowCollapseManager() {}

    private static final String BODY_ID = "brittle_hollow";

    /** Base interval — early in the loop. */
    private static final int BASE_INTERVAL = 600; // 30s
    /** Min interval — late in the loop, lots of collapses. */
    private static final int MIN_INTERVAL = 100;  // 5s

    /** Half-edge of the cube of blocks scooped per collapse. */
    private static final int COLLAPSE_HALF_EDGE = 1;

    /** How far above the planet's surface we sample collapse origins. */
    private static final double SURFACE_OFFSET = 1.5;

    private static final RandomSource RNG = RandomSource.create(0xBEEF_BEEFL);
    private static int nextTriggerTick = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int now = server.getTickCount();
        if (now < nextTriggerTick) return;

        ServerLevel solar = server.getLevel(OuterCraftsDimensions.SOLAR_SYSTEM_LEVEL);
        if (solar == null) {
            nextTriggerTick = now + BASE_INTERVAL;
            return;
        }

        CelestialBody bh = CelestialBodyRegistry.get(BODY_ID);
        if (bh == null) {
            nextTriggerTick = now + BASE_INTERVAL;
            return;
        }

        Map<String, Vec3> positions = CelestialBodyRegistry.resolveAllPositions(solar.getGameTime());
        Vec3 center = positions.get(BODY_ID);
        if (center == null) {
            nextTriggerTick = now + BASE_INTERVAL;
            return;
        }

        triggerCollapse(solar, bh, center);

        // Schedule the next collapse — interval shrinks as the loop progresses.
        double progress = org.chabelabela.outer_crafts.timeloop.TimeLoopManager.getLoopProgress();
        int interval = (int) (BASE_INTERVAL + (MIN_INTERVAL - BASE_INTERVAL) * progress);
        nextTriggerTick = now + Math.max(MIN_INTERVAL, interval);
    }

    private static void triggerCollapse(ServerLevel level, CelestialBody body, Vec3 center) {
        // Pick a random direction on the unit sphere
        Vec3 dir = randomUnitVector(RNG);
        double r = body.radius() + SURFACE_OFFSET;
        BlockPos centre = BlockPos.containing(
                center.x + dir.x * r,
                center.y + dir.y * r,
                center.z + dir.z * r);

        if (!level.isLoaded(centre)) return;

        // Scoop a small cube; convert each non-air block into a FallingBlockEntity.
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int spawned = 0;
        for (int dx = -COLLAPSE_HALF_EDGE; dx <= COLLAPSE_HALF_EDGE; dx++) {
            for (int dy = -COLLAPSE_HALF_EDGE; dy <= COLLAPSE_HALF_EDGE; dy++) {
                for (int dz = -COLLAPSE_HALF_EDGE; dz <= COLLAPSE_HALF_EDGE; dz++) {
                    cursor.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    if (!level.isLoaded(cursor)) continue;
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir() || state.is(Blocks.BEDROCK)) continue;

                    // Replace with air, then spawn a FallingBlockEntity at the same spot.
                    level.removeBlock(cursor, false);
                    FallingBlockEntity falling = FallingBlockEntity.fall(level, cursor, state);
                    // Initial velocity toward the planet centre — gravity mixin will
                    // continue to accelerate it as it travels.
                    Vec3 toward = center.subtract(Vec3.atCenterOf(cursor)).normalize().scale(0.05);
                    falling.setDeltaMovement(toward);
                    spawned++;
                }
            }
        }

        if (spawned > 0) {
            OuterCrafts.LOGGER.debug("[BrittleHollow] Collapsed {} blocks at {}", spawned, centre);
        }
    }

    private static Vec3 randomUnitVector(RandomSource random) {
        // Marsaglia method: sample on the unit sphere
        double u, v, s;
        do {
            u = random.nextDouble() * 2.0 - 1.0;
            v = random.nextDouble() * 2.0 - 1.0;
            s = u * u + v * v;
        } while (s >= 1.0 || s == 0.0);
        double t = 2.0 * Math.sqrt(1.0 - s);
        return new Vec3(u * t, v * t, 1.0 - 2.0 * s);
    }
}
