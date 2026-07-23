package org.coffeepop.latchac.core.check.impl.combat;

import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckInfo;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.player.LatchPlayer;

import java.util.*;

/**
 * Detects KillAura (Aimbot + AutoClicker + Hitbox + TickBase) through
 * multi-dimensional entropy analysis.
 * <p>
 * Five sub-dimensions: rotation entropy, snap detection, click pattern,
 * multi-target detection, and swing-attack synchronization.
 */
@CheckInfo(name = "KillAura", type = CheckType.COMBAT, maxVL = 30)
public class CheckKillAura extends Check {

    private static final int ROTATION_HISTORY = 40;
    private static final double ENTROPY_THRESHOLD = 2.0;
    private static final int LOW_ENTROPY_TICKS_THRESHOLD = 20;

    private static final double SNAP_SPEED_HIGH = 15.0;
    private static final double SNAP_SPEED_LOW = 0.5;

    private static final int CLICK_HISTORY = 100;
    private static final double CLICK_STD_THRESHOLD = 8.0;
    private static final double CLICK_ACF_THRESHOLD = 0.6;
    private static final int CLICK_MIN_FOR_ANALYSIS = 20;

    private static final long SWING_SYNC_MAX_NS = 50_000_000L;

    private final Map<UUID, KAState> states = new HashMap<>();

    public CheckKillAura() {}

    @Override
    public void onCheck(LatchPlayer p) {
        if (!p.hasPosition()) return;

        KAState s = states.computeIfAbsent(p.getUniqueId(), id -> new KAState());

        // ---- A. Rotation Entropy ----
        double dxz = p.getDeltaXZ();
        if (dxz > 0.001 || s.lastWasAttacking) {
            s.rotationHistory.add(new double[]{p.getDeltaYaw(), p.getDeltaPitch()});
            if (s.rotationHistory.size() > ROTATION_HISTORY) s.rotationHistory.poll();
        } else {
            if (s.lowEntropyTicks > 0) s.lowEntropyTicks--;
        }

        if (s.rotationHistory.size() >= ROTATION_HISTORY) {
            double entropy = computeRotationEntropy(s.rotationHistory);
            s.lastEntropy = entropy;
            if (entropy < ENTROPY_THRESHOLD) {
                s.lowEntropyTicks++;
                if (s.lowEntropyTicks >= LOW_ENTROPY_TICKS_THRESHOLD && s.lastWasAttacking) {
                    flag(p, "entropy=" + String.format("%.2f", entropy)
                            + " ticks=" + s.lowEntropyTicks);
                }
            } else {
                s.lowEntropyTicks = Math.max(0, s.lowEntropyTicks - 2);
            }
        }

        // ---- B. Rotation Snap Detection ----
        float deltaYaw = Math.abs(p.getDeltaYaw());
        double angularSpeed = deltaYaw * Math.PI / 180.0 / 0.05;
        if (s.lastAngularSpeed < SNAP_SPEED_LOW && angularSpeed > SNAP_SPEED_HIGH
                && s.lastWasAttacking) {
            flag(p, "snap from=" + String.format("%.1f", s.lastAngularSpeed)
                    + " to=" + String.format("%.1f", angularSpeed) + " rad/s");
        }
        s.lastAngularSpeed = angularSpeed;

        s.lastWasAttacking = false;
    }

    @Override
    public void onAttack(LatchPlayer p, int entityId) {
        KAState s = states.computeIfAbsent(p.getUniqueId(), id -> new KAState());
        long now = System.nanoTime();
        s.lastWasAttacking = true;

        // ---- C. Click Pattern Analysis ----
        if (s.lastClickTime > 0) {
            long intervalNs = now - s.lastClickTime;
            double intervalMs = intervalNs / 1_000_000.0;
            s.clickIntervals.add(intervalMs);
            if (s.clickIntervals.size() > CLICK_HISTORY) s.clickIntervals.poll();

            if (s.clickIntervals.size() >= CLICK_MIN_FOR_ANALYSIS) {
                double std = computeStd(s.clickIntervals);
                double acf = computeACF(s.clickIntervals);

                if (std < CLICK_STD_THRESHOLD) {
                    s.stableClickCount++;
                    if (s.stableClickCount >= CLICK_HISTORY) {
                        flag(p, "click_std=" + String.format("%.1f", std) + "ms");
                        s.stableClickCount = 0;
                    }
                } else {
                    s.stableClickCount = Math.max(0, s.stableClickCount - 5);
                }

                if (acf > CLICK_ACF_THRESHOLD) {
                    flag(p, "click_acf=" + String.format("%.2f", acf));
                }
            }
        }
        s.lastClickTime = now;

        // ---- D. Multi-Target Detection ----
        int lastTarget = p.getLastTargetId();
        if (lastTarget >= 0 && lastTarget != entityId && s.lastAttackTick == s.ticks) {
            flag(p, "multitarget");
        }
        s.lastAttackTick = s.ticks;
        s.ticks++;

        // ---- E. Swing-Attack Synchronization ----
        long lastSwing = p.getLastSwingTime();
        if (lastSwing > 0) {
            long deltaNs = Math.abs(now - lastSwing);
            if (deltaNs > SWING_SYNC_MAX_NS * 2) {
                s.noSwingAttackCount++;
                if (s.noSwingAttackCount > 5) {
                    flag(p, "noswing=" + s.noSwingAttackCount);
                }
            } else {
                s.noSwingAttackCount = 0;
            }
        }
    }

    private double computeRotationEntropy(Queue<double[]> history) {
        double sumDY = 0, sumDP = 0, sumDY2 = 0, sumDP2 = 0;
        int n = history.size();
        for (double[] d : history) {
            sumDY += d[0];
            sumDP += d[1];
            sumDY2 += d[0] * d[0];
            sumDP2 += d[1] * d[1];
        }
        double varYaw = (sumDY2 / n) - (sumDY / n) * (sumDY / n);
        double varPitch = (sumDP2 / n) - (sumDP / n) * (sumDP / n);
        return Math.log1p(varYaw + varPitch + 0.001) * 2.0;
    }

    private double computeStd(Deque<Double> values) {
        double mean = values.stream().mapToDouble(d -> d).average().orElse(0);
        double variance = values.stream()
                .mapToDouble(d -> Math.pow(d - mean, 2))
                .average().orElse(0);
        return Math.sqrt(variance);
    }

    private double computeACF(Deque<Double> values) {
        List<Double> list = new ArrayList<>(values);
        int n = list.size();
        if (n < 2) return 0;
        double mean = list.stream().mapToDouble(d -> d).average().orElse(0);
        double num = 0, den = 0;
        for (int i = 1; i < n; i++) {
            num += (list.get(i) - mean) * (list.get(i - 1) - mean);
            den += Math.pow(list.get(i) - mean, 2);
        }
        den += Math.pow(list.get(0) - mean, 2);
        return den > 0 ? num / den : 0;
    }

    private static class KAState {
        final Queue<double[]> rotationHistory = new ArrayDeque<>();
        double lastEntropy;
        int lowEntropyTicks;
        double lastAngularSpeed;
        boolean lastWasAttacking;

        final Deque<Double> clickIntervals = new ArrayDeque<>();
        long lastClickTime;
        int stableClickCount;

        int ticks;
        int lastAttackTick = -1;

        int noSwingAttackCount;
    }

    @Override
    public void onQuit(UUID playerId) {
        states.remove(playerId);
    }
}
