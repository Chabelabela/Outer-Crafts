package org.chabelabela.outer_crafts.world.planet;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Giant's Deep: ocean planet with floating islands scattered around its equator.
 *
 * <p>The ocean is rendered as a deep {@code BLUE_ICE} bulk (visually convincing
 * as deep water + non-flowing, so it can't escape the spherical surface into the
 * void below — the original {@code Blocks.WATER} bulk leaked sideways and drained
 * the planet). A thin 2-block skin of real {@code WATER} sits on top of the ice
 * shell so the player can wade and the ship can splash on landing; the ice
 * underneath holds the skin in place at the planet's curved boundary.
 *
 * <p>Islands are placed using {@link SphericalNoise#islandStrength} —
 * deterministic Poisson-disc-style sphere sampling with smooth falloffs around
 * each island centre, instead of XZ-grid stripes.
 */
public final class GiantsDeepGenerator implements PlanetGenerator {

    private static final long ISLAND_SEED = 0x6A0987FE_DEADC0DEL;

    /** Coarse latitude/longitude binning for island centre placement. */
    private static final int ISLAND_LAT_BANDS = 6;
    private static final int ISLAND_LON_BANDS = 12;

    /** Angular radius of each island in radians (~7°). */
    private static final double ISLAND_RADIUS_RAD = 0.12;

    /** Strength threshold above which the surface emerges as land. */
    private static final double LAND_THRESHOLD = 0.35;

    /**
     * Below this normalized distance, the ocean is solid blue ice (non-flowing).
     * Above this and below {@link #WATER_SKIN_INNER}, it's the swimmable water
     * skin contained by ice walls below and air above.
     */
    private static final double WATER_SKIN_INNER = 0.96;
    /** At/above this distance, blocks are surface-level (islands or water skin). */
    private static final double SURFACE_INNER = 0.99;

    @Override
    public BlockState getBlockAt(double normalizedDistance, double surfaceNoise,
                                 double localX, double localY, double localZ,
                                 RandomSource random) {
        if (normalizedDistance < 0.15) {
            // Core: prismarine "Nomai pearl"
            return random.nextFloat() < 0.3f
                    ? Blocks.DARK_PRISMARINE.defaultBlockState()
                    : Blocks.PRISMARINE.defaultBlockState();
        } else if (normalizedDistance < 0.4) {
            // Inner seabed
            float roll = random.nextFloat();
            if (roll < 0.15f) return Blocks.GRAVEL.defaultBlockState();
            if (roll < 0.25f) return Blocks.CLAY.defaultBlockState();
            if (roll < 0.30f) return Blocks.PRISMARINE.defaultBlockState();
            return Blocks.STONE.defaultBlockState();
        } else if (normalizedDistance < WATER_SKIN_INNER) {
            // Bulk ocean — solid blue ice. Visually a deep blue ocean from outside,
            // physically inert so it cannot drain into the void at the sphere's edge.
            return Blocks.BLUE_ICE.defaultBlockState();
        } else if (normalizedDistance < SURFACE_INNER) {
            // Thin swimmable water skin (~3 blocks thick on the sphere). Contained
            // below by the ice shell, capped above by air → cannot flow into space.
            return Blocks.WATER.defaultBlockState();
        } else {
            // Surface band: islands break through the water skin
            double theta = SphericalNoise.theta(localX, localY, localZ);
            double phi   = SphericalNoise.phi(localX, localZ);
            double strength = SphericalNoise.islandStrength(
                    theta, phi, ISLAND_SEED,
                    ISLAND_LAT_BANDS, ISLAND_LON_BANDS, ISLAND_RADIUS_RAD);

            // Combine spherical-island strength with per-XZ surface noise so
            // islands aren't perfectly flat plateaus.
            double landFactor = strength + (surfaceNoise - 0.5) * 0.15;

            if (landFactor > LAND_THRESHOLD) {
                if (normalizedDistance > 0.998 && landFactor > 0.55) {
                    return Blocks.GRASS_BLOCK.defaultBlockState();
                } else if (normalizedDistance > 0.99) {
                    return Blocks.DIRT.defaultBlockState();
                } else {
                    return Blocks.STONE.defaultBlockState();
                }
            }
            // Not an island here — keep water at the very surface so the
            // "ocean" reads as continuous from above.
            return Blocks.WATER.defaultBlockState();
        }
    }

    @Override
    public double getEffectiveRadius(double baseRadius, double surfaceNoise) {
        return baseRadius * (1.0 + surfaceNoise * 0.03);
    }

    @Override
    public boolean hasAtmosphere() { return true; }

    @Override
    public double atmosphereThickness() { return 30.0; }
}
