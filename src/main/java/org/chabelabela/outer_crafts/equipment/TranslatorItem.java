package org.chabelabela.outer_crafts.equipment;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.chabelabela.outer_crafts.network.EquipmentNetworking;
import org.chabelabela.outer_crafts.registry.OuterCraftsBlocks;
import org.chabelabela.outer_crafts.timeloop.ShipLogData;
import org.chabelabela.outer_crafts.world.block.NomaiTextBlock;

public class TranslatorItem extends Item {

    public TranslatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.PASS;

        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof NomaiTextBlock nomaiBlock)) {
            return InteractionResult.PASS;
        }

        ServerPlayer player = (ServerPlayer) context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        String textId = nomaiBlock.getTextId(level, pos);
        String translatedText = NomaiTextRegistry.getTranslation(textId);

        if (translatedText == null) {
            translatedText = "[ Corrupted text - unable to translate ]";
        }

        ShipLogData.PlayerShipLog log = ShipLogData.getOrCreate(player);
        boolean isNew = !log.getTranslatedTexts().contains(textId);
        log.addTranslatedText(textId);

        String rumorId = NomaiTextRegistry.getAssociatedRumor(textId);
        if (rumorId != null) {
            log.addRumor(rumorId);
        }

        // Push the updated log to the client so the Ship Log UI sees it.
        ShipLogData.markDirty(player);

        PacketDistributor.sendToPlayer(player, new EquipmentNetworking.TranslatorTextPayload(
                textId, translatedText, isNew
        ));

        return InteractionResult.SUCCESS;
    }
}
