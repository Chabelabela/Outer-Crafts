package org.chabelabela.outer_crafts.equipment;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.chabelabela.outer_crafts.timeloop.ShipLogData;
import org.chabelabela.outer_crafts.world.dimension.OuterCraftsDimensions;

import java.util.Set;

/**
 * Advanced Warp Drive (3.8 ending hook).
 *
 * <p>Outer Wilds' endgame artifact: once the player has gathered enough rumors
 * and translated the relevant Nomai texts, they can craft / find this item.
 * Right-clicking it sends the player to the Eye dimension if all conditions are
 * met, otherwise displays a hint about what's still missing.
 *
 * <p>The Eye dimension itself ({@code outer_crafts:the_eye}) needs its own
 * dimension JSON + chunk generator — left as a future-work scaffold here.
 * For now, using the drive without the Eye dim available teleports the player
 * back to TH with a "you need more memories" message.
 */
public class AdvancedWarpDriveItem extends Item {

    /** Minimum number of rumors discovered to allow Eye access. */
    private static final int RUMOR_THRESHOLD = 8;
    /** Minimum number of Nomai texts translated. */
    private static final int TEXT_THRESHOLD = 5;

    public AdvancedWarpDriveItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        ShipLogData.PlayerShipLog log = ShipLogData.getOrCreate(sp);
        int rumors = log.getDiscoveredRumors().size();
        int texts  = log.getTranslatedTexts().size();

        if (rumors < RUMOR_THRESHOLD || texts < TEXT_THRESHOLD) {
            sp.sendSystemMessage(Component.translatable(
                    "message.outer_crafts.warp_drive.incomplete",
                    rumors, RUMOR_THRESHOLD, texts, TEXT_THRESHOLD));
            return InteractionResult.CONSUME;
        }

        // Fully unlocked — try to teleport to the Eye dimension.
        ServerLevel eye = ((ServerLevel) sp.level()).getServer().getLevel(OuterCraftsDimensions.EYE_LEVEL);
        if (eye == null) {
            // Eye dimension not present — log + fall back so the player isn't stranded.
            sp.sendSystemMessage(Component.translatable(
                    "message.outer_crafts.warp_drive.unbuilt"));
            return InteractionResult.CONSUME;
        }
        sp.teleportTo(eye, 0, 64, 0, Set.<Relative>of(), sp.getYRot(), 0f, true);
        sp.sendSystemMessage(Component.translatable(
                "message.outer_crafts.warp_drive.success"));
        return InteractionResult.CONSUME;
    }
}
