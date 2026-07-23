package org.coffeepop.latchac.core.engine;

import org.coffeepop.latchac.core.player.LatchPlayer;

/**
 * Predicts legal movement bounds using either platform-specific NMS simulation
 * or pure physics derivation as fallback.
 */
public final class PredictionEngine {
    private static SimulatorProvider simulator;

    private PredictionEngine() {}

    /** Set by platform layer at startup. Uses NMS for precise prediction. */
    public static void setSimulator(SimulatorProvider provider) {
        simulator = provider;
    }

    /** Returns the NMS simulator, or null if not initialized. */
    public static SimulatorProvider getSimulator() { return simulator; }

    public static MotionBounds predict(LatchPlayer p, double slipperiness) {
        // Try NMS simulator first for maximum precision
        if (simulator != null) {
            try {
                double[] nms = simulator.predict(p.getPlatformPlayer(),
                        p.getDeltaX(), p.getDeltaY(), p.getDeltaZ(),
                        p.isOnGround(), p.isSprinting(), p.isSneaking());
                return new MotionBounds(
                        -nms[0], nms[0],
                        nms[2], nms[3],
                        -nms[1], nms[1],
                        p.getY(), nms[4] > 0.5);
            } catch (Exception ignored) {}
        }

        // Fallback: pure physics constants
        return predictFallback(p, slipperiness);
    }

    private static MotionBounds predictFallback(LatchPlayer p, double slipperiness) {
        double lastDx = p.getDeltaX();
        double lastDy = p.getDeltaY();
        double lastDz = p.getDeltaZ();

        boolean onGround = p.isOnGround();
        boolean sprinting = p.isSprinting();
        boolean sneaking = p.isSneaking();

        double friction = onGround ? slipperiness * PhysicsConstants.AIR_DRAG_HORIZONTAL
                                   : PhysicsConstants.AIR_DRAG_HORIZONTAL;
        double speedBase = PhysicsConstants.groundSpeedPerTick(
                PhysicsConstants.DEFAULT_MOVEMENT_SPEED, slipperiness);

        if (sprinting) speedBase *= PhysicsConstants.SPRINT_MULTIPLIER;
        if (sneaking) speedBase *= PhysicsConstants.SNEAK_MULTIPLIER;

        double maxHorizontal = Math.abs(lastDx) * friction + speedBase;
        double maxDx = maxHorizontal;
        double maxDz = maxHorizontal;
        double minDx = -maxHorizontal;
        double minDz = -maxHorizontal;

        double maxDy, minDy, expectedY;
        boolean groundedByPhysics;

        if (onGround) {
            maxDy = PhysicsConstants.JUMP_VELOCITY + 0.1;
            minDy = 0;
            expectedY = p.getY();
            groundedByPhysics = true;
        } else {
            double predictedDy = lastDy + PhysicsConstants.GRAVITY;
            maxDy = predictedDy + 0.02;
            minDy = predictedDy - 0.02;
            expectedY = p.getY() + predictedDy;
            groundedByPhysics = PhysicsConstants.isValidStandingY(expectedY)
                    && Math.abs(predictedDy) < 0.005;
        }

        return new MotionBounds(minDx, maxDx, minDy, maxDy, minDz, maxDz, expectedY, groundedByPhysics);
    }

    public static boolean isPhysicallyGrounded(MotionBounds bounds) {
        return bounds.groundedByPhysics();
    }
}
