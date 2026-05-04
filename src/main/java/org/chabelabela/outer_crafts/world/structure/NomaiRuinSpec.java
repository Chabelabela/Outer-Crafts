package org.chabelabela.outer_crafts.world.structure;

import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Scaffold (3.7) for Nomai ruin placement specs.
 *
 * <p>Each ruin is anchored to a celestial body and placed at a specific spherical
 * coordinate on its surface. The structure body (block layout, populated text walls)
 * is meant to be loaded from datapack {@code data/outer_crafts/nomai_ruin/*.json}
 * once we have NBT structure templates — for now this class declares the
 * placement records so the rest of the codebase can reference them.
 *
 * <p>To populate ruins, future work needs to:
 * <ol>
 *   <li>Author NBT structure templates (Vault format) for each named ruin.</li>
 *   <li>Add a JSON loader (similar to {@code CelestialBodyLoader}) that builds
 *       a {@code Map<String, RuinSpec>} from datapack files.</li>
 *   <li>Hook {@code SolarSystemChunkGenerator#applyBiomeDecoration} to place
 *       loaded structures at the spec coordinates.</li>
 * </ol>
 */
public final class NomaiRuinSpec {

    private NomaiRuinSpec() {}

    public record Spec(
            String id,
            String bodyId,
            /** polar angle in radians, 0 = north pole */
            double theta,
            /** azimuth in radians */
            double phi,
            /** rotation around the local up axis, radians */
            double yaw,
            /** vault structure id, e.g. "outer_crafts:sunless_city" */
            String structureId
    ) {
        public Vec3 surfaceOffset(double bodyRadius) {
            double sinT = Math.sin(theta);
            return new Vec3(
                    sinT * Math.cos(phi) * bodyRadius,
                    Math.cos(theta) * bodyRadius,
                    sinT * Math.sin(phi) * bodyRadius);
        }
    }

    /** Placeholder catalogue — fill in as NBT templates land. */
    public static final List<Spec> CANONICAL = List.of(
            new Spec("sunless_city",       "ember_twin",     Math.toRadians(150), Math.toRadians(  0),
                    0.0, "outer_crafts:sunless_city"),
            new Spec("hanging_city",       "brittle_hollow", Math.toRadians( 60), Math.toRadians( 90),
                    0.0, "outer_crafts:hanging_city"),
            new Spec("ash_twin_project",   "ash_twin",       Math.toRadians( 30), Math.toRadians(180),
                    0.0, "outer_crafts:ash_twin_project"),
            new Spec("construction_yard",  "giants_deep",    Math.toRadians( 90), Math.toRadians( 60),
                    0.0, "outer_crafts:construction_yard"),
            new Spec("nomai_vessel",       "dark_bramble",   Math.toRadians( 90), Math.toRadians(-60),
                    0.0, "outer_crafts:nomai_vessel"),
            new Spec("quantum_tower",      "ash_twin",       Math.toRadians( 90), Math.toRadians(  0),
                    0.0, "outer_crafts:quantum_tower")
    );
}
