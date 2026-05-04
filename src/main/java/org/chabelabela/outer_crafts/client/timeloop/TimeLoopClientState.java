package org.chabelabela.outer_crafts.client.timeloop;

import org.chabelabela.outer_crafts.network.TimeLoopNetworking.TimeLoopStatePayload;
import org.chabelabela.outer_crafts.timeloop.TimeLoopConstants;
import org.chabelabela.outer_crafts.timeloop.TimeLoopPhase;

/**
 * Client-side mirror of the time loop state, updated via network packets.
 * All rendering code reads from this singleton.
 */
public final class TimeLoopClientState {

    private TimeLoopClientState() {}

    private static long currentTick = 0;
    private static TimeLoopPhase phase = TimeLoopPhase.RUNNING;
    private static int loopCount = 0;
    private static double sunRadius = 200.0;
    private static double deathWaveRadius = 0.0;
    private static float sunR = 1.0f, sunG = 0.95f, sunB = 0.6f;

    /**
     * Called when a TimeLoopStatePayload is received from the server.
     */
    public static void updateFromPayload(TimeLoopStatePayload payload) {
        currentTick = payload.currentTick();
        phase = payload.phase();
        loopCount = payload.loopCount();
        sunRadius = payload.sunRadius();
        deathWaveRadius = payload.deathWaveRadius();
        sunR = payload.sunColorR();
        sunG = payload.sunColorG();
        sunB = payload.sunColorB();
    }

    // ── Queries for renderers ───────────────────────────────────────────────

    public static TimeLoopPhase getPhase() { return phase; }
    public static long getCurrentTick() { return currentTick; }
    public static int getLoopCount() { return loopCount; }
    public static double getSunRadius() { return sunRadius; }
    public static double getDeathWaveRadius() { return deathWaveRadius; }
    public static float getSunR() { return sunR; }
    public static float getSunG() { return sunG; }
    public static float getSunB() { return sunB; }

    /** @return Remaining seconds for HUD display */
    public static int getRemainingSeconds() {
        long remaining = TimeLoopConstants.LOOP_DURATION_TICKS - currentTick;
        return (int) Math.max(0, remaining / 20);
    }

    /** @return MM:SS formatted time remaining */
    public static String getFormattedTimeRemaining() {
        int totalSeconds = getRemainingSeconds();
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return "%02d:%02d".formatted(minutes, seconds);
    }
}
