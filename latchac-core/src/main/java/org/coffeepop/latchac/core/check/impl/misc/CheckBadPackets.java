package org.coffeepop.latchac.core.check.impl.misc;

import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckInfo;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.player.LatchPlayer;

/**
 * Zero-tolerance packet firewall — physically impossible packet values.
 */
@CheckInfo(name = "BadPackets", type = CheckType.EXPLOIT, maxVL = 40)
public class CheckBadPackets extends Check {

    private static final double MAX_DY = 5.0;
    private static final double MAX_DXZ = 10.0;

    public CheckBadPackets() {}

    @Override
    public void onCheck(LatchPlayer p) {
        if (!p.hasPosition()) return;
        if (p.shouldExemptMovement()) return;

        double dy = Math.abs(p.getDeltaY());
        double dxz = p.getDeltaXZ();

        // Impossible vertical jump
        if (dy > MAX_DY && !p.isInLiquid() && !p.isFlightExempted()) {
            flag(p, "impossible_dy=" + String.format("%.2f", dy));
        }

        // Impossible horizontal teleport (no liquid, no teleport)
        if (dxz > MAX_DXZ && !p.isInLiquid()) {
            flag(p, "impossible_dxz=" + String.format("%.2f", dxz));
        }

        // Pitch out of range
        float pitch = Math.abs(p.getPitch());
        if (pitch > 90.0f) {
            flag(p, "pitch=" + String.format("%.1f", pitch));
        }
    }
}
