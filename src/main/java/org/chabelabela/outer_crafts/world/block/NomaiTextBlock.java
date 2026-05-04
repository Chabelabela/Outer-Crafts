package org.chabelabela.outer_crafts.world.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class NomaiTextBlock extends Block {

    public static final IntegerProperty TEXT_VARIANT = IntegerProperty.create("text_variant", 0, 7);

    public static final MapCodec<NomaiTextBlock> CODEC = simpleCodec(NomaiTextBlock::new);

    public NomaiTextBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TEXT_VARIANT, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TEXT_VARIANT);
    }

    public String getTextId(Level level, BlockPos pos) {
        var textIds = org.chabelabela.outer_crafts.equipment.NomaiTextRegistry.allTextIds()
                .stream().toList();
        if (textIds.isEmpty()) return "unknown";

        long hash = ((long) pos.getX() * 73856093L)
                  ^ ((long) pos.getY() * 19349669L)
                  ^ ((long) pos.getZ() * 83492791L);
        int index = (int) (Math.abs(hash) % textIds.size());
        return textIds.get(index);
    }
}
