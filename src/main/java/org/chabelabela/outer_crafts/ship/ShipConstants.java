package org.chabelabela.outer_crafts.ship;

public final class ShipConstants {

    private ShipConstants() {}

    public static final double MAIN_THRUST = 0.12;
    public static final double LATERAL_THRUST = 0.06;
    public static final double ROTATION_SPEED = 2.5;
    public static final double MAX_VELOCITY = 3.0;
    public static final double ATMOSPHERIC_DRAG = 0.02;
    public static final double SPACE_DRAG = 0.001;

    public static final double MAX_FUEL = 100.0;
    public static final double FUEL_DRAIN_MAIN = 0.08;
    public static final double FUEL_DRAIN_LATERAL = 0.04;
    public static final double FUEL_REFILL_RATE = 1.5;

    public static final int SHIP_OXYGEN_CAPACITY = 36000;
    public static final int SHIP_O2_REFILL_PER_TICK = 10;

    public static final double DAMAGE_VELOCITY_THRESHOLD = 0.8;
    public static final double DAMAGE_MULTIPLIER = 25.0;
    public static final int COMPONENT_MAX_HEALTH = 100;
    public static final int REPAIR_AMOUNT = 20;

    public static final float SHIP_WIDTH = 3.5f;
    public static final float SHIP_HEIGHT = 3.0f;
    public static final double INTERACTION_RANGE = 5.0;
    public static final double COCKPIT_Y_OFFSET = 1.2;
}
