package org.chabelabela.outer_crafts.world.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.chabelabela.outer_crafts.timeloop.MemoryStatueRegistry;

/**
 * Memory Statue (3.10).
 *
 * <p>Outer Wilds-style "pairing" device. Right-clicking pairs the player with this
 * statue; on subsequent deaths the {@code MemoryMaskOverlay} can play a recap of
 * what they discovered in the last loop, "transmitting" the memory back from the
 * statue to the player's mind across loops.
 *
 * <p>The pairing data is stored in {@link MemoryStatueRegistry} which is itself
 * a thin wrapper over per-player attachments — survives reloads and respawns.
 */
public class MemoryStatueBlock extends Block {

    public static final MapCodec<MemoryStatueBlock> CODEC = simpleCodec(MemoryStatueBlock::new);

    public MemoryStatueBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        boolean nowPaired = MemoryStatueRegistry.togglePairing(sp);
        sp.sendSystemMessage(Component.translatable(nowPaired
                ? "message.outer_crafts.memory_statue.paired"
                : "message.outer_crafts.memory_statue.unpaired"));
        return InteractionResult.CONSUME;
    }
}
