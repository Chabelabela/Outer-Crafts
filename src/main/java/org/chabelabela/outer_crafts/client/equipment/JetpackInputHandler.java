package org.chabelabela.outer_crafts.client.equipment;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.chabelabela.outer_crafts.network.EquipmentNetworking;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side input handler for the jetpack.
 * <p>
 * Captures player input (WASD + Space/Shift) and sends it as a thrust vector
 * to the server each tick while the jetpack is active.
 * <p>
 * Key bindings:
 * - R: Toggle jetpack on/off
 * - Space: Thrust up (along local gravity "up")
 * - Shift: Thrust down
 * - WASD: Forward/backward/strafe
 * <p>
 * Key mappings are registered via {@code RegisterKeyMappingsEvent} in {@code OuterCrafts}.
 * The tick callback is driven by {@code ClientTickEvent.Post} in {@code OuterCrafts.ClientForgeEvents}.
 */
public final class JetpackInputHandler {

    private JetpackInputHandler() {}

    public static final KeyMapping.Category OUTER_CRAFTS_CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath("outer_crafts", "outer_crafts"));

    public static final KeyMapping JETPACK_TOGGLE_KEY = new KeyMapping(
            "key.outer_crafts.jetpack_toggle",
            GLFW.GLFW_KEY_R,
            OUTER_CRAFTS_CATEGORY
    );

    private static boolean jetpackActive = false;

    /** Called every client tick from {@code OuterCrafts.ClientForgeEvents}. */
    public static void onClientTick() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        // Toggle jetpack
        while (JETPACK_TOGGLE_KEY.consumeClick()) {
            jetpackActive = !jetpackActive;
            ClientPacketDistributor.sendToServer(new EquipmentNetworking.JetpackTogglePayload(jetpackActive));
        }

        if (!jetpackActive) return;

        // Build input vector from current movement keys
        double x = 0, y = 0, z = 0;

        if (client.options.keyUp.isDown()) z += 1.0;       // Forward
        if (client.options.keyDown.isDown()) z -= 1.0;      // Backward
        if (client.options.keyLeft.isDown()) x -= 1.0;      // Strafe left
        if (client.options.keyRight.isDown()) x += 1.0;     // Strafe right
        if (client.options.keyJump.isDown()) y += 1.0;      // Up
        if (client.options.keyShift.isDown()) y -= 1.0;     // Down

        // Only send if there's actual input
        if (x != 0 || y != 0 || z != 0) {
            ClientPacketDistributor.sendToServer(new EquipmentNetworking.JetpackInputPayload(x, y, z));
        }
    }

    public static boolean isJetpackActive() {
        return jetpackActive;
    }

    public static void setJetpackActive(boolean active) {
        jetpackActive = active;
    }
}
