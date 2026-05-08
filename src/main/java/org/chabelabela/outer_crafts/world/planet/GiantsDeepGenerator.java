package org.chabelabela.outer_crafts.world.planet;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Giant's Deep: ocean planet with floating islands scattered around its equator.
 *
 * <p><b>Why no real water:</b> on a finite sphere, every {@code Blocks.WATER}
 * placed at the surface band has at least one neighbour outside the sphere
 * (the chunk gen skips placing anything past the planet's effective radius).
 * MC water flows into any non-solid neighbour → drains off the sphere into
 * the void below → planet empties out within seconds. There is no clean way
 * to seal a sphere against vanilla water flow without a custom non-flowing
 * fluid block, which is deferred work.
 *
 * <p>Solution: the entire ocean is rendered in {@link Blocks#BLUE_ICE}. From
 * orbit it reads as a deep-blue oceanic gas-giant; up close it's a frozen,
 * slippery surface you can walk on. The iconic OW look is preserved without
 * the leak.
 *
 * <p>Islands are placed using {@link SphericalNoise#islandStrength} —
 * deterministic Poisson-disc-style sphere sampling with smooth falloffs
 * around each island centre, instead of XZ-grid stripes.
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
            // Inner seabed: stone, gravel, clay, occasional prismarine
            float roll = random.nextFloat();
            if (roll < 0.15f) return Blocks.GRAVEL.defaultBlockState();
            if (roll < 0.25f) return Blocks.CLAY.defaultBlockState();
            if (roll < 0.30f) return Blocks.PRISMARINE.defaultBlockState();
            return Blocks.STONE.defaultBlockState();
        } else if (normalizedDistance < 0.99) {
            // Bulk ocean: solid blue ice. Visually a deep blue ocean from
            // outside; physically inert so it cannot drain into the void at
            // the sphere's edge.
            return Blocks.BLUE_ICE.defaultBlockState();
        } else {
            // Surface band: islands break through the ice
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
            // Not an island here — surface continues as the blue-ice ocean.
            return Blocks.BLUE_ICE.defaultBlockState();
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
