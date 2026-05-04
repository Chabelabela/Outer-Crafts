package org.chabelabela.outer_crafts.equipment;

import java.util.*;

public final class NomaiTextRegistry {

    private NomaiTextRegistry() {}

    private static final Map<String, String> TRANSLATIONS = new LinkedHashMap<>();
    private static final Map<String, String> ASSOCIATED_RUMORS = new LinkedHashMap<>();

    public static void registerDefaults() {
        // ── Timber Hearth ───────────────────────────────────────────
        register("th_observatory_01",
                "The signal originates from beyond the edges of our star system. " +
                "We must find its source.",
                "eye_signal_origin");
        register("th_observatory_02",
                "We have constructed a device to locate the signal's source. " +
                "It orbits the Sun, awaiting activation.",
                "orbital_probe_cannon");
        register("th_mines_01",
                "Warning: Unstable quantum material detected in the lower caverns. " +
                "Do not observe fragments simultaneously.",
                "quantum_shard_th");

        // ── Ember Twin ──────────────────────────────────────────────
        register("et_sunless_city_01",
                "The sand is rising. We must relocate our archives before the " +
                "Sunless City is buried beneath Ash Twin's flow.",
                "sand_transfer_threat");
        register("et_sunless_city_02",
                "The Ash Twin Project will allow us to send information back " +
                "in time — 22 minutes, to be precise. This is our only hope.",
                "ash_twin_project");
        register("et_anglerfish_cave_01",
                "Caution: The anglerfish are blind. They hunt by sound alone. " +
                "Move silently and they will not detect you.",
                "anglerfish_behavior");

        // ── Brittle Hollow ──────────────────────────────────────────
        register("bh_hanging_city_01",
                "The Southern Observatory has been established to study the " +
                "quantum properties of the sixth location.",
                "quantum_moon_study");
        register("bh_hanging_city_02",
                "Hollow's Lantern grows more volatile. The volcanic bombardment " +
                "is accelerating the collapse of the crust.",
                "brittle_hollow_collapse");
        register("bh_gravity_cannon_01",
                "The gravity cannon is calibrated to launch vessels toward " +
                "the target destination. Ensure proper alignment before firing.",
                "gravity_cannon");

        // ── Giant's Deep ────────────────────────────────────────────
        register("gd_construction_yard_01",
                "The Orbital Probe Cannon is nearly complete. Each loop " +
                "it fires in a random direction, searching for the Eye.",
                "orbital_probe_cannon");
        register("gd_statue_island_01",
                "The statues will pair with the first conscious being they " +
                "observe after activation. This pairing is permanent.",
                "nomai_statues");
        register("gd_tower_01",
                "The jellyfish of Giant's Deep are immune to the electric " +
                "barrier of the core. Perhaps they could shield a vessel.",
                "jellyfish_shield");

        // ── Dark Bramble ────────────────────────────────────────────
        register("db_vessel_01",
                "Our vessel was consumed by the growth. We are scattered " +
                "across its impossible interior. Send help.",
                "nomai_vessel");
        register("db_feldspar_note_01",
                "If you're reading this, I made it deeper than anyone. " +
                "The anglerfish can't see — just don't make noise. - Feldspar",
                "feldspar_location");

        // ── Ash Twin / Quantum Moon ─────────────────────────────────
        register("at_tower_01",
                "The Quantum Moon exists in superposition, orbiting all " +
                "celestial bodies simultaneously until observed.",
                "quantum_moon_nature");
        register("at_tower_02",
                "To reach the sixth location, one must be on the Quantum Moon " +
                "while its position cannot be determined.",
                "sixth_location");
    }

    private static void register(String textId, String translation, String rumorId) {
        TRANSLATIONS.put(textId, translation);
        if (rumorId != null) {
            ASSOCIATED_RUMORS.put(textId, rumorId);
        }
    }

    public static String getTranslation(String textId) {
        return TRANSLATIONS.get(textId);
    }

    public static String getAssociatedRumor(String textId) {
        return ASSOCIATED_RUMORS.get(textId);
    }

    public static Set<String> allTextIds() {
        return Collections.unmodifiableSet(TRANSLATIONS.keySet());
    }
}
