package org.chabelabela.outer_crafts.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.equipment.JetpackHandler;
import org.chabelabela.outer_crafts.equipment.SpacesuitData;

public final class EquipmentNetworking {

    private EquipmentNetworking() {}

    public record JetpackInputPayload(double x, double y, double z) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<JetpackInputPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
                        OuterCrafts.MODID, "jetpack_input"));

        public static final StreamCodec<RegistryFriendlyByteBuf, JetpackInputPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeDouble(payload.x);
                            buf.writeDouble(payload.y);
                            buf.writeDouble(payload.z);
                        },
                        buf -> new JetpackInputPayload(
                                buf.readDouble(), buf.readDouble(), buf.readDouble()
                        )
                );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record JetpackTogglePayload(boolean active) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<JetpackTogglePayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
                        OuterCrafts.MODID, "jetpack_toggle"));

        public static final StreamCodec<RegistryFriendlyByteBuf, JetpackTogglePayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> buf.writeBoolean(payload.active),
                        buf -> new JetpackTogglePayload(buf.readBoolean())
                );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SuitStatePayload(
            int oxygen,
            double propellant,
            boolean jetpackActive,
            boolean oxygenCritical,
            boolean propellantCritical
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SuitStatePayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
                        OuterCrafts.MODID, "suit_state"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SuitStatePayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeInt(payload.oxygen);
                            buf.writeDouble(payload.propellant);
                            buf.writeBoolean(payload.jetpackActive);
                            buf.writeBoolean(payload.oxygenCritical);
                            buf.writeBoolean(payload.propellantCritical);
                        },
                        buf -> new SuitStatePayload(
                                buf.readInt(), buf.readDouble(),
                                buf.readBoolean(), buf.readBoolean(), buf.readBoolean()
                        )
                );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record SignalDetectedPayload(
            String frequencyId,
            String signalName,
            float strength,
            float directionYaw,
            float directionPitch
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SignalDetectedPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
                        OuterCrafts.MODID, "signal_detected"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SignalDetectedPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeUtf(payload.frequencyId);
                            buf.writeUtf(payload.signalName);
                            buf.writeFloat(payload.strength);
                            buf.writeFloat(payload.directionYaw);
                            buf.writeFloat(payload.directionPitch);
                        },
                        buf -> new SignalDetectedPayload(
                                buf.readUtf(), buf.readUtf(),
                                buf.readFloat(), buf.readFloat(), buf.readFloat()
                        )
                );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record TranslatorTextPayload(
            String textId,
            String translatedText,
            boolean isNewDiscovery
    ) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<TranslatorTextPayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
                        OuterCrafts.MODID, "translator_text"));

        public static final StreamCodec<RegistryFriendlyByteBuf, TranslatorTextPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeUtf(payload.textId);
                            buf.writeUtf(payload.translatedText);
                            buf.writeBoolean(payload.isNewDiscovery);
                        },
                        buf -> new TranslatorTextPayload(
                                buf.readUtf(), buf.readUtf(), buf.readBoolean()
                        )
                );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * Equipment payload schema version. Bump rules:
     * <ul>
     *   <li>Patch (1.0 → 1.1): adding optional fields to existing payloads</li>
     *   <li>Minor (1.0 → 2.0): adding new payloads, removing old ones</li>
     *   <li>Major: breaking change to existing payload byte layout</li>
     * </ul>
     * Versioned per-feature so a ship-only schema bump doesn't disconnect
     * clients running an older equipment-only update, and vice-versa.
     */
    public static final String PROTOCOL_VERSION = "1.0";

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToServer(JetpackInputPayload.TYPE, JetpackInputPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        JetpackHandler.applyThrust((ServerPlayer) ctx.player(),
                                new Vec3(payload.x(), payload.y(), payload.z()))));

        registrar.playToServer(JetpackTogglePayload.TYPE, JetpackTogglePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    SpacesuitData.SuitState suit = SpacesuitData.getOrCreate((ServerPlayer) ctx.player());
                    suit.setJetpackActive(payload.active());
                }));

        registrar.playToClient(SuitStatePayload.TYPE, SuitStatePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        org.chabelabela.outer_crafts.client.equipment.SpacesuitHudOverlay.updateFromPayload(
                                payload.oxygen(), payload.propellant(), payload.jetpackActive(),
                                payload.oxygenCritical(), payload.propellantCritical())));

        registrar.playToClient(SignalDetectedPayload.TYPE, SignalDetectedPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        org.chabelabela.outer_crafts.client.equipment.SignalscopeHudOverlay.updateFromPayload(
                                payload.frequencyId(), payload.signalName(), payload.strength(),
                                payload.directionYaw(), payload.directionPitch())));

        registrar.playToClient(TranslatorTextPayload.TYPE, TranslatorTextPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        org.chabelabela.outer_crafts.client.equipment.TranslatorHudOverlay.updateFromPayload(
                                payload.textId(), payload.translatedText(), payload.isNewDiscovery())));

        OuterCrafts.LOGGER.info("[Outer Crafts] Equipment networking registered.");
    }

    public static void syncSuitState(ServerPlayer player) {
        SpacesuitData.SuitState state = SpacesuitData.getOrCreate(player);
        PacketDistributor.sendToPlayer(player, new SuitStatePayload(
                state.getOxygen(),
                state.getPropellant(),
                state.isJetpackActive(),
                state.isOxygenCritical(),
                state.isPropellantCritical()
        ));
    }
}
