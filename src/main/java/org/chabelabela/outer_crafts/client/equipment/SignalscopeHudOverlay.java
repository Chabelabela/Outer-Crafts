package org.chabelabela.outer_crafts.client.equipment;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.chabelabela.outer_crafts.equipment.SignalFrequency;
import org.chabelabela.outer_crafts.registry.OuterCraftsAttachments;

/**
 * Client-side HUD overlay for the Signalscope.
 *
 * <p>Two-tier display:
 * <ul>
 *   <li>The currently-tuned frequency is read from the local player's
 *       {@link OuterCraftsAttachments#SIGNALSCOPE_FREQUENCY} attachment and is
 *       rendered as a small label whenever the player is holding the signalscope.</li>
 *   <li>When a signal detection packet arrives (via
 *       {@link org.chabelabela.outer_crafts.network.EquipmentNetworking}),
 *       a more detailed readout (signal name, strength bar, direction indicator)
 *       fades in for {@value #DISPLAY_DURATION_MS} ms.</li>
 * </ul>
 *
 * <p>Registered via {@code RegisterGuiLayersEvent} in {@code OuterCrafts.ClientModEvents}.
 */
public final class SignalscopeHudOverlay {

    private SignalscopeHudOverlay() {}

    private static String currentFrequency = "";
    private static String currentSignalName = "";
    private static float signalStrength = 0.0f;
    private static float directionYaw = 0.0f;
    @SuppressWarnings("unused")
    private static float directionPitch = 0.0f;
    private static long lastUpdateTime = 0;

    private static final long DISPLAY_DURATION_MS = 3000;

    /** Called from {@code EquipmentNetworking} when a signal is detected. */
    public static void updateFromPayload(String frequencyId, String signalName,
                                         float strength, float yaw, float pitch) {
        currentFrequency = frequencyId;
        currentSignalName = signalName;
        signalStrength = strength;
        directionYaw = yaw;
        directionPitch = pitch;
        lastUpdateTime = System.currentTimeMillis();
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        if (!isHoldingSignalscope(mc.player)) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int centerX = screenWidth / 2;
        int y = 22;

        // ── Always-visible: currently-tuned frequency ──────────────
        String tunedId = mc.player.getData(OuterCraftsAttachments.SIGNALSCOPE_FREQUENCY);
        SignalFrequency.Frequency tunedFreq = SignalFrequency.getFrequency(tunedId);
        String tunedName = tunedFreq != null ? tunedFreq.displayName() : tunedId.toUpperCase();
        // Bicolor (gray label + aqua name) is baked into the lang string via §-codes.
        String tunedLabel = Component.translatable(
                "hud.outer_crafts.signalscope.tuned", tunedName).getString();
        int tunedWidth = mc.font.width(tunedLabel);
        graphics.text(mc.font, tunedLabel, centerX - tunedWidth / 2, y, 0xFFFFFFFF, true);

        // ── Recent-detection readout ───────────────────────────────
        long elapsed = System.currentTimeMillis() - lastUpdateTime;
        if (elapsed > DISPLAY_DURATION_MS || currentSignalName.isEmpty()) return;

        // Fade out over the last second
        float alpha = elapsed > DISPLAY_DURATION_MS - 1000
                ? (float) (DISPLAY_DURATION_MS - elapsed) / 1000.0f
                : 1.0f;
        int alphaInt = (int) (alpha * 255) << 24;

        int detectY = y + 14;

        int nameWidth = mc.font.width(currentSignalName);
        graphics.text(mc.font, currentSignalName,
                centerX - nameWidth / 2, detectY, 0x00FFFFFF | alphaInt, true);

        // Signal strength bar
        int barWidth = 60;
        int barX = centerX - barWidth / 2;
        int barY = detectY + 12;
        graphics.fill(barX, barY, barX + barWidth, barY + 4, 0x00333333 | alphaInt);
        int filledWidth = (int) (barWidth * signalStrength);
        if (filledWidth > 0) {
            graphics.fill(barX, barY, barX + filledWidth, barY + 4, 0x0044FF44 | alphaInt);
        }

        // Direction indicator
        int circleX = centerX;
        int circleY = detectY + 30;
        int radius = 15;
        drawDirectionIndicator(graphics, mc, circleX, circleY, radius, alphaInt);
    }

    private static boolean isHoldingSignalscope(Player player) {
        var main = player.getMainHandItem();
        var off = player.getOffhandItem();
        return main.is(org.chabelabela.outer_crafts.registry.OuterCraftsItems.SIGNALSCOPE.get())
                || off.is(org.chabelabela.outer_crafts.registry.OuterCraftsItems.SIGNALSCOPE.get());
    }

    private static void drawDirectionIndicator(GuiGraphicsExtractor graphics, Minecraft mc,
                                                int cx, int cy, int radius, int alpha) {
        graphics.outline(cx - radius, cy - radius, radius * 2, radius * 2, 0x00888888 | alpha);

        if (mc.player == null) return;

        float playerYaw = mc.player.getYRot();
        float relativeYaw = directionYaw - playerYaw;
        float radYaw = (float) Math.toRadians(relativeYaw);

        int dotX = cx + (int) (Math.sin(radYaw) * radius * 0.8);
        int dotY = cy - (int) (Math.cos(radYaw) * radius * 0.8);

        graphics.fill(dotX - 2, dotY - 2, dotX + 2, dotY + 2, 0x0044FF44 | alpha);
    }
}
