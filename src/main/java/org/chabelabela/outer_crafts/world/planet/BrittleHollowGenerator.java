package org.chabelabela.outer_crafts.world.planet;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Brittle Hollow: hollow planet with a black hole at the centre, supported by 8 pillars
 * that radiate from the equator. The southern pole hosts Hollow's Lantern.
 *
 * <p>Pillars are now placed using {@link SphericalNoise#isInLongitudinalPillar} —
 * proper longitudinal radial pillars on the sphere instead of XZ-grid stripes.
 */
public final class BrittleHollowGenerator implements PlanetGenerator {

    /** Number of pillars equally spaced around the equator. */
    private static final int PILLAR_COUNT = 8;
    /** Half-angular width of each pillar in radians (~9°). */
    private static final double PILLAR_HALF_WIDTH = 0.16;
    /** Restrict pillars to the equatorial band ~ 35° to 145° polar angle. */
    private static final double PILLAR_THETA_MIN = Math.toRadians(35);
    private static final double PILLAR_THETA_MAX = Math.toRadians(145);

    @Override
    public BlockState getBlockAt(double normalizedDistance, double surfaceNoise,
                                 double localX, double localY, double localZ,
                                 RandomSource random) {
        if (normalizedDistance < 0.15) {
            // Black hole zone — empty
            return null;
        } else if (normalizedDistance < 0.25) {
            return random.nextFloat() < 0.1f
                    ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                    : Blocks.OBSIDIAN.defaultBlockState();
        } else if (normalizedDistance < 0.6) {
            // Hollow interior with radial pillars
            double theta = SphericalNoise.theta(localX, localY, localZ);
            double phi   = SphericalNoise.phi(localX, localZ);
            boolean inPillar = SphericalNoise.isInLongitudinalPillar(
                    theta, phi, PILLAR_COUNT, PILLAR_HALF_WIDTH,
                    PILLAR_THETA_MIN, PILLAR_THETA_MAX);
            return inPillar ? Blocks.DEEPSLATE.defaultBlockState() : null;
        } else if (normalizedDistance < 0.85) {
            float roll = random.nextFloat();
            if (roll < 0.05f) return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
            if (roll < 0.10f) return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            return Blocks.DEEPSLATE.defaultBlockState();
        } else {
            // Outer crust — Brittle Hollow's iconic crumbling brick exterior
            float roll = random.nextFloat();
            if (roll < 0.20f) return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            if (roll < 0.35f) return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            if (roll < 0.50f) return Blocks.STONE_BRICKS.defaultBlockState();
            return Blocks.STONE.defaultBlockState();
        }
    }

    @Override
    public double getEffectiveRadius(double baseRadius, double surfaceNoise) {
        return baseRadius * (1.0 + surfaceNoise * 0.07);
    }
}
