package org.chabelabela.outer_crafts.client.ship;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.chabelabela.outer_crafts.client.equipment.JetpackInputHandler;
import org.chabelabela.outer_crafts.network.ShipNetworking;
import org.chabelabela.outer_crafts.ship.ShipEntity;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side input handler for piloting the ship.
 * <p>
 * When the player is riding a ShipEntity, captures:
 * - W/S: Forward/backward thrust
 * - A/D: Yaw rotation (turn left/right)
 * - Space/Shift: Vertical thrust (up/down)
 * - Q/E: Lateral thrust (strafe left/right)
 * - Mouse Y: Pitch control
 * - L: Open Ship Log
 * <p>
 * Key mappings are registered via {@code RegisterKeyMappingsEvent} in {@code OuterCrafts}.
 * The tick callback is driven by {@code ClientTickEvent.Post} in {@code OuterCrafts.ClientForgeEvents}.
 */
public final class ShipInputHandler {

    private ShipInputHandler() {}

    public static final KeyMapping STRAFE_LEFT_KEY = new KeyMapping(
            "key.outer_crafts.ship_strafe_left",
            GLFW.GLFW_KEY_Q,
            JetpackInputHandler.OUTER_CRAFTS_CATEGORY
    );

    public static final KeyMapping STRAFE_RIGHT_KEY = new KeyMapping(
            "key.outer_crafts.ship_strafe_right",
            GLFW.GLFW_KEY_E,
            JetpackInputHandler.OUTER_CRAFTS_CATEGORY
    );

    public static final KeyMapping SHIP_LOG_KEY = new KeyMapping(
            "key.outer_crafts.ship_log",
            GLFW.GLFW_KEY_L,
            JetpackInputHandler.OUTER_CRAFTS_CATEGORY
    );

    /** Called every client tick from {@code OuterCrafts.ClientForgeEvents}. */
    public static void onClientTick() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;
        if (!(player.getVehicle() instanceof ShipEntity)) return;

        // Ship Log key
        while (SHIP_LOG_KEY.consumeClick()) {
            ShipLogScreen.open();
        }

        // Thrust input
        double forward = 0, lateral = 0, vertical = 0;
        double yawInput = 0, pitchInput = 0;

        if (client.options.keyUp.isDown()) forward += 1.0;
        if (client.options.keyDown.isDown()) forward -= 1.0;
        if (STRAFE_LEFT_KEY.isDown()) lateral -= 1.0;
        if (STRAFE_RIGHT_KEY.isDown()) lateral += 1.0;
        if (client.options.keyJump.isDown()) vertical += 1.0;
        if (client.options.keyShift.isDown()) vertical -= 1.0;

        // A/D for yaw rotation
        if (client.options.keyLeft.isDown()) yawInput -= 1.0;
        if (client.options.keyRight.isDown()) yawInput += 1.0;

        // Mouse Y for pitch — send the player's absolute pitch in degrees.
        // The server SNAPS the ship pitch to this value (no per-tick accumulation),
        // which fixes the runaway-feedback bug where every tick added more pitch.
        pitchInput = player.getXRot();

        // Always send so pitch tracks the player's look even when no thrust input.
        ClientPacketDistributor.sendToServer(new ShipNetworking.ShipInputPayload(
                forward, lateral, vertical, yawInput, pitchInput
        ));
    }

    public static boolean isInShip() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getVehicle() instanceof ShipEntity;
    }
}
