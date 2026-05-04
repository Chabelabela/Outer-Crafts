package org.chabelabela.outer_crafts.ship;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ShipComponent {

    private final String id;
    private final String displayName;
    private int health;
    private final int maxHealth;

    public ShipComponent(String id, String displayName, int maxHealth) {
        this.id = id;
        this.displayName = displayName;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }

    public float getHealthPercent() {
        return (float) health / maxHealth;
    }

    public boolean isDestroyed() {
        return health <= 0;
    }

    public boolean isDamaged() {
        return health < maxHealth;
    }

    public boolean isCritical() {
        return health > 0 && health < maxHealth * 0.25f;
    }

    public boolean damage(int amount) {
        boolean wasAlive = health > 0;
        health = Math.max(0, health - amount);
        return wasAlive && health <= 0;
    }

    public int repair(int amount) {
        int before = health;
        health = Math.min(maxHealth, health + amount);
        return health - before;
    }

    public void reset() {
        health = maxHealth;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        tag.putInt("Health", health);
        return tag;
    }

    public void readTag(CompoundTag tag) {
        if (tag.contains("Health")) {
            this.health = Math.clamp(tag.getIntOr("Health", maxHealth), 0, maxHealth);
        }
    }

    public void writeOutput(ValueOutput output) {
        output.putString("Id", id);
        output.putInt("Health", health);
    }

    public void readInput(ValueInput input) {
        this.health = Math.clamp(input.getIntOr("Health", maxHealth), 0, maxHealth);
    }
}
