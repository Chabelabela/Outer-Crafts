package org.chabelabela.outer_crafts.client.ship;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.chabelabela.outer_crafts.network.ShipNetworking;
import org.chabelabela.outer_crafts.ship.ShipConstants;

/**
 * Client-side cockpit HUD overlay for when the player is piloting the ship.
 * <p>
 * Registered via {@code RegisterGuiLayersEvent} in {@code OuterCrafts.ClientModEvents}.
 */
public final class ShipHudOverlay {

    private ShipHudOverlay() {}

    // ── State from server sync ──────────────────────────────────────────
    private static float fuel = (float) ShipConstants.MAX_FUEL;
    private static boolean engineOn = false;
    private static boolean landed = false;
    private static boolean destroyed = false;
    private static int shipOxygen = ShipConstants.SHIP_OXYGEN_CAPACITY;
    private static float velocity = 0;
    private static float hullHealth = 1.0f;
    private static float engineHealth = 1.0f;
    private static float leftThrusterHealth = 1.0f;
    private static float rightThrusterHealth = 1.0f;
    private static float cockpitHealth = 1.0f;
    private static float oxygenModuleHealth = 1.0f;
    private static boolean active = false;

    /** Called from {@code ShipNetworking} when a ship state packet arrives. */
    public static void updateFromPayload(ShipNetworking.ShipStatePayload payload) {
        fuel = payload.fuel();
        engineOn = payload.engineOn();
        landed = payload.landed();
        destroyed = payload.destroyed();
        shipOxygen = payload.shipOxygen();
        velocity = payload.velocityMagnitude();
        hullHealth = payload.hullHealth();
        engineHealth = payload.engineHealth();
        leftThrusterHealth = payload.leftThrusterHealth();
        rightThrusterHealth = payload.rightThrusterHealth();
        cockpitHealth = payload.cockpitHealth();
        oxygenModuleHealth = payload.oxygenModuleHealth();
        active = true;
    }

    public static void deactivate() {
        active = false;
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!active || !ShipInputHandler.isInShip()) {
            active = false;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        // If cockpit is destroyed, show static/noise instead of instruments
        if (cockpitHealth <= 0) {
            renderDeadCockpit(graphics, mc);
            return;
        }

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        // ── Top Center: Velocity ─────────────────────────────────────
        String velText = Component.translatable("hud.outer_crafts.ship.velocity", velocity * 20.0).getString();
        int velWidth = mc.font.width(velText);
        graphics.text(mc.font, velText,
                sw / 2 - velWidth / 2, 8, 0xFF33CCFF, true);

        // Landing indicator
        if (landed) {
            String landText = Component.translatable("hud.outer_crafts.ship.landed").getString();
            int landWidth = mc.font.width(landText);
            graphics.text(mc.font, landText,
                    sw / 2 - landWidth / 2, 20, 0xFF44FF44, true);
        }

        // Engine status
        if (engineOn) {
            String engText = Component.translatable("hud.outer_crafts.ship.engine_active").getString();
            int engWidth = mc.font.width(engText);
            graphics.text(mc.font, engText,
                    sw / 2 - engWidth / 2, landed ? 32 : 20, 0xFFFFAA33, true);
        }

        // ── Top Right: Fuel Gauge ────────────────────────────────────
        int fuelX = sw - 100;
        int fuelY = 10;
        double fuelPercent = fuel / ShipConstants.MAX_FUEL;
        int fuelColor = fuelPercent < 0.15 ? getFlashColor(0xFFFF3333, 0xFF880000) : 0xFF33AAFF;

        graphics.text(mc.font, Component.translatable("hud.outer_crafts.ship.fuel"),
                fuelX, fuelY, 0xFFCCCCCC, true);
        drawHorizontalBar(graphics, fuelX + 30, fuelY, 50, 8, fuelPercent, fuelColor);

        String fuelText = Component.translatable("hud.outer_crafts.ship.component.percent",
                fuelPercent * 100).getString();
        graphics.text(mc.font, fuelText,
                fuelX + 84, fuelY, 0xFFCCCCCC, true);

        // ── Left Side: Component Status ──────────────────────────────
        int compX = 10;
        int compY = 60;
        drawComponentStatus(graphics, mc, compX, compY,      "hull",            hullHealth);
        drawComponentStatus(graphics, mc, compX, compY + 12, "engine",          engineHealth);
        drawComponentStatus(graphics, mc, compX, compY + 24, "left_thruster",   leftThrusterHealth);
        drawComponentStatus(graphics, mc, compX, compY + 36, "right_thruster",  rightThrusterHealth);
        drawComponentStatus(graphics, mc, compX, compY + 48, "cockpit",         cockpitHealth);
        drawComponentStatus(graphics, mc, compX, compY + 60, "oxygen_module",   oxygenModuleHealth);

        // ── Damage warnings ──────────────────────────────────────────
        if (destroyed) {
            String warn = Component.translatable("hud.outer_crafts.ship.destroyed").getString();
            int warnWidth = mc.font.width(warn);
            int flashColor = getFlashColor(0xFFFF0000, 0xFFFF8800);
            graphics.text(mc.font, warn,
                    sw / 2 - warnWidth / 2, sh / 2 - 20, flashColor, true);
        } else if (hullHealth < 0.25f) {
            String warn = Component.translatable("hud.outer_crafts.ship.hull_critical").getString();
            int warnWidth = mc.font.width(warn);
            graphics.text(mc.font, warn,
                    sw / 2 - warnWidth / 2, sh - 40,
                    getFlashColor(0xFFFF4444, 0xFFAA0000), true);
        }
    }

    private static void drawComponentStatus(GuiGraphicsExtractor graphics, Minecraft mc,
                                             int x, int y, String componentKey, float health) {
        int color;
        if (health <= 0) color = 0xFFFF0000;
        else if (health < 0.25f) color = getFlashColor(0xFFFF4444, 0xFFAA0000);
        else if (health < 0.5f) color = 0xFFFFAA00;
        else color = 0xFF44FF44;

        String name = Component.translatable("hud.outer_crafts.ship.component." + componentKey).getString();
        String status = health <= 0
                ? Component.translatable("hud.outer_crafts.ship.component.offline").getString()
                : Component.translatable("hud.outer_crafts.ship.component.percent", health * 100).getString();
        graphics.text(mc.font, name + ": " + status, x, y, color, true);
    }

    private static void drawHorizontalBar(GuiGraphicsExtractor graphics, int x, int y,
                                           int width, int height, double percent, int color) {
        graphics.fill(x, y, x + width, y + height, 0xFF333333);
        int filled = (int) (width * Math.clamp(percent, 0.0, 1.0));
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + height, color);
        }
        graphics.outline(x, y, width, height, 0xFF888888);
    }

    private static void renderDeadCockpit(GuiGraphicsExtractor graphics, Minecraft mc) {
        int sw = mc.getWindow().getGuiScaledWidth();
        String msg = Component.translatable("hud.outer_crafts.ship.instruments_offline").getString();
        int w = mc.font.width(msg);
        graphics.text(mc.font, msg,
                sw / 2 - w / 2, 10,
                getFlashColor(0xFFFF0000, 0xFF440000), true);
    }

    private static int getFlashColor(int a, int b) {
        return (System.currentTimeMillis() / 300) % 2 == 0 ? a : b;
    }
}
