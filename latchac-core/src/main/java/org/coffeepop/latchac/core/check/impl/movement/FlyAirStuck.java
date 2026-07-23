package org.coffeepop.latchac.core.check.impl.movement;

import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckInfo;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.engine.PhysicsConstants;
import org.coffeepop.latchac.core.engine.PredictionEngine;
import org.coffeepop.latchac.core.player.LatchPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckInfo(name = "FlyAirStuck", type = CheckType.MOVEMENT, maxVL = 30)
public class FlyAirStuck extends Check {

    private static final int HOVER_TICKS = 15;
    private final Map<UUID, Integer> hoverTicks = new ConcurrentHashMap<>();

    public FlyAirStuck() {}

    @Override
    public void onCheck(LatchPlayer p) {
        if (!p.hasPosition()) return;
        if (p.isInVehicle()) return;
        if (p.shouldExemptMovement()) { hoverTicks.remove(p.getUniqueId()); return; }
        if (p.isOnGround()) { hoverTicks.remove(p.getUniqueId()); return; }

        var bounds = PredictionEngine.predict(p, p.getFootSlipperiness());

        if (!bounds.groundedByPhysics() && Math.abs(p.getDeltaY()) < 0.005) {
            int ticks = hoverTicks.merge(p.getUniqueId(), 1, Integer::sum);
            if (ticks >= HOVER_TICKS) {
                flagAndSetback(p, "hover_ticks=" + ticks);
                hoverTicks.remove(p.getUniqueId());
            }
        } else {
            hoverTicks.remove(p.getUniqueId());
        }
    }

    @Override
    public void onQuit(UUID playerId) { hoverTicks.remove(playerId); }
}
