package org.chabelabela.outer_crafts.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.equipment.SpacesuitData;
import org.chabelabela.outer_crafts.equipment.SpacesuitConstants;

import java.util.Optional;
import java.util.UUID;

/**
 * Central registry for all NeoForge {@link AttachmentType}s used by Outer Crafts.
 *
 * <p>Replaces the legacy {@code static ConcurrentHashMap<UUID, ...>} singletons that
 * previously stored per-player runtime state. Attachments live on the
 * {@link net.minecraft.world.entity.Entity} (or {@link net.minecraft.world.level.Level} /
 * {@link net.minecraft.world.level.chunk.ChunkAccess}), which gives us:
 *
 * <ul>
 *   <li>Automatic NBT save/load via the registered codec</li>
 *   <li>Optional auto-sync to the owning client via the registered StreamCodec</li>
 *   <li>{@code copyOnDeath()} to persist across respawns</li>
 *   <li>No leaked memory when a player disconnects</li>
 * </ul>
 */
public final class OuterCraftsAttachments {

    private OuterCraftsAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, OuterCrafts.MODID);

    // ── SuitState ───────────────────────────────────────────────────────────
    // Stored on every ServerPlayer. Auto-synced to the owning client so the
    // HUD overlay updates without manual EquipmentNetworking.syncSuitState calls.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SpacesuitData.SuitState>> SUIT_STATE =
            ATTACHMENT_TYPES.register("suit_state",
                    () -> AttachmentType.builder(SpacesuitData.SuitState::new)
                            .serialize(SpacesuitData.SuitState.MAP_CODEC)
                            .sync(SpacesuitData.SuitState.STREAM_CODEC)
                            .copyOnDeath()
                            .build());

    // ── Selected signalscope frequency (string id) ──────────────────────────
    public static final String DEFAULT_FREQUENCY = "outer_wilds_ventures";

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<String>> SIGNALSCOPE_FREQUENCY =
            ATTACHMENT_TYPES.register("signalscope_frequency",
                    () -> AttachmentType.builder(() -> DEFAULT_FREQUENCY)
                            .serialize(simpleStringCodec())
                            .sync(ByteBufCodecs.STRING_UTF8.cast())
                            .copyOnDeath()
                            .build());

    // ── Deployed scout (UUID of the active ScoutEntity, if any) ─────────────
    // Replaces ScoutLauncherItem.DEPLOYED_SCOUTS — the entity itself lives in
    // the world, we just remember which one belongs to which player.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Optional<UUID>>> DEPLOYED_SCOUT =
            ATTACHMENT_TYPES.register("deployed_scout",
                    () -> AttachmentType.<Optional<UUID>>builder(() -> Optional.<UUID>empty())
                            .serialize(deployedScoutCodec())
                            .build());

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static com.mojang.serialization.MapCodec<String> simpleStringCodec() {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("value").forGetter(s -> s)
        ).apply(instance, s -> s));
    }

    private static com.mojang.serialization.MapCodec<Optional<UUID>> deployedScoutCodec() {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                UUIDUtil.CODEC.optionalFieldOf("uuid").forGetter(o -> o)
        ).apply(instance, o -> o));
    }

    /**
     * StreamCodec helper — kept here for consumers that need to register
     * additional sync channels for these attachments.
     */
    public static StreamCodec<RegistryFriendlyByteBuf, SpacesuitData.SuitState> suitStateStreamCodec() {
        return SpacesuitData.SuitState.STREAM_CODEC;
    }

    // Force-load reference for the constants so the static init order is deterministic.
    @SuppressWarnings("unused")
    private static final int FORCE_LOAD_CONSTANTS = SpacesuitConstants.MAX_OXYGEN;
}
