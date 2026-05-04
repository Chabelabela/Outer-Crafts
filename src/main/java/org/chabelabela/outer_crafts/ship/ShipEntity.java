package org.chabelabela.outer_crafts.ship;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.chabelabela.outer_crafts.equipment.SpacesuitData;
import org.chabelabela.outer_crafts.physics.GravityManager;
import org.chabelabela.outer_crafts.physics.SphericalGravityHelper;

import java.util.List;

public class ShipEntity extends Entity {

    private static final EntityDataAccessor<Float> DATA_FUEL = SynchedEntityData.defineId(
            ShipEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_ENGINE_ON = SynchedEntityData.defineId(
            ShipEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LANDED = SynchedEntityData.defineId(
            ShipEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_DESTROYED = SynchedEntityData.defineId(
            ShipEntity.class, EntityDataSerializers.BOOLEAN);

    private final ShipDamageSystem damageSystem = new ShipDamageSystem();
    private int shipOxygen = ShipConstants.SHIP_OXYGEN_CAPACITY;

    private float shipYaw = 0;
    private float shipPitch = 0;
    private float shipRoll = 0;

    private Vec3 preCollisionVelocity = Vec3.ZERO;
    private int damageCooldown = 0;

    public ShipEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FUEL, (float) ShipConstants.MAX_FUEL);
        builder.define(DATA_ENGINE_ON, false);
        builder.define(DATA_LANDED, false);
        builder.define(DATA_DESTROYED, false);
    }

    @Override
    public void tick() {
        super.tick();

        if (isDestroyed()) return;

        if (damageCooldown > 0) damageCooldown--;

        preCollisionVelocity = getDeltaMovement();

        if (!this.level().isClientSide()) {
            tickPhysics();
            tickOxygen();
        }

        Vec3 velocity = getDeltaMovement();
        this.move(MoverType.SELF, velocity);

        if ((this.horizontalCollision || this.verticalCollision) && damageCooldown <= 0) {
            double impactSpeed = preCollisionVelocity.length();
            if (!this.level().isClientSide()
                    && damageSystem.applyImpactDamage(impactSpeed, this.getRandom())) {
                onShipDestroyed();
            }
            damageCooldown = 20;
        }
    }

    private void tickPhysics() {
        long tick = level().getGameTime();
        GravityManager.GravityState gravState = GravityManager.computeGravityState(this, tick);

        Vec3 velocity = getDeltaMovement();

        if (!gravState.isInDeepSpace()) {
            velocity = velocity.add(gravState.gravityVector());
        }

        double drag = gravState.isInDeepSpace()
                ? ShipConstants.SPACE_DRAG
                : ShipConstants.ATMOSPHERIC_DRAG;
        velocity = velocity.scale(1.0 - drag);

        boolean onSurface = gravState.isOnSurface() && velocity.length() < 0.05;
        this.entityData.set(DATA_LANDED, onSurface);

        if (velocity.length() > ShipConstants.MAX_VELOCITY) {
            velocity = velocity.normalize().scale(ShipConstants.MAX_VELOCITY);
        }

        setDeltaMovement(velocity);
    }

    private void tickOxygen() {
        double leakMult = damageSystem.getOxygenLeakMultiplier();
        if (leakMult > 1.0) {
            shipOxygen = Math.max(0, shipOxygen - (int) leakMult);
        }

        Entity pilot = getFirstPassenger();
        if (pilot instanceof ServerPlayer player && shipOxygen > 0) {
            SpacesuitData.SuitState suit = SpacesuitData.getOrCreate(player);
            if (suit.isSuitEquipped()) {
                suit.restoreOxygen(ShipConstants.SHIP_O2_REFILL_PER_TICK);
                suit.restorePropellant(0.5);
            }
        }
    }

    public void applyThrustInput(double forward, double lateral, double vertical,
                                  double yawInput, double pitchInput) {
        if (isDestroyed()) return;

        // Yaw is still a per-tick delta from A/D press.
        shipYaw += (float) (yawInput * ShipConstants.ROTATION_SPEED);
        // Pitch is now an ABSOLUTE target value (player's look pitch in degrees).
        // Snapping to it instead of accumulating fixes the runaway feedback loop
        // where shipPitch += player.getXRot()/90 * ROTATION_SPEED ran away each tick.
        shipPitch = Math.clamp((float) pitchInput, -89.0f, 89.0f);
        this.setYRot(shipYaw);
        this.setXRot(shipPitch);

        // Fuel and thrust gating are intentionally checked AFTER applying rotation
        // so the ship's orientation tracks the player's look even when out of fuel.
        float fuel = entityData.get(DATA_FUEL);
        if (fuel <= 0) return;

        boolean anyThrust = forward != 0 || lateral != 0 || vertical != 0;
        if (!anyThrust) return;

        double yawRad = Math.toRadians(shipYaw);
        double pitchRad = Math.toRadians(shipPitch);

        Vec3 shipForward = new Vec3(
                -Math.sin(yawRad) * Math.cos(pitchRad),
                -Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();

        long tick = level().getGameTime();
        GravityManager.GravityState gravState = GravityManager.computeGravityState(this, tick);
        Vec3 localUp = gravState.isInDeepSpace()
                ? new Vec3(0, 1, 0)
                : gravState.localUp();

        Vec3 shipRight = shipForward.cross(localUp).normalize();
        if (shipRight.lengthSqr() < 1e-8) {
            shipRight = shipForward.cross(new Vec3(0, 0, 1)).normalize();
        }

        double mainMult = damageSystem.getMainThrustMultiplier();
        double leftMult = damageSystem.getLeftThrusterMultiplier();
        double rightMult = damageSystem.getRightThrusterMultiplier();

        Vec3 thrust = Vec3.ZERO;

        if (forward != 0) {
            thrust = thrust.add(shipForward.scale(forward * ShipConstants.MAIN_THRUST * mainMult));
        }

        if (lateral != 0) {
            double latMult = lateral > 0 ? rightMult : leftMult;
            thrust = thrust.add(shipRight.scale(lateral * ShipConstants.LATERAL_THRUST * latMult));
        }

        if (vertical != 0) {
            thrust = thrust.add(localUp.scale(vertical * ShipConstants.LATERAL_THRUST));
        }

        Vec3 newVelocity = getDeltaMovement().add(thrust);
        setDeltaMovement(newVelocity);
        this.hurtMarked = true;

        double fuelDrain = 0;
        if (forward != 0) fuelDrain += ShipConstants.FUEL_DRAIN_MAIN;
        if (lateral != 0) fuelDrain += ShipConstants.FUEL_DRAIN_LATERAL;
        if (vertical != 0) fuelDrain += ShipConstants.FUEL_DRAIN_LATERAL;
        entityData.set(DATA_FUEL, (float) Math.max(0, fuel - fuelDrain));

        entityData.set(DATA_ENGINE_ON, true);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 interactionLocation) {
        if (isDestroyed()) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            return attemptRepair(player);
        }

        if (!this.hasPassenger(player)) {
            if (!this.level().isClientSide()) {
                player.startRiding(this);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private InteractionResult attemptRepair(Player player) {
        if (this.level().isClientSide()) return InteractionResult.PASS;

        ShipComponent worst = null;
        for (ShipComponent comp : damageSystem.all().values()) {
            if (comp.isDamaged()) {
                if (worst == null || comp.getHealthPercent() < worst.getHealthPercent()) {
                    worst = comp;
                }
            }
        }

        if (worst == null) {
            if (player instanceof ServerPlayer sp) {
                sp.sendOverlayMessage(
                        Component.translatable("message.outer_crafts.ship.no_damage"));
            }
            return InteractionResult.PASS;
        }

        int repaired = worst.repair(ShipConstants.REPAIR_AMOUNT);
        if (player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(
                    Component.translatable("message.outer_crafts.ship.repaired",
                            worst.getDisplayName(), repaired));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        entityData.set(DATA_ENGINE_ON, false);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(net.minecraft.world.entity.LivingEntity passenger) {
        double yawRad = Math.toRadians(this.getYRot());
        double offsetX = Math.cos(yawRad) * 2.5;
        double offsetZ = Math.sin(yawRad) * 2.5;
        return this.position().add(offsetX, 0.5, offsetZ);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return !isDestroyed() && this.getPassengers().isEmpty();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return new Vec3(0.0, ShipConstants.COCKPIT_Y_OFFSET, 0.0);
    }

    private void onShipDestroyed() {
        entityData.set(DATA_DESTROYED, true);

        List<Entity> passengers = this.getPassengers();
        for (Entity passenger : passengers) {
            passenger.stopRiding();
            if (passenger instanceof ServerPlayer player) {
                player.sendSystemMessage(
                        Component.translatable("message.outer_crafts.ship.destroyed"));
                player.hurt(player.damageSources().explosion(this, this), 8.0f);
            }
        }

        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isInvulnerable()) return false;

        damageSystem.get("hull").damage((int) amount);
        if (damageSystem.isShipDestroyed()) {
            onShipDestroyed();
        }
        return true;
    }

    public float getFuel() { return entityData.get(DATA_FUEL); }
    public boolean isEngineOn() { return entityData.get(DATA_ENGINE_ON); }
    public boolean isLanded() { return entityData.get(DATA_LANDED); }
    public boolean isDestroyed() { return entityData.get(DATA_DESTROYED); }
    public ShipDamageSystem getDamageSystem() { return damageSystem; }
    public int getShipOxygen() { return shipOxygen; }
    public float getShipYaw() { return shipYaw; }
    public float getShipPitch() { return shipPitch; }

    public void setFuel(float fuel) {
        entityData.set(DATA_FUEL, Math.clamp(fuel, 0f, (float) ShipConstants.MAX_FUEL));
    }

    public void resetForNewLoop() {
        damageSystem.resetAll();
        entityData.set(DATA_FUEL, (float) ShipConstants.MAX_FUEL);
        entityData.set(DATA_DESTROYED, false);
        entityData.set(DATA_ENGINE_ON, false);
        shipOxygen = ShipConstants.SHIP_OXYGEN_CAPACITY;
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        entityData.set(DATA_FUEL, input.getFloatOr("Fuel", (float) ShipConstants.MAX_FUEL));
        entityData.set(DATA_DESTROYED, input.getBooleanOr("Destroyed", false));
        shipOxygen = input.getIntOr("ShipOxygen", ShipConstants.SHIP_OXYGEN_CAPACITY);
        shipYaw = input.getFloatOr("ShipYaw", 0f);
        shipPitch = input.getFloatOr("ShipPitch", 0f);
        shipRoll = input.getFloatOr("ShipRoll", 0f);
        input.child("DamageSystem").ifPresent(damageSystem::readInput);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putFloat("Fuel", entityData.get(DATA_FUEL));
        output.putBoolean("Destroyed", isDestroyed());
        output.putInt("ShipOxygen", shipOxygen);
        output.putFloat("ShipYaw", shipYaw);
        output.putFloat("ShipPitch", shipPitch);
        output.putFloat("ShipRoll", shipRoll);
        damageSystem.writeOutput(output.child("DamageSystem"));
    }
}
