package org.coffeepop.latchac.core.check.impl.combat;

import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckInfo;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.player.LatchPlayer;

@CheckInfo(name = "AutoClick", type = CheckType.COMBAT, maxVL = 30)
public class CheckAutoClick extends Check {

    private static final double MIN_CPS = 5.0;
    private static final double CPS_HARD_MAX = 20.0;

    public CheckAutoClick() {}

    @Override
    public void onCheck(LatchPlayer p) {
        double cps = p.getCPS();
        if (cps < MIN_CPS) return; // not clicking

        var profiler = LatchAC.get().getBaselineProfiler();
        var id = p.getUniqueId();

        profiler.update(id, "cps", cps);
        var cpsMetric = profiler.getMetric(id, "cps");

        int score = 0;

        // CPS anomaly (only above baseline)
        if (cpsMetric != null && cpsMetric.zScore() > 3.0 && cps > cpsMetric.getBaseline()) {
            score++;
        }

        // clickIntervalCV anomaly (already tracked by KillAura)
        var cvMetric = profiler.getMetric(id, "clickIntervalCV");
        if (cvMetric != null && cvMetric.isAnomalous()) {
            score++;
        }

        // Flag: 2+ signals OR CPS > 20 flat threshold
        if (score >= 2 || cps > CPS_HARD_MAX) {
            flag(p, "cps=" + String.format("%.1f", cps)
                    + " score=" + score);
        }
    }
}
