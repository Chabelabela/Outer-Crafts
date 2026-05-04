package org.chabelabela.outer_crafts.world.planet;

import java.util.Map;

public final class PlanetGenerators {

    private PlanetGenerators() {}

    private static final Map<String, PlanetGenerator> GENERATORS = Map.ofEntries(
            Map.entry("sun", new SunGenerator()),
            Map.entry("ash_twin", new AshTwinGenerator()),
            Map.entry("ember_twin", new EmberTwinGenerator()),
            Map.entry("timber_hearth", new TimberHearthGenerator()),
            Map.entry("brittle_hollow", new BrittleHollowGenerator()),
            Map.entry("hollows_lantern", new HollowsLanternGenerator()),
            Map.entry("giants_deep", new GiantsDeepGenerator()),
            Map.entry("dark_bramble", new DarkBrambleGenerator()),
            Map.entry("quantum_moon", new QuantumMoonGenerator())
    );

    public static PlanetGenerator get(String bodyId) {
        return GENERATORS.get(bodyId);
    }
}
