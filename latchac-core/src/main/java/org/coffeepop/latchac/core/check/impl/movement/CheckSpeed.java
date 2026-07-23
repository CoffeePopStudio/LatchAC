package org.coffeepop.latchac.core.check.impl.movement;

import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckInfo;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.player.LatchPlayer;

/**
 * Detects speed hacks via BaselineProfiler — compares current deltaXZ
 * against the player's own historical movement pattern.
 */
@CheckInfo(name = "Speed", type = CheckType.MOVEMENT, maxVL = 35)
public class CheckSpeed extends Check {

    public CheckSpeed() {}

    @Override
    public void onCheck(LatchPlayer p) {
        if (!p.hasPosition()) return;
        if (p.isInVehicle()) return;
        if (p.isInLiquid()) return;
        if (p.shouldExemptMovement()) return;

        double dxz = p.getDeltaXZ();
        if (dxz < 0.001) return;

        var profiler = LatchAC.get().getBaselineProfiler();
        var id = p.getUniqueId();

        profiler.update(id, "movementSpeed", dxz);

        var metric = profiler.getMetric(id, "movementSpeed");
        if (metric != null && metric.isAnomalous() && metric.getCurrentValue() > metric.getBaseline()) {
            flag(p, "dxz=" + String.format("%.4f", dxz)
                    + " baseline=" + String.format("%.4f", metric.getBaseline())
                    + " z=" + String.format("%.1f", metric.zScore()));
        }
    }
}
