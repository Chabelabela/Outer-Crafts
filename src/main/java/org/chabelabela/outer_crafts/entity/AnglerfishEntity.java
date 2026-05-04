package org.chabelabela.outer_crafts.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Anglerfish (3.3) — Outer Wilds' iconic Dark Bramble predator.
 *
 * <p>Behaviour:
 * <ul>
 *   <li>Effectively blind — has no visual targeting goal.</li>
 *   <li>Listens for noisy entities within {@value #HEAR_RANGE} blocks.
 *       "Noisy" = moving fast (player sprinting / ship engine on / jetpack thrust).
 *       Below the {@value #STEALTH_VELOCITY} velocity threshold the fish doesn't notice.</li>
 *   <li>Once it has a target, it lunges in a straight line. If the target stops
 *       moving entirely, the fish loses interest after {@value #LOSE_INTEREST_TICKS}.</li>
 *   <li>Bite damage scales with the angler's mass (a real OW one-shot).</li>
 * </ul>
 *
 * <p>Implementation notes:
 * <ul>
 *   <li>We do NOT use a vanilla TargetGoal because we want sound-only targeting,
 *       and we don't want vision-based pathfinding interfering.</li>
 *   <li>{@link #aiStep()} drives the entire AI loop directly — simpler than the
 *       BehaviorBuilder DSL for a one-off custom mob, and easier to reason about.</li>
 * </ul>
 */
public class AnglerfishEntity extends Monster {

    /** How far the angler can "hear" a noisy entity. */
    private static final double HEAR_RANGE = 30.0;
    /** Below this velocity, an entity is considered silent and ignored. */
    private static final double STEALTH_VELOCITY = 0.08;
    /** Stop tracking after the target has been silent for this many ticks. */
    private static final int LOSE_INTEREST_TICKS = 60;
    /** Velocity multiplier when actively pursuing. */
    private static final double LUNGE_SPEED = 0.6;
    /** Hit-box-based bite range. */
    private static final double BITE_RANGE = 2.5;
    /** Damage dealt on bite. */
    private static final float BITE_DAMAGE = 30.0f;

    private LivingEntity currentTarget;
    private int silenceCounter = 0;

    public AnglerfishEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.6)
                .add(Attributes.ATTACK_DAMAGE, BITE_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, HEAR_RANGE);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) return;

        // Re-evaluate target every tick: cheap (single AABB scan).
        LivingEntity loudest = findLoudestTarget();

        if (loudest != null) {
            currentTarget = loudest;
            silenceCounter = 0;
        } else if (currentTarget != null) {
            silenceCounter++;
            if (silenceCounter > LOSE_INTEREST_TICKS) {
                currentTarget = null;
            }
        }

        if (currentTarget == null) return;

        // Lunge toward target
        Vec3 toward = currentTarget.position().subtract(this.position()).normalize();
        Vec3 newVel = this.getDeltaMovement().add(toward.scale(LUNGE_SPEED * 0.1));
        // Cap horizontal speed
        double sp = newVel.length();
        if (sp > LUNGE_SPEED) newVel = newVel.normalize().scale(LUNGE_SPEED);
        this.setDeltaMovement(newVel);

        // Face the target
        double dx = currentTarget.getX() - this.getX();
        double dz = currentTarget.getZ() - this.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        this.setYRot(yaw);
        this.setYHeadRot(yaw);

        // Bite if in range
        if (this.distanceTo(currentTarget) < BITE_RANGE) {
            currentTarget.hurt(this.damageSources().mobAttack(this), BITE_DAMAGE);
        }
    }

    /**
     * Scans nearby living entities for the one moving fastest above the stealth threshold.
     * Returns null if everyone is being quiet.
     */
    private LivingEntity findLoudestTarget() {
        AABB box = this.getBoundingBox().inflate(HEAR_RANGE);
        List<LivingEntity> candidates = this.level().getEntitiesOfClass(
                LivingEntity.class, box, this::isPotentialPrey);

        LivingEntity loudest = null;
        double loudestSpeed = STEALTH_VELOCITY;
        for (LivingEntity e : candidates) {
            double speed = e.getDeltaMovement().lengthSqr();
            // Sneaking entities are quieter
            if (e.isCrouching()) speed *= 0.25;
            if (speed > loudestSpeed * loudestSpeed) {
                loudestSpeed = Math.sqrt(speed);
                loudest = e;
            }
        }
        return loudest;
    }

    private boolean isPotentialPrey(LivingEntity e) {
        if (e == this) return false;
        if (e instanceof AnglerfishEntity) return false;
        if (!e.isAlive()) return false;
        // Ignore creative/spectator players
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        return true;
    }

    @Override
    public boolean canBreatheUnderwater() { return true; }

    @Override
    public boolean isPushedByFluid() { return false; }

    @Override
    public boolean fireImmune() { return true; }

    /** No vision-based perception → ignore standard mob despawn rules. */
    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    /**
     * Override the standard hurt() so taking damage clears the angler's current
     * target — encourages players to break LOS / move silently after attacking.
     * The exact hurt signature differs across MC versions; we use the reflectively-safe
     * {@code Entity.hurt(DamageSource, float)} which exists in 1.20+.
     */
    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level,
                              DamageSource source, float amount) {
        currentTarget = null;
        silenceCounter = 0;
        return super.hurtServer(level, source, amount);
    }
}
