package org.chabelabela.outer_crafts.physics;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.chabelabela.outer_crafts.registry.CelestialBodyRegistry;

import java.util.Map;

public final class GravityManager {

    private GravityManager() {}

    public record GravityState(
            Vec3 gravityVector,
            Vec3 localUp,
            CelestialBody dominantBody,
            double distanceToCenter
    ) {
        public static final GravityState DEEP_SPACE = new GravityState(
                Vec3.ZERO, new Vec3(0, 1, 0), null, Double.MAX_VALUE
        );

        public boolean isInDeepSpace() { return dominantBody == null; }

        public boolean isOnSurface() {
            return dominantBody != null && distanceToCenter <= dominantBody.radius() + 2.0;
        }
    }

    public static GravityState computeGravityState(Entity entity, long tick) {
        return computeGravityStateForPosition(entity.position(), tick);
    }

    public static GravityState computeGravityStateForPosition(Vec3 position, long tick) {
        Map<String, Vec3> resolvedPositions = CelestialBodyRegistry.resolveAllPositions(tick);

        CelestialBody dominantBody = null;
        Vec3 dominantCenter = Vec3.ZERO;
        double strongestAccel = 0.0;
        double dominantDistance = Double.MAX_VALUE;

        for (var entry : CelestialBodyRegistry.allBodies().entrySet()) {
            CelestialBody body = entry.getValue();
            Vec3 bodyCenter = resolvedPositions.get(body.id());
            if (bodyCenter == null) continue;

            double distance = position.distanceTo(bodyCenter);
            if (!body.isWithinInfluence(distance)) continue;

            double accel = body.gravitationalAcceleration(distance);
            if (accel > strongestAccel) {
                strongestAccel = accel;
                dominantBody = body;
                dominantCenter = bodyCenter;
                dominantDistance = distance;
            }
        }

        if (dominantBody == null || strongestAccel < GravityConstants.GRAVITY_EPSILON) {
            return GravityState.DEEP_SPACE;
        }

        Vec3 gravityVector = SphericalGravityHelper.computeGravityVector(position, dominantCenter, dominantBody);
        Vec3 localUp = SphericalGravityHelper.computeLocalUp(position, dominantCenter);
        return new GravityState(gravityVector, localUp, dominantBody, dominantDistance);
    }

    public static Vec3 getGravityVector(Entity entity, long tick) {
        return computeGravityState(entity, tick).gravityVector();
    }

    public static Vec3 getLocalUp(Entity entity, long tick) {
        return computeGravityState(entity, tick).localUp();
    }
}
