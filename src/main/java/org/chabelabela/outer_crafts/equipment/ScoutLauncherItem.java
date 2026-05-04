package org.chabelabela.outer_crafts.equipment;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.chabelabela.outer_crafts.entity.ScoutEntity;
import org.chabelabela.outer_crafts.registry.OuterCraftsAttachments;
import org.chabelabela.outer_crafts.registry.OuterCraftsEntities;

import java.util.Optional;
import java.util.UUID;

/**
 * Scout Launcher: launches a scout drone, takes photos, and recalls.
 *
 * <p>The deployed scout is tracked via a {@link OuterCraftsAttachments#DEPLOYED_SCOUT}
 * attachment storing the scout entity's UUID. The actual entity lives in the world,
 * so we look it up via {@link ServerLevel#getEntity(UUID)} on each interaction.
 *
 * <p>This replaces the legacy {@code static Map<UUID, ScoutEntity>} which leaked
 * memory on disconnect and didn't survive world reloads.
 */
public class ScoutLauncherItem extends Item {

    public ScoutLauncherItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        ServerLevel serverLevel = (ServerLevel) level;

        ScoutEntity existing = lookupDeployedScout(serverPlayer, serverLevel);

        // Sneak + use → take photo with the deployed scout
        if (player.isShiftKeyDown() && existing != null) {
            ScoutEntity.PhotoResult photo = existing.takePhoto();
            if (photo.observedQuantum()) {
                serverPlayer.sendOverlayMessage(
                        Component.translatable("hud.outer_crafts.scout.quantum_locked")
                                .withStyle(ChatFormatting.AQUA)
                );
            } else {
                serverPlayer.sendOverlayMessage(
                        Component.translatable("hud.outer_crafts.scout.photo_taken")
                                .withStyle(ChatFormatting.GRAY)
                );
            }
            return InteractionResult.SUCCESS;
        }

        // Use again → recall (kill) the existing scout
        if (existing != null) {
            existing.discard();
            setDeployedScout(serverPlayer, null);
            serverPlayer.sendOverlayMessage(
                    Component.translatable("hud.outer_crafts.scout.recalled")
                            .withStyle(ChatFormatting.GRAY)
            );
            return InteractionResult.SUCCESS;
        }

        // Otherwise: launch a new scout
        ScoutEntity scout = new ScoutEntity(OuterCraftsEntities.SCOUT.get(), serverLevel);
        scout.launchFrom(player);
        serverLevel.addFreshEntity(scout);
        setDeployedScout(serverPlayer, scout.getUUID());

        serverPlayer.sendOverlayMessage(
                Component.translatable("hud.outer_crafts.scout.launched")
                        .withStyle(ChatFormatting.GREEN)
        );

        return InteractionResult.CONSUME;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Looks up the player's currently-deployed scout entity, or {@code null} if none
     * is alive. Auto-clears the attachment if the entity has despawned.
     */
    public static ScoutEntity lookupDeployedScout(ServerPlayer player, ServerLevel level) {
        Optional<UUID> uuid = player.getData(OuterCraftsAttachments.DEPLOYED_SCOUT);
        if (uuid.isEmpty()) return null;

        Entity entity = level.getEntity(uuid.get());
        if (entity instanceof ScoutEntity scout && scout.isAlive()) {
            return scout;
        }
        // Stale reference: clear it
        setDeployedScout(player, null);
        return null;
    }

    private static void setDeployedScout(ServerPlayer player, UUID uuid) {
        player.setData(OuterCraftsAttachments.DEPLOYED_SCOUT, Optional.ofNullable(uuid));
    }
}
