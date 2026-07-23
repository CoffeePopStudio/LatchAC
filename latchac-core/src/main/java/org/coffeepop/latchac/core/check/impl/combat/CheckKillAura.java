package org.coffeepop.latchac.core.check.impl.combat;

import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckInfo;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.player.LatchPlayer;

@CheckInfo(name = "KillAura", type = CheckType.COMBAT, maxVL = 30)
public class CheckKillAura extends Check {

    public CheckKillAura() {}

    @Override
    public void onCheck(LatchPlayer p) {
        if (!p.hasPosition()) return;

        var profiler = LatchAC.get().getBaselineProfiler();
        double rotationJitter = Math.abs(p.getDeltaYaw()) + Math.abs(p.getDeltaPitch());
        profiler.update(p.getUniqueId(), "rotationVariance", rotationJitter);
    }

    @Override
    public void onAttack(LatchPlayer p, int entityId) {
        var profiler = LatchAC.get().getBaselineProfiler();
        var id = p.getUniqueId();
        int vlAdd = 0;

        var rotMetric = profiler.getMetric(id, "rotationVariance");
        if (rotMetric != null && rotMetric.getCurrentValue() < rotMetric.getBaseline() * 0.3) {
            vlAdd += 1;
        }

        long now = System.nanoTime();
        long lastAtk = p.getLastAttackTime();
        if (lastAtk > 0) {
            double intervalMs = (now - lastAtk) / 1_000_000.0;
            profiler.update(id, "clickIntervalCV", intervalMs);
            var clickMetric = profiler.getMetric(id, "clickIntervalCV");
            if (clickMetric != null && clickMetric.isAnomalous()) {
                vlAdd += 1;
            }
        }

        long lastSwing = p.getLastSwingTime();
        if (lastSwing > 0) {
            double delayNs = Math.abs(now - lastSwing);
            profiler.update(id, "swingAttackDelay", delayNs / 1_000_000.0);
            var swingMetric = profiler.getMetric(id, "swingAttackDelay");
            if (swingMetric != null && swingMetric.getCurrentValue() < swingMetric.getBaseline() * 0.2) {
                vlAdd += 1;
            }
        }

        if (vlAdd > 0) {
            flag(p, "ka_score=" + vlAdd);
        }
    }
}
