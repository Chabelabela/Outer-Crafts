package org.chabelabela.outer_crafts.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.chabelabela.outer_crafts.physics.GravityEntityAccess;
import org.chabelabela.outer_crafts.physics.SphericalGravityHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-side camera mixin. Rotates the camera so that "up" on screen aligns
 * with the local gravity up vector at the player's position.
 * <p>
 * This is what makes standing on a sphere feel natural — the horizon tilts
 * as you move around the planet surface.
 */
@Mixin(Camera.class)
public abstract class CameraOrientationMixin {

    @Shadow
    private Quaternionf rotation;

    @Shadow
    private Entity entity;

    /**
     * Smoothed local-up on the client side, for buttery camera transitions.
     */
    @Unique
    private Vec3 outerCrafts$clientSmoothedUp = new Vec3(0, 1, 0);

    @Unique
    private static final double CLIENT_SMOOTH_SPEED = 0.12;

    /**
     * After the camera's standard update, apply an additional rotation to align
     * the camera's up axis with the local gravity up vector.
     */
    @Inject(method = "update", at = @At("TAIL"))
    private void outerCrafts$applyCameraGravityRotation(DeltaTracker deltaTracker, CallbackInfo ci) {
        if (this.entity == null) return;

        // Retrieve local up via the duck interface injected by EntityMoveMixin
        if (!(this.entity instanceof GravityEntityAccess access)) return;
        Vec3 localUp = access.outerCrafts$getLocalUp();

        Vec3 vanillaUp = new Vec3(0, 1, 0);

        // Smoothly interpolate toward the target up
        this.outerCrafts$clientSmoothedUp = SphericalGravityHelper.slerpUp(
                this.outerCrafts$clientSmoothedUp,
                localUp,
                CLIENT_SMOOTH_SPEED
        );

        Vec3 smoothed = this.outerCrafts$clientSmoothedUp;

        // If local up is essentially vanilla up, skip rotation (common case optimization)
        double dot = vanillaUp.dot(smoothed);
        if (dot > 0.9999) return;

        // Compute the rotation quaternion from vanilla up → local up
        Quaternionf gravityRotation = outerCrafts$computeUpRotation(vanillaUp, smoothed);

        // Prepend the gravity rotation to the existing camera rotation
        this.rotation.premul(gravityRotation);
    }

    /**
     * Computes a quaternion that rotates {@code from} to {@code to}.
     */
    @Unique
    private static Quaternionf outerCrafts$computeUpRotation(Vec3 from, Vec3 to) {
        Vector3f f = new Vector3f((float) from.x, (float) from.y, (float) from.z);
        Vector3f t = new Vector3f((float) to.x, (float) to.y, (float) to.z);

        float dotProduct = f.dot(t);

        if (dotProduct > 0.99999f) {
            return new Quaternionf(); // identity
        }

        if (dotProduct < -0.99999f) {
            // 180° rotation — pick an arbitrary perpendicular axis
            Vector3f perp = Math.abs(f.x) < 0.9f
                    ? new Vector3f(1, 0, 0)
                    : new Vector3f(0, 1, 0);
            Vector3f axis = new Vector3f();
            f.cross(perp, axis).normalize();
            return new Quaternionf().rotationAxis((float) Math.PI, axis);
        }

        // Standard quaternion from two vectors:
        // q = (cross, 1 + dot), normalized
        Vector3f cross = new Vector3f();
        f.cross(t, cross);

        Quaternionf q = new Quaternionf(cross.x, cross.y, cross.z, 1.0f + dotProduct);
        q.normalize();
        return q;
    }
}
