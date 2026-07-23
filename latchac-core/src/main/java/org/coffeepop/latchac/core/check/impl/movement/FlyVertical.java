package org.coffeepop.latchac.core.check.impl.movement;

import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckInfo;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.player.LatchPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects abnormal upward movement by checking vertical acceleration.
 * <p>
 * A vanilla jump produces decreasing deltaY each tick (gravity accelerates
 * downward). A Jetpack / Vanilla fly produces constant or increasing deltaY.
 * We track whether deltaY is decelerating naturally or staying stable.
 */
@CheckInfo(name = "FlyVertical", type = CheckType.MOVEMENT, maxVL = 25)
public class FlyVertical extends Check {

    private static final int STABLE_THRESHOLD = 3;      // consecutive ticks without deceleration
    private static final double DECEL_FACTOR = 0.85;    // dy must drop to < 85% of previous to be "falling"
    private final Map<UUID, VerticalState> states = new ConcurrentHashMap<>();

    public FlyVertical() {}

    @Override
    public void onCheck(LatchPlayer p) {
        if (!p.hasPosition()) return;
        if (p.isInVehicle()) return;
        if (p.isInLiquid()) { states.remove(p.getUniqueId()); return; }
        if (p.isOnGround()) { states.remove(p.getUniqueId()); return; }

        double dy = p.getDeltaY();
        if (dy <= 0) { states.remove(p.getUniqueId()); return; }

        VerticalState s = states.computeIfAbsent(p.getUniqueId(), id -> new VerticalState());

        // Natural jump: deltaY drops each tick (e.g. 0.42 → 0.34 → 0.26 → 0.18 → 0.10 → 0.02)
        // Fly: deltaY stays stable or increases
        if (s.lastDy > 0 && dy >= s.lastDy * DECEL_FACTOR) {
            s.stableTicks++;
        } else {
            s.stableTicks = 0;
        }
        s.lastDy = dy;
        s.ticks++;

        if (s.stableTicks >= STABLE_THRESHOLD) {
            flagAndSetback(p, "ticks=" + s.ticks + " stable=" + s.stableTicks
                    + " dy=" + String.format("%.3f", dy));
            states.remove(p.getUniqueId());
        }
    }

    private static class VerticalState { int ticks; int stableTicks; double lastDy; }

    @Override
    public void onQuit(UUID playerId) {
        states.remove(playerId);
    }
}
