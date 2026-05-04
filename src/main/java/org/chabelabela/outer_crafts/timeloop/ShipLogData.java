package org.chabelabela.outer_crafts.timeloop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.server.level.ServerPlayer;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.network.ShipLogNetworking;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ShipLogData {

    private ShipLogData() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<UUID, PlayerShipLog> CACHE = new ConcurrentHashMap<>();

    private static Path saveDirectory;

    public static final class PlayerShipLog {
        private final Set<String> discoveredRumors = new LinkedHashSet<>();
        private final Set<RumorConnection> rumorConnections = new LinkedHashSet<>();
        private final Set<String> exploredLocations = new LinkedHashSet<>();
        private final Set<String> translatedTexts = new LinkedHashSet<>();
        private final Set<String> discoveredFrequencies = new LinkedHashSet<>();
        private int totalLoops = 0;
        private long totalTicksPlayed = 0;
        private final Map<String, Integer> deathsByCategory = new LinkedHashMap<>();

        public void addRumor(String rumorId) { discoveredRumors.add(rumorId); }
        public void addRumorConnection(String a, String b) {
            rumorConnections.add(new RumorConnection(a, b));
        }
        public void addExploredLocation(String locationId) { exploredLocations.add(locationId); }
        public void addTranslatedText(String textId) { translatedTexts.add(textId); }
        public void addFrequency(String frequencyId) { discoveredFrequencies.add(frequencyId); }
        public void incrementLoops() { totalLoops++; }
        public void addPlayedTicks(long ticks) { totalTicksPlayed += ticks; }
        public void recordDeath(String cause) {
            deathsByCategory.merge(cause, 1, Integer::sum);
        }

        public Set<String> getDiscoveredRumors() { return Collections.unmodifiableSet(discoveredRumors); }
        public Set<String> getExploredLocations() { return Collections.unmodifiableSet(exploredLocations); }
        public Set<String> getTranslatedTexts() { return Collections.unmodifiableSet(translatedTexts); }
        public Set<String> getDiscoveredFrequencies() { return Collections.unmodifiableSet(discoveredFrequencies); }
        public int getTotalLoops() { return totalLoops; }
        public long getTotalTicksPlayed() { return totalTicksPlayed; }
        public boolean hasRumor(String id) { return discoveredRumors.contains(id); }
        public boolean hasExplored(String id) { return exploredLocations.contains(id); }

        public JsonObject toJson() {
            JsonObject root = new JsonObject();

            root.add("rumors", setToJsonArray(discoveredRumors));
            JsonArray connections = new JsonArray();
            for (RumorConnection conn : rumorConnections) {
                JsonObject c = new JsonObject();
                c.addProperty("a", conn.a());
                c.addProperty("b", conn.b());
                connections.add(c);
            }
            root.add("rumorConnections", connections);
            root.add("exploredLocations", setToJsonArray(exploredLocations));
            root.add("translatedTexts", setToJsonArray(translatedTexts));
            root.add("discoveredFrequencies", setToJsonArray(discoveredFrequencies));
            root.addProperty("totalLoops", totalLoops);
            root.addProperty("totalTicksPlayed", totalTicksPlayed);

            JsonObject deaths = new JsonObject();
            deathsByCategory.forEach(deaths::addProperty);
            root.add("deathsByCategory", deaths);

            return root;
        }

        public static PlayerShipLog fromJson(JsonObject root) {
            PlayerShipLog log = new PlayerShipLog();
            jsonArrayToSet(root.getAsJsonArray("rumors"), log.discoveredRumors);

            JsonArray connections = root.getAsJsonArray("rumorConnections");
            if (connections != null) {
                for (JsonElement el : connections) {
                    JsonObject c = el.getAsJsonObject();
                    log.rumorConnections.add(new RumorConnection(
                            c.get("a").getAsString(), c.get("b").getAsString()
                    ));
                }
            }

            jsonArrayToSet(root.getAsJsonArray("exploredLocations"), log.exploredLocations);
            jsonArrayToSet(root.getAsJsonArray("translatedTexts"), log.translatedTexts);
            jsonArrayToSet(root.getAsJsonArray("discoveredFrequencies"), log.discoveredFrequencies);

            if (root.has("totalLoops")) log.totalLoops = root.get("totalLoops").getAsInt();
            if (root.has("totalTicksPlayed")) log.totalTicksPlayed = root.get("totalTicksPlayed").getAsLong();

            if (root.has("deathsByCategory")) {
                JsonObject deaths = root.getAsJsonObject("deathsByCategory");
                for (var entry : deaths.entrySet()) {
                    log.deathsByCategory.put(entry.getKey(), entry.getValue().getAsInt());
                }
            }

            return log;
        }
    }

    public record RumorConnection(String a, String b) {
        @Override
        public boolean equals(Object o) {
            return o instanceof RumorConnection rc
                    && ((a.equals(rc.a) && b.equals(rc.b)) || (a.equals(rc.b) && b.equals(rc.a)));
        }

        @Override
        public int hashCode() {
            return a.hashCode() + b.hashCode();
        }
    }

    public static void init(Path worldSaveDir) {
        saveDirectory = worldSaveDir.resolve(TimeLoopConstants.SHIP_LOG_DIRECTORY);
        try {
            Files.createDirectories(saveDirectory);
        } catch (IOException e) {
            OuterCrafts.LOGGER.error("[Ship Log] Failed to create save directory", e);
        }
        OuterCrafts.LOGGER.info("[Ship Log] Save directory: {}", saveDirectory);
    }

    public static PlayerShipLog getOrCreate(ServerPlayer player) {
        return CACHE.computeIfAbsent(player.getUUID(), uuid -> {
            PlayerShipLog loaded = loadFromDisk(uuid);
            return loaded != null ? loaded : new PlayerShipLog();
        });
    }

    public static void saveForPlayer(ServerPlayer player) {
        PlayerShipLog log = getOrCreate(player);
        log.incrementLoops();
        log.addPlayedTicks(TimeLoopManager.getCurrentTick());
        saveToDisk(player.getUUID(), log);
        // Push the new loop count to the client immediately.
        ShipLogNetworking.syncToPlayer(player);
    }

    public static void loadForPlayer(ServerPlayer player) {
        PlayerShipLog loaded = loadFromDisk(player.getUUID());
        if (loaded != null) {
            CACHE.put(player.getUUID(), loaded);
        }
        // Whether or not we loaded anything, mirror the current state to the client.
        ShipLogNetworking.syncToPlayer(player);
    }

    /**
     * Convenience: mark a player's log dirty and push it to their client.
     * Call this from anywhere that mutates a {@link PlayerShipLog} (rumor added,
     * location explored, text translated, etc).
     */
    public static void markDirty(ServerPlayer player) {
        ShipLogNetworking.syncToPlayer(player);
    }

    private static void saveToDisk(UUID uuid, PlayerShipLog log) {
        if (saveDirectory == null) return;
        Path file = saveDirectory.resolve(uuid + ".json");
        try {
            String json = GSON.toJson(log.toJson());
            Files.writeString(file, json);
            OuterCrafts.LOGGER.debug("[Ship Log] Saved data for {}", uuid);
        } catch (IOException e) {
            OuterCrafts.LOGGER.error("[Ship Log] Failed to save data for {}", uuid, e);
        }
    }

    private static PlayerShipLog loadFromDisk(UUID uuid) {
        if (saveDirectory == null) return null;
        Path file = saveDirectory.resolve(uuid + ".json");
        if (!Files.exists(file)) return null;
        try {
            String json = Files.readString(file);
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            OuterCrafts.LOGGER.debug("[Ship Log] Loaded data for {}", uuid);
            return PlayerShipLog.fromJson(root);
        } catch (Exception e) {
            OuterCrafts.LOGGER.error("[Ship Log] Failed to load data for {}", uuid, e);
            return null;
        }
    }

    public static void clearCache() {
        CACHE.clear();
    }

    private static JsonArray setToJsonArray(Set<String> set) {
        JsonArray arr = new JsonArray();
        set.forEach(arr::add);
        return arr;
    }

    private static void jsonArrayToSet(JsonArray arr, Set<String> set) {
        if (arr == null) return;
        for (JsonElement el : arr) {
            set.add(el.getAsString());
        }
    }
}
