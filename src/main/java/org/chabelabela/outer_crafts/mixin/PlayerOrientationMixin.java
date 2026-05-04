package org.chabelabela.outer_crafts.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.chabelabela.outer_crafts.physics.GravityManager;
import org.chabelabela.outer_crafts.physics.SphericalGravityHelper;
import org.chabelabela.outer_crafts.world.dimension.OuterCraftsDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server-side player orientation mixin. Transforms the player's movement input
 * from their local reference frame (relative to gravity) into world-space.
 * <p>
 * In vanilla, the player's movement input assumes Y-up. When standing on a sphere,
 * "forward" and "strafe" must be rotated to lie on the tangent plane at the player's
 * position on the sphere.
 */
@Mixin(ServerPlayer.class)
public abstract class PlayerOrientationMixin {

    /**
     * Smoothly interpolated "up" vector — prevents jarring snaps when
     * transitioning between gravity fields.
     */
    @Unique
    private Vec3 outerCrafts$smoothedUp = new Vec3(0, 1, 0);

    /**
     * Interpolation speed for gravity transitions (0.0–1.0 per tick).
     * Lower = smoother but laggier response.
     */
    @Unique
    private static final double GRAVITY_TRANSITION_SPEED = 0.15;

    /**
     * Injected at the end of the player's tick to smoothly transition gravity orientation.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void outerCrafts$smoothGravityTransition(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;

        // A1 hygiene: only recompute smoothed-up in the solar system.
        // Elsewhere, the cached value stays at vanilla (0,1,0) which is what
        // JetpackHandler.computeVanillaThrust assumes. Without this guard we
        // were driving the smoothed-up away from Y+ in overworld based on
        // stray celestial-body positions registered at world origin — the
        // root cause of the "jetpack goes diagonal" playtest bug.
        if (!player.level().dimension().equals(OuterCraftsDimensions.SOLAR_SYSTEM_LEVEL)) {
            this.outerCrafts$smoothedUp = new Vec3(0, 1, 0);
            return;
        }

        long tick = player.level().getGameTime();

        GravityManager.GravityState state = GravityManager.computeGravityState(player, tick);
        Vec3 targetUp = state.localUp();

        // Slerp toward the target up vector for smooth transitions
        this.outerCrafts$smoothedUp = SphericalGravityHelper.slerpUp(
                this.outerCrafts$smoothedUp,
                targetUp,
                GRAVITY_TRANSITION_SPEED
        );
    }

    /**
     * Exposes the smoothed-up vector for network sync to the client.
     */
    @Unique
    public Vec3 outerCrafts$getSmoothedUp() {
        return this.outerCrafts$smoothedUp;
    }
}
