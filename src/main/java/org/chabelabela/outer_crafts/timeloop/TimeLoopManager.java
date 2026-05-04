package org.chabelabela.outer_crafts.timeloop;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.network.TimeLoopNetworking;

import java.util.List;

public final class TimeLoopManager {

    private TimeLoopManager() {}

    private static long currentTick = 0;
    private static TimeLoopPhase phase = TimeLoopPhase.RUNNING;
    private static int loopCount = 0;
    private static boolean active = true;
    private static MinecraftServer serverInstance;

    public static void onServerTickEnd(ServerTickEvent.Post event) {
        onServerTick(event.getServer());
    }

    private static void onServerTick(MinecraftServer server) {
        serverInstance = server;

        if (!active) return;

        currentTick++;

        switch (phase) {
            case RUNNING -> tickRunning(server);
            case SUPERNOVA_WARNING -> tickWarning(server);
            case SUPERNOVA -> tickSupernova(server);
            case RESETTING -> tickResetting(server);
        }
    }

    private static void tickRunning(MinecraftServer server) {
        if (currentTick >= TimeLoopConstants.WARNING_START_TICK) {
            transitionTo(TimeLoopPhase.SUPERNOVA_WARNING, server);
        }

        if (currentTick % 100 == 0) {
            syncToAllClients(server);
        }
    }

    private static void tickWarning(MinecraftServer server) {
        if (currentTick >= TimeLoopConstants.LOOP_DURATION_TICKS) {
            transitionTo(TimeLoopPhase.SUPERNOVA, server);
            SupernovaManager.beginSupernova(server);
            return;
        }

        double warningProgress = getWarningProgress();
        SupernovaManager.tickSunExpansion(warningProgress);

        syncToAllClients(server);
    }

    private static void tickSupernova(MinecraftServer server) {
        long supernovaTick = currentTick - TimeLoopConstants.LOOP_DURATION_TICKS;

        if (supernovaTick >= TimeLoopConstants.SUPERNOVA_DURATION) {
            killAllPlayers(server);
            transitionTo(TimeLoopPhase.RESETTING, server);
            return;
        }

        SupernovaManager.tickDeathWave(server, supernovaTick);
        syncToAllClients(server);
    }

    private static void tickResetting(MinecraftServer server) {
        long resetTick = currentTick - TimeLoopConstants.LOOP_DURATION_TICKS
                - TimeLoopConstants.SUPERNOVA_DURATION;

        if (resetTick >= TimeLoopConstants.RESET_DURATION) {
            executeLoopReset(server);
            return;
        }

        syncToAllClients(server);
    }

    private static void transitionTo(TimeLoopPhase newPhase, MinecraftServer server) {
        TimeLoopPhase oldPhase = phase;
        phase = newPhase;
        OuterCrafts.LOGGER.info("[Time Loop] Phase transition: {} → {} at tick {}",
                oldPhase, newPhase, currentTick);
        syncToAllClients(server);
    }

    private static void executeLoopReset(MinecraftServer server) {
        OuterCrafts.LOGGER.info("[Time Loop] Executing loop reset. Completing loop #{}.", loopCount);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ShipLogData.saveForPlayer(player);
        }

        SupernovaManager.reset();

        // Send each player back to the surface of Timber Hearth (Pillar 6) and
        // refill their suit. Falls back to the overworld spawn if anything in the
        // solar-system path is missing (e.g. dimension still loading).
        ServerLevel solar = server.getLevel(
                org.chabelabela.outer_crafts.world.dimension.OuterCraftsDimensions.SOLAR_SYSTEM_LEVEL);
        var positions = org.chabelabela.outer_crafts.registry.CelestialBodyRegistry
                .resolveAllPositions(solar != null ? solar.getGameTime() : 0);
        var th = org.chabelabela.outer_crafts.registry.CelestialBodyRegistry.get("timber_hearth");
        var thCenter = positions.get("timber_hearth");

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.isDeadOrDying()) {
                player.setHealth(player.getMaxHealth());
            }

            if (solar != null && th != null && thCenter != null) {
                double surfaceY = thCenter.y + th.radius() + 4.0;
                player.teleportTo(solar, thCenter.x, surfaceY, thCenter.z,
                        java.util.Set.of(), 0.0f, 0.0f, true);
            } else {
                ServerLevel overworld = server.overworld();
                var respawnData = overworld.getLevelData().getRespawnData();
                var spawnPos = respawnData.pos();
                player.teleportTo(overworld,
                        spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                        java.util.Set.of(), 0.0f, 0.0f, true);
            }

            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);

            // Refill the spacesuit (oxygen + propellant) so each loop starts fresh.
            org.chabelabela.outer_crafts.equipment.SpacesuitData.getOrCreate(player).reset();

            ShipLogData.loadForPlayer(player);
        }

        loopCount++;
        currentTick = 0;
        phase = TimeLoopPhase.RUNNING;

        syncToAllClients(server);
        OuterCrafts.LOGGER.info("[Time Loop] Loop #{} started.", loopCount);
    }

    public static void onPlayerDeath(ServerPlayer player) {
        if (phase == TimeLoopPhase.RESETTING) return;

        ShipLogData.saveForPlayer(player);

        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        MinecraftServer server = serverLevel.getServer();

        List<ServerPlayer> alivePlayers = server.getPlayerList().getPlayers().stream()
                .filter(p -> !p.isDeadOrDying())
                .toList();

        if (alivePlayers.isEmpty()) {
            OuterCrafts.LOGGER.info("[Time Loop] All players dead. Triggering loop reset.");
            phase = TimeLoopPhase.RESETTING;
            currentTick = TimeLoopConstants.LOOP_DURATION_TICKS + TimeLoopConstants.SUPERNOVA_DURATION;
            syncToAllClients(server);
        }
    }

    private static void killAllPlayers(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isDeadOrDying()) {
                player.kill(server.overworld());
            }
        }
    }

    private static void syncToAllClients(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            TimeLoopNetworking.sendTimeLoopState(player, currentTick, phase, loopCount);
        }
    }

    public static double getLoopProgress() {
        return Math.clamp((double) currentTick / TimeLoopConstants.LOOP_DURATION_TICKS, 0.0, 1.0);
    }

    public static double getWarningProgress() {
        if (phase != TimeLoopPhase.SUPERNOVA_WARNING) return 0.0;
        long warningTick = currentTick - TimeLoopConstants.WARNING_START_TICK;
        return Math.clamp((double) warningTick / TimeLoopConstants.WARNING_PHASE_DURATION, 0.0, 1.0);
    }

    public static double getSupernovaProgress() {
        if (phase != TimeLoopPhase.SUPERNOVA) return 0.0;
        long supernovaTick = currentTick - TimeLoopConstants.LOOP_DURATION_TICKS;
        return Math.clamp((double) supernovaTick / TimeLoopConstants.SUPERNOVA_DURATION, 0.0, 1.0);
    }

    public static double getResetProgress() {
        if (phase != TimeLoopPhase.RESETTING) return 0.0;
        long resetTick = currentTick - TimeLoopConstants.LOOP_DURATION_TICKS
                - TimeLoopConstants.SUPERNOVA_DURATION;
        return Math.clamp((double) resetTick / TimeLoopConstants.RESET_DURATION, 0.0, 1.0);
    }

    public static TimeLoopPhase getPhase() { return phase; }
    public static long getCurrentTick() { return currentTick; }
    public static int getLoopCount() { return loopCount; }
    public static boolean isActive() { return active; }

    public static void setActive(boolean value) { active = value; }

    public static void forceReset() {
        if (serverInstance != null) {
            phase = TimeLoopPhase.RESETTING;
            currentTick = TimeLoopConstants.LOOP_DURATION_TICKS + TimeLoopConstants.SUPERNOVA_DURATION;
        }
    }

    public static int getRemainingSeconds() {
        long remaining = TimeLoopConstants.LOOP_DURATION_TICKS - currentTick;
        return (int) Math.max(0, remaining / 20);
    }
}
