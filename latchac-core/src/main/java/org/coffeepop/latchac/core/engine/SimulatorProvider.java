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
}
