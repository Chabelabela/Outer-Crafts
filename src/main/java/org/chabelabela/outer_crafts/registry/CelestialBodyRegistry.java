package org.chabelabela.outer_crafts.registry;

import net.minecraft.world.phys.Vec3;
import org.chabelabela.outer_crafts.physics.CelestialBody;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CelestialBodyRegistry {

    private CelestialBodyRegistry() {}

    private static final Map<String, CelestialBody> BODIES = new LinkedHashMap<>();

    public static void register(CelestialBody body) {
        if (BODIES.containsKey(body.id())) {
            throw new IllegalArgumentException("Duplicate celestial body ID: " + body.id());
        }
        if (body.parentId() != null && !BODIES.containsKey(body.parentId())) {
            throw new IllegalArgumentException(
                    "Parent body '%s' not found for '%s'. Register parents first."
                            .formatted(body.parentId(), body.id())
            );
        }
        BODIES.put(body.id(), body);
    }

    public static Map<String, CelestialBody> allBodies() {
        return Collections.unmodifiableMap(BODIES);
    }

    /**
     * Replaces an already-registered body's record with a new one of the same id.
     * Skips parent-validation since we're updating an entry in place. Used by
     * dynamic systems like the Quantum Moon manager.
     */
    public static void replace(CelestialBody body) {
        if (!BODIES.containsKey(body.id())) {
            throw new IllegalArgumentException(
                    "Cannot replace unknown celestial body: " + body.id());
        }
        BODIES.put(body.id(), body);
        invalidateCache();
    }

    public static CelestialBody get(String id) {
        return BODIES.get(id);
    }

    // ── Per-tick position cache ─────────────────────────────────────────
    // resolveAllPositions is called from many hot paths every tick (per-entity
    // gravity mixin, jetpack handler, ship physics, scout tick, sky renderer,
    // signalscope scan, chunk generator). All call sites within a single
    // tick produce the same map, so we memoize one tick's worth and evict
    // when the tick changes. Read access is plain field reads after the
    // first call per tick — no map allocation, no orbital math repetition.
    private static volatile long cachedTick = Long.MIN_VALUE;
    private static volatile Map<String, Vec3> cachedPositions = Collections.emptyMap();

    public static Map<String, Vec3> resolveAllPositions(long tick) {
        // Fast path: same tick as last call → return memoized snapshot.
        if (tick == cachedTick) {
            return cachedPositions;
        }
        Map<String, Vec3> positions = computePositions(tick);
        cachedPositions = positions;
        cachedTick = tick;
        return positions;
    }

    private static Map<String, Vec3> computePositions(long tick) {
        Map<String, Vec3> positions = new LinkedHashMap<>(BODIES.size());

        for (CelestialBody body : BODIES.values()) {
            Vec3 parentCenter;
            if (body.parentId() == null) {
                parentCenter = body.basePosition();
            } else {
                parentCenter = positions.get(body.parentId());
                if (parentCenter == null) {
                    parentCenter = BODIES.get(body.parentId()).basePosition();
                }
            }
            positions.put(body.id(), body.positionAtTick(tick, parentCenter));
        }

        return Collections.unmodifiableMap(positions);
    }

    /** Force-invalidate the cache — call after registry mutations or world reload. */
    public static void invalidateCache() {
        cachedTick = Long.MIN_VALUE;
        cachedPositions = Collections.emptyMap();
    }

    public static Vec3 resolvePosition(String id, long tick) {
        CelestialBody body = BODIES.get(id);
        if (body == null) {
            throw new IllegalArgumentException("Unknown celestial body: " + id);
        }

        Vec3 parentCenter;
        if (body.parentId() == null) {
            parentCenter = body.basePosition();
        } else {
            parentCenter = resolvePosition(body.parentId(), tick);
        }
        return body.positionAtTick(tick, parentCenter);
    }

    /**
     * Hard-coded defaults — the JSON datapack files at {@code data/outer_crafts/celestial_body/*.json}
     * override these. Bodies are spread roughly 2000–3500 blocks from the sun so a slow
     * ship trip from one to the next takes ~30s; planet radii are large enough (55–180)
     * that real exploration is possible (e.g. TH circumference ≈ 690 blocks).
     *
     * <p>Surface gravity is the design parameter: 0.08 ≈ vanilla MC pull, 0.04 = floaty,
     * 0.12 = heavy.
     */
    public static void registerDefaults() {
        final double Y = 100.0;

        // ── Sun: anchor at origin, huge ─────────────────────────────────────────
        register(new CelestialBody(
                "sun",
                /* surfaceGravity */ 0.18, /* radius */ 180.0,
                new Vec3(0, Y, 0),
                0, 0L, 0.0, null
        ));

        // ── Hourglass Twins: binary at ~700 from sun ────────────────────────────
        register(new CelestialBody(
                "twin_center",
                0.0, 0.0,
                new Vec3(700, Y, 0),
                0, 0L, 0.0, "sun"
        ));
        register(new CelestialBody(
                "ash_twin",
                /* surfaceGravity */ 0.05, /* radius */ 65.0,
                new Vec3(820, Y, 0),
                0, 0L, 0.0, "twin_center"
        ));
        register(new CelestialBody(
                "ember_twin",
                /* surfaceGravity */ 0.05, /* radius */ 65.0,
                new Vec3(580, Y, 0),
                0, 0L, 0.0, "twin_center"
        ));

        // ── Timber Hearth: starting world, vanilla-feel gravity ─────────────────
        register(new CelestialBody(
                "timber_hearth",
                /* surfaceGravity */ 0.08, /* radius */ 110.0,
                new Vec3(800.0, Y, 1385.640646),
                0, 0L, 0.0, "sun"
        ));

        // ── Brittle Hollow: hollow planet with a black hole (and its lantern moon)
        register(new CelestialBody(
                "brittle_hollow",
                /* surfaceGravity */ 0.075, /* radius */ 120.0,
                new Vec3(-1100.0, Y, 1905.255894),
                0, 0L, 0.0, "sun"
        ));
        register(new CelestialBody(
                "hollows_lantern",
                /* surfaceGravity */ 0.04, /* radius */ 38.0,
                new Vec3(-900.0, Y, 1905.255894),
                0, 0L, 0.0, "brittle_hollow"
        ));

        // ── Giant's Deep: ocean gas-giant, heavy gravity ────────────────────────
        register(new CelestialBody(
                "giants_deep",
                /* surfaceGravity */ 0.12, /* radius */ 160.0,
                new Vec3(-2200.0, Y, 0.0),
                0, 0L, 0.0, "sun"
        ));

        // ── Dark Bramble: thorny seed-world ─────────────────────────────────────
        register(new CelestialBody(
                "dark_bramble",
                /* surfaceGravity */ 0.06, /* radius */ 130.0,
                new Vec3(-1838.477631, Y, -1838.477631),
                0, 0L, 0.0, "sun"
        ));

        // ── Quantum Moon: low-G, teleports when unobserved ──────────────────────
        register(new CelestialBody(
                "quantum_moon",
                /* surfaceGravity */ 0.04, /* radius */ 55.0,
                new Vec3(0.0, Y, 1100.0),
                0, 0L, 0.0, "sun"
        ));
    }

    public static void clear() {
        BODIES.clear();
        invalidateCache();
    }
}
