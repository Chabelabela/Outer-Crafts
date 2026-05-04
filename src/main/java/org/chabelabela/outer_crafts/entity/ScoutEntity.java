package org.chabelabela.outer_crafts.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.chabelabela.outer_crafts.physics.GravityManager;

import java.util.UUID;

public class ScoutEntity extends Entity {

    private static final EntityDataAccessor<Boolean> DATA_STUCK = SynchedEntityData.defineId(
            ScoutEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LIGHT_ON = SynchedEntityData.defineId(
            ScoutEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PHOTO_MODE = SynchedEntityData.defineId(
            ScoutEntity.class, EntityDataSerializers.BOOLEAN);

    private UUID ownerId;
    private int age = 0;

    private static final int MAX_AGE = 6000;
    private static final double LAUNCH_SPEED = 1.5;

    public ScoutEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STUCK, false);
        builder.define(DATA_LIGHT_ON, true);
        builder.define(DATA_PHOTO_MODE, false);
    }

    public void launchFrom(Entity owner) {
        this.ownerId = owner.getUUID();
        Vec3 lookDir = owner.getLookAngle().normalize();
        this.setDeltaMovement(lookDir.scale(LAUNCH_SPEED));
        this.setPos(owner.getEyePosition().add(lookDir.scale(0.5)));
        this.setYRot(owner.getYRot());
        this.setXRot(owner.getXRot());
    }

    @Override
    public void tick() {
        super.tick();
        age++;

        if (age > MAX_AGE) {
            this.discard();
            return;
        }

        if (isStuck()) {
            return;
        }

        if (!this.level().isClientSide()) {
            long tick = this.level().getGameTime();
            GravityManager.GravityState gravState = GravityManager.computeGravityState(this, tick);
            if (!gravState.isInDeepSpace()) {
                Vec3 velocity = this.getDeltaMovement().add(gravState.gravityVector());
                this.setDeltaMovement(velocity);
            }
        }

        Vec3 velocity = this.getDeltaMovement();
        this.move(MoverType.SELF, velocity);

        if (this.horizontalCollision || this.verticalCollision) {
            setStuck(true);
            this.setDeltaMovement(Vec3.ZERO);
        }

        if (!isStuck()) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.99));
        }
    }

    public PhotoResult takePhoto() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return PhotoResult.EMPTY;
        }
        this.entityData.set(DATA_PHOTO_MODE, true);

        var nearbyEntities = serverLevel.getEntities(this,
                this.getBoundingBox().inflate(50.0), e -> true);

        boolean observedQuantum = false;
        for (Entity entity : nearbyEntities) {
            if (entity.entityTags().contains("quantum")) {
                observedQuantum = true;
            }
        }

        return new PhotoResult(this.position(), this.getLookAngle(), observedQuantum);
    }

    public record PhotoResult(Vec3 position, Vec3 direction, boolean observedQuantum) {
        public static final PhotoResult EMPTY = new PhotoResult(Vec3.ZERO, Vec3.ZERO, false);
    }

    public boolean isStuck() { return this.entityData.get(DATA_STUCK); }
    public void setStuck(boolean stuck) { this.entityData.set(DATA_STUCK, stuck); }

    public boolean isLightOn() { return this.entityData.get(DATA_LIGHT_ON); }
    public void setLightOn(boolean on) { this.entityData.set(DATA_LIGHT_ON, on); }

    public UUID getOwnerId() { return ownerId; }
    public int getAge() { return age; }

    @Override
    public boolean isCurrentlyGlowing() {
        return isLightOn();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.age = input.getIntOr("Age", 0);
        input.getString("Owner").ifPresent(s -> {
            try { this.ownerId = UUID.fromString(s); } catch (IllegalArgumentException ignored) {}
        });
        setStuck(input.getBooleanOr("Stuck", false));
        setLightOn(input.getBooleanOr("LightOn", true));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Age", this.age);
        if (this.ownerId != null) {
            output.putString("Owner", this.ownerId.toString());
        }
        output.putBoolean("Stuck", isStuck());
        output.putBoolean("LightOn", isLightOn());
    }
}
