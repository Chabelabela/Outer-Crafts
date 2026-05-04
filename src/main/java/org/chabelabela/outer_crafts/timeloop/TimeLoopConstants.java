package org.chabelabela.outer_crafts.timeloop;

public final class TimeLoopConstants {

    private TimeLoopConstants() {}

    public static final long LOOP_DURATION_TICKS = 22L * 60L * 20L;
    public static final long WARNING_PHASE_DURATION = 60L * 20L;
    public static final long WARNING_START_TICK = LOOP_DURATION_TICKS - WARNING_PHASE_DURATION;
    public static final long SUPERNOVA_DURATION = 5L * 20L;
    public static final long RESET_DURATION = 8L * 20L;
    public static final double DEATH_WAVE_SPEED = 200.0;
    public static final double SUN_MAX_EXPANSION = 5.0;
    public static final double SUN_EXPANSION_EXPONENT = 3.0;
    public static final String SHIP_LOG_DIRECTORY = "outer_crafts_ship_logs";
}
