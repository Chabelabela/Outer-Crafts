package org.chabelabela.outer_crafts.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.world.block.FuelRefillBlock;
import org.chabelabela.outer_crafts.world.block.MemoryStatueBlock;
import org.chabelabela.outer_crafts.world.block.NomaiTextBlock;
import org.chabelabela.outer_crafts.world.block.OxygenRefillBlock;
import org.chabelabela.outer_crafts.world.block.QuantumShardBlock;

public final class OuterCraftsBlocks {

    private OuterCraftsBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(OuterCrafts.MODID);

    // MC 1.21+ requires BlockBehaviour.Properties to have its ResourceKey set.
    // DeferredRegister.Blocks.register(name, Function<Identifier, T>) injects the
    // block's Identifier; we wrap it into a ResourceKey<Block> for .setId().

    public static final DeferredBlock<NomaiTextBlock> NOMAI_TEXT_WALL = BLOCKS.register(
            "nomai_text_wall",
            id -> new NomaiTextBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(50.0f, 1200.0f)
                    .lightLevel(state -> 7)
                    .sound(SoundType.AMETHYST)
                    .noOcclusion()));

    public static final DeferredBlock<OxygenRefillBlock> OXYGEN_REFILL_STATION = BLOCKS.register(
            "oxygen_refill_station",
            id -> new OxygenRefillBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(3.0f, 6.0f)
                    .lightLevel(state -> 4)
                    .sound(SoundType.METAL)));

    public static final DeferredBlock<FuelRefillBlock> FUEL_REFILL_STATION = BLOCKS.register(
            "fuel_refill_station",
            id -> new FuelRefillBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0f, 6.0f)
                    .lightLevel(state -> 4)
                    .sound(SoundType.METAL)));

    /** Quantum Shard — moves when unobserved. See {@link QuantumShardBlock}. */
    public static final DeferredBlock<QuantumShardBlock> QUANTUM_SHARD = BLOCKS.register(
            "quantum_shard",
            id -> new QuantumShardBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.0f, 8.0f)
                    .lightLevel(state -> 5)
                    .sound(SoundType.AMETHYST)
                    .noOcclusion()));

    /** Memory Statue — pair to recall ship-log discoveries on death. */
    public static final DeferredBlock<MemoryStatueBlock> MEMORY_STATUE = BLOCKS.register(
            "memory_statue",
            id -> new MemoryStatueBlock(BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id))
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(50.0f, 1200.0f)
                    .lightLevel(state -> 3)
                    .sound(SoundType.STONE)));
}
