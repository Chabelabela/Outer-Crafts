package org.chabelabela.outer_crafts.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.chabelabela.outer_crafts.OuterCrafts;

public final class OuterCraftsItemGroups {

    private OuterCraftsItemGroups() {}

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OuterCrafts.MODID);

    public static final ResourceKey<CreativeModeTab> OUTER_CRAFTS_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                    Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "outer_crafts"));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            TABS.register("outer_crafts", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.outer_crafts.outer_crafts"))
                    .icon(() -> new ItemStack(OuterCraftsItems.SPACESUIT_HELMET.get()))
                    .displayItems((params, output) -> {
                        output.accept(OuterCraftsItems.SPACESUIT_HELMET);
                        output.accept(OuterCraftsItems.SPACESUIT_CHESTPLATE);
                        output.accept(OuterCraftsItems.SPACESUIT_LEGGINGS);
                        output.accept(OuterCraftsItems.SPACESUIT_BOOTS);
                        output.accept(OuterCraftsItems.SIGNALSCOPE);
                        output.accept(OuterCraftsItems.SCOUT_LAUNCHER);
                        output.accept(OuterCraftsItems.TRANSLATOR_TOOL);
                        output.accept(OuterCraftsItems.OXYGEN_TANK);
                        output.accept(OuterCraftsItems.FUEL_CANISTER);
                        output.accept(OuterCraftsItems.ADVANCED_WARP_DRIVE);
                        // Blocks (must be the paired BlockItem, not the DeferredBlock,
                        // otherwise asItem() returns Items.AIR and the tab build crashes
                        // with "stack count must be 1").
                        output.accept(OuterCraftsItems.NOMAI_TEXT_WALL_ITEM);
                        output.accept(OuterCraftsItems.OXYGEN_REFILL_STATION_ITEM);
                        output.accept(OuterCraftsItems.FUEL_REFILL_STATION_ITEM);
                        output.accept(OuterCraftsItems.QUANTUM_SHARD_ITEM);
                        output.accept(OuterCraftsItems.MEMORY_STATUE_ITEM);
                    })
                    .build());
}
