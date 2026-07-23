package org.coffeepop.latchac.core.engine;

/**
 * Core-facing interface for platform-specific NMS motion simulation.
 * Implemented by version-specific NMS modules in the Paper layer.
 */
public interface SimulatorProvider {
    /**
     * @return predicted {maxDx, maxDz, minDy, maxDy, grounded(1.0/0.0)}
     */
    double[] predict(Object platformPlayer, double lastDx, double lastDy, double lastDz,
                     boolean onGround, boolean sprinting, boolean sneaking);

    /**
     * Returns the theoretical max horizontal movement speed for one tick,
     * accounting for entity attributes, sprint/sneak, and foot block friction.
     * Used by Movement Efficiency metric (dxz / theoretical).
     */
    double getTheoreticalMaxSpeed(Object platformPlayer, double slipperiness,
                                   boolean sprinting, boolean sneaking);

    /**
     * Whether the player is currently elytra-flying (fall-flying).
     * Returns false if platform does not support NMS lookup.
     */
    default boolean isFallFlying(Object platformPlayer) { return false; }
}
