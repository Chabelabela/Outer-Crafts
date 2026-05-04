package org.chabelabela.outer_crafts.client.equipment;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.chabelabela.outer_crafts.equipment.SpacesuitConstants;

/**
 * Client-side HUD overlay for the spacesuit: displays Oxygen and Propellant bars.
 * <p>
 * Rendered in the top-right corner of the screen (per playtest feedback v2).
 * Bars turn red and flash when resources are critically low.
 * <p>
 * Registered via {@code RegisterGuiLayersEvent} in {@code OuterCrafts.ClientModEvents}.
 */
public final class SpacesuitHudOverlay {

    private SpacesuitHudOverlay() {}

    // ── Client-side state (updated from network packets) ────────────────
    private static int oxygen = SpacesuitConstants.MAX_OXYGEN;
    private static double propellant = SpacesuitConstants.MAX_PROPELLANT;
    private static boolean jetpackActive = false;
    private static boolean oxygenCritical = false;
    private static boolean propellantCritical = false;
    private static boolean suitActive = false;

    public static void updateFromPayload(int oxygen, double propellant,
                                          boolean jetpackActive,
                                          boolean oxygenCritical,
                                          boolean propellantCritical) {
        SpacesuitHudOverlay.oxygen = oxygen;
        SpacesuitHudOverlay.propellant = propellant;
        SpacesuitHudOverlay.jetpackActive = jetpackActive;
        SpacesuitHudOverlay.oxygenCritical = oxygenCritical;
        SpacesuitHudOverlay.propellantCritical = propellantCritical;
        SpacesuitHudOverlay.suitActive = true;
        // Keep the input handler in sync with server-confirmed state
        JetpackInputHandler.setJetpackActive(jetpackActive);
    }

    public static void deactivate() {
        suitActive = false;
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!suitActive) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int barWidth = 80;
        String fuelLabel = Component.translatable("hud.outer_crafts.fuel").getString();
        String oxygenLabel = Component.translatable("hud.outer_crafts.oxygen").getString();
        int labelWidth = mc.font.width(fuelLabel) + 4; // widest label
        int x = screenWidth - barWidth - labelWidth - 10;
        int y = 6;

        // Oxygen bar
        double oxygenPercent = (double) oxygen / SpacesuitConstants.MAX_OXYGEN;
        int oxygenColor = oxygenCritical ? getFlashingColor(0xFFFF3333, 0xFF880000) : 0xFF33AAFF;
        drawResourceBar(graphics, mc, x, y, oxygenLabel, oxygenPercent, oxygenColor);

        // Propellant bar
        double propPercent = propellant / SpacesuitConstants.MAX_PROPELLANT;
        int propColor = propellantCritical ? getFlashingColor(0xFFFF3333, 0xFF880000) : 0xFFFF8833;
        drawResourceBar(graphics, mc, x, y + 14, fuelLabel, propPercent, propColor);

        // Jetpack indicator
        if (jetpackActive) {
            graphics.text(mc.font,
                    Component.literal("§a").append(Component.translatable("hud.outer_crafts.jetpack_active")),
                    x, y + 28, 0x55FF55, true);
        }
    }

    private static void drawResourceBar(GuiGraphicsExtractor graphics, Minecraft mc,
                                         int x, int y, String label, double percent, int color) {
        int barWidth = 80;
        int barHeight = 8;
        int labelWidth = mc.font.width(label) + 4;

        // Label
        graphics.text(mc.font, label, x, y, 0xFFCCCCCC, true);

        // Background
        int barX = x + labelWidth;
        graphics.fill(barX, y, barX + barWidth, y + barHeight, 0xFF333333);

        // Filled portion
        int filledWidth = (int) (barWidth * Math.clamp(percent, 0.0, 1.0));
        if (filledWidth > 0) {
            graphics.fill(barX, y, barX + filledWidth, y + barHeight, color);
        }

        // Border
        graphics.outline(barX, y, barWidth, barHeight, 0xFF888888);
    }

    private static int getFlashingColor(int colorA, int colorB) {
        return (System.currentTimeMillis() / 250) % 2 == 0 ? colorA : colorB;
    }
}
