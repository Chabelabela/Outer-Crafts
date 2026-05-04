package org.chabelabela.outer_crafts.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.ship.ShipEntity;

public final class ShipNetworking {

    private ShipNetworking() {}

    public record ShipInputPayload(
            double forward, double lateral, double vertical,
            double yawInput, double pitchInput
    ) implements CustomPacketPayload {
        public static final Type<ShipInputPayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "ship_input"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ShipInputPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, p) -> {
                            buf.writeDouble(p.forward);
                            buf.writeDouble(p.lateral);
                            buf.writeDouble(p.vertical);
                            buf.writeDouble(p.yawInput);
                            buf.writeDouble(p.pitchInput);
                        },
                        buf -> new ShipInputPayload(
                                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                                buf.readDouble(), buf.readDouble()
                        )
                );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record ShipStatePayload(
            float fuel,
            boolean engineOn,
            boolean landed,
            boolean destroyed,
            int shipOxygen,
            float velocityMagnitude,
            float hullHealth,
            float engineHealth,
            float leftThrusterHealth,
            float rightThrusterHealth,
            float cockpitHealth,
            float oxygenModuleHealth
    ) implements CustomPacketPayload {
        public static final Type<ShipStatePayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "ship_state"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ShipStatePayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, p) -> {
                            buf.writeFloat(p.fuel);
                            buf.writeBoolean(p.engineOn);
                            buf.writeBoolean(p.landed);
                            buf.writeBoolean(p.destroyed);
                            buf.writeInt(p.shipOxygen);
                            buf.writeFloat(p.velocityMagnitude);
                            buf.writeFloat(p.hullHealth);
                            buf.writeFloat(p.engineHealth);
                            buf.writeFloat(p.leftThrusterHealth);
                            buf.writeFloat(p.rightThrusterHealth);
                            buf.writeFloat(p.cockpitHealth);
                            buf.writeFloat(p.oxygenModuleHealth);
                        },
                        buf -> new ShipStatePayload(
                                buf.readFloat(), buf.readBoolean(), buf.readBoolean(),
                                buf.readBoolean(), buf.readInt(), buf.readFloat(),
                                buf.readFloat(), buf.readFloat(), buf.readFloat(),
                                buf.readFloat(), buf.readFloat(), buf.readFloat()
                        )
                );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record OpenShipLogPayload(boolean mapMode) implements CustomPacketPayload {
        public static final Type<OpenShipLogPayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "open_ship_log"));

        public static final StreamCodec<RegistryFriendlyByteBuf, OpenShipLogPayload> STREAM_CODEC =
                StreamCodec.of(
                        (buf, p) -> buf.writeBoolean(p.mapMode),
                        buf -> new OpenShipLogPayload(buf.readBoolean())
                );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Ship payload schema version. Bump independently of equipment/timeloop. */
    public static final String PROTOCOL_VERSION = "1.0";

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToServer(ShipInputPayload.TYPE, ShipInputPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player().getVehicle() instanceof ShipEntity ship) {
                        ship.applyThrustInput(
                                payload.forward(), payload.lateral(), payload.vertical(),
                                payload.yawInput(), payload.pitchInput()
                        );
                    }
                }));

        registrar.playToClient(ShipStatePayload.TYPE, ShipStatePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        org.chabelabela.outer_crafts.client.ship.ShipHudOverlay.updateFromPayload(payload)));

        registrar.playToClient(OpenShipLogPayload.TYPE, OpenShipLogPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        net.minecraft.client.Minecraft.getInstance().setScreen(
                                new org.chabelabela.outer_crafts.client.ship.ShipLogScreen(payload.mapMode()))));

        OuterCrafts.LOGGER.info("[Outer Crafts] Ship networking registered.");
    }

    public static void syncShipState(ServerPlayer player, ShipEntity ship) {
        PacketDistributor.sendToPlayer(player, new ShipStatePayload(
                ship.getFuel(),
                ship.isEngineOn(),
                ship.isLanded(),
                ship.isDestroyed(),
                ship.getShipOxygen(),
                (float) ship.getDeltaMovement().length(),
                ship.getDamageSystem().get("hull").getHealthPercent(),
                ship.getDamageSystem().get("engine").getHealthPercent(),
                ship.getDamageSystem().get("thruster_left").getHealthPercent(),
                ship.getDamageSystem().get("thruster_right").getHealthPercent(),
                ship.getDamageSystem().get("cockpit").getHealthPercent(),
                ship.getDamageSystem().get("oxygen_module").getHealthPercent()
        ));
    }
}
