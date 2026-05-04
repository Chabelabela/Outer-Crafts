package org.chabelabela.outer_crafts.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.chabelabela.outer_crafts.physics.GravityConstants;
import org.chabelabela.outer_crafts.physics.GravityManager;
import org.chabelabela.outer_crafts.world.dimension.OuterCraftsDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Core gravity mixin. Intercepts LivingEntity's travel method to replace
 * vanilla's flat Y-axis gravity with spherical gravity toward celestial bodies.
 * <p>
 * Strategy:
 * <ol>
 *   <li>At the HEAD of {@code travel()}, cancel vanilla gravity by adding +{@code VANILLA_GRAVITY}
 *       (= 0.08) to Y velocity. Vanilla applies {@code -getDefaultGravity()} (= -0.08) to
 *       deltaMovement somewhere upstream of {@code travel()} in the tick pipeline; our +0.08
 *       neutralises it.</li>
 *   <li>Apply our spherical gravity vector computed by {@link GravityManager}.</li>
 * </ol>
 *
 * <p>This approach is minimally invasive — we don't overwrite travel(), we layer on top of it.
 *
 * <p><b>Compatibility note:</b> The +0.08 trick assumes {@code LivingEntity.getEffectiveGravity()}
 * returns ~0.08 (the MC vanilla default). If a mod or version change alters effective gravity,
 * the neutralisation will be wrong. A more robust alternative is {@code setNoGravity(true)} on
 * the entity in the solar-system dimension and applying our gravity entirely separately —
 * deferred until/unless this becomes an issue.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityGravityMixin extends Entity {

    // Required by extending Entity — never called, just satisfies the compiler
    protected LivingEntityGravityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    /**
     * Injected at the head of travel() to replace vanilla gravity with spherical gravity.
     */
    @Inject(method = "travel", at = @At("HEAD"))
    private void outerCrafts$applySphericalGravity(Vec3 movementInput, CallbackInfo ci) {
        if (this.level().isClientSide()) {
            return;
        }

        // Only apply spherical gravity in the solar system dimension
        if (!this.level().dimension().equals(OuterCraftsDimensions.SOLAR_SYSTEM_LEVEL)) {
            return;
        }

        if (this.isNoGravity()) {
            return;
        }

        long tick = this.level().getGameTime();
        Vec3 currentVelocity = this.getDeltaMovement();

        // Step 1: Counteract vanilla gravity (which was already applied as -0.08 Y).
        // We add it back so we're starting from a gravity-neutral state.
        Vec3 neutralized = currentVelocity.add(0.0, GravityConstants.VANILLA_GRAVITY, 0.0);

        // Step 2: Compute and apply spherical gravity
        GravityManager.GravityState state = GravityManager.computeGravityState(
                (Entity) (Object) this, tick
        );

        Vec3 newVelocity = neutralized.add(state.gravityVector());

        this.setDeltaMovement(newVelocity);
    }
}
