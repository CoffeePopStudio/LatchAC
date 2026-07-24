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

@CheckInfo(name = "FlyGroundSpoof", type = CheckType.MOVEMENT, maxVL = 30)
public class FlyGroundSpoof extends Check {

    private static final int SPOOF_TICKS = 10;
    private final Map<UUID, Integer> spoofTicks = new ConcurrentHashMap<>();

    public FlyGroundSpoof() {}

    @Override
    public void onCheck(LatchPlayer p) {
        if (!p.hasPosition()) return;
        if (p.isInVehicle()) return;
        if (p.isFlightExempted()) { spoofTicks.remove(p.getUniqueId()); return; }
        if (p.shouldExemptMovement()) { spoofTicks.remove(p.getUniqueId()); return; }
        if (!p.isOnGround()) { spoofTicks.remove(p.getUniqueId()); return; }

        var bounds = PredictionEngine.predict(p, p.getFootSlipperiness());

        if (!bounds.groundedByPhysics()) {
            int ticks = spoofTicks.merge(p.getUniqueId(), 1, Integer::sum);
            if (ticks >= SPOOF_TICKS) {
                flagAndSetback(p, "spoof_ticks=" + ticks
                        + " y=" + String.format("%.3f", p.getY())
                        + " dy=" + String.format("%.4f", p.getDeltaY()));
                spoofTicks.remove(p.getUniqueId());
            }
        } else {
            spoofTicks.remove(p.getUniqueId());
        }
    }

    @Override
    public void onQuit(UUID playerId) { spoofTicks.remove(playerId); }
}
