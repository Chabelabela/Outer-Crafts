package org.chabelabela.outer_crafts.timeloop;

import net.minecraft.server.level.ServerPlayer;

/**
 * Tracks which players are currently "paired" with a Memory Statue.
 *
 * <p>Pairing is the OW mechanic where touching a statue lets your memories
 * transfer back across the time loop. We model it as a simple boolean per
 * player stored in the player's persistent NBT (so it survives loop resets,
 * disconnects, and world reloads without needing a custom AttachmentType).
 */
public final class MemoryStatueRegistry {

    private MemoryStatueRegistry() {}

    private static final String NBT_KEY = "outer_crafts:memory_paired";

    /** Returns the pairing state for a player. */
    public static boolean isPaired(ServerPlayer player) {
        return player.getPersistentData().getBooleanOr(NBT_KEY, false);
    }

    /**
     * Toggles the pairing state and returns the new value.
     */
    public static boolean togglePairing(ServerPlayer player) {
        boolean newState = !isPaired(player);
        player.getPersistentData().putBoolean(NBT_KEY, newState);
        return newState;
    }

    /** Force-sets a pairing state. */
    public static void setPaired(ServerPlayer player, boolean paired) {
        player.getPersistentData().putBoolean(NBT_KEY, paired);
    }
}
