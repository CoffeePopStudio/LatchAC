package org.coffeepop.latchac.core.check.impl.movement;

import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckInfo;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.engine.PhysicsConstants;
import org.coffeepop.latchac.core.engine.PredictionEngine;
import org.coffeepop.latchac.core.player.LatchPlayer;

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

        var bounds = PredictionEngine.predict(p, PhysicsConstants.DEFAULT_SLIPPERINESS);

        if (!bounds.isValidDeltaXZ(dxz)) {
            flagAndSetback(p, "dxz=" + String.format("%.4f", dxz)
                    + " max=" + String.format("%.4f",
                            Math.max(Math.abs(bounds.maxDx()), Math.abs(bounds.maxDz()))));
        }

        double forwardSpeed = Math.abs(dxz * Math.cos(Math.toRadians(p.getDeltaYaw())));
        double lateralSpeed = Math.abs(dxz * Math.sin(Math.toRadians(p.getDeltaYaw())));
        if (forwardSpeed > 0.15 && lateralSpeed / forwardSpeed > 1.2) {
            flag(p, "strafe=" + String.format("%.3f", lateralSpeed / forwardSpeed));
        }
    }
}
