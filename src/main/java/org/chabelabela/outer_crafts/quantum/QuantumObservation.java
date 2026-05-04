package org.chabelabela.outer_crafts.quantum;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shared utilities for "observation-driven" quantum behaviour à la Outer Wilds.
 *
 * <p>An object is <b>observed</b> when at least one player can plausibly see it:
 * within line-of-sight, inside their view frustum, and within a configurable range.
 *
 * <p>This is the foundation for the Quantum Moon (3.1) and Quantum Shards (3.2):
 * they re-roll their position only when nobody is looking.
 */
public final class QuantumObservation {

    private QuantumObservation() {}

    /** Default angular cone (cosine) within which something counts as "in view". */
    public static final double DEFAULT_VIEW_CONE_COS = Math.cos(Math.toRadians(60));

    /**
     * Returns {@code true} if any online player on {@code level} has a clear,
     * frustum-aligned line-of-sight to {@code targetCenter} within {@code maxRange}.
     *
     * <p>Note: also counts as observed if a player is <i>standing on</i> the target
     * (closer than {@code requiredRadius} blocks). This handles the Quantum Moon
     * case: while you're walking on its surface it can't teleport away.
     *
     * @param level the level whose player list to check
     * @param targetCenter the world-space centre of the target
     * @param requiredRadius distance below which "you're touching it" = observed
     * @param maxRange line-of-sight check range
     * @param viewConeCos {@link #DEFAULT_VIEW_CONE_COS} or stricter
     */
    public static boolean isObserved(ServerLevel level,
                                     Vec3 targetCenter,
                                     double requiredRadius,
                                     double maxRange,
                                     double viewConeCos) {
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            Vec3 eye = player.getEyePosition();

            double distSq = eye.distanceToSqr(targetCenter);

            // Touching the surface always counts.
            if (distSq <= requiredRadius * requiredRadius) return true;

            if (distSq > maxRange * maxRange) continue;

            Vec3 toTarget = targetCenter.subtract(eye).normalize();
            Vec3 lookDir = player.getLookAngle().normalize();
            if (lookDir.dot(toTarget) < viewConeCos) continue;

            // Final LOS test: clip rays through the world, ignore the target's own blocks.
            HitResult hit = level.clip(new ClipContext(
                    eye, targetCenter,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            // If clip stops before reaching the target, something solid is in the way.
            // Treat any hit closer than 95% of the way as obstructed.
            double clipDist = hit.getLocation().distanceTo(eye);
            double directDist = targetCenter.distanceTo(eye);
            if (clipDist >= directDist * 0.95) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convenience overload using {@link #DEFAULT_VIEW_CONE_COS}.
     */
    public static boolean isObserved(ServerLevel level, Vec3 targetCenter,
                                     double requiredRadius, double maxRange) {
        return isObserved(level, targetCenter, requiredRadius, maxRange, DEFAULT_VIEW_CONE_COS);
    }

    /** Same as {@link #isObserved}, applied to an entity's centre. */
    public static boolean isObserved(Entity entity, double maxRange) {
        if (!(entity.level() instanceof ServerLevel level)) return false;
        return isObserved(level, entity.position().add(0, entity.getBbHeight() * 0.5, 0),
                entity.getBbWidth() * 1.5, maxRange);
    }
}
