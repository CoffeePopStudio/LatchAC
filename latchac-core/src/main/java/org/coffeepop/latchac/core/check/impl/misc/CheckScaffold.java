package org.coffeepop.latchac.core.check.impl.misc;

import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckInfo;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.player.LatchPlayer;

/**
 * Scaffold detection: abnormal block placement patterns.
 * 3-dim cross-validation: pitch stability, placement interval, dxz during placement.
 */
@CheckInfo(name = "Scaffold", type = CheckType.MISC, maxVL = 25)
public class CheckScaffold extends Check {

    private static final int SCORE_THRESHOLD = 2;

    public CheckScaffold() {}

    @Override
    public void onCheck(LatchPlayer p) {
        double interval = p.getScaffoldInterval();
        if (interval <= 0) return;

        var profiler = LatchAC.get().getBaselineProfiler();
        var id = p.getUniqueId();
        int score = 0;

        // 1. Pitch stability (scaffold = fixed downward angle ~70-85°)
        float pitch = p.getScaffoldPitch();
        profiler.update(id, "scaffoldPitch", pitch);
        var pitchMetric = profiler.getMetric(id, "scaffoldPitch");
        if (pitchMetric != null && pitchMetric.zScore() > 3.0
                && Math.abs(pitchMetric.getCurrentValue() - pitchMetric.getBaseline()) < pitchMetric.getBaselineStd() * 0.5) {
            score++;
        }

        // 2. Placement interval (scaffold = rapid consistent rhythm)
        profiler.update(id, "scaffoldInterval", interval);
        var intMetric = profiler.getMetric(id, "scaffoldInterval");
        if (intMetric != null && intMetric.zScore() > 3.0
                && intMetric.getCurrentValue() < intMetric.getBaseline() * 0.5) {
            score++;
        }

        // 3. DXZ during placement (scaffold = precise backward stepping)
        double dxz = p.getScaffoldDXZ();
        if (dxz > 0.01) {
            profiler.update(id, "scaffoldDXZ", dxz);
            var dxzMetric = profiler.getMetric(id, "scaffoldDXZ");
            if (dxzMetric != null && dxzMetric.zScore() > 3.0) {
                score++;
            }
        }

        if (score >= SCORE_THRESHOLD) {
            flag(p, "scaffold=" + score
                    + " pitch=" + String.format("%.1f", pitch)
                    + " intv=" + String.format("%.1f", interval) + "ms");
        }
    }
}
