package org.coffeepop.latchac.core.check.impl.inventory;

import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckInfo;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.player.LatchPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects movement packets while an external container (chest, furnace, etc.) is open.
 * <p>
 * Flags once per container session with a 5-second cooldown to avoid VL spam.
 */
@CheckInfo(name = "InvMove", type = CheckType.INVENTORY)
public class CheckInvMove extends Check {

    private static final long FLAG_COOLDOWN = 5000;
    private final Map<UUID, Long> lastFlag = new ConcurrentHashMap<>();

    @Override
    public void onCheck(LatchPlayer p) {
        if (!p.isInContainer()) return;

        long now = System.currentTimeMillis();
        Long last = lastFlag.get(p.getUniqueId());
        if (last != null && now - last < FLAG_COOLDOWN) return;

        lastFlag.put(p.getUniqueId(), now);
        flag(p, "container=" + p.getContainerType());
    }
}
