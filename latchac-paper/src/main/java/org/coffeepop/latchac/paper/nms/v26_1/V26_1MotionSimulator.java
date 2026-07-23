package org.coffeepop.latchac.paper.nms.v26_1;

import net.minecraft.world.entity.LivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.coffeepop.latchac.core.engine.SimulatorProvider;

/**
 * Paper 1.21.3 NMS motion prediction via paperweight-userdev.
 * Direct Mojang-mapped access — no reflection needed.
 */
public class V26_1MotionSimulator implements SimulatorProvider {

    private static final double AIR_FRICTION = 0.91;
    private static final double GROUND_FRICTION = 0.6;
    private static final double GRAVITY = -0.08;
    private static final double JUMP_VELOCITY = 0.42;
    private static final double SPRINT_FACTOR = 1.3;
    private static final double SNEAK_FACTOR = 0.3;
    // From PhysicsConstants.groundSpeedPerTick formula
    private static final double GROUND_SPEED_COEFF = 0.21168;

    @Override
    public double[] predict(Object platformPlayer, double lastDx, double lastDy, double lastDz,
                            boolean onGround, boolean sprinting, boolean sneaking) {
        Player bukkitPlayer = (Player) platformPlayer;
        LivingEntity entity = ((CraftPlayer) bukkitPlayer).getHandle();

        float moveSpeed = entity.getSpeed();
        // getBlockSpeedFactor is protected — default to 0.6 (applies to most blocks)
        float slipperiness = 0.6f;

        if (sprinting) moveSpeed *= SPRINT_FACTOR;
        if (sneaking) moveSpeed *= SNEAK_FACTOR;

        double maxDx, maxDz, minDy, maxDy;
        boolean grounded;

        if (onGround) {
            double friction = GROUND_FRICTION * slipperiness;
            double accel = moveSpeed * 0.98;
            maxDx = Math.abs(lastDx) * friction + accel;
            maxDz = Math.abs(lastDz) * friction + accel;
            minDy = 0;
            maxDy = JUMP_VELOCITY;
            grounded = true;
        } else {
            maxDx = Math.abs(lastDx) * AIR_FRICTION + moveSpeed * 0.02;
            maxDz = Math.abs(lastDz) * AIR_FRICTION + moveSpeed * 0.02;
            double predictedDy = lastDy + GRAVITY;
            minDy = predictedDy - 0.02;
            maxDy = predictedDy + 0.02;
            grounded = false;
        }

        return new double[]{maxDx, maxDz, minDy, maxDy, grounded ? 1.0 : 0.0};
    }

    @Override
    public double getTheoreticalMaxSpeed(Object platformPlayer, double slipperiness,
                                          boolean sprinting, boolean sneaking) {
        Player bukkitPlayer = (Player) platformPlayer;
        LivingEntity entity = ((CraftPlayer) bukkitPlayer).getHandle();

        float moveSpeed = entity.getSpeed();
        if (sprinting) moveSpeed *= SPRINT_FACTOR;
        if (sneaking) moveSpeed *= SNEAK_FACTOR;

        // Minecraft ground movement formula:
        // speed = coeff * attr * (1 - 0.91 * friction) * friction^3
        return GROUND_SPEED_COEFF * moveSpeed * (1.0 - 0.91 * slipperiness) * Math.pow(slipperiness, 3);
    }

    @Override
    public boolean isFallFlying(Object platformPlayer) {
        Player bukkitPlayer = (Player) platformPlayer;
        LivingEntity entity = ((CraftPlayer) bukkitPlayer).getHandle();
        return entity.isFallFlying();
    }
}
