package org.coffeepop.latchac.core.check.impl.movement;

import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckInfo;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.player.LatchPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects hovering in the air: player is off ground for too many ticks
 * with negligible vertical movement.
 * <p>
 * Only fires when {@code onGround = false}. Vanilla gravity guarantees
 * a falling player will accumulate significant deltaY within a few ticks.
 */
@CheckInfo(name = "FlyAirStuck", type = CheckType.MOVEMENT, maxVL = 30)
public class FlyAirStuck extends Check {

    private static final int MIN_TICKS = 10;
    private static final double MIN_TOTAL_DY = 0.04;
    private final Map<UUID, AirState> states = new ConcurrentHashMap<>();

    public FlyAirStuck() {}

    @Override
    public void onCheck(LatchPlayer p) {
        if (!p.hasPosition()) return;
        if (p.isInVehicle()) return;

        if (p.isOnGround()) {
            states.remove(p.getUniqueId());
            return;
        }

        AirState s = states.computeIfAbsent(p.getUniqueId(), id -> new AirState());
        s.ticks++;
        s.totalDY += Math.abs(p.getDeltaY());

        if (s.ticks < MIN_TICKS) return;

        if (s.totalDY < MIN_TOTAL_DY) {
            flagAndSetback(p, "ticks=" + s.ticks + " totalDY=" + String.format("%.4f", s.totalDY));
        }
        states.remove(p.getUniqueId());
    }

    private static class AirState { int ticks; double totalDY; }
}
