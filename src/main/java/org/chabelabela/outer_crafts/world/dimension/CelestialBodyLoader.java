package org.chabelabela.outer_crafts.world.dimension;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.physics.CelestialBody;
import org.chabelabela.outer_crafts.registry.CelestialBodyRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads {@link CelestialBody} definitions from
 * {@code data/<namespace>/celestial_body/*.json} on every datapack reload.
 *
 * <p>Lets users (and the mod's own resources) add or override planets without
 * recompiling: drop a JSON file in a datapack and it appears in the registry on
 * world load or {@code /reload}.
 *
 * <p>The loader topologically sorts bodies so that parents are registered before
 * their children, satisfying {@link CelestialBodyRegistry#register(CelestialBody)}'s
 * "parents first" constraint.
 */
@EventBusSubscriber(modid = OuterCrafts.MODID)
public final class CelestialBodyLoader extends SimplePreparableReloadListener<Map<Identifier, CelestialBody>> {

    private static final Gson GSON = new Gson();

    private static final FileToIdConverter LISTER =
            FileToIdConverter.json("celestial_body");

    public static final Identifier LISTENER_ID =
            Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "celestial_body_loader");

    @SubscribeEvent
    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(LISTENER_ID, new CelestialBodyLoader());
    }

    @Override
    protected Map<Identifier, CelestialBody> prepare(ResourceManager rm, ProfilerFiller profiler) {
        Map<Identifier, CelestialBody> out = new LinkedHashMap<>();
        var resources = LISTER.listMatchingResources(rm);

        for (var entry : resources.entrySet()) {
            Identifier fileId = entry.getKey();
            Identifier bodyId = LISTER.fileToId(fileId);
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonElement json = GSON.fromJson(reader, JsonElement.class);
                CelestialBody.CODEC.parse(JsonOps.INSTANCE, json)
                        .resultOrPartial(err -> OuterCrafts.LOGGER.error(
                                "[CelestialBodyLoader] Failed to parse {}: {}", fileId, err))
                        .ifPresent(body -> out.put(bodyId, body));
            } catch (IOException | RuntimeException e) {
                OuterCrafts.LOGGER.error("[CelestialBodyLoader] IO error reading {}", fileId, e);
            }
        }
        return out;
    }

    @Override
    protected void apply(Map<Identifier, CelestialBody> parsed, ResourceManager rm, ProfilerFiller profiler) {
        CelestialBodyRegistry.clear();

        // Topological sort: parents first.
        List<CelestialBody> ordered = topoSort(parsed.values());

        int registered = 0;
        for (CelestialBody body : ordered) {
            try {
                CelestialBodyRegistry.register(body);
                registered++;
            } catch (IllegalArgumentException e) {
                OuterCrafts.LOGGER.warn("[CelestialBodyLoader] {}", e.getMessage());
            }
        }
        CelestialBodyRegistry.invalidateCache();
        OuterCrafts.LOGGER.info("[CelestialBodyLoader] Registered {} celestial bodies from datapack.",
                registered);
    }

    private static List<CelestialBody> topoSort(Iterable<CelestialBody> bodies) {
        // Index by id for parent lookup
        Map<String, CelestialBody> byId = new LinkedHashMap<>();
        for (CelestialBody b : bodies) byId.put(b.id(), b);

        List<CelestialBody> result = new ArrayList<>(byId.size());
        java.util.Set<String> visited = new java.util.HashSet<>();
        for (CelestialBody body : byId.values()) {
            visit(body, byId, visited, result);
        }
        return result;
    }

    private static void visit(CelestialBody body,
                              Map<String, CelestialBody> byId,
                              java.util.Set<String> visited,
                              List<CelestialBody> out) {
        if (!visited.add(body.id())) return;
        if (body.parentId() != null) {
            CelestialBody parent = byId.get(body.parentId());
            if (parent != null) visit(parent, byId, visited, out);
        }
        out.add(body);
    }
}
