package org.chabelabela.outer_crafts.world.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.chabelabela.outer_crafts.equipment.SpacesuitConstants;
import org.chabelabela.outer_crafts.equipment.SpacesuitData;

public class OxygenRefillBlock extends Block {

    public static final MapCodec<OxygenRefillBlock> CODEC = simpleCodec(OxygenRefillBlock::new);

    public OxygenRefillBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean headInside) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return;

        SpacesuitData.SuitState suit = SpacesuitData.getOrCreate(player);
        if (suit.isSuitEquipped()) {
            suit.restoreOxygen(SpacesuitConstants.OXYGEN_REFILL_PER_TICK);
        }
    }
}
