package org.chabelabela.outer_crafts.physics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * A celestial body in the solar system.
 *
 * <p>Designers specify <b>{@code surfaceGravity}</b> directly (the acceleration in
 * blocks-per-tick² felt by an entity standing exactly on the body's surface), instead
 * of the abstract {@code mass}. The mass needed for the {@code G·m/r²} inverse-square
 * falloff is derived from {@code surfaceGravity · radius² / G}. This makes tuning
 * intuitive — vanilla MC gravity is {@code 0.08}, so {@code surfaceGravity = 0.08}
 * gives a Timber-Hearth-feel; {@code 0.12} feels heavier (Giant's Deep), {@code 0.04}
 * feels low-G (Hourglass Twins, Quantum Moon).
 *
 * <p>JSON schema (datapack):
 * <pre>{@code
 * {
 *   "id":              "timber_hearth",
 *   "surface_gravity": 0.08,
 *   "radius":          110.0,
 *   "base_position":   [800.0, 100.0, 1385.640],
 *   "orbital_radius":  0.0,
 *   "orbital_period":  0,
 *   "orbital_phase":   0.0,
 *   "parent_id":       "sun"
 * }
 * }</pre>
 */
public record CelestialBody(
        String id,
        double surfaceGravity,
        double radius,
        Vec3 basePosition,
        double orbitalRadius,
        long orbitalPeriod,
        double orbitalPhase,
        String parentId
) {
    public static final Codec<CelestialBody> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(CelestialBody::id),
            Codec.DOUBLE.fieldOf("surface_gravity").forGetter(CelestialBody::surfaceGravity),
            Codec.DOUBLE.fieldOf("radius").forGetter(CelestialBody::radius),
            Vec3.CODEC.fieldOf("base_position").forGetter(CelestialBody::basePosition),
            Codec.DOUBLE.optionalFieldOf("orbital_radius", 0.0).forGetter(CelestialBody::orbitalRadius),
            Codec.LONG.optionalFieldOf("orbital_period", 0L).forGetter(CelestialBody::orbitalPeriod),
            Codec.DOUBLE.optionalFieldOf("orbital_phase", 0.0).forGetter(CelestialBody::orbitalPhase),
            Codec.STRING.optionalFieldOf("parent_id").forGetter(b -> Optional.ofNullable(b.parentId()))
    ).apply(instance, (id, sg, radius, basePos, orbR, orbP, orbPh, parent) ->
            new CelestialBody(id, sg, radius, basePos, orbR, orbP, orbPh, parent.orElse(null))
    ));

    /**
     * Mass derived from surface gravity: {@code m = g · r² / G}. The G·m/r²
     * inverse-square law then gives exactly {@code surfaceGravity} at {@code r = radius}.
     */
    public double mass() {
        if (radius <= 0.0) return 0.0;
        return surfaceGravity * radius * radius / GravityConstants.G;
    }

    public Vec3 positionAtTick(long tick, Vec3 parentCenter) {
        if (orbitalPeriod <= 0 || orbitalRadius <= 0.0) {
            return basePosition;
        }
        double angle = orbitalPhase + (2.0 * Math.PI * tick) / orbitalPeriod;
        double x = parentCenter.x + orbitalRadius * Math.cos(angle);
        double z = parentCenter.z + orbitalRadius * Math.sin(angle);
        return new Vec3(x, parentCenter.y, z);
    }

    /**
     * Acceleration in blocks-per-tick² at the given distance from the body's centre.
     * At {@code distance == radius}, returns exactly {@link #surfaceGravity()}.
     * Falls off as inverse-square outside; clamped at {@link GravityConstants#MAX_GRAVITY}
     * to avoid exploding values near r=0.
     */
    public double gravitationalAcceleration(double distance) {
        if (radius <= 0.0) return 0.0;
        double effectiveDistance = Math.max(distance, radius * 0.25); // clamp inside core
        double ratio = radius / effectiveDistance;
        return surfaceGravity * ratio * ratio;
    }

    public boolean isWithinSurface(double distance) {
        return distance <= radius;
    }

    public boolean isWithinInfluence(double distance) {
        return distance <= radius * GravityConstants.INFLUENCE_MULTIPLIER;
    }
}
