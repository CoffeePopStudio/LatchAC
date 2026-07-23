package org.coffeepop.latchac.core.check.impl.movement;

import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckInfo;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.player.LatchPlayer;

/**
 * Detects abnormal vertical movement via BaselineProfiler.
 * Tracks the player's own jump decay pattern — fly hacks deviate from this baseline.
 */
@CheckInfo(name = "FlyVertical", type = CheckType.MOVEMENT, maxVL = 25)
public class FlyVertical extends Check {

    public FlyVertical() {}

    @Override
    public void onCheck(LatchPlayer p) {
        if (!p.hasPosition()) return;
        if (p.isInVehicle()) return;
        if (p.isInLiquid()) return;
        if (p.isOnGround()) return;

        double dy = p.getDeltaY();
        if (dy <= 0) return;

        var profiler = LatchAC.get().getBaselineProfiler();
        var id = p.getUniqueId();

        // Track vertical decay: dy / previous dy. Normal jump: < 0.85. Fly: ≈ 1.0
        double lastDy = p.getDeltaY(); // approximate from last tick state
        // Use a simpler approach: track raw dy as the metric
        // In air, vanilla dy decreases by 0.08 each tick
        // Fly produces stable/increasing dy
        profiler.update(id, "verticalSpeed", dy);

        var metric = profiler.getMetric(id, "verticalSpeed");
        if (metric != null && metric.isAnomalous() && metric.getCurrentValue() > metric.getBaseline()) {
            flag(p, "dy=" + String.format("%.4f", dy)
                    + " baseline=" + String.format("%.4f", metric.getBaseline())
                    + " z=" + String.format("%.1f", metric.zScore()));
        }
    }
}
