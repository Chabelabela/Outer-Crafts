package org.chabelabela.outer_crafts.physics;

import net.minecraft.world.phys.Vec3;

public final class SphericalGravityHelper {

    private SphericalGravityHelper() {}

    public static Vec3 computeGravityDirection(Vec3 entityPos, Vec3 bodyCenter) {
        Vec3 delta = bodyCenter.subtract(entityPos);
        double length = delta.length();
        if (length < 1.0E-9) {
            return Vec3.ZERO;
        }
        return delta.scale(1.0 / length);
    }

    public static Vec3 computeGravityVector(Vec3 entityPos, Vec3 bodyCenter, CelestialBody body) {
        Vec3 direction = computeGravityDirection(entityPos, bodyCenter);
        if (direction.equals(Vec3.ZERO)) {
            return Vec3.ZERO;
        }
        double distance = entityPos.distanceTo(bodyCenter);
        double accel = body.gravitationalAcceleration(distance);
        accel = Math.min(accel, GravityConstants.MAX_GRAVITY);
        return direction.scale(accel);
    }

    public static Vec3 computeLocalUp(Vec3 entityPos, Vec3 bodyCenter) {
        Vec3 down = computeGravityDirection(entityPos, bodyCenter);
        return down.scale(-1.0);
    }

    public static Vec3 projectOntoTangentPlane(Vec3 movement, Vec3 localUp) {
        if (localUp.equals(Vec3.ZERO)) {
            return movement;
        }
        double dot = movement.dot(localUp);
        return movement.subtract(localUp.scale(dot));
    }

    public static Vec3 rotateToLocalFrame(Vec3 vec, Vec3 localUp) {
        Vec3 vanillaUp = new Vec3(0, 1, 0);
        if (localUp.equals(Vec3.ZERO)) return vec;
        double dot = vanillaUp.dot(localUp);
        if (dot > 0.99999) return vec;
        if (dot < -0.99999) return new Vec3(vec.x, -vec.y, -vec.z);
        Vec3 axis = vanillaUp.cross(localUp);
        double axisLength = axis.length();
        if (axisLength < 1.0E-9) return vec;
        axis = axis.scale(1.0 / axisLength);
        double angle = Math.acos(Math.clamp(dot, -1.0, 1.0));
        return rodriguesRotation(vec, axis, angle);
    }

    public static Vec3 rodriguesRotation(Vec3 v, Vec3 axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double dotAV = axis.dot(v);
        Vec3 cross = axis.cross(v);
        return v.scale(cos).add(cross.scale(sin)).add(axis.scale(dotAV * (1.0 - cos)));
    }

    public static float[] localUpToAngles(Vec3 localUp) {
        double pitch = Math.asin(Math.clamp(-localUp.y, -1.0, 1.0));
        double yaw = Math.atan2(-localUp.x, localUp.z);
        return new float[]{(float) Math.toDegrees(pitch), (float) Math.toDegrees(yaw)};
    }

    public static Vec3 slerpUp(Vec3 from, Vec3 to, double partialTick) {
        double dot = Math.clamp(from.dot(to), -1.0, 1.0);
        double theta = Math.acos(dot);
        if (theta < 1.0E-6) return to;
        double sinTheta = Math.sin(theta);
        double a = Math.sin((1.0 - partialTick) * theta) / sinTheta;
        double b = Math.sin(partialTick * theta) / sinTheta;
        return from.scale(a).add(to.scale(b)).normalize();
    }
}
