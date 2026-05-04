package org.chabelabela.outer_crafts.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.timeloop.TimeLoopPhase;

public final class TimeLoopNetworking {

    private TimeLoopNetworking() {}

    public record TimeLoopStatePayload(
            long currentTick,
            int phaseOrdinal,
            int loopCount,
            double sunRadius,
            double deathWaveRadius,
            float sunColorR,
            float sunColorG,
            float sunColorB
    ) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<TimeLoopStatePayload> TYPE =
                new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
                        OuterCrafts.MODID, "time_loop_state"));

        public static final StreamCodec<RegistryFriendlyByteBuf, TimeLoopStatePayload> STREAM_CODEC =
                StreamCodec.of(TimeLoopStatePayload::write, TimeLoopStatePayload::read);

        private static void write(RegistryFriendlyByteBuf buf, TimeLoopStatePayload payload) {
            buf.writeLong(payload.currentTick);
            buf.writeVarInt(payload.phaseOrdinal);
            buf.writeVarInt(payload.loopCount);
            buf.writeDouble(payload.sunRadius);
            buf.writeDouble(payload.deathWaveRadius);
            buf.writeFloat(payload.sunColorR);
            buf.writeFloat(payload.sunColorG);
            buf.writeFloat(payload.sunColorB);
        }

        private static TimeLoopStatePayload read(RegistryFriendlyByteBuf buf) {
            return new TimeLoopStatePayload(
                    buf.readLong(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat()
            );
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public TimeLoopPhase phase() {
            TimeLoopPhase[] values = TimeLoopPhase.values();
            if (phaseOrdinal >= 0 && phaseOrdinal < values.length) {
                return values[phaseOrdinal];
            }
            return TimeLoopPhase.RUNNING;
        }
    }

    /** Time-loop payload schema version. Bump independently of equipment/ship. */
    public static final String PROTOCOL_VERSION = "1.0";

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(TimeLoopStatePayload.TYPE, TimeLoopStatePayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() ->
                        org.chabelabela.outer_crafts.client.timeloop.TimeLoopClientState.updateFromPayload(payload)));

        OuterCrafts.LOGGER.info("[Networking] Registered TimeLoopState S2C payload");
    }

    public static void sendTimeLoopState(
            ServerPlayer player, long currentTick,
            TimeLoopPhase phase, int loopCount
    ) {
        double sunRadius = org.chabelabela.outer_crafts.timeloop.SupernovaManager.getCurrentSunRadius();
        double deathWaveRadius = org.chabelabela.outer_crafts.timeloop.SupernovaManager.getDeathWaveRadius();

        double warningProgress = org.chabelabela.outer_crafts.timeloop.TimeLoopManager.getWarningProgress();
        double supernovaProgress = org.chabelabela.outer_crafts.timeloop.TimeLoopManager.getSupernovaProgress();
        float[] sunColor = org.chabelabela.outer_crafts.timeloop.SupernovaManager.computeSunColor(
                warningProgress, supernovaProgress);

        TimeLoopStatePayload payload = new TimeLoopStatePayload(
                currentTick,
                phase.ordinal(),
                loopCount,
                sunRadius,
                deathWaveRadius,
                sunColor[0], sunColor[1], sunColor[2]
        );

        PacketDistributor.sendToPlayer(player, payload);
    }
}
