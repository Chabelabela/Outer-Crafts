package org.chabelabela.outer_crafts.equipment;

import net.minecraft.world.phys.Vec3;

import java.util.*;

public final class SignalFrequency {

    private SignalFrequency() {}

    public record Frequency(String id, String displayName, int color) {}

    public record SignalSource(
            String id,
            String displayName,
            String frequencyId,
            Vec3 position,
            String celestialBodyId,
            double maxDetectionRange
    ) {}

    private static final Map<String, Frequency> FREQUENCIES = new LinkedHashMap<>();
    private static final List<SignalSource> SIGNAL_SOURCES = new ArrayList<>();

    public static final Frequency OUTER_WILDS_VENTURES = registerFrequency(
            new Frequency("outer_wilds_ventures", "Outer Wilds Ventures", 0xFF8844));
    public static final Frequency QUANTUM_FLUCTUATIONS = registerFrequency(
            new Frequency("quantum_fluctuations", "Quantum Fluctuations", 0x44AAFF));
    public static final Frequency DISTRESS_BEACONS = registerFrequency(
            new Frequency("distress_beacons", "Distress Beacons", 0xFF4444));
    public static final Frequency DEEP_SPACE = registerFrequency(
            new Frequency("deep_space", "Deep Space", 0xAA44FF));

    public static void registerDefaults() {
        registerSource(new SignalSource("esker", "Esker's Whistling", "outer_wilds_ventures", Vec3.ZERO, "ember_twin", 2000.0));
        registerSource(new SignalSource("riebeck", "Riebeck's Banjo", "outer_wilds_ventures", Vec3.ZERO, "brittle_hollow", 2000.0));
        registerSource(new SignalSource("gabbro", "Gabbro's Flute", "outer_wilds_ventures", Vec3.ZERO, "giants_deep", 2000.0));
        registerSource(new SignalSource("feldspar", "Feldspar's Harmonica", "outer_wilds_ventures", Vec3.ZERO, "dark_bramble", 2000.0));
        registerSource(new SignalSource("chert", "Chert's Drums", "outer_wilds_ventures", Vec3.ZERO, "ember_twin", 2000.0));
        registerSource(new SignalSource("quantum_moon_signal", "Quantum Moon", "quantum_fluctuations", Vec3.ZERO, "quantum_moon", 5000.0));
        registerSource(new SignalSource("quantum_shard_grove", "Quantum Shard (Grove)", "quantum_fluctuations", Vec3.ZERO, "timber_hearth", 500.0));
        registerSource(new SignalSource("escape_pod_1", "Escape Pod 1", "distress_beacons", Vec3.ZERO, "brittle_hollow", 1500.0));
        registerSource(new SignalSource("escape_pod_2", "Escape Pod 2", "distress_beacons", Vec3.ZERO, "ember_twin", 1500.0));
        registerSource(new SignalSource("escape_pod_3", "Escape Pod 3", "distress_beacons", Vec3.ZERO, "giants_deep", 1500.0));
        registerSource(new SignalSource("eye_of_universe", "Eye of the Universe", "deep_space", new Vec3(100000, 0, 100000), null, 200000.0));
    }

    private static Frequency registerFrequency(Frequency freq) {
        FREQUENCIES.put(freq.id(), freq);
        return freq;
    }

    public static void registerSource(SignalSource source) { SIGNAL_SOURCES.add(source); }
    public static Map<String, Frequency> allFrequencies() { return Collections.unmodifiableMap(FREQUENCIES); }
    public static Frequency getFrequency(String id) { return FREQUENCIES.get(id); }
    public static List<SignalSource> allSources() { return Collections.unmodifiableList(SIGNAL_SOURCES); }

    public static List<SignalSource> sourcesForFrequency(String frequencyId) {
        return SIGNAL_SOURCES.stream().filter(s -> s.frequencyId().equals(frequencyId)).toList();
    }
}
