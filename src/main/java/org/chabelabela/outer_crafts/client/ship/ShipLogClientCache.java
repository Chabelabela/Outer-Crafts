package org.chabelabela.outer_crafts.client.ship;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Client-side cache of Ship Log data.
 * <p>
 * Populated from network sync when the player first enters the ship
 * and updated incrementally as new discoveries are made.
 * <p>
 * This avoids needing to read server-side ShipLogData from the client.
 */
public final class ShipLogClientCache {

    private ShipLogClientCache() {}

    private static final Set<String> discoveredRumors = new LinkedHashSet<>();
    private static final Set<String> exploredLocations = new LinkedHashSet<>();
    private static final Set<String> translatedTexts = new LinkedHashSet<>();
    private static int totalLoops = 0;

    // ── Update API (called from network handlers) ───────────────────────

    public static void addRumor(String rumorId) {
        discoveredRumors.add(rumorId);
    }

    public static void addExploredLocation(String locationId) {
        exploredLocations.add(locationId);
    }

    public static void addTranslatedText(String textId) {
        translatedTexts.add(textId);
    }

    public static void setTotalLoops(int loops) {
        totalLoops = loops;
    }

    /**
     * Bulk update from a full sync payload.
     */
    public static void fullSync(Set<String> rumors, Set<String> locations,
                                 Set<String> texts, int loops) {
        discoveredRumors.clear();
        discoveredRumors.addAll(rumors);
        exploredLocations.clear();
        exploredLocations.addAll(locations);
        translatedTexts.clear();
        translatedTexts.addAll(texts);
        totalLoops = loops;
    }

    public static void reset() {
        discoveredRumors.clear();
        exploredLocations.clear();
        translatedTexts.clear();
        totalLoops = 0;
    }

    // ── Query API ───────────────────────────────────────────────────────

    public static Set<String> getDiscoveredRumors() {
        return Collections.unmodifiableSet(discoveredRumors);
    }

    public static Set<String> getExploredLocations() {
        return Collections.unmodifiableSet(exploredLocations);
    }

    public static Set<String> getTranslatedTexts() {
        return Collections.unmodifiableSet(translatedTexts);
    }

    public static int getTotalLoops() {
        return totalLoops;
    }
}
