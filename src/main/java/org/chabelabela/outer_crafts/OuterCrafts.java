package org.chabelabela.outer_crafts;

import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import org.chabelabela.outer_crafts.client.entity.ScoutModel;
import org.chabelabela.outer_crafts.client.entity.ShipModel;
import org.chabelabela.outer_crafts.entity.AnglerfishEntity;
import org.chabelabela.outer_crafts.client.equipment.JetpackInputHandler;
import org.chabelabela.outer_crafts.client.equipment.SignalscopeHudOverlay;
import org.chabelabela.outer_crafts.client.equipment.SpacesuitHudOverlay;
import org.chabelabela.outer_crafts.client.equipment.TranslatorHudOverlay;
import org.chabelabela.outer_crafts.client.render.ScoutRenderer;
import org.chabelabela.outer_crafts.client.render.ShipRenderer;
import org.chabelabela.outer_crafts.client.ship.ShipHudOverlay;
import org.chabelabela.outer_crafts.client.ship.ShipInputHandler;
import org.chabelabela.outer_crafts.client.sky.SpaceSkyRenderer;
import org.chabelabela.outer_crafts.client.timeloop.MemoryMaskOverlay;
import org.chabelabela.outer_crafts.equipment.NomaiTextRegistry;
import org.chabelabela.outer_crafts.equipment.SignalFrequency;
import org.chabelabela.outer_crafts.network.EquipmentNetworking;
import org.chabelabela.outer_crafts.network.ShipLogNetworking;
import org.chabelabela.outer_crafts.network.ShipNetworking;
import org.chabelabela.outer_crafts.network.TimeLoopNetworking;
import org.chabelabela.outer_crafts.registry.CelestialBodyRegistry;
import org.chabelabela.outer_crafts.registry.OuterCraftsAttachments;
import org.chabelabela.outer_crafts.registry.OuterCraftsBlocks;
import org.chabelabela.outer_crafts.registry.OuterCraftsEntities;
import org.chabelabela.outer_crafts.registry.OuterCraftsItemGroups;
import org.chabelabela.outer_crafts.registry.OuterCraftsItems;
import org.chabelabela.outer_crafts.timeloop.TimeLoopManager;
import org.chabelabela.outer_crafts.world.dimension.OuterCraftsDimensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(OuterCrafts.MODID)
public class OuterCrafts {

    public static final String MODID = "outer_crafts";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public OuterCrafts(IEventBus modEventBus) {
        LOGGER.info("[Outer Crafts] Initializing core systems...");

        // ── Registries ──────────────────────────────────────────────────
        OuterCraftsItems.ITEMS.register(modEventBus);
        OuterCraftsBlocks.BLOCKS.register(modEventBus);
        OuterCraftsEntities.ENTITIES.register(modEventBus);
        OuterCraftsItemGroups.TABS.register(modEventBus);
        OuterCraftsAttachments.ATTACHMENT_TYPES.register(modEventBus);

        // ── Mob attributes ───────────────────────────────────────────────
        modEventBus.addListener(OuterCrafts::onCreateMobAttributes);

        // ── Networking payloads ──────────────────────────────────────────
        modEventBus.addListener(EquipmentNetworking::register);
        modEventBus.addListener(ShipNetworking::register);
        modEventBus.addListener(TimeLoopNetworking::register);
        modEventBus.addListener(ShipLogNetworking::register);

        // ── Chunk generator codec (must run during RegisterEvent, before registries freeze) ─
        modEventBus.addListener(OuterCraftsDimensions::onRegister);

        // ── Static content registries ────────────────────────────────────
        // CelestialBodies are now datapack-driven via CelestialBodyLoader,
        // which subscribes to AddReloadListenerEvent. The hardcoded defaults
        // remain available as a fallback (no datapack present) — see below.
        CelestialBodyRegistry.registerDefaults();
        SignalFrequency.registerDefaults();
        NomaiTextRegistry.registerDefaults();

        // ── Server-side game events ──────────────────────────────────────
        NeoForge.EVENT_BUS.addListener(TimeLoopManager::onServerTickEnd);

        LOGGER.info("[Outer Crafts] All core systems loaded.");
    }

    /** Mod-bus event: register attributes for all our LivingEntity subclasses. */
    private static void onCreateMobAttributes(EntityAttributeCreationEvent event) {
        event.put(OuterCraftsEntities.ANGLERFISH.get(), AnglerfishEntity.createAttributes().build());
    }

    // ── Mod bus client events (entity renderers, GUI layers, key mappings) ──
    // EventBusSubscriber auto-routes: IModBusEvent → mod bus, others → game bus.

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(OuterCraftsEntities.SCOUT.get(), ScoutRenderer::new);
            event.registerEntityRenderer(OuterCraftsEntities.SHIP.get(), ShipRenderer::new);
        }

        @SubscribeEvent
        public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(ScoutModel.LAYER, ScoutModel::createBodyLayer);
            event.registerLayerDefinition(ShipModel.LAYER, ShipModel::createBodyLayer);
        }

        @SubscribeEvent
        public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
            // Memory mask / supernova overlay (time loop)
            event.registerBelowAll(
                    Identifier.fromNamespaceAndPath(MODID, "memory_mask_overlay"),
                    (graphics, delta) -> MemoryMaskOverlay.render(graphics, delta));
            // Spacesuit O2 + fuel bars
            event.registerBelowAll(
                    Identifier.fromNamespaceAndPath(MODID, "spacesuit_hud"),
                    (graphics, delta) -> SpacesuitHudOverlay.render(graphics, delta));
            // Signalscope signal readout
            event.registerBelowAll(
                    Identifier.fromNamespaceAndPath(MODID, "signalscope_hud"),
                    (graphics, delta) -> SignalscopeHudOverlay.render(graphics, delta));
            // Nomai translator overlay
            event.registerBelowAll(
                    Identifier.fromNamespaceAndPath(MODID, "translator_hud"),
                    (graphics, delta) -> TranslatorHudOverlay.render(graphics, delta));
            // Ship cockpit HUD
            event.registerBelowAll(
                    Identifier.fromNamespaceAndPath(MODID, "ship_hud"),
                    (graphics, delta) -> ShipHudOverlay.render(graphics, delta));
        }

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.registerCategory(JetpackInputHandler.OUTER_CRAFTS_CATEGORY);
            event.register(JetpackInputHandler.JETPACK_TOGGLE_KEY);
            event.register(ShipInputHandler.STRAFE_LEFT_KEY);
            event.register(ShipInputHandler.STRAFE_RIGHT_KEY);
            event.register(ShipInputHandler.SHIP_LOG_KEY);
        }
    }

    // ── NeoForge bus client events (tick, render stage) ─────────────────────

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientForgeEvents {

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            JetpackInputHandler.onClientTick();
            ShipInputHandler.onClientTick();
        }

        @SubscribeEvent
        public static void onRenderLevelStage(RenderLevelStageEvent.AfterSky event) {
            // Render planets as 3D spheres at their actual world positions,
            // after the vanilla sky pass and before world geometry — so they
            // sit behind solid blocks (correct occlusion) but in front of the
            // void/star backdrop.
            SpaceSkyRenderer.onRenderLevelStage(event);
        }
    }
}
