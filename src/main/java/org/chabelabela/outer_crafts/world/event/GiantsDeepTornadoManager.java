package org.chabelabela.outer_crafts.world.event;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.physics.CelestialBody;
import org.chabelabela.outer_crafts.registry.CelestialBodyRegistry;
import org.chabelabela.outer_crafts.world.dimension.OuterCraftsDimensions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Giant's Deep tornadoes (3.5).
 *
 * <p>A small set of invisible-but-particle-rendered tornado columns drifts across
 * Giant's Deep's atmosphere. When an entity (player, ship, falling island) enters
 * a tornado's footprint, it gets punted upward.
 *
 * <p>Tornadoes spawn at random positions on a ring above Giant's Deep's water
 * line and live for {@value #TORNADO_LIFETIME_TICKS} ticks each. We don't use
 * a real Entity here — these are just managed records — to keep packet/sync
 * cost zero on clients (particles are sent from the server level directly).
 */
@EventBusSubscriber(modid = OuterCrafts.MODID)
public final class GiantsDeepTornadoManager {

    private GiantsDeepTornadoManager() {}

    private static final String BODY_ID = "giants_deep";

    /** Maximum simultaneous tornadoes. */
    private static final int MAX_ACTIVE = 6;
    /** New-tornado check interval. */
    private static final int SPAWN_INTERVAL = 200; // 10s
    /** How long each tornado persists. */
    private static final int TORNADO_LIFETIME_TICKS = 1200; // 1 min
    /** Tornado lateral influence radius. */
    private static final double TORNADO_RADIUS = 8.0;
    /** Vertical speed delta added to entities caught in a tornado, per tick. */
    private static final double LIFT_PER_TICK = 0.6;

    private static final RandomSource RNG = RandomSource.create(0xCA7_F00DL);

    private static final List<Tornado> ACTIVE = new ArrayList<>();
    private static int nextSpawnTick = 0;

    private record Tornado(double cx, double cy, double cz, int expiresAtTick) {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int now = server.getTickCount();

        ServerLevel solar = server.getLevel(OuterCraftsDimensions.SOLAR_SYSTEM_LEVEL);
        if (solar == null) return;

        CelestialBody gd = CelestialBodyRegistry.get(BODY_ID);
        if (gd == null) return;

        Vec3 center = CelestialBodyRegistry.resolveAllPositions(solar.getGameTime()).get(BODY_ID);
        if (center == null) return;

        // Expire dead tornadoes
        ACTIVE.removeIf(t -> now >= t.expiresAtTick());

        // Maybe spawn a new one
        if (now >= nextSpawnTick && ACTIVE.size() < MAX_ACTIVE) {
            spawnTornado(gd, center, now);
            nextSpawnTick = now + SPAWN_INTERVAL;
        }

        // Tick each active tornado: render particles + apply lift to entities
        for (Tornado t : ACTIVE) {
            tickTornado(solar, t);
        }
    }

    private static void spawnTornado(CelestialBody gd, Vec3 center, int now) {
        // Pick a random direction on the unit sphere — tornado origin is just above the surface.
        double theta = RNG.nextDouble() * 2.0 * Math.PI;
        double altitude = gd.radius() + 6.0;
        // Constrain to the equatorial-ish band so they're easier to spot.
        double polar = Math.PI / 2.0 + (RNG.nextDouble() - 0.5) * 0.6;
        double x = center.x + altitude * Math.sin(polar) * Math.cos(theta);
        double y = center.y + altitude * Math.cos(polar);
        double z = center.z + altitude * Math.sin(polar) * Math.sin(theta);

        ACTIVE.add(new Tornado(x, y, z, now + TORNADO_LIFETIME_TICKS));
        OuterCrafts.LOGGER.debug("[Tornado] Spawned at ({}, {}, {})", (int) x, (int) y, (int) z);
    }

    private static void tickTornado(ServerLevel level, Tornado t) {
        // Spawn particles in a vertical helix shape
        for (int i = 0; i < 8; i++) {
            double phase = (level.getGameTime() * 0.2 + i * (Math.PI / 4));
            double rx = Math.cos(phase) * TORNADO_RADIUS * 0.6;
            double rz = Math.sin(phase) * TORNADO_RADIUS * 0.6;
            double ry = i * 1.5;
            level.sendParticles(ParticleTypes.CLOUD,
                    t.cx + rx, t.cy + ry, t.cz + rz,
                    1, 0.1, 0.1, 0.1, 0.0);
        }

        // Lift entities inside the cylinder
        AABB box = new AABB(
                t.cx - TORNADO_RADIUS, t.cy - 4, t.cz - TORNADO_RADIUS,
                t.cx + TORNADO_RADIUS, t.cy + 30, t.cz + TORNADO_RADIUS);
        Iterator<Entity> it = level.getEntities(null, box).iterator();
        while (it.hasNext()) {
            Entity e = it.next();
            // Check planar distance — keep the column shape
            double dx = e.getX() - t.cx;
            double dz = e.getZ() - t.cz;
            if (dx * dx + dz * dz > TORNADO_RADIUS * TORNADO_RADIUS) continue;

            Vec3 v = e.getDeltaMovement();
            e.setDeltaMovement(v.x, v.y + LIFT_PER_TICK, v.z);
            e.hurtMarked = true;
        }
    }
}
