package org.chabelabela.outer_crafts.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.chabelabela.outer_crafts.entity.ScoutEntity;
import org.chabelabela.outer_crafts.physics.GravityEntityAccess;
import org.chabelabela.outer_crafts.physics.GravityManager;
import org.chabelabela.outer_crafts.ship.ShipEntity;
import org.chabelabela.outer_crafts.world.dimension.OuterCraftsDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into Entity to store per-entity gravity state and reorient movement vectors.
 * <p>
 * When an entity moves on a spherical surface, its "forward" direction must be
 * projected onto the tangent plane of the sphere. This mixin handles that projection
 * and exposes the current gravity state for other systems to query.
 */
@Mixin(Entity.class)
public abstract class EntityMoveMixin implements GravityEntityAccess {

    @Shadow
    public abstract Vec3 position();

    @Shadow
    public abstract net.minecraft.world.level.Level level();

    // ── Per-entity gravity state (cached each tick) ─────────────────────

    /**
     * Cached local "up" vector for this entity, updated each tick.
     * Defaults to vanilla up (0, 1, 0).
     */
    @Unique
    private Vec3 outerCrafts$localUp = new Vec3(0, 1, 0);

    /**
     * Cached dominant celestial body ID this entity is influenced by.
     * Null means deep space.
     */
    @Unique
    private String outerCrafts$dominantBodyId = null;

    /**
     * Whether this entity is currently on the surface of a celestial body.
     */
    @Unique
    private boolean outerCrafts$onCelestialSurface = false;

    /**
     * Injected at the start of each entity tick to refresh gravity state.
     * <p>
     * Scoped to entity types that actually need gravity orientation:
     * <ul>
     *   <li>{@link LivingEntity} — players and mobs that walk on planets</li>
     *   <li>{@link ShipEntity} / {@link ScoutEntity} — custom physics-driven entities</li>
     *   <li>{@link FallingBlockEntity} — Brittle Hollow collapse blocks (future)</li>
     * </ul>
     * Arrows, dropped items, projectiles, particles, etc. retain default
     * vanilla {@code (0, 1, 0)} up — none of them query the gravity-state interface.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void outerCrafts$updateGravityState(CallbackInfo ci) {
        if (this.level().isClientSide()) return;

        // Only compute gravity state in the solar system dimension
        if (!this.level().dimension().equals(OuterCraftsDimensions.SOLAR_SYSTEM_LEVEL)) return;

        // Whitelist: skip the body-walk for entity types that never query gravity.
        Entity self = (Entity) (Object) this;
        if (!(self instanceof LivingEntity)
                && !(self instanceof ShipEntity)
                && !(self instanceof ScoutEntity)
                && !(self instanceof FallingBlockEntity)) {
            return;
        }

        long tick = this.level().getGameTime();
        GravityManager.GravityState state = GravityManager.computeGravityState(self, tick);

        this.outerCrafts$localUp = state.localUp();
        this.outerCrafts$dominantBodyId = state.dominantBody() != null
                ? state.dominantBody().id()
                : null;
        this.outerCrafts$onCelestialSurface = state.isOnSurface();
    }

    // ── Accessors (for other mixins and systems) ────────────────────────

    /**
     * Returns the cached local "up" direction for this entity.
     * Called from client-side camera mixin and movement code.
     */
    @Unique
    public Vec3 outerCrafts$getLocalUp() {
        return this.outerCrafts$localUp;
    }

    /**
     * Returns the dominant body ID this entity is gravitationally bound to.
     */
    @Unique
    public String outerCrafts$getDominantBodyId() {
        return this.outerCrafts$dominantBodyId;
    }

    /**
     * Returns whether this entity is near the surface of a celestial body.
     */
    @Unique
    public boolean outerCrafts$isOnCelestialSurface() {
        return this.outerCrafts$onCelestialSurface;
    }
}
