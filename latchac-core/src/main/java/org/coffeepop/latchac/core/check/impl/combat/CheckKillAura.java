package org.coffeepop.latchac.core.check.impl.combat;

import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckInfo;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.player.LatchPlayer;

@CheckInfo(name = "KillAura", type = CheckType.COMBAT, maxVL = 30)
public class CheckKillAura extends Check {

    private static final int SCORE_THRESHOLD = 3;

    public CheckKillAura() {}

    @Override
    public void onCheck(LatchPlayer p) {
        if (!p.hasPosition()) return;

        var profiler = LatchAC.get().getBaselineProfiler();
        var id = p.getUniqueId();

        // Track rotation variance (existing)
        double rotationJitter = Math.abs(p.getDeltaYaw()) + Math.abs(p.getDeltaPitch());
        profiler.update(id, "rotationVariance", rotationJitter);

        // Track turn jerk (new)
        float turnJerk = p.getTurnJerk();
        profiler.update(id, "turnJerk", turnJerk);

        // Track pitch stability (new)
        profiler.update(id, "pitchStability", Math.abs(p.getDeltaPitch()));
    }

    @Override
    public void onAttack(LatchPlayer p, int entityId) {
        var profiler = LatchAC.get().getBaselineProfiler();
        var id = p.getUniqueId();
        int score = 0;

        // 1. Rotation variance low
        var rotMetric = profiler.getMetric(id, "rotationVariance");
        if (rotMetric != null && rotMetric.zScore() > 3.0
                && rotMetric.getCurrentValue() < rotMetric.getBaseline()) {
            score++;
        }

        // 2. Turn jerk low (smooth aim)
        var jerkMetric = profiler.getMetric(id, "turnJerk");
        if (jerkMetric != null && jerkMetric.zScore() > 3.0
                && jerkMetric.getCurrentValue() < jerkMetric.getBaseline()) {
            score++;
        }

        // 3. Pitch stability (locked on target)
        var pitchMetric = profiler.getMetric(id, "pitchStability");
        if (pitchMetric != null && pitchMetric.zScore() > 3.0
                && pitchMetric.getCurrentValue() < pitchMetric.getBaseline()) {
            score++;
        }

        // 4. Click interval CV anomaly
        long now = System.nanoTime();
        long lastAtk = p.getLastAttackTime();
        if (lastAtk > 0) {
            double intervalMs = (now - lastAtk) / 1_000_000.0;
            profiler.update(id, "clickIntervalCV", intervalMs);
            var clickMetric = profiler.getMetric(id, "clickIntervalCV");
            if (clickMetric != null && clickMetric.isAnomalous()) {
                score++;
            }
        }

        // 5. Swing-attack delay low
        long lastSwing = p.getLastSwingTime();
        if (lastSwing > 0) {
            double delayMs = Math.abs(now - lastSwing) / 1_000_000.0;
            profiler.update(id, "swingAttackDelay", delayMs);
            var swingMetric = profiler.getMetric(id, "swingAttackDelay");
            if (swingMetric != null && swingMetric.zScore() > 3.0
                    && swingMetric.getCurrentValue() < swingMetric.getBaseline() * 0.2) {
                score++;
            }
        }

        if (score >= SCORE_THRESHOLD) {
            flag(p, "ka_score=" + score + "/5");
        }
    }
}
