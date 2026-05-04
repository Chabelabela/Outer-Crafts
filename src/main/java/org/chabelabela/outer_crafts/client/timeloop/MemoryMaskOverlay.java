package org.chabelabela.outer_crafts.client.timeloop;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.chabelabela.outer_crafts.timeloop.TimeLoopConstants;
import org.chabelabela.outer_crafts.timeloop.TimeLoopPhase;

/**
 * Renders the "Memory Mask" overlay during the RESETTING phase and the time loop HUD.
 * <p>
 * The Memory Mask is the iconic Outer Wilds death/loop transition:
 * <ol>
 *   <li>Screen flashes white (supernova blinding light)</li>
 *   <li>Fades to deep blue-black</li>
 *   <li>Subtle eye-mask shape fades in (the Nomai statue memory recording)</li>
 *   <li>Fades back to normal as the new loop begins</li>
 * </ol>
 * <p>
 * Also renders the time loop HUD (remaining time) during normal gameplay,
 * and the supernova screen effects during WARNING/SUPERNOVA phases.
 * <p>
 * Registered via {@code RegisterGuiLayersEvent} in {@code OuterCrafts.ClientModEvents}.
 */
public final class MemoryMaskOverlay {

    private MemoryMaskOverlay() {}

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        TimeLoopPhase phase = TimeLoopClientState.getPhase();

        switch (phase) {
            case RUNNING -> {} // No overlay during normal gameplay
            case SUPERNOVA_WARNING -> renderWarningOverlay(graphics, screenWidth, screenHeight);
            case SUPERNOVA -> renderSupernovaFlash(graphics, screenWidth, screenHeight);
            case RESETTING -> renderMemoryMask(graphics, mc, screenWidth, screenHeight);
        }
    }

    // ── Warning Phase Overlay ───────────────────────────────────────────

    /**
     * During the warning phase, render an increasingly intense red vignette.
     */
    private static void renderWarningOverlay(GuiGraphicsExtractor graphics,
                                             int screenWidth, int screenHeight) {
        long tick = TimeLoopClientState.getCurrentTick();
        long warningStart = TimeLoopConstants.WARNING_START_TICK;
        long warningDuration = TimeLoopConstants.WARNING_PHASE_DURATION;
        double progress = Math.clamp((double) (tick - warningStart) / warningDuration, 0.0, 1.0);

        // Red vignette with increasing opacity
        int alpha = (int) (progress * 120); // max 120/255 opacity
        int color = (alpha << 24) | 0xFF2200; // ARGB: variable alpha, red-orange

        graphics.fill(0, 0, screenWidth, screenHeight, color);
    }

    // ── Supernova Flash ─────────────────────────────────────────────────

    /**
     * During the supernova, render an expanding white flash that overtakes the screen.
     */
    private static void renderSupernovaFlash(GuiGraphicsExtractor graphics,
                                             int screenWidth, int screenHeight) {
        long tick = TimeLoopClientState.getCurrentTick();
        long supernovaStart = TimeLoopConstants.LOOP_DURATION_TICKS;
        long supernovaDuration = TimeLoopConstants.SUPERNOVA_DURATION;
        double progress = Math.clamp(
                (double) (tick - supernovaStart) / supernovaDuration, 0.0, 1.0);

        // Blinding white flash, starts at 50% opacity, ends at 100%
        int alpha = (int) (255 * (0.5 + 0.5 * progress));
        int color = (alpha << 24) | 0xFFFFFF; // ARGB: white

        graphics.fill(0, 0, screenWidth, screenHeight, color);
    }

    // ── Memory Mask ─────────────────────────────────────────────────────

    /**
     * The iconic death transition.
     * Phase 1 (0.0–0.3): White flash fades to deep blue
     * Phase 2 (0.3–0.7): Deep blue/black with subtle pulsing
     * Phase 3 (0.7–1.0): Fade back to transparent (new loop starting)
     */
    private static void renderMemoryMask(GuiGraphicsExtractor graphics, Minecraft mc,
                                         int screenWidth, int screenHeight) {
        long tick = TimeLoopClientState.getCurrentTick();
        long resetStart = TimeLoopConstants.LOOP_DURATION_TICKS
                + TimeLoopConstants.SUPERNOVA_DURATION;
        long resetDuration = TimeLoopConstants.RESET_DURATION;
        double progress = Math.clamp(
                (double) (tick - resetStart) / resetDuration, 0.0, 1.0);

        int color;

        if (progress < 0.3) {
            // Phase 1: White → deep blue
            double p = progress / 0.3;
            int r = (int) (255 * (1.0 - p));
            int g = (int) (255 * (1.0 - p));
            int b = (int) (255 * (1.0 - 0.6 * p)); // blue stays higher
            color = (0xFF << 24) | (r << 16) | (g << 8) | b;
        } else if (progress < 0.7) {
            // Phase 2: Deep blue-black (the "memory recording" phase)
            double p = (progress - 0.3) / 0.4;
            double pulse = 0.5 + 0.5 * Math.sin(p * Math.PI * 4); // gentle pulse
            int intensity = (int) (20 + 15 * pulse);
            color = (0xFF << 24) | (intensity / 2 << 16) | (intensity / 2 << 8) | intensity;
        } else {
            // Phase 3: Fade to transparent (new loop begins)
            double p = (progress - 0.7) / 0.3;
            int alpha = (int) (255 * (1.0 - p));
            color = (alpha << 24) | 0x000822; // dark blue, fading out
        }

        graphics.fill(0, 0, screenWidth, screenHeight, color);

        // During phase 2, draw "Waking up..." text centered
        if (progress >= 0.5 && progress < 0.7) {
            double textAlpha = Math.sin((progress - 0.5) / 0.2 * Math.PI);
            int textColor = ((int) (textAlpha * 200) << 24) | 0x8899CC;
            String text = Component.translatable("hud.outer_crafts.timeloop.waking_up").getString();
            int textWidth = mc.font.width(text);
            graphics.text(mc.font, text,
                    (screenWidth - textWidth) / 2,
                    screenHeight / 2 - 5,
                    textColor, false);
        }
    }
}
