package org.coffeepop.latchac.core.engine;

/**
 * Output of PredictionEngine — legal motion bounds for one tick.
 */
public record MotionBounds(
    double minDx,
    double maxDx,
    double minDy,
    double maxDy,
    double minDz,
    double maxDz,
    double expectedY,
    boolean groundedByPhysics
) {
    private static final double TOLERANCE = 1.05;

    public boolean isValidDeltaXZ(double deltaXZ) {
        double max = Math.max(Math.abs(maxDx), Math.abs(maxDz)) * TOLERANCE;
        return deltaXZ <= max;
    }

    public boolean isValidDeltaY(double deltaY) {
        return deltaY >= minDy * TOLERANCE && deltaY <= maxDy * TOLERANCE;
    }
}
