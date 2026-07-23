package org.coffeepop.latchac.core.check.impl.combat;

import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckInfo;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.player.LatchPlayer;

import java.util.*;

@CheckInfo(name = "Velocity", type = CheckType.COMBAT, maxVL = 30)
public class CheckVelocity extends Check {

    private static final int TRACK_TICKS = 8;
    private static final double AIR_FRICTION = 0.91;

    private final Map<UUID, VelState> states = new HashMap<>();

    public CheckVelocity() {}

    @Override
    public void onVelocity(LatchPlayer p, double vx, double vy, double vz) {
        var s = new VelState();
        s.pendingVX = vx;
        s.pendingVY = vy;
        s.pendingVZ = vz;
        s.jumpResetCheck = (vy <= 0 && Math.abs(vx) + Math.abs(vz) > 0.05);
        states.put(p.getUniqueId(), s);
    }

    @Override
    public void onCheck(LatchPlayer p) {
        VelState s = states.get(p.getUniqueId());
        if (s == null || s.tick >= TRACK_TICKS) {
            states.remove(p.getUniqueId());
            return;
        }
        s.tick++;
        s.accumDX += p.getDeltaX();
        s.accumDY += p.getDeltaY();
        s.accumDZ += p.getDeltaZ();

        if (s.tick < TRACK_TICKS) return;

        double expectedTotal = Math.hypot(s.pendingVX, s.pendingVZ);
        double frictionSum = (1 - Math.pow(AIR_FRICTION, TRACK_TICKS)) / (1 - AIR_FRICTION);
        double expectedDisplacement = expectedTotal * frictionSum;
        double actualDisplacement = Math.hypot(s.accumDX, s.accumDZ);

        if (expectedDisplacement > 0.1 && actualDisplacement < 0.05) {
            flag(p, "zero_vel expected=" + String.format("%.3f", expectedDisplacement));
        }

        if (s.jumpResetCheck && p.getDeltaY() > 0.3) {
            s.jrCount++;
            if (s.jrCount >= 8) {
                flag(p, "jr_auto=" + s.jrCount);
                s.jrCount = 0;
            }
        }

        states.remove(p.getUniqueId());
    }

    private static class VelState {
        double pendingVX, pendingVY, pendingVZ;
        int tick;
        double accumDX, accumDY, accumDZ;
        boolean jumpResetCheck;
        int jrCount;
    }

    @Override
    public void onQuit(UUID playerId) { states.remove(playerId); }
}
