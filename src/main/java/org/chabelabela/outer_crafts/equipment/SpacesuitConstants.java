package org.chabelabela.outer_crafts.equipment;

public final class SpacesuitConstants {

    private SpacesuitConstants() {}

    public static final int MAX_OXYGEN = 9000;
    public static final int OXYGEN_DRAIN_PER_TICK = 1;
    public static final int OXYGEN_REFILL_PER_TICK = 20;
    public static final int OXYGEN_TANK_RESTORE = 3000;

    public static final double MAX_PROPELLANT = 100.0;
    public static final double PROPELLANT_DRAIN_PER_TICK = 0.15;
    public static final double PROPELLANT_REFILL_PER_TICK = 2.0;
    public static final double FUEL_CANISTER_RESTORE = 35.0;

    public static final double JETPACK_THRUST = 0.065;
    public static final double JETPACK_VERTICAL_THRUST_MULTIPLIER = 1.4;
    public static final double JETPACK_MAX_VELOCITY = 1.8;
    public static final double ZERO_G_DRAG = 0.005;
    public static final double REFILL_STATION_RANGE = 4.0;

    public static final String[] BREATHABLE_BODIES = {"timber_hearth", "giants_deep"};
}
