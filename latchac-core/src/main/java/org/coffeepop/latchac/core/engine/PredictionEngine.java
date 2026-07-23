package org.coffeepop.latchac.core.engine;

import org.coffeepop.latchac.core.player.LatchPlayer;

/**
 * Predicts legal movement bounds for the next tick using vanilla Minecraft physics.
 * Pure physics derivation from wiki/mojang source — no cheat source references.
 */
public final class PredictionEngine {
    private PredictionEngine() {}

    public static MotionBounds predict(LatchPlayer p, double slipperiness) {
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
