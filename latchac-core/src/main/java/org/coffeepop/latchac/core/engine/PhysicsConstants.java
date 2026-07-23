package org.coffeepop.latchac.core.engine;

/**
 * Verified Minecraft physics constants from minecraft.wiki and Entity.move().
 * Sources: https://minecraft.wiki/w/属性/速度, Entity.java, LivingEntity.java
 */
public final class PhysicsConstants {
    private PhysicsConstants() {}

    public static final double DEFAULT_MOVEMENT_SPEED = 0.7;
    public static final double GROUND_SPEED_COEFFICIENT = 0.21168;
    public static final double SPRINT_MULTIPLIER = 1.3;
    public static final double SNEAK_MULTIPLIER = 0.3;
    public static final double JUMP_VELOCITY = 0.42;
    public static final double JUMP_BOOST_PER_LEVEL = 0.1;
    public static final double GRAVITY = -0.08;
    public static final double AIR_DRAG_HORIZONTAL = 0.91;
    public static final double AIR_DRAG_VERTICAL = 0.98;
    public static final double DEFAULT_SLIPPERINESS = 0.6;
    public static final double WALK_SPEED_PER_TICK = 0.21585;
    public static final double SPRINT_SPEED_PER_TICK = 0.2806;
    public static final double MIN_FREE_FALL_10T = 0.8;
    public static final double SLOWNESS_PER_LEVEL = -0.15;

    public static double speedEffectMultiplier(int level) {
        return 1.0 + 0.2 * level;
    }

    public static double groundSpeedPerTick(double movementAttr, double slipperiness) {
        return GROUND_SPEED_COEFFICIENT * movementAttr
                * (1.0 - 0.91 * slipperiness) * Math.pow(slipperiness, 3);
    }

    public static boolean isValidStandingY(double y) {
        double frac = y - Math.floor(y);
        return Math.abs(frac) < 0.005
                || Math.abs(frac - 0.5) < 0.005
                || Math.abs(frac - 0.5625) < 0.005;
    }
}
