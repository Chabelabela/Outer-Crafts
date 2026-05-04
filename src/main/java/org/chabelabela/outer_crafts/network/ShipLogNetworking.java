package org.chabelabela.outer_crafts.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.timeloop.ShipLogData;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Network channel for ship-log state. The server-side {@link ShipLogData} mirrors
 * its current state to the owning client's {@code ShipLogClientCache} via
 * {@link ShipLogSyncPayload} on these triggers:
 * <ul>
 *   <li>Player login</li>
 *   <li>New rumor / location / translation discovered</li>
 *   <li>Loop reset</li>
 * </ul>
 */
public final class ShipLogNetworking {

    private ShipLogNetworking() {}

    /** Independent protocol version — bump only for ShipLog payload changes. */
    public static final String PROTOCOL_VERSION = "1.0";

    public record ShipLogSyncPayload(
            List<String> rumors,
            List<String> locations,
            List<String> texts,
            int totalLoops
    ) implements CustomPacketPayload {

        public static final Type<ShipLogSyncPayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "ship_log_sync"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ShipLogSyncPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ShipLogSyncPayload::rumors,
                        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ShipLogSyncPayload::locations,
                        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ShipLogSyncPayload::texts,
                        ByteBufCodecs.VAR_INT,                                  ShipLogSyncPayload::totalLoops,
                        ShipLogSyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(ShipLogSyncPayload.TYPE, ShipLogSyncPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    Set<String> rumors = new LinkedHashSet<>(payload.rumors());
                    Set<String> locations = new LinkedHashSet<>(payload.locations());
                    Set<String> texts = new LinkedHashSet<>(payload.texts());
                    org.chabelabela.outer_crafts.client.ship.ShipLogClientCache.fullSync(
                            rumors, locations, texts, payload.totalLoops());
                }));
        OuterCrafts.LOGGER.info("[Outer Crafts] Ship log networking registered.");
    }

    /**
     * Sends the entire ship log for {@code player} to that player's client.
     * Cheap enough to call on every discovery — payload is a few hundred bytes.
     */
    public static void syncToPlayer(ServerPlayer player) {
        ShipLogData.PlayerShipLog log = ShipLogData.getOrCreate(player);
        ShipLogSyncPayload payload = new ShipLogSyncPayload(
                List.copyOf(log.getDiscoveredRumors()),
                List.copyOf(log.getExploredLocations()),
                List.copyOf(log.getTranslatedTexts()),
                log.getTotalLoops()
        );
        PacketDistributor.sendToPlayer(player, payload);
    }
}
