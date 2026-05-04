package org.chabelabela.outer_crafts.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.entity.AnglerfishEntity;
import org.chabelabela.outer_crafts.entity.ScoutEntity;
import org.chabelabela.outer_crafts.ship.ShipEntity;

public final class OuterCraftsEntities {

    private OuterCraftsEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, OuterCrafts.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ScoutEntity>> SCOUT =
            ENTITIES.register("scout",
                    () -> EntityType.Builder.<ScoutEntity>of(ScoutEntity::new, MobCategory.MISC)
                            .sized(0.35f, 0.35f)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                                    Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "scout"))));

    public static final DeferredHolder<EntityType<?>, EntityType<ShipEntity>> SHIP =
            ENTITIES.register("ship",
                    () -> EntityType.Builder.<ShipEntity>of(ShipEntity::new, MobCategory.MISC)
                            .sized(3.5f, 3.0f)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                                    Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "ship"))));

    /** Dark Bramble's blind sound-tracking predator. See {@link AnglerfishEntity}. */
    public static final DeferredHolder<EntityType<?>, EntityType<AnglerfishEntity>> ANGLERFISH =
            ENTITIES.register("anglerfish",
                    () -> EntityType.Builder.<AnglerfishEntity>of(AnglerfishEntity::new, MobCategory.MONSTER)
                            .sized(2.4f, 1.6f)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                                    Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "anglerfish"))));
}
