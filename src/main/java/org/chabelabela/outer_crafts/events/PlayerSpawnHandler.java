package org.chabelabela.outer_crafts.events;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.physics.CelestialBody;
import org.chabelabela.outer_crafts.registry.CelestialBodyRegistry;
import org.chabelabela.outer_crafts.registry.OuterCraftsItems;
import org.chabelabela.outer_crafts.world.dimension.OuterCraftsDimensions;

import java.util.Set;

/**
 * Pillar 6 — Outer Wilds-style first-join experience.
 *
 * <p>When a player logs in for the first time:
 * <ol>
 *   <li>Teleport them to the surface of Timber Hearth (the canon "campfire" body).</li>
 *   <li>Hand them a starter spacesuit (helmet + chestplate w/ jetpack).</li>
 *   <li>Set their respawn anchor to TH so subsequent loop resets / deaths return them there.</li>
 *   <li>Send a welcoming overlay message.</li>
 * </ol>
 *
 * <p>This relies on a persistent flag (custom NBT tag on the player) so subsequent logins
 * don't re-teleport. We use {@code persistentData()} from NeoForge's
 * {@link Player#getPersistentData()}.
 */
@EventBusSubscriber(modid = OuterCrafts.MODID)
public final class PlayerSpawnHandler {

    private PlayerSpawnHandler() {}

    /** NBT key on persistent player data marking that the intro has run. */
    public static final String PERSISTENT_KEY = OuterCrafts.MODID + ":pillar6_complete";

    private static final String SPAWN_BODY_ID = "timber_hearth";
    /** Y-offset above the body's surface to teleport the player. */
    private static final double SPAWN_HEIGHT_ABOVE_SURFACE = 4.0;

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Idempotency guard
        var data = player.getPersistentData();
        if (data.getBooleanOr(PERSISTENT_KEY, false)) return;

        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        ServerLevel solar = server.getLevel(OuterCraftsDimensions.SOLAR_SYSTEM_LEVEL);
        if (solar == null) {
            OuterCrafts.LOGGER.warn("[Pillar6] Solar system dimension not loaded — skipping intro.");
            return;
        }

        CelestialBody timberHearth = CelestialBodyRegistry.get(SPAWN_BODY_ID);
        if (timberHearth == null) {
            OuterCrafts.LOGGER.warn("[Pillar6] '{}' body not registered — skipping intro.", SPAWN_BODY_ID);
            return;
        }

        Vec3 center = CelestialBodyRegistry.resolveAllPositions(solar.getGameTime()).get(SPAWN_BODY_ID);
        if (center == null) return;

        double surfaceY = center.y + timberHearth.radius() + SPAWN_HEIGHT_ABOVE_SURFACE;
        player.teleportTo(solar, center.x, surfaceY, center.z,
                Set.<Relative>of(), 0.0f, 0.0f, true);
        // The TimeLoopManager will re-place the player at TH on every loop reset,
        // so we don't need to mutate the vanilla RespawnConfig here.

        // Hand out the starter spacesuit (helmet + chestplate with built-in jetpack).
        equipStarterSuit(player);

        data.putBoolean(PERSISTENT_KEY, true);

        player.sendSystemMessage(Component.translatable("message.outer_crafts.pillar6.welcome"));
        OuterCrafts.LOGGER.info("[Pillar6] Welcomed first-join player: {}", player.getName().getString());
    }

    /**
     * Equip helmet + chestplate if those slots are empty.
     * The chestplate-equipped state drives jetpack availability.
     */
    private static void equipStarterSuit(ServerPlayer player) {
        if (player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            ItemStack helmet = makeEquippable(
                    OuterCraftsItems.SPACESUIT_HELMET.get().getDefaultInstance(),
                    EquipmentSlot.HEAD);
            player.setItemSlot(EquipmentSlot.HEAD, helmet);
        }
        if (player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
            ItemStack chest = makeEquippable(
                    OuterCraftsItems.SPACESUIT_CHESTPLATE.get().getDefaultInstance(),
                    EquipmentSlot.CHEST);
            player.setItemSlot(EquipmentSlot.CHEST, chest);
        }
    }

    private static ItemStack makeEquippable(ItemStack stack, EquipmentSlot slot) {
        // The default item already carries the EQUIPPABLE component from registration,
        // but defensively re-set it so the suit equips into the right slot if some other
        // mod stripped the component.
        if (!stack.has(DataComponents.EQUIPPABLE)) {
            stack.set(DataComponents.EQUIPPABLE,
                    Equippable.builder(slot).setAsset(OuterCraftsItems.SPACESUIT_ASSET).build());
        }
        return stack;
    }

    /**
     * Vanilla spawn-anchor blocks (beds, charged respawn anchors) try to override our
     * intent for what counts as "home". For the time loop we want every loop reset
     * to send the player back to TH regardless of bed location, so we leave this
     * subscribed but only react if the spawn-set is happening in the solar system dim.
     */
    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        // Intentionally a no-op — leaving as a hook point for future use.
    }
}
