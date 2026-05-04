package org.chabelabela.outer_crafts.world.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.physics.CelestialBody;
import org.chabelabela.outer_crafts.registry.CelestialBodyRegistry;
import org.chabelabela.outer_crafts.timeloop.TimeLoopConstants;
import org.chabelabela.outer_crafts.timeloop.TimeLoopManager;
import org.chabelabela.outer_crafts.world.dimension.OuterCraftsDimensions;

import java.util.Map;

/**
 * Hourglass Twins sand transfer (3.6).
 *
 * <p>Across each 22-minute loop, sand streams from one twin to the other.
 * Specifically: the sand column rises out of Ember Twin's "north" pole and falls
 * onto Ash Twin's "south" pole, gradually burying Sunless City and uncovering Ash
 * Twin's hidden towers.
 *
 * <p>The implementation here is a coarse but visible approximation: each
 * {@value #TICK_INTERVAL}-tick window we look at how far through the loop we are
 * and place / remove sand columns proportionally above each twin's poles. The
 * transfer reverses in subsequent loops if you stick around for them.
 */
@EventBusSubscriber(modid = OuterCrafts.MODID)
public final class SandTransferManager {

    private SandTransferManager() {}

    /** How often we tick (cheap — only updates a handful of blocks per slice). */
    private static final int TICK_INTERVAL = 80;

    private static final String EMBER_ID = "ember_twin";
    private static final String ASH_ID = "ash_twin";

    /** Maximum height of the sand column above either twin. */
    private static final int MAX_COLUMN_HEIGHT = 30;
    /** Lateral half-width of the column footprint (square). */
    private static final int COLUMN_HALF_WIDTH = 3;

    private static final RandomSource RNG = RandomSource.create(0xA5C0DE_FEEDFACEL);

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % TICK_INTERVAL != 0) return;

        ServerLevel solar = server.getLevel(OuterCraftsDimensions.SOLAR_SYSTEM_LEVEL);
        if (solar == null) return;

        CelestialBody ember = CelestialBodyRegistry.get(EMBER_ID);
        CelestialBody ash   = CelestialBodyRegistry.get(ASH_ID);
        if (ember == null || ash == null) return;

        Map<String, Vec3> positions = CelestialBodyRegistry.resolveAllPositions(solar.getGameTime());
        Vec3 emberPos = positions.get(EMBER_ID);
        Vec3 ashPos   = positions.get(ASH_ID);
        if (emberPos == null || ashPos == null) return;

        double progress = TimeLoopManager.getCurrentTick()
                / (double) TimeLoopConstants.LOOP_DURATION_TICKS;
        progress = Math.clamp(progress, 0.0, 1.0);

        // Ember loses sand → its column shrinks over the loop.
        // Ash gains sand → its column grows.
        int emberColumnHeight = (int) (MAX_COLUMN_HEIGHT * (1.0 - progress));
        int ashColumnHeight   = (int) (MAX_COLUMN_HEIGHT * progress);

        // We don't rebuild the whole column every tick — we shift one layer
        // per tick window in the direction of the gradient.
        BlockPos emberTop = topOfBody(ember, emberPos, emberColumnHeight);
        BlockPos ashTop   = topOfBody(ash,   ashPos,   ashColumnHeight);

        // Ember side: place sand if our predicted column is higher than current,
        // remove from the top if it's lower.
        adjustColumn(solar, emberTop, +1);   // try to remove (going down)
        adjustColumn(solar, ashTop, -1);     // try to add (going up)
    }

    /** Returns the world block position at the top of the column above {@code bodyCenter}. */
    private static BlockPos topOfBody(CelestialBody body, Vec3 bodyCenter, int columnHeight) {
        double topY = bodyCenter.y + body.radius() + columnHeight;
        return BlockPos.containing(bodyCenter.x, topY, bodyCenter.z);
    }

    /**
     * Slides one layer of sand up or down within {@code COLUMN_HALF_WIDTH × 2 + 1}
     * footprint centred on {@code top}.
     *
     * @param direction +1 = remove a layer (sand draining), -1 = add a layer (filling)
     */
    private static void adjustColumn(ServerLevel level, BlockPos top, int direction) {
        BlockState sand = Blocks.SAND.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -COLUMN_HALF_WIDTH; dx <= COLUMN_HALF_WIDTH; dx++) {
            for (int dz = -COLUMN_HALF_WIDTH; dz <= COLUMN_HALF_WIDTH; dz++) {
                cursor.set(top.getX() + dx, top.getY(), top.getZ() + dz);
                if (!level.isLoaded(cursor)) continue;

                BlockState here = level.getBlockState(cursor);
                if (direction > 0) {
                    // Remove the topmost sand block if present
                    if (here.is(Blocks.SAND) || here.is(Blocks.RED_SAND)) {
                        level.removeBlock(cursor, false);
                    }
                } else {
                    // Add a sand block if empty
                    if (here.isAir() || here.canBeReplaced()) {
                        level.setBlockAndUpdate(cursor, sand);
                    }
                }
            }
        }
    }

    /** Convenience: forces a transfer step regardless of the rate limit. Used by tests / debug commands. */
    public static void debugStep(MinecraftServer server) {
        ServerLevel solar = server.getLevel(OuterCraftsDimensions.SOLAR_SYSTEM_LEVEL);
        if (solar == null) return;
        CelestialBody ember = CelestialBodyRegistry.get(EMBER_ID);
        CelestialBody ash   = CelestialBodyRegistry.get(ASH_ID);
        if (ember == null || ash == null) return;
        Map<String, Vec3> p = CelestialBodyRegistry.resolveAllPositions(solar.getGameTime());
        BlockPos eTop = topOfBody(ember, p.get(EMBER_ID), MAX_COLUMN_HEIGHT / 2);
        BlockPos aTop = topOfBody(ash,   p.get(ASH_ID),   MAX_COLUMN_HEIGHT / 2);
        adjustColumn(solar, eTop, RNG.nextBoolean() ? +1 : -1);
        adjustColumn(solar, aTop, RNG.nextBoolean() ? +1 : -1);
    }
}
