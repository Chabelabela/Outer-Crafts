package org.chabelabela.outer_crafts.client.equipment;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.List;

/**
 * Client-side HUD overlay for the Nomai Translator Tool.
 * <p>
 * Registered via {@code RegisterGuiLayersEvent} in {@code OuterCrafts.ClientModEvents}.
 */
public final class TranslatorHudOverlay {

    private TranslatorHudOverlay() {}

    private static String currentTextId = "";
    private static String currentText = "";
    private static boolean isNewDiscovery = false;
    private static long displayStartTime = 0;

    private static final long DISPLAY_DURATION_MS = 8000;
    private static final int MAX_TEXT_WIDTH = 300;

    /** Called from {@code EquipmentNetworking} when translated text is received. */
    public static void updateFromPayload(String textId, String text, boolean isNew) {
        currentTextId = textId;
        currentText = text;
        isNewDiscovery = isNew;
        displayStartTime = System.currentTimeMillis();
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        long elapsed = System.currentTimeMillis() - displayStartTime;
        if (elapsed > DISPLAY_DURATION_MS || currentText.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        // Fade in over first 500ms, fade out over last 1000ms
        float alpha;
        if (elapsed < 500) {
            alpha = elapsed / 500.0f;
        } else if (elapsed > DISPLAY_DURATION_MS - 1000) {
            alpha = (float) (DISPLAY_DURATION_MS - elapsed) / 1000.0f;
        } else {
            alpha = 1.0f;
        }
        int alphaInt = (int) (alpha * 255);

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int panelWidth = Math.min(MAX_TEXT_WIDTH, screenWidth - 40);
        int panelX = (screenWidth - panelWidth) / 2;

        // Word-wrap the text
        List<Component> lines = mc.font.getSplitter()
                .splitLines(currentText, panelWidth - 16, Style.EMPTY)
                .stream()
                .map(formattedText -> (Component) Component.literal(formattedText.getString()))
                .toList();

        int lineHeight = 11;
        int panelHeight = 16 + lines.size() * lineHeight + (isNewDiscovery ? 14 : 0);
        int panelY = screenHeight - panelHeight - 60;

        // Background panel
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight,
                (alphaInt * 180 / 255) << 24 | 0x001A2A);
        graphics.outline(panelX, panelY, panelWidth, panelHeight,
                (alphaInt << 24) | 0x33AACC);

        int textY = panelY + 8;

        // "NEW ENTRY" flash for new discoveries
        if (isNewDiscovery && (System.currentTimeMillis() / 400) % 2 == 0) {
            String newLabel = Component.translatable("hud.outer_crafts.translator.new_entry").getString();
            int labelWidth = mc.font.width(newLabel);
            graphics.text(mc.font, newLabel,
                    panelX + (panelWidth - labelWidth) / 2, textY,
                    (alphaInt << 24) | 0x44FFAA, true);
            textY += 14;
        } else if (isNewDiscovery) {
            textY += 14;
        }

        // Translated text lines
        for (Component line : lines) {
            graphics.text(mc.font, line,
                    panelX + 8, textY, (alphaInt << 24) | 0xCCEEFF, true);
            textY += lineHeight;
        }
    }
}
