package org.chabelabela.outer_crafts.equipment;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.chabelabela.outer_crafts.physics.GravityEntityAccess;
import org.chabelabela.outer_crafts.physics.GravityManager;
import org.chabelabela.outer_crafts.physics.SphericalGravityHelper;
import org.chabelabela.outer_crafts.world.dimension.OuterCraftsDimensions;

public final class JetpackHandler {

    private JetpackHandler() {}

    public static void applyThrust(ServerPlayer player, Vec3 input) {
        SpacesuitData.SuitState suit = SpacesuitData.getOrCreate(player);
        if (!suit.isChestplateEquipped() || !suit.isJetpackActive()) return;

        boolean hasFuel = suit.drainPropellant(SpacesuitConstants.PROPELLANT_DRAIN_PER_TICK);
        if (!hasFuel) {
            suit.setJetpackActive(false);
            return;
        }

        Vec3 thrust;
        boolean inSolarSystem = player.level().dimension().equals(OuterCraftsDimensions.SOLAR_SYSTEM_LEVEL);

        // Compute gravity state at most once per call. Cached body positions
        // make this cheap, but the deep-space test was previously redundant.
        GravityManager.GravityState gravState = null;
        boolean inDeepSpace = false;

        if (inSolarSystem) {
            long tick = player.level().getGameTime();
            gravState = GravityManager.computeGravityState(player, tick);
            inDeepSpace = gravState.isInDeepSpace();

            thrust = inDeepSpace
                    ? computeZeroGThrust(player, input)
                    : computeSurfaceThrust(player, input, gravState);
        } else {
            thrust = computeVanillaThrust(player, input);
        }

        Vec3 currentVelocity = player.getDeltaMovement();
        Vec3 newVelocity = currentVelocity.add(thrust);

        double speed = newVelocity.length();
        if (speed > SpacesuitConstants.JETPACK_MAX_VELOCITY) {
            newVelocity = newVelocity.normalize().scale(SpacesuitConstants.JETPACK_MAX_VELOCITY);
        }

        // Apply zero-G drag only in deep space — reuse the gravState we just computed.
        if (inDeepSpace) {
            newVelocity = newVelocity.scale(1.0 - SpacesuitConstants.ZERO_G_DRAG);
        }

        player.setDeltaMovement(newVelocity);
        player.hurtMarked = true;
    }

    private static Vec3 computeZeroGThrust(ServerPlayer player, Vec3 input) {
        Vec3 lookDir = player.getLookAngle();
        double thrust = SpacesuitConstants.JETPACK_THRUST;

        Vec3 forward = lookDir.normalize().scale(input.z * thrust);

        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = lookDir.cross(up).normalize();
        if (right.lengthSqr() < 1e-8) {
            right = new Vec3(1, 0, 0);
        }
        Vec3 strafe = right.scale(input.x * thrust);

        Vec3 vertical = up.scale(input.y * thrust);

        return forward.add(strafe).add(vertical);
    }

    private static Vec3 computeSurfaceThrust(ServerPlayer player, Vec3 input,
                                              GravityManager.GravityState gravState) {
        Vec3 localUp = gravState.localUp();
        double thrust = SpacesuitConstants.JETPACK_THRUST;

        Vec3 vertical = localUp.scale(
                input.y * thrust * SpacesuitConstants.JETPACK_VERTICAL_THRUST_MULTIPLIER
        );

        Vec3 lookDir = player.getLookAngle();
        Vec3 tangentForward = SphericalGravityHelper.projectOntoTangentPlane(lookDir, localUp)
                .normalize();
        if (tangentForward.lengthSqr() < 1e-8) {
            tangentForward = SphericalGravityHelper.projectOntoTangentPlane(
                    new Vec3(0, 0, 1), localUp).normalize();
        }
        Vec3 tangentRight = tangentForward.cross(localUp).normalize();

        Vec3 forward = tangentForward.scale(input.z * thrust);
        Vec3 strafe = tangentRight.scale(input.x * thrust);

        return vertical.add(forward).add(strafe);
    }

    private static Vec3 computeVanillaThrust(ServerPlayer player, Vec3 input) {
        double thrust = SpacesuitConstants.JETPACK_THRUST;
        Vec3 up = new Vec3(0, 1, 0);

        Vec3 vertical = up.scale(input.y * thrust * SpacesuitConstants.JETPACK_VERTICAL_THRUST_MULTIPLIER);

        Vec3 lookDir = player.getLookAngle();
        Vec3 horizontalLook = new Vec3(lookDir.x, 0, lookDir.z).normalize();
        if (horizontalLook.lengthSqr() < 1e-8) {
            horizontalLook = new Vec3(0, 0, 1);
        }
        Vec3 right = horizontalLook.cross(up).normalize();

        Vec3 forward = horizontalLook.scale(input.z * thrust);
        Vec3 strafe = right.scale(input.x * thrust);

        return vertical.add(forward).add(strafe);
    }
}
