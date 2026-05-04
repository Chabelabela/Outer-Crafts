package org.chabelabela.outer_crafts.client.ship;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

/**
 * The Ship Log UI: a fullscreen overlay accessible from the ship cockpit (L key).
 * <p>
 * Two modes, toggled via tabs:
 * <ul>
 *   <li><b>Rumor Mode</b>: A node-graph showing discovered clues and their connections.</li>
 *   <li><b>Map Mode</b>: A 2D overhead view of the solar system.</li>
 * </ul>
 */
public class ShipLogScreen extends Screen {

    private boolean mapMode;

    private final Set<String> discoveredRumors;
    private final Set<String> exploredLocations;
    private final Set<String> translatedTexts;
    private final int totalLoops;

    private static final Map<String, int[]> PLANET_MAP_POSITIONS = Map.of(
            "sun", new int[]{0, 0},
            "ember_twin", new int[]{-60, -30},
            "ash_twin", new int[]{-55, -25},
            "timber_hearth", new int[]{40, 50},
            "brittle_hollow", new int[]{-70, 60},
            "giants_deep", new int[]{80, -40},
            "dark_bramble", new int[]{-90, -80}
    );

    private static final Map<String, int[]> RUMOR_POSITIONS = new LinkedHashMap<>();
    private static final List<String[]> RUMOR_CONNECTIONS = new ArrayList<>();

    static {
        RUMOR_POSITIONS.put("eye_signal_origin", new int[]{0, -80});
        RUMOR_POSITIONS.put("orbital_probe_cannon", new int[]{50, -50});
        RUMOR_POSITIONS.put("ash_twin_project", new int[]{-50, -30});
        RUMOR_POSITIONS.put("quantum_moon_nature", new int[]{80, 0});
        RUMOR_POSITIONS.put("quantum_moon_study", new int[]{80, 30});
        RUMOR_POSITIONS.put("sixth_location", new int[]{100, 15});
        RUMOR_POSITIONS.put("sand_transfer_threat", new int[]{-80, 10});
        RUMOR_POSITIONS.put("anglerfish_behavior", new int[]{-40, 60});
        RUMOR_POSITIONS.put("nomai_vessel", new int[]{-80, 80});
        RUMOR_POSITIONS.put("jellyfish_shield", new int[]{50, 50});
        RUMOR_POSITIONS.put("brittle_hollow_collapse", new int[]{-20, 40});
        RUMOR_POSITIONS.put("gravity_cannon", new int[]{-40, 20});
        RUMOR_POSITIONS.put("nomai_statues", new int[]{30, 20});
        RUMOR_POSITIONS.put("feldspar_location", new int[]{-60, 60});
        RUMOR_POSITIONS.put("quantum_shard_th", new int[]{60, 60});

        RUMOR_CONNECTIONS.add(new String[]{"eye_signal_origin", "orbital_probe_cannon"});
        RUMOR_CONNECTIONS.add(new String[]{"eye_signal_origin", "ash_twin_project"});
        RUMOR_CONNECTIONS.add(new String[]{"ash_twin_project", "sand_transfer_threat"});
        RUMOR_CONNECTIONS.add(new String[]{"orbital_probe_cannon", "nomai_statues"});
        RUMOR_CONNECTIONS.add(new String[]{"quantum_moon_nature", "quantum_moon_study"});
        RUMOR_CONNECTIONS.add(new String[]{"quantum_moon_study", "sixth_location"});
        RUMOR_CONNECTIONS.add(new String[]{"quantum_moon_nature", "quantum_shard_th"});
        RUMOR_CONNECTIONS.add(new String[]{"anglerfish_behavior", "dark_bramble"});
        RUMOR_CONNECTIONS.add(new String[]{"anglerfish_behavior", "nomai_vessel"});
        RUMOR_CONNECTIONS.add(new String[]{"nomai_vessel", "feldspar_location"});
        RUMOR_CONNECTIONS.add(new String[]{"jellyfish_shield", "orbital_probe_cannon"});
        RUMOR_CONNECTIONS.add(new String[]{"brittle_hollow_collapse", "gravity_cannon"});
    }

    /**
     * Primary constructor used by {@code ShipNetworking} via
     * {@code new ShipLogScreen(payload.mapMode())}.
     */
    public ShipLogScreen(boolean startInMapMode) {
        super(Component.translatable("hud.outer_crafts.shiplog.title"));
        this.mapMode = startInMapMode;
        this.discoveredRumors = ShipLogClientCache.getDiscoveredRumors();
        this.exploredLocations = ShipLogClientCache.getExploredLocations();
        this.translatedTexts = ShipLogClientCache.getTranslatedTexts();
        this.totalLoops = ShipLogClientCache.getTotalLoops();
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new ShipLogScreen(false));
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(
                Component.translatable("hud.outer_crafts.shiplog.button.rumor"),
                btn -> mapMode = false
        ).bounds(width / 2 - 110, 10, 100, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("hud.outer_crafts.shiplog.button.map"),
                btn -> mapMode = true
        ).bounds(width / 2 + 10, 10, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Dark background
        graphics.fill(0, 0, width, height, 0xDD000A14);

        // Title
        String title = Component.translatable(mapMode
                ? "hud.outer_crafts.shiplog.title.map"
                : "hud.outer_crafts.shiplog.title.rumor").getString();
        int titleWidth = font.width(title);
        graphics.text(font, title,
                width / 2 - titleWidth / 2, 40, 0xFF33CCFF, true);

        // Stats bar
        String stats = Component.translatable("hud.outer_crafts.shiplog.stats",
                totalLoops, discoveredRumors.size(),
                exploredLocations.size(), translatedTexts.size()).getString();
        int statsWidth = font.width(stats);
        graphics.text(font, stats,
                width / 2 - statsWidth / 2, height - 20, 0xFF888888, true);

        if (mapMode) {
            renderMapMode(graphics);
        } else {
            renderRumorMode(graphics);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderRumorMode(GuiGraphicsExtractor graphics) {
        int cx = width / 2;
        int cy = height / 2;

        for (String[] conn : RUMOR_CONNECTIONS) {
            int[] posA = RUMOR_POSITIONS.get(conn[0]);
            int[] posB = RUMOR_POSITIONS.get(conn[1]);
            if (posA == null || posB == null) continue;

            boolean bothDiscovered = discoveredRumors.contains(conn[0])
                    && discoveredRumors.contains(conn[1]);
            int lineColor = bothDiscovered ? 0xFF336688 : 0xFF1A3344;

            drawLine(graphics, cx + posA[0], cy + posA[1], cx + posB[0], cy + posB[1], lineColor);
        }

        for (var entry : RUMOR_POSITIONS.entrySet()) {
            String rumorId = entry.getKey();
            int[] pos = entry.getValue();
            int nx = cx + pos[0];
            int ny = cy + pos[1];

            boolean discovered = discoveredRumors.contains(rumorId);

            int nodeColor = discovered ? 0xFF33AACC : 0xFF1A3344;
            graphics.fill(nx - 4, ny - 4, nx + 4, ny + 4, nodeColor);
            graphics.outline(nx - 4, ny - 4, 8, 8,
                    discovered ? 0xFF44CCEE : 0xFF223344);

            String label = discovered ? formatRumorName(rumorId) : "?";
            int labelWidth = font.width(label);
            int labelColor = discovered ? 0xFFCCEEFF : 0xFF445566;
            graphics.text(font, label,
                    nx - labelWidth / 2, ny + 8, labelColor, true);
        }
    }

    private void renderMapMode(GuiGraphicsExtractor graphics) {
        int cx = width / 2;
        int cy = height / 2;

        for (int r = 40; r <= 120; r += 20) {
            drawCircleOutline(graphics, cx, cy, r, 0xFF112233);
        }

        for (var entry : PLANET_MAP_POSITIONS.entrySet()) {
            String bodyId = entry.getKey();
            int[] pos = entry.getValue();
            int px = cx + pos[0] * 2;
            int py = cy + pos[1] * 2;

            boolean explored = exploredLocations.contains(bodyId);

            int dotSize = bodyId.equals("sun") ? 8 : 5;
            int color = switch (bodyId) {
                case "sun" -> 0xFFFFCC33;
                case "ember_twin" -> 0xFFFF6633;
                case "ash_twin" -> 0xFFCCBB88;
                case "timber_hearth" -> 0xFF44AA44;
                case "brittle_hollow" -> 0xFF8866CC;
                case "giants_deep" -> 0xFF3366CC;
                case "dark_bramble" -> 0xFF336633;
                default -> 0xFF888888;
            };

            graphics.fill(px - dotSize, py - dotSize, px + dotSize, py + dotSize, color);

            String name = formatBodyName(bodyId);
            int nameWidth = font.width(name);
            graphics.text(font, name,
                    px - nameWidth / 2, py + dotSize + 4, 0xFFCCCCCC, true);

            if (explored) {
                graphics.text(font, "✓",
                        px + dotSize + 2, py - 4, 0xFF44FF44, true);
            }
        }
    }

    private void drawLine(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) return;

        float xStep = (float) dx / steps;
        float yStep = (float) dy / steps;

        for (int i = 0; i <= steps; i += 2) {
            int x = x1 + (int) (xStep * i);
            int y = y1 + (int) (yStep * i);
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private void drawCircleOutline(GuiGraphicsExtractor graphics, int cx, int cy, int radius, int color) {
        int segments = 64;
        for (int i = 0; i < segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            int x = cx + (int) (radius * Math.cos(angle));
            int y = cy + (int) (radius * Math.sin(angle));
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private static String formatRumorName(String id) {
        String spaced = id.replace('_', ' ');
        return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
    }

    private static String formatBodyName(String id) {
        String[] words = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
