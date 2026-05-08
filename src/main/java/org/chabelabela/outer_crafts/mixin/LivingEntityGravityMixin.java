package org.chabelabela.outer_crafts.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.chabelabela.outer_crafts.physics.GravityManager;
import org.chabelabela.outer_crafts.physics.SphericalGravityHelper;
import org.chabelabela.outer_crafts.world.dimension.OuterCraftsDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Spherical-gravity mixin for {@link LivingEntity}.
 *
 * <p>Replaces vanilla's flat Y-axis gravity with a per-tick pull toward whichever
 * celestial body's gravity well dominates at the entity's position. Runs on
 * <strong>both client and server</strong> so client-side prediction stays in sync
 * with server authority — without that, the player constantly rubberbands.
 *
 * <p>Vanilla gravity in the solar-system dimension is zeroed out by
 * {@link LivingEntityEffectiveGravityMixin}, so we don't need the fragile
 * {@code +0.08} neutralisation trick the previous implementation used.
 *
 * <p>When the entity is on a planet's surface (within {@code radius + 2}) and
 * moving roughly along the surface, its horizontal motion is reprojected onto
 * the local tangent plane so it follows the curvature of the sphere instead of
 * walking straight off the edge in world coordinates.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityGravityMixin extends Entity {

    /** Margin above the surface within which we consider the entity "walking". */
    private static final double SURFACE_GRIP_MARGIN = 2.0;

    // Required by extending Entity — never called, just satisfies the compiler
    protected LivingEntityGravityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Inject at {@code travel()} HEAD: replace whatever Y-velocity adjustment
     * vanilla would do (already neutralised via {@link LivingEntityEffectiveGravityMixin})
     * with our spherical pull, then snap the horizontal component onto the
     * sphere's tangent plane if the entity is grounded on a body.
     */
    @Inject(method = "travel", at = @At("HEAD"))
    private void outerCrafts$applySphericalGravity(Vec3 movementInput, CallbackInfo ci) {
        // Run on both sides — client predicts, server authoritative.
        if (!this.level().dimension().equals(OuterCraftsDimensions.SOLAR_SYSTEM_LEVEL)) {
            return;
        }
        if (this.isNoGravity()) {
            return;
        }

        long tick = this.level().getGameTime();
        Entity self = (Entity) (Object) this;

        GravityManager.GravityState state = GravityManager.computeGravityState(self, tick);

        if (state.isInDeepSpace()) {
            // No body's well claims this entity → drift in zero-G. Nothing to do.
            return;
        }

        Vec3 velocity = this.getDeltaMovement();

        // Apply spherical gravity to velocity.
        velocity = velocity.add(state.gravityVector());

        // Tangent-plane snap: when the entity is on or just above a body's
        // surface, project the *horizontal* (perpendicular-to-localUp) component
        // of velocity onto the tangent plane so they follow the curve instead
        // of walking off the edge along a world axis.
        if (state.dominantBody() != null) {
            double surfaceDist = state.distanceToCenter() - state.dominantBody().radius();
            if (surfaceDist <= SURFACE_GRIP_MARGIN) {
                Vec3 localUp = state.localUp();
                // Decompose velocity into vertical (along localUp) + horizontal (perpendicular).
                double upComponent = velocity.dot(localUp);
                Vec3 vertical = localUp.scale(upComponent);
                Vec3 horizontal = velocity.subtract(vertical);
                // Re-project horizontal onto the tangent plane (subtract any drift along localUp).
                horizontal = SphericalGravityHelper.projectOntoTangentPlane(horizontal, localUp);
                velocity = vertical.add(horizontal);
            }
        }

        this.setDeltaMovement(velocity);
    }
}
