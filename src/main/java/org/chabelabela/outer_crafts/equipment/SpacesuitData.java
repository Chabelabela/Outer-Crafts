package org.chabelabela.outer_crafts.equipment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.physics.GravityEntityAccess;
import org.chabelabela.outer_crafts.registry.OuterCraftsAttachments;

import java.util.Set;

/**
 * Spacesuit per-player runtime state (oxygen, propellant, jetpack flag, equipped flags).
 *
 * <p>State is stored as a NeoForge {@link net.neoforged.neoforge.attachment.AttachmentType}
 * on the {@link ServerPlayer} (see {@link OuterCraftsAttachments#SUIT_STATE}). This file
 * exposes:
 * <ul>
 *   <li>The {@link SuitState} record/class with mutators</li>
 *   <li>{@link #MAP_CODEC} for save/load</li>
 *   <li>{@link #STREAM_CODEC} for client sync</li>
 *   <li>{@link #getOrCreate(ServerPlayer)} — convenience wrapper around {@code getData()}</li>
 *   <li>{@link #tickPlayer(ServerPlayer)} — per-tick oxygen drain</li>
 * </ul>
 */
public final class SpacesuitData {

    private SpacesuitData() {}

    private static final Set<String> BREATHABLE = Set.of(SpacesuitConstants.BREATHABLE_BODIES);

    /**
     * Mutable spacesuit state. Mutations are in-place — the attachment system
     * tracks the same instance per player; calling {@code setData} again is
     * unnecessary unless you intend to replace the whole record.
     */
    public static final class SuitState {
        private int oxygen;
        private double propellant;
        private boolean jetpackActive;
        private boolean suitEquipped;
        private boolean chestplateEquipped;

        /** Default constructor used by {@code AttachmentType.builder(...)}. */
        public SuitState() {
            this(SpacesuitConstants.MAX_OXYGEN, SpacesuitConstants.MAX_PROPELLANT,
                    false, false, false);
        }

        public SuitState(int oxygen, double propellant,
                         boolean jetpackActive, boolean suitEquipped, boolean chestplateEquipped) {
            this.oxygen = oxygen;
            this.propellant = propellant;
            this.jetpackActive = jetpackActive;
            this.suitEquipped = suitEquipped;
            this.chestplateEquipped = chestplateEquipped;
        }

        public int getOxygen() { return oxygen; }
        public double getPropellant() { return propellant; }
        public boolean isJetpackActive() { return jetpackActive; }
        public boolean isSuitEquipped() { return suitEquipped; }
        public boolean isChestplateEquipped() { return chestplateEquipped; }

        public void setJetpackActive(boolean active) { this.jetpackActive = active; }
        public void setSuitEquipped(boolean equipped) { this.suitEquipped = equipped; }
        public void setChestplateEquipped(boolean equipped) { this.chestplateEquipped = equipped; }

        public double getOxygenPercent() {
            return (double) oxygen / SpacesuitConstants.MAX_OXYGEN;
        }

        public double getPropellantPercent() {
            return propellant / SpacesuitConstants.MAX_PROPELLANT;
        }

        public boolean isOxygenCritical() {
            return oxygen < SpacesuitConstants.MAX_OXYGEN * 0.15;
        }

        public boolean isPropellantCritical() {
            return propellant < SpacesuitConstants.MAX_PROPELLANT * 0.15;
        }

        public boolean drainOxygen(int amount) {
            oxygen = Math.max(0, oxygen - amount);
            return oxygen <= 0;
        }

        public void restoreOxygen(int amount) {
            oxygen = Math.min(SpacesuitConstants.MAX_OXYGEN, oxygen + amount);
        }

        public boolean drainPropellant(double amount) {
            if (propellant < amount) return false;
            propellant = Math.max(0.0, propellant - amount);
            return true;
        }

        public void restorePropellant(double amount) {
            propellant = Math.min(SpacesuitConstants.MAX_PROPELLANT, propellant + amount);
        }

        public void reset() {
            oxygen = SpacesuitConstants.MAX_OXYGEN;
            propellant = SpacesuitConstants.MAX_PROPELLANT;
            jetpackActive = false;
        }

        // ── Codecs (used by AttachmentType registration) ───────────────

        public static final MapCodec<SuitState> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.fieldOf("oxygen").forGetter(SuitState::getOxygen),
                Codec.DOUBLE.fieldOf("propellant").forGetter(SuitState::getPropellant),
                Codec.BOOL.fieldOf("jetpack_active").forGetter(SuitState::isJetpackActive),
                Codec.BOOL.fieldOf("suit_equipped").forGetter(SuitState::isSuitEquipped),
                Codec.BOOL.fieldOf("chestplate_equipped").forGetter(SuitState::isChestplateEquipped)
        ).apply(instance, SuitState::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, SuitState> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT,    SuitState::getOxygen,
                ByteBufCodecs.DOUBLE,     SuitState::getPropellant,
                ByteBufCodecs.BOOL,       SuitState::isJetpackActive,
                ByteBufCodecs.BOOL,       SuitState::isSuitEquipped,
                ByteBufCodecs.BOOL,       SuitState::isChestplateEquipped,
                SuitState::new);
    }

    // ── Public access ──────────────────────────────────────────────────

    /**
     * Returns the {@link SuitState} attached to {@code player}, creating one with
     * default values on first access. Mutations made through the returned object
     * are persisted automatically by NeoForge's attachment framework.
     */
    public static SuitState getOrCreate(ServerPlayer player) {
        return player.getData(OuterCraftsAttachments.SUIT_STATE);
    }

    /**
     * Per-tick maintenance. Called from {@link org.chabelabela.outer_crafts.events.OuterCraftsEvents}
     * for every server player that is wearing the suit.
     */
    public static void tickPlayer(ServerPlayer player) {
        SuitState state = getOrCreate(player);
        if (!state.suitEquipped) return;

        String bodyId = ((GravityEntityAccess) player).outerCrafts$getDominantBodyId();
        boolean needsOxygen = bodyId == null || !BREATHABLE.contains(bodyId);

        if (needsOxygen) {
            boolean depleted = state.drainOxygen(SpacesuitConstants.OXYGEN_DRAIN_PER_TICK);
            if (depleted && player.tickCount % 40 == 0) {
                player.hurt(player.damageSources().drown(), 2.0f);
            }
        }
    }

    /**
     * Reset every online player's suit state — called after a loop reset.
     */
    public static void resetAll(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            getOrCreate(player).reset();
        }
        OuterCrafts.LOGGER.debug("[Spacesuit] All suit states reset for new loop.");
    }
}
