package org.coffeepop.latchac.core.check.impl.movement;

import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckInfo;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.player.LatchPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects clients faking the {@code onGround} flag.
 * <p>
 * A client in the air may send {@code onGround = true} to trick simple
 * flight checks. We catch this by noticing that the player's Y position
 * never settles to a standard block level (multiples of 0.5).
 * <p>
 * Real ground positions are at {@code N.0} (full block), {@code N.5} (slab),
 * {@code N.5625} (bed), etc. A flying player at Y=120.3 claiming onGround
 * for many ticks is clearly spoofing.
 */
@CheckInfo(name = "FlyGroundSpoof", type = CheckType.MOVEMENT, maxVL = 30)
public class FlyGroundSpoof extends Check {

    private static final int MIN_TICKS = 15;
    private static final double BLOCK_STEP = 0.5;
    private final Map<UUID, SpoofState> states = new ConcurrentHashMap<>();

    public FlyGroundSpoof() {}

    @Override
    public void onCheck(LatchPlayer p) {
        if (!p.hasPosition()) return;
        if (p.isInVehicle()) return;
        if (p.shouldExemptMovement()) {
            states.remove(p.getUniqueId());
            return;
        }
        if (!p.isOnGround()) {
            states.remove(p.getUniqueId());
            return;
        }

        double y = p.getY();
        double frac = y - Math.floor(y / BLOCK_STEP) * BLOCK_STEP;

        // Close to a valid block level (0.0, 0.5) → legit, reset counter
        if (Math.abs(frac) < 0.01 || Math.abs(frac - BLOCK_STEP) < 0.01) {
            states.remove(p.getUniqueId());
            return;
        }

        SpoofState s = states.computeIfAbsent(p.getUniqueId(), id -> new SpoofState());
        s.ticks++;
        s.lastY = y;

        if (s.ticks >= MIN_TICKS) {
            flagAndSetback(p, "ticks=" + s.ticks + " y=" + String.format("%.2f", y)
                    + " frac=" + String.format("%.3f", frac));
            states.remove(p.getUniqueId());
        }
    }

    private static class SpoofState { int ticks; double lastY; }

    @Override
    public void onQuit(UUID playerId) {
        states.remove(playerId);
    }
}
