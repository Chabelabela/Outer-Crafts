package org.chabelabela.outer_crafts.registry;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.equipment.AdvancedWarpDriveItem;
import org.chabelabela.outer_crafts.equipment.ScoutLauncherItem;
import org.chabelabela.outer_crafts.equipment.SignalscopeItem;
import org.chabelabela.outer_crafts.equipment.TranslatorItem;

public final class OuterCraftsItems {

    private OuterCraftsItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OuterCrafts.MODID);

    public static final ResourceKey<EquipmentAsset> SPACESUIT_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID,
                    Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "spacesuit"));

    public static final DeferredItem<Item> SPACESUIT_HELMET = ITEMS.registerItem("spacesuit_helmet",
            props -> new Item(props.stacksTo(1)
                    .component(DataComponents.EQUIPPABLE,
                            Equippable.builder(EquipmentSlot.HEAD).setAsset(SPACESUIT_ASSET).build())));

    public static final DeferredItem<Item> SPACESUIT_CHESTPLATE = ITEMS.registerItem("spacesuit_chestplate",
            props -> new Item(props.stacksTo(1)
                    .component(DataComponents.EQUIPPABLE,
                            Equippable.builder(EquipmentSlot.CHEST).setAsset(SPACESUIT_ASSET).build())));

    public static final DeferredItem<Item> SPACESUIT_LEGGINGS = ITEMS.registerItem("spacesuit_leggings",
            props -> new Item(props.stacksTo(1)
                    .component(DataComponents.EQUIPPABLE,
                            Equippable.builder(EquipmentSlot.LEGS).setAsset(SPACESUIT_ASSET).build())));

    public static final DeferredItem<Item> SPACESUIT_BOOTS = ITEMS.registerItem("spacesuit_boots",
            props -> new Item(props.stacksTo(1)
                    .component(DataComponents.EQUIPPABLE,
                            Equippable.builder(EquipmentSlot.FEET).setAsset(SPACESUIT_ASSET).build())));

    public static final DeferredItem<SignalscopeItem> SIGNALSCOPE = ITEMS.registerItem("signalscope",
            props -> new SignalscopeItem(props.stacksTo(1)));

    public static final DeferredItem<ScoutLauncherItem> SCOUT_LAUNCHER = ITEMS.registerItem("scout_launcher",
            props -> new ScoutLauncherItem(props.stacksTo(1)));

    public static final DeferredItem<TranslatorItem> TRANSLATOR_TOOL = ITEMS.registerItem("translator_tool",
            props -> new TranslatorItem(props.stacksTo(1)));

    public static final DeferredItem<Item> OXYGEN_TANK = ITEMS.registerItem("oxygen_tank",
            props -> new Item(props.stacksTo(16)));

    public static final DeferredItem<Item> FUEL_CANISTER = ITEMS.registerItem("fuel_canister",
            props -> new Item(props.stacksTo(16)));

    /** Endgame artifact (3.8). See {@link AdvancedWarpDriveItem}. */
    public static final DeferredItem<AdvancedWarpDriveItem> ADVANCED_WARP_DRIVE = ITEMS.registerItem(
            "advanced_warp_drive",
            props -> new AdvancedWarpDriveItem(props
                    .stacksTo(1)
                    .fireResistant()));

    // ── BlockItems ──────────────────────────────────────────────────────────
    // Each block needs a paired BlockItem so it's pickable, holdable, and
    // representable in creative tabs. registerSimpleBlockItem mirrors the
    // block's id/namespace and uses default Item.Properties (stacksTo 64).

    public static final DeferredItem<BlockItem> NOMAI_TEXT_WALL_ITEM =
            ITEMS.registerSimpleBlockItem(OuterCraftsBlocks.NOMAI_TEXT_WALL);

    public static final DeferredItem<BlockItem> OXYGEN_REFILL_STATION_ITEM =
            ITEMS.registerSimpleBlockItem(OuterCraftsBlocks.OXYGEN_REFILL_STATION);

    public static final DeferredItem<BlockItem> FUEL_REFILL_STATION_ITEM =
            ITEMS.registerSimpleBlockItem(OuterCraftsBlocks.FUEL_REFILL_STATION);

    public static final DeferredItem<BlockItem> QUANTUM_SHARD_ITEM =
            ITEMS.registerSimpleBlockItem(OuterCraftsBlocks.QUANTUM_SHARD);

    public static final DeferredItem<BlockItem> MEMORY_STATUE_ITEM =
            ITEMS.registerSimpleBlockItem(OuterCraftsBlocks.MEMORY_STATUE);
}
