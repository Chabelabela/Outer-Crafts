package org.chabelabela.outer_crafts.equipment;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.chabelabela.outer_crafts.network.EquipmentNetworking;
import org.chabelabela.outer_crafts.registry.CelestialBodyRegistry;
import org.chabelabela.outer_crafts.registry.OuterCraftsAttachments;
import org.chabelabela.outer_crafts.physics.CelestialBody;
import org.chabelabela.outer_crafts.timeloop.ShipLogData;

import java.util.Map;

public class SignalscopeItem extends Item {

    private static final double DETECTION_CONE_DEGREES = 15.0;
    private static final double DETECTION_CONE_COS = Math.cos(Math.toRadians(DETECTION_CONE_DEGREES));

    public SignalscopeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;

        if (player.isShiftKeyDown()) {
            cycleFrequency(player);
            serverPlayer.sendOverlayMessage(
                    Component.translatable("hud.outer_crafts.signalscope.cycled",
                            getSelectedFrequencyName(player))
                            .withStyle(ChatFormatting.AQUA));
            return InteractionResult.SUCCESS;
        }

        scanForSignals(serverPlayer);
        return InteractionResult.SUCCESS;
    }

    /** Reads the player's currently-tuned frequency ID from the data attachment. */
    public static String getSelectedFrequency(Player player) {
        return player.getData(OuterCraftsAttachments.SIGNALSCOPE_FREQUENCY);
    }

    public static String getSelectedFrequencyName(Player player) {
        String freqId = getSelectedFrequency(player);
        SignalFrequency.Frequency freq = SignalFrequency.getFrequency(freqId);
        return freq != null ? freq.displayName() : freqId;
    }

    private void cycleFrequency(Player player) {
        var freqIds = SignalFrequency.allFrequencies().keySet().stream().toList();
        String current = getSelectedFrequency(player);
        int currentIndex = freqIds.indexOf(current);
        String next = freqIds.get((currentIndex + 1) % freqIds.size());
        player.setData(OuterCraftsAttachments.SIGNALSCOPE_FREQUENCY, next);
    }

    private void scanForSignals(ServerPlayer player) {
        Vec3 playerPos = player.position();
        Vec3 lookDir = player.getLookAngle().normalize();
        long tick = player.level().getGameTime();
        Map<String, Vec3> bodyPositions = CelestialBodyRegistry.resolveAllPositions(tick);
        String freqId = getSelectedFrequency(player);

        boolean foundSignal = false;
        for (SignalFrequency.SignalSource source : SignalFrequency.sourcesForFrequency(freqId)) {
            Vec3 sourcePos = resolveSourcePosition(source, bodyPositions);
            if (sourcePos == null) continue;

            Vec3 toSource = sourcePos.subtract(playerPos);
            double distance = toSource.length();

            if (distance > source.maxDetectionRange()) continue;

            Vec3 dirToSource = toSource.normalize();
            double dot = lookDir.dot(dirToSource);
            if (dot < DETECTION_CONE_COS) continue;

            float strength = (float) (dot * (1.0 - distance / source.maxDetectionRange()));
            strength = Math.clamp(strength, 0.0f, 1.0f);

            float yaw = (float) Math.toDegrees(Math.atan2(-toSource.x, toSource.z));
            float pitch = (float) Math.toDegrees(Math.atan2(toSource.y, Math.sqrt(toSource.x * toSource.x + toSource.z * toSource.z)));

            PacketDistributor.sendToPlayer(player, new EquipmentNetworking.SignalDetectedPayload(
                    source.frequencyId(), source.displayName(), strength, yaw, pitch
            ));

            ShipLogData.PlayerShipLog log = ShipLogData.getOrCreate(player);
            int prevFreqs = log.getDiscoveredFrequencies().size();
            log.addFrequency(source.frequencyId());
            // Only push a sync if we genuinely added a new frequency.
            if (log.getDiscoveredFrequencies().size() != prevFreqs) {
                ShipLogData.markDirty(player);
            }
            foundSignal = true;
        }

        if (!foundSignal) {
            player.sendOverlayMessage(
                    Component.translatable("hud.outer_crafts.signalscope.no_signal",
                            getSelectedFrequencyName(player))
                            .withStyle(ChatFormatting.GRAY));
        }
    }

    private Vec3 resolveSourcePosition(SignalFrequency.SignalSource source, Map<String, Vec3> bodyPositions) {
        if (source.celestialBodyId() == null) {
            return source.position();
        }
        Vec3 bodyCenter = bodyPositions.get(source.celestialBodyId());
        if (bodyCenter == null) return null;
        return bodyCenter.add(source.position());
    }
}
