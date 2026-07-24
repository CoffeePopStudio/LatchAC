package org.coffeepop.latchac.core.check.impl.movement;

import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckInfo;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.player.LatchPlayer;

/**
 * Detects abnormal vertical movement — now with groundRatio cross-validation.
 * Fly hacks: sustained time in air + abnormal vertical pattern.
 */
@CheckInfo(name = "FlyVertical", type = CheckType.MOVEMENT, maxVL = 25)
public class FlyVertical extends Check {

    public FlyVertical() {}

    @Override
    public void onCheck(LatchPlayer p) {
        if (!p.hasPosition()) return;
        if (p.isInVehicle()) return;
        if (p.isInLiquid()) return;
        if (p.isFlightExempted()) return;

        int score = 0;

        var profiler = LatchAC.get().getBaselineProfiler();
        var id = p.getUniqueId();

        // 1. Vertical speed anomaly (only upward bursts in air)
        if (!p.isOnGround()) {
            double dy = p.getDeltaY();
            if (dy > 0) {
                profiler.update(id, "verticalSpeed", dy);
                var metric = profiler.getMetric(id, "verticalSpeed");
                if (metric != null && metric.zScore() > 3.0 && metric.getCurrentValue() > metric.getBaseline()) {
                    score++;
                }
            }
        }

        // 2. Ground ratio anomaly (spending too much time in air)
        profiler.update(id, "groundRatio", p.getGroundRatio());
        var grMetric = profiler.getMetric(id, "groundRatio");
        if (grMetric != null && grMetric.zScore() > 3.0
                && grMetric.getCurrentValue() < grMetric.getBaseline()) {
            score++;
        }

        if (score >= 2) {
            flag(p, "score=" + score
                    + " dy=" + String.format("%.4f", p.getDeltaY())
                    + " gr=" + String.format("%.2f", p.getGroundRatio()));
        }
    }
}
