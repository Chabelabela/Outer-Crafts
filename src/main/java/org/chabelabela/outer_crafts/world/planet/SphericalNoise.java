package org.chabelabela.outer_crafts.world.planet;

/**
 * Helpers for sampling features on a planetary sphere using spherical coordinates.
 *
 * <p>Replaces the previous practice of bucketing on world-aligned XZ ({@code x%20, z%20}),
 * which produces visible parallel stripes on a sphere — pillars all line up along the
 * planet's "equatorial" world axis instead of radiating from its centre.
 *
 * <p>Coordinate convention used here:
 * <ul>
 *   <li>{@code theta} (polar angle): {@code [0, π]} — 0 at the north pole (+Y), π at the south pole</li>
 *   <li>{@code phi} (azimuth): {@code (-π, π]} — atan2-style, 0 along +X</li>
 * </ul>
 */
public final class SphericalNoise {

    private SphericalNoise() {}

    /**
     * Returns the polar angle in radians for a point on the sphere centred at origin.
     * 0 → north pole (+Y), π → south pole.
     */
    public static double theta(double localX, double localY, double localZ) {
        double r = Math.sqrt(localX * localX + localY * localY + localZ * localZ);
        if (r < 1.0e-6) return 0.0;
        return Math.acos(Math.clamp(localY / r, -1.0, 1.0));
    }

    /**
     * Returns the azimuthal angle in radians, range {@code (-π, π]}.
     */
    public static double phi(double localX, double localZ) {
        return Math.atan2(localZ, localX);
    }

    /**
     * Tests whether the given spherical position falls inside one of {@code longitudeCount}
     * radial pillars equally spaced around the sphere.
     *
     * @param theta polar angle from {@link #theta(double, double, double)}
     * @param phi azimuth from {@link #phi(double, double)}
     * @param longitudeCount number of pillars around the equator (e.g. 8 for Brittle Hollow)
     * @param halfWidthRadians half-angular-width of each pillar in radians (e.g. 0.08)
     * @param thetaMin only consider points with {@code theta >= thetaMin} (skip north cap)
     * @param thetaMax only consider points with {@code theta <= thetaMax} (skip south cap)
     */
    public static boolean isInLongitudinalPillar(double theta, double phi,
                                                 int longitudeCount, double halfWidthRadians,
                                                 double thetaMin, double thetaMax) {
        if (theta < thetaMin || theta > thetaMax) return false;
        double slice = (2.0 * Math.PI) / longitudeCount;
        double normalisedPhi = phi + Math.PI;                    // [0, 2π)
        double offsetInSlice = normalisedPhi % slice;
        double centeredOffset = Math.min(offsetInSlice, slice - offsetInSlice);
        return centeredOffset < halfWidthRadians;
    }

    /**
     * Smooth strength field for sphere-scattered "island" features.
     *
     * <p>Implementation sketch: divides the sphere into a coarse {@code latitudeBands ×
     * longitudeBands} grid; each cell deterministically hashes to a single island
     * centre inside it (jittered). Returns the gaussian falloff from the nearest
     * island centre. Strength {@code > peakThreshold} → land; below → ocean.
     *
     * @return strength in {@code [0, 1]}, {@code 1} at the very peak of an island
     */
    public static double islandStrength(double theta, double phi,
                                         long seed,
                                         int latitudeBands, int longitudeBands,
                                         double islandRadiusRadians) {
        double latStep = Math.PI / latitudeBands;
        double lonStep = (2.0 * Math.PI) / longitudeBands;
        double normalisedPhi = phi + Math.PI;                    // [0, 2π)

        int latBin = (int) Math.floor(theta / latStep);
        int lonBin = (int) Math.floor(normalisedPhi / lonStep);

        double bestSqDist = Double.MAX_VALUE;

        // Sample the 3×3 neighbourhood of bins so islands near borders are picked up.
        for (int dLat = -1; dLat <= 1; dLat++) {
            int lb = latBin + dLat;
            if (lb < 0 || lb >= latitudeBands) continue;
            for (int dLon = -1; dLon <= 1; dLon++) {
                int lonbWrapped = ((lonBin + dLon) % longitudeBands + longitudeBands) % longitudeBands;

                long h = hash(seed, lb, lonbWrapped);
                // Reject some bins entirely so we get sparse islands rather than a grid.
                if ((h & 0x3) != 0) continue;

                double centerTheta = (lb + 0.5 + jitter01(h, 0) - 0.5) * latStep;
                double centerPhi   = (lonbWrapped + 0.5 + jitter01(h, 1) - 0.5) * lonStep - Math.PI;

                double dt = theta - centerTheta;
                double dp = wrapAngle(phi - centerPhi);
                // Approximate spherical distance: sqrt(dt² + sin(theta)²·dp²)
                double sinT = Math.sin(theta);
                double sqDist = dt * dt + (sinT * dp) * (sinT * dp);
                if (sqDist < bestSqDist) bestSqDist = sqDist;
            }
        }

        if (bestSqDist == Double.MAX_VALUE) return 0.0;
        double dist = Math.sqrt(bestSqDist);
        if (dist >= islandRadiusRadians) return 0.0;
        double t = 1.0 - (dist / islandRadiusRadians);
        // Smoothstep-ish falloff
        return t * t * (3.0 - 2.0 * t);
    }

    private static double wrapAngle(double a) {
        while (a >  Math.PI) a -= 2.0 * Math.PI;
        while (a <= -Math.PI) a += 2.0 * Math.PI;
        return a;
    }

    private static long hash(long seed, int a, int b) {
        long h = seed;
        h ^= a * 0x9E3779B97F4A7C15L;
        h ^= b * 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 27;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        return h;
    }

    /** Deterministic [0, 1) jitter from a hash, salted with the channel index. */
    private static double jitter01(long h, int channel) {
        long mixed = (h ^ ((long) channel * 0xBF58476D1CE4E5B9L));
        mixed ^= mixed >>> 33;
        return ((mixed >>> 11) & ((1L << 24) - 1)) / (double) (1L << 24);
    }
}
