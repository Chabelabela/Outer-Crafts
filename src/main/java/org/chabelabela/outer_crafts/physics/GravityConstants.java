package org.chabelabela.outer_crafts.physics;

/**
 * Tuning constants for the spherical-gravity system.
 *
 * <p>The physics model: each body has a {@code surfaceGravity} (acceleration at
 * its surface in blocks-per-tick²). Outside the surface, accel falls off as
 * {@code 1/r²} like real gravity. Inside the body's "influence radius"
 * ({@code radius * INFLUENCE_MULTIPLIER}), the strongest body wins and pulls
 * the entity toward its centre — this is what makes the player walk around the
 * outside of a sphere instead of falling through to the centre.
 */
public final class GravityConstants {

    private GravityConstants() {}

    /**
     * Newtonian {@code G} retained for legacy callers, but no longer drives the
     * gravity model directly. Surface accel is the design parameter; mass is
     * {@code surfaceGravity · radius² / G}.
     */
    public static final double G = 0.08;

    /**
     * Bodies tug at entities within {@code radius * INFLUENCE_MULTIPLIER} of
     * their centre. Beyond that, deep-space (zero-g). 5× was chosen so the
     * dominant-body switch happens cleanly when leaving one planet's well
     * before entering another.
     */
    public static final double INFLUENCE_MULTIPLIER = 5.0;

    /**
     * Anything weaker than this is rounded to deep-space zero. Slightly larger
     * than the previous 1e-6 so we don't bother computing micro-pulls from
     * far-away tiny moons that contribute nothing perceivable.
     */
    public static final double GRAVITY_EPSILON = 1.0E-4;

    /**
     * Hard ceiling on per-tick acceleration. Prevents extreme values when an
     * entity glitches near a body's centre. Lowered from 0.5 → 0.25 because
     * 0.5 blocks/tick² is roughly 5× vanilla and turned into a player-launching
     * cannon when the influence well misbehaved.
     */
    public static final double MAX_GRAVITY = 0.25;

    public static final double DEEP_SPACE_GRAVITY = 0.0;
    public static final double VANILLA_GRAVITY = 0.08;
}
