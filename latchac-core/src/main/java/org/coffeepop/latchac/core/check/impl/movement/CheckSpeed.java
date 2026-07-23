package org.coffeepop.latchac.core.check.impl.movement;

import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckInfo;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.player.LatchPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects speed hacks by comparing actual movement with physics-based predictions.
 * <p>
 * Uses vanilla Minecraft movement physics to compute the maximum legal horizontal
 * speed per tick. Movements exceeding this bound with a 15% tolerance are flagged.
 * Additionally detects impossible strafe ratios and automated BHop patterns.
 */
@CheckInfo(name = "Speed", type = CheckType.MOVEMENT, maxVL = 35)
public class CheckSpeed extends Check {

    private static final double TOLERANCE = 1.15;
    private static final double MAX_STRAFE_RATIO = 1.3;
    private static final double GROUND_FRICTION = 0.546;
    private static final double AIR_FRICTION = 0.91;
    private static final double BASE_ACCEL = 0.1;
    private static final double SPRINT_FACTOR = 1.3;
    private static final int BHOP_CONSISTENT_THRESHOLD = 5;

    private final Map<UUID, SpeedState> states = new ConcurrentHashMap<>();

    public CheckSpeed() {}

    @Override
    public void onCheck(LatchPlayer p) {
        if (!p.hasPosition()) return;
        if (p.isInVehicle()) return;
        if (p.isInLiquid()) return;
        if (p.shouldExemptMovement()) {
            states.remove(p.getUniqueId());
            return;
        }

        double deltaXZ = p.getDeltaXZ();
        if (deltaXZ < 0.001) {
            states.remove(p.getUniqueId());
            return;
        }

        SpeedState s = states.computeIfAbsent(p.getUniqueId(), id -> new SpeedState());

        // ---- A. Physics Simulation ----
        double friction = p.isOnGround() ? GROUND_FRICTION : AIR_FRICTION;
        double maxSpeed = BASE_ACCEL * (p.isSprinting() ? SPRINT_FACTOR : 1.0);
        double predictedMax = s.lastSpeed * friction + maxSpeed * TOLERANCE;

        if (deltaXZ > predictedMax && s.lastSpeed > 0.05) {
            flagAndSetback(p, "deltaXZ=" + String.format("%.4f", deltaXZ)
                    + " max=" + String.format("%.4f", predictedMax)
                    + " friction=" + String.format("%.3f", friction));
        }

        s.lastSpeed = deltaXZ;
        s.lastFriction = friction;

        // ---- B. Strafe Constraint ----
        double forwardSpeed = Math.abs(deltaXZ * Math.cos(Math.toRadians(p.getDeltaYaw())));
        double lateralSpeed = Math.abs(deltaXZ * Math.sin(Math.toRadians(p.getDeltaYaw())));
        if (forwardSpeed > 0.15 && lateralSpeed / forwardSpeed > MAX_STRAFE_RATIO) {
            flag(p, "strafe=" + String.format("%.3f", lateralSpeed / forwardSpeed));
        }

        // ---- D. Jump Pattern Analysis (BHop detection) ----
        double dy = p.getDeltaY();
        if (dy > 0.3 && dy < 0.45 && !p.isOnGround()) {
            int tick = s.bhopStreak;
            if (tick > 0 && Math.abs(tick - s.lastJumpTick) <= 1) {
                s.consistentJumpCount++;
            } else {
                s.consistentJumpCount = 1;
            }
            s.lastJumpTick = 0;
            s.bhopStreak++;
            if (s.consistentJumpCount >= BHOP_CONSISTENT_THRESHOLD) {
                flag(p, "bhop=" + s.consistentJumpCount);
                s.consistentJumpCount = 0;
            }
        } else if (s.lastJumpTick >= 0) {
            s.lastJumpTick++;
        }
    }

    private static class SpeedState {
        double lastSpeed;
        double lastFriction;
        int bhopStreak;
        int lastJumpTick = -1;
        int consistentJumpCount;
    }

    @Override
    public void onQuit(UUID playerId) {
        states.remove(playerId);
    }
}
