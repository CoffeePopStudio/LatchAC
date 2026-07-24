package org.coffeepop.latchac.core.check.impl.combat;

import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckInfo;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.player.LatchPlayer;

/**
 * Physical impossibility: attack distance beyond vanilla max reach.
 * Survival: 3.8 (3.0 vanilla + latency + sprint knockback).
 * Creative: 6.5.
 */
@CheckInfo(name = "Reach", type = CheckType.COMBAT, maxVL = 30)
public class CheckReach extends Check {

    private static final double MAX_SURVIVAL = 3.8;
    private static final double MAX_CREATIVE = 6.5;

    public CheckReach() {}

    @Override
    public void onAttack(LatchPlayer p, int entityId) {
        double dx = p.getX() - p.getTargetX();
        double dy = p.getY() - p.getTargetY();
        double dz = p.getZ() - p.getTargetZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        double maxReach = p.shouldExemptMovement() ? MAX_CREATIVE : MAX_SURVIVAL;

        if (dist > maxReach) {
            flagAndSetback(p, "dist=" + String.format("%.3f", dist)
                    + " max=" + String.format("%.2f", maxReach));
        }
    }
}
