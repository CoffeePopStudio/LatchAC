package org.coffeepop.latchac.core.check.impl.movement;

import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckInfo;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.engine.MotionBounds;
import org.coffeepop.latchac.core.engine.PhysicsConstants;
import org.coffeepop.latchac.core.engine.PredictionEngine;
import org.coffeepop.latchac.core.player.LatchPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckInfo(name = "FlyVertical", type = CheckType.MOVEMENT, maxVL = 25)
public class FlyVertical extends Check {

    private final Map<UUID, Integer> stableTicks = new ConcurrentHashMap<>();

    public FlyVertical() {}

    @Override
    public void onCheck(LatchPlayer p) {
        if (!p.hasPosition()) return;
        if (p.isInVehicle()) return;
        if (p.isInLiquid()) { stableTicks.remove(p.getUniqueId()); return; }
        if (p.isOnGround()) { stableTicks.remove(p.getUniqueId()); return; }

        double dy = p.getDeltaY();
        if (dy <= 0) { stableTicks.remove(p.getUniqueId()); return; }

        var bounds = PredictionEngine.predict(p, PhysicsConstants.DEFAULT_SLIPPERINESS);
        double predictedDy = (bounds.maxDy() + bounds.minDy()) / 2.0;

        if (dy >= predictedDy + 0.04) {
            int ticks = stableTicks.merge(p.getUniqueId(), 1, Integer::sum);
            if (ticks >= 5) {
                flagAndSetback(p, "dy=" + String.format("%.4f", dy)
                        + " predicted=" + String.format("%.4f", predictedDy));
                stableTicks.remove(p.getUniqueId());
            }
        } else {
            stableTicks.remove(p.getUniqueId());
        }
    }

    @Override
    public void onQuit(UUID playerId) { stableTicks.remove(playerId); }
}
