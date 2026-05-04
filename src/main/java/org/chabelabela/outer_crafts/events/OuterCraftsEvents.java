package org.chabelabela.outer_crafts.events;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.equipment.SpacesuitData;
import org.chabelabela.outer_crafts.network.EquipmentNetworking;
import org.chabelabela.outer_crafts.network.ShipLogNetworking;
import org.chabelabela.outer_crafts.network.ShipNetworking;
import org.chabelabela.outer_crafts.registry.OuterCraftsItems;
import org.chabelabela.outer_crafts.ship.ShipEntity;
import org.chabelabela.outer_crafts.timeloop.ShipLogData;
import org.chabelabela.outer_crafts.timeloop.TimeLoopManager;

import java.nio.file.Path;

/**
 * Consolidated NeoForge event listeners replacing the following Fabric-style mixins:
 * <ul>
 *   <li>{@code PlayerDeathMixin} → {@link LivingDeathEvent}</li>
 *   <li>{@code SpacesuitTickMixin} → {@link EntityTickEvent.Post}</li>
 *   <li>{@code ShipPilotMixin} → {@link EntityTickEvent.Post} (vehicle check)</li>
 *   <li>{@code ServerLifecycleMixin} → {@link ServerStartedEvent}</li>
 * </ul>
 *
 * <p>NeoForge's {@code @EventBusSubscriber} auto-routes based on whether the event
 * implements {@code IModBusEvent}: these all run on the main game bus.
 */
@EventBusSubscriber(modid = OuterCrafts.MODID)
public final class OuterCraftsEvents {

    private OuterCraftsEvents() {}

    // ── Player death → notify time loop ─────────────────────────────────────

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TimeLoopManager.onPlayerDeath(player);
        }
    }

    // ── Per-tick: suit + ship pilot sync ────────────────────────────────────

    /**
     * Fires AFTER each entity ticks. Filters down to ServerPlayer to:
     * <ol>
     *   <li>Tick spacesuit O2/fuel drain and equipment detection</li>
     *   <li>Sync ship state if the player is piloting a {@link ShipEntity}</li>
     * </ol>
     */
    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // ── Spacesuit ────────────────────────────────────────────────
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean hasChestplate = chestplate.is(OuterCraftsItems.SPACESUIT_CHESTPLATE);
        boolean suited = helmet.is(OuterCraftsItems.SPACESUIT_HELMET) || hasChestplate;

        SpacesuitData.SuitState suitState = SpacesuitData.getOrCreate(player);
        suitState.setSuitEquipped(suited);
        suitState.setChestplateEquipped(hasChestplate);

        if (suited) {
            SpacesuitData.tickPlayer(player);
            // Throttle network sync to every 10 ticks (0.5s)
            if (player.tickCount % 10 == 0) {
                EquipmentNetworking.syncSuitState(player);
            }
        }

        // ── Ship pilot sync (5 ticks = 250ms) ────────────────────────
        if (player.getVehicle() instanceof ShipEntity ship && player.tickCount % 5 == 0) {
            ShipNetworking.syncShipState(player, ship);
        }
    }

    // ── Server startup → init ShipLog persistence ───────────────────────────

    // ── Player join → ship-log full sync to client ──────────────────────────

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ShipLogNetworking.syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        Path worldDir = server.getWorldPath(LevelResource.ROOT);
        ShipLogData.init(worldDir);
        OuterCrafts.LOGGER.info("[Outer Crafts] Ship log persistence initialised at {}", worldDir);
    }

    // ── Optional: tag entities joining the solar-system dim for fast filter ─
    // (No-op placeholder: future hook point if we need it.)
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // intentionally empty — kept as a registration anchor
    }
}
