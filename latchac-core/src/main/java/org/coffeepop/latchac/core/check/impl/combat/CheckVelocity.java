package org.coffeepop.latchac.core.check.impl.combat;

import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckInfo;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.player.LatchPlayer;

import java.util.*;

/**
 * Detects velocity (knockback) modifications by comparing server-sent velocity
 * packets with actual player displacement in subsequent ticks.
 * <p>
 * Tracks three violation patterns: ratio deviation, consistency bias, and
 * automated jump reset detection.
 */
@CheckInfo(name = "Velocity", type = CheckType.COMBAT, maxVL = 30)
public class CheckVelocity extends Check {

    private static final int TRACK_TICKS = 8;
    private static final double RATIO_MIN = 0.7;
    private static final double RATIO_MAX = 1.3;
    private static final int DEVIATION_HISTORY_SIZE = 10;
    private static final int CONSISTENCY_THRESHOLD = 7;
    private static final double JR_SUCCESS_THRESHOLD = 0.9;

    private final Map<UUID, VelocityState> states = new HashMap<>();

    public CheckVelocity() {}

    @Override
    public void onVelocity(LatchPlayer p, double vx, double vy, double vz) {
        VelocityState s = states.computeIfAbsent(p.getUniqueId(), id -> new VelocityState());
        s.pendingVX = vx;
        s.pendingVY = vy;
        s.pendingVZ = vz;
        s.tickSinceVelocity = 0;
        s.accumDX = 0;
        s.accumDY = 0;
        s.accumDZ = 0;
        s.jumpResetOpportunity = (vy <= 0 && Math.abs(vx) + Math.abs(vz) > 0.05);
    }

    @Override
    public void onCheck(LatchPlayer p) {
        VelocityState s = states.get(p.getUniqueId());
        if (s == null || s.tickSinceVelocity >= TRACK_TICKS) {
            states.remove(p.getUniqueId());
            return;
        }

        s.tickSinceVelocity++;
        s.accumDX += p.getDeltaX();
        s.accumDY += p.getDeltaY();
        s.accumDZ += p.getDeltaZ();

        if (s.tickSinceVelocity < TRACK_TICKS) return;

        // ---- A. Expected vs Actual Ratio ----
        if (Math.abs(s.pendingVX) + Math.abs(s.pendingVZ) > 0.01) {
            double totalExpected = Math.hypot(s.pendingVX, s.pendingVZ);
            double totalActual = Math.hypot(s.accumDX, s.accumDZ);
            if (totalExpected > 0.01) {
                double ratio = totalActual / totalExpected;
                if (ratio < RATIO_MIN || ratio > RATIO_MAX) {
                    flag(p, "ratio=" + String.format("%.2f", ratio)
                            + " expected=" + String.format("%.3f", totalExpected));
                }

                double dotProduct = s.pendingVX * s.accumDX + s.pendingVZ * s.accumDZ;
                if (dotProduct < -0.01) {
                    flag(p, "reversal dot=" + String.format("%.4f", dotProduct));
                }
            }
        }

        // ---- B. Consistency Scoring ----
        double deviation = Math.abs(s.accumDX - s.pendingVX * 0.9)
                + Math.abs(s.accumDZ - s.pendingVZ * 0.9);
        s.deviationHistory.addLast(deviation);
        if (s.deviationHistory.size() > DEVIATION_HISTORY_SIZE) s.deviationHistory.pollFirst();
        if (s.deviationHistory.size() >= DEVIATION_HISTORY_SIZE) {
            int consistentCount = 0;
            for (double d : s.deviationHistory) {
                if (d < 0.1) consistentCount++;
            }
            if (consistentCount >= CONSISTENCY_THRESHOLD) {
                flag(p, "consistency=" + consistentCount + "/" + DEVIATION_HISTORY_SIZE);
            }
        }

        // ---- C. Jump Reset Automation ----
        if (s.jumpResetOpportunity) {
            s.jumpResetAttempts++;
            if (p.getDeltaY() > 0.3 && s.tickSinceVelocity <= 2) {
                s.jumpResetSuccesses++;
            }
            if (s.jumpResetAttempts >= 10) {
                double rate = (double) s.jumpResetSuccesses / s.jumpResetAttempts;
                if (rate > JR_SUCCESS_THRESHOLD) {
                    flag(p, "jr_auto=" + String.format("%.0f%%", rate * 100));
                }
                s.jumpResetAttempts = 0;
                s.jumpResetSuccesses = 0;
            }
        }

        states.remove(p.getUniqueId());
    }

    private static class VelocityState {
        double pendingVX, pendingVY, pendingVZ;
        int tickSinceVelocity;
        double accumDX, accumDY, accumDZ;
        boolean jumpResetOpportunity;
        int jumpResetAttempts;
        int jumpResetSuccesses;
        final Deque<Double> deviationHistory = new ArrayDeque<>();
    }

    @Override
    public void onQuit(UUID playerId) {
        states.remove(playerId);
    }
}
