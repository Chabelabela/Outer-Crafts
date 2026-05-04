package org.chabelabela.outer_crafts.ship;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ShipDamageSystem {

    private final Map<String, ShipComponent> components = new LinkedHashMap<>();

    public ShipDamageSystem() {
        int hp = ShipConstants.COMPONENT_MAX_HEALTH;
        components.put("hull", new ShipComponent("hull", "Hull", hp));
        components.put("engine", new ShipComponent("engine", "Main Engine", hp));
        components.put("thruster_left", new ShipComponent("thruster_left", "Left Thruster", hp));
        components.put("thruster_right", new ShipComponent("thruster_right", "Right Thruster", hp));
        components.put("cockpit", new ShipComponent("cockpit", "Cockpit", hp));
        components.put("oxygen_module", new ShipComponent("oxygen_module", "Oxygen Module", hp));
    }

    public ShipComponent get(String id) {
        return components.get(id);
    }

    public Map<String, ShipComponent> all() {
        return components;
    }

    public boolean isShipDestroyed() {
        return components.get("hull").isDestroyed();
    }

    public double getMainThrustMultiplier() {
        return components.get("engine").getHealthPercent();
    }

    public double getLeftThrusterMultiplier() {
        return components.get("thruster_left").getHealthPercent();
    }

    public double getRightThrusterMultiplier() {
        return components.get("thruster_right").getHealthPercent();
    }

    public boolean isCockpitFunctional() {
        return !components.get("cockpit").isDestroyed();
    }

    public boolean isOxygenIntact() {
        return !components.get("oxygen_module").isDestroyed();
    }

    public double getOxygenLeakMultiplier() {
        ShipComponent o2 = components.get("oxygen_module");
        if (o2.isDestroyed()) return 5.0;
        if (o2.isCritical()) return 2.0;
        return 1.0;
    }

    /**
     * Applies impact damage to the hull plus a random secondary component.
     *
     * <p>The {@code random} source must be deterministic (e.g.
     * {@code level.random} or {@code RandomSource.create(seed)}) so impacts
     * replay identically in tests, demos, and across save reloads — and so
     * server-side decisions can be deterministically replicated to clients
     * if we ever need to.
     */
    public boolean applyImpactDamage(double impactSpeed, net.minecraft.util.RandomSource random) {
        if (impactSpeed < ShipConstants.DAMAGE_VELOCITY_THRESHOLD) return false;

        int damage = (int) ((impactSpeed - ShipConstants.DAMAGE_VELOCITY_THRESHOLD)
                * ShipConstants.DAMAGE_MULTIPLIER);
        if (damage <= 0) return false;

        components.get("hull").damage(damage);

        var componentList = components.values().stream()
                .filter(c -> !c.getId().equals("hull"))
                .toList();
        if (!componentList.isEmpty()) {
            int idx = random.nextInt(componentList.size());
            componentList.get(idx).damage(damage / 2);
        }

        return isShipDestroyed();
    }

    /**
     * Backwards-compatible overload that uses a thread-local fallback random.
     * Prefer the {@code (impactSpeed, RandomSource)} overload — call sites
     * inside an entity should pass {@code level.random}.
     */
    @Deprecated
    public boolean applyImpactDamage(double impactSpeed) {
        return applyImpactDamage(impactSpeed, net.minecraft.util.RandomSource.create());
    }

    public int repairComponent(String componentId, int amount) {
        ShipComponent comp = components.get(componentId);
        if (comp == null) return 0;
        return comp.repair(amount);
    }

    public void resetAll() {
        components.values().forEach(ShipComponent::reset);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (ShipComponent comp : components.values()) {
            list.add(comp.toTag());
        }
        tag.put("Components", list);
        return tag;
    }

    public void readTag(CompoundTag tag) {
        if (!tag.contains("Components")) return;
        ListTag list = tag.getListOrEmpty("Components");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag compTag = list.getCompoundOrEmpty(i);
            String id = compTag.getStringOr("Id", "");
            ShipComponent comp = components.get(id);
            if (comp != null) {
                comp.readTag(compTag);
            }
        }
    }

    public void writeOutput(ValueOutput output) {
        for (ShipComponent comp : components.values()) {
            comp.writeOutput(output.child(comp.getId()));
        }
    }

    public void readInput(ValueInput input) {
        for (Map.Entry<String, ShipComponent> entry : components.entrySet()) {
            input.child(entry.getKey()).ifPresent(child -> entry.getValue().readInput(child));
        }
    }
}
