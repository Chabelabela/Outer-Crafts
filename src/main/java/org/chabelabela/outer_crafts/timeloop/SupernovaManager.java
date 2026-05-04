package org.chabelabela.outer_crafts.timeloop;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.physics.CelestialBody;
import org.chabelabela.outer_crafts.registry.CelestialBodyRegistry;

public final class SupernovaManager {

    private SupernovaManager() {}

    private static double currentSunRadius = 0.0;
    private static double baseSunRadius = 200.0;
    private static double deathWaveRadius = 0.0;
    private static boolean supernovaActive = false;

    public static void tickSunExpansion(double warningProgress) {
        CelestialBody sun = CelestialBodyRegistry.get("sun");
        if (sun != null) {
            baseSunRadius = sun.radius();
        }

        double expansionFactor = 1.0 + (TimeLoopConstants.SUN_MAX_EXPANSION - 1.0)
                * Math.pow(warningProgress, TimeLoopConstants.SUN_EXPANSION_EXPONENT);

        currentSunRadius = baseSunRadius * expansionFactor;
    }

    public static void beginSupernova(MinecraftServer server) {
        supernovaActive = true;
        deathWaveRadius = currentSunRadius;
        OuterCrafts.LOGGER.info("[Supernova] The Sun has gone supernova! Death wave initiated at radius {}",
                deathWaveRadius);
    }

    public static void tickDeathWave(MinecraftServer server, long supernovaTick) {
        if (!supernovaActive) return;

        deathWaveRadius += TimeLoopConstants.DEATH_WAVE_SPEED;

        long worldTick = server.overworld().getGameTime();
        Vec3 sunCenter = CelestialBodyRegistry.resolvePosition("sun", worldTick);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isDeadOrDying()) continue;

            double distance = player.position().distanceTo(sunCenter);
            if (distance <= deathWaveRadius) {
                player.kill(server.overworld());
                OuterCrafts.LOGGER.debug("[Supernova] Death wave reached player {} at distance {}",
                        player.getName().getString(), distance);
            }
        }
    }

    public static void reset() {
        currentSunRadius = baseSunRadius;
        deathWaveRadius = 0.0;
        supernovaActive = false;
    }

    public static double getCurrentSunRadius() { return currentSunRadius; }
    public static double getDeathWaveRadius() { return deathWaveRadius; }
    public static boolean isSupernovaActive() { return supernovaActive; }

    public static float[] computeSunColor(double warningProgress, double supernovaProgress) {
        if (supernovaProgress > 0.0) {
            float t = (float) supernovaProgress;
            return new float[]{
                    1.0f,
                    lerp(0.3f, 1.0f, t),
                    lerp(0.1f, 1.0f, t)
            };
        }

        if (warningProgress > 0.0) {
            float t = (float) warningProgress;
            if (t < 0.5f) {
                float p = t * 2.0f;
                return new float[]{
                        lerp(1.0f, 0.4f, p),
                        lerp(0.95f, 0.5f, p),
                        lerp(0.6f, 1.0f, p)
                };
            } else {
                float p = (t - 0.5f) * 2.0f;
                return new float[]{
                        lerp(0.4f, 1.0f, p),
                        lerp(0.5f, 0.3f, p),
                        lerp(1.0f, 0.1f, p)
                };
            }
        }

        return new float[]{1.0f, 0.95f, 0.6f};
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
