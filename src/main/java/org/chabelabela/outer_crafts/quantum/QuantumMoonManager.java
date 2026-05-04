package org.chabelabela.outer_crafts.quantum;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.physics.CelestialBody;
import org.chabelabela.outer_crafts.registry.CelestialBodyRegistry;
import org.chabelabela.outer_crafts.world.dimension.OuterCraftsDimensions;

import java.util.List;
import java.util.Random;

/**
 * Quantum Moon (3.1): a celestial body that quantum-teleports to a different orbit
 * the moment nobody is looking at it.
 *
 * <p>Each tick (rate-limited to {@value #CHECK_INTERVAL_TICKS}-tick intervals) we:
 * <ol>
 *   <li>Resolve the moon's current world position.</li>
 *   <li>Ask {@link QuantumObservation#isObserved} whether any player can see it.</li>
 *   <li>If <i>not observed</i>, pick a new orbit slot and update the registry's
 *       in-memory entry for the moon (a fresh {@link CelestialBody} record with a
 *       new {@code basePosition}).</li>
 * </ol>
 *
 * <p>The moon record itself is mutated through a registry-aware override map so the
 * datapack-loaded record stays the canonical default; observation just picks the
 * "current location" each cycle.
 */
@EventBusSubscriber(modid = OuterCrafts.MODID)
public final class QuantumMoonManager {

    private QuantumMoonManager() {}

    public static final String MOON_ID = "quantum_moon";

    /** Tick cadence — checking every 20 ticks (1s) is plenty. */
    private static final int CHECK_INTERVAL_TICKS = 20;

    /** Maximum LOS range for "is observed". */
    private static final double OBSERVATION_RANGE = 600.0;

    /**
     * Candidate orbit anchors — the moon teleports between these. The strings
     * here must match the IDs of bodies in {@link CelestialBodyRegistry}; we use
     * each body's resolved position + an offset.
     */
    private static final List<OrbitSlot> ORBIT_SLOTS = List.of(
            new OrbitSlot("sun",            new Vec3(  0.0,   0.0,  120.0)),
            new OrbitSlot("ember_twin",     new Vec3( 50.0,  10.0,    0.0)),
            new OrbitSlot("brittle_hollow", new Vec3(  0.0,  20.0,   70.0)),
            new OrbitSlot("dark_bramble",   new Vec3(-40.0,  -5.0,  -40.0)),
            new OrbitSlot("giants_deep",    new Vec3(  0.0,  60.0,  -90.0)),
            new OrbitSlot("timber_hearth",  new Vec3( 80.0,  20.0,   30.0))
    );

    private static final Random RNG = new Random();
    private static int currentSlot = 0;

    public record OrbitSlot(String parentId, Vec3 offset) {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) return;

        ServerLevel solar = server.getLevel(OuterCraftsDimensions.SOLAR_SYSTEM_LEVEL);
        if (solar == null) return;

        CelestialBody moon = CelestialBodyRegistry.get(MOON_ID);
        if (moon == null) return;

        Vec3 moonPos = CelestialBodyRegistry.resolveAllPositions(solar.getGameTime()).get(MOON_ID);
        if (moonPos == null) return;

        boolean observed = QuantumObservation.isObserved(solar, moonPos,
                moon.radius() + 4.0, OBSERVATION_RANGE);

        if (observed) return;

        // Re-roll. Keep choosing until we land on a different slot than the
        // current one (so movement is always perceptible when the player turns away).
        int newSlot;
        do { newSlot = RNG.nextInt(ORBIT_SLOTS.size()); }
        while (newSlot == currentSlot && ORBIT_SLOTS.size() > 1);
        currentSlot = newSlot;
        OrbitSlot slot = ORBIT_SLOTS.get(currentSlot);

        // Look up parent's current resolved position + offset → new base position
        Vec3 parentPos = CelestialBodyRegistry.resolveAllPositions(solar.getGameTime())
                .get(slot.parentId());
        if (parentPos == null) return;
        Vec3 newPos = parentPos.add(slot.offset());

        // Replace the moon record in the registry with a new one in the new spot.
        CelestialBody updated = new CelestialBody(
                moon.id(), moon.surfaceGravity(), moon.radius(),
                newPos, 0.0, 0L, 0.0, slot.parentId());
        CelestialBodyRegistry.replace(updated);
        CelestialBodyRegistry.invalidateCache();

        OuterCrafts.LOGGER.debug("[QuantumMoon] Re-rolled to slot {} (parent={})",
                currentSlot, slot.parentId());
    }
}
