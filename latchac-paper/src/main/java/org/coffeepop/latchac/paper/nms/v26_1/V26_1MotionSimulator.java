package org.coffeepop.latchac.paper.nms.v26_1;

import org.bukkit.entity.Player;
import org.coffeepop.latchac.core.engine.SimulatorProvider;

import java.lang.reflect.Method;

/**
 * Paper 26.1 NMS motion prediction using Mojang-mapped reflection.
 * MC 26.1+ ships deobfuscated — class/method names are directly accessible.
 */
public class V26_1MotionSimulator implements SimulatorProvider {

    private static final double AIR_FRICTION = 0.91;
    private static final double GROUND_FRICTION = 0.6;
    private static final double GRAVITY = -0.08;
    private static final double JUMP_VELOCITY = 0.42;
    private static final double SPRINT_FACTOR = 1.3;
    private static final double DEFAULT_SLIPPERINESS = 0.6;

    private Method getSpeedMethod;
    private Method getBlockSpeedFactorMethod;

    public V26_1MotionSimulator() {
        try {
            // MC 26.1+ Mojang-mapped classes
            Class<?> livingEntityClass = Class.forName("net.minecraft.world.entity.LivingEntity");
            getSpeedMethod = livingEntityClass.getDeclaredMethod("getSpeed");
            getSpeedMethod.setAccessible(true);
            getBlockSpeedFactorMethod = livingEntityClass.getDeclaredMethod("getBlockSpeedFactor");
            getBlockSpeedFactorMethod.setAccessible(true);
        } catch (Exception e) {
            // Fall back to constants if reflection fails
        }
    }

    @Override
    public double[] predict(Object platformPlayer, double lastDx, double lastDy, double lastDz,
                            boolean onGround, boolean sprinting, boolean sneaking) {
        Player player = (Player) platformPlayer;

        // Get vanilla movement speed attribute
        double moveSpeed = 0.1; // default: 0.7 attr → 0.1 base
        double slipperiness = DEFAULT_SLIPPERINESS;

        try {
            // Get underlying NMS Entity
            var craftPlayerHandle = player.getClass().getMethod("getHandle").invoke(player);
            if (getSpeedMethod != null) {
                moveSpeed = (float) getSpeedMethod.invoke(craftPlayerHandle);
            }
            if (getBlockSpeedFactorMethod != null) {
                slipperiness = (float) getBlockSpeedFactorMethod.invoke(craftPlayerHandle);
            }
        } catch (Exception ignored) {}

        // Apply sprint/sneak modifiers
        if (sprinting) moveSpeed *= SPRINT_FACTOR;
        if (sneaking) moveSpeed *= 0.3;

        double maxDx, maxDz, minDy, maxDy;
        boolean grounded;

        if (onGround) {
            // Ground physics: friction + strafe acceleration
            double friction = GROUND_FRICTION * slipperiness;
            // Maximum possible: current speed preserved by friction + max acceleration
            double accel = moveSpeed * 0.98; // approximate strafe factor
            maxDx = Math.abs(lastDx) * friction + accel;
            maxDz = Math.abs(lastDz) * friction + accel;
            minDy = 0;
            maxDy = JUMP_VELOCITY;
            grounded = true;
        } else {
            // Air physics: drag + minimal acceleration
            maxDx = Math.abs(lastDx) * AIR_FRICTION + moveSpeed * 0.02;
            maxDz = Math.abs(lastDz) * AIR_FRICTION + moveSpeed * 0.02;
            double predictedDy = lastDy + GRAVITY;
            minDy = predictedDy - 0.02;
            maxDy = predictedDy + 0.02;
            grounded = false;
        }

        return new double[]{maxDx, maxDz, minDy, maxDy, grounded ? 1.0 : 0.0};
    }
}
