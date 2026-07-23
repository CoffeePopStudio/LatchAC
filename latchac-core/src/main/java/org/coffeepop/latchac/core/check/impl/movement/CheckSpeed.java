package org.coffeepop.latchac.core.check.impl.movement;

import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckInfo;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.engine.PredictionEngine;
import org.coffeepop.latchac.core.player.LatchPlayer;

/**
 * Speed hack detection: individual baseline + NMS-powered movement efficiency.
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

        // Elytra fall-flying has unique physics — exempt
        var sim = PredictionEngine.getSimulator();
        if (sim != null && sim.isFallFlying(p.getPlatformPlayer())) return;

        double dxz = p.getDeltaXZ();
        if (dxz < 0.001) return;

        var profiler = LatchAC.get().getBaselineProfiler();
        var id = p.getUniqueId();
        int score = 0;

        // 1. Movement speed baseline (existing)
        profiler.update(id, "movementSpeed", dxz);
        var speedMetric = profiler.getMetric(id, "movementSpeed");
        if (speedMetric != null && speedMetric.zScore() > 3.0
                && speedMetric.getCurrentValue() > speedMetric.getBaseline()) {
            score++;
        }

        // 2. Movement efficiency vs NMS theoretical max
        if (sim != null && p.isOnGround()) {
            double theoretical = sim.getTheoreticalMaxSpeed(p.getPlatformPlayer(),
                    p.getFootSlipperiness(), p.isSprinting(), p.isSneaking());
            if (theoretical > 0.001) {
                double efficiency = dxz / theoretical;
                profiler.update(id, "movementEfficiency", efficiency);
                var effMetric = profiler.getMetric(id, "movementEfficiency");
                if (effMetric != null && effMetric.zScore() > 3.0
                        && effMetric.getCurrentValue() > effMetric.getBaseline()) {
                    score++;
                }
                // Direct physics violation: dxz > theoretical max
                if (efficiency > 1.0) {
                    score += 2;
                }
            }
        }

        if (score >= 1) {
            flag(p, "dxz=" + String.format("%.4f", dxz) + " score=" + score);
        }
    }
}
