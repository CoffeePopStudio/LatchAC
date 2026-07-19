package org.coffeepop.latchac.core.exempt;

import java.util.Set;
import java.util.UUID;

/**
 * 豁免判定所需的最小上下文，平台无关。
 * tags 由采集层注入，例如 "fake-player"（假人授信）、未来的 "mod:litematica-printer"。
 */
public record ExemptionContext(
        UUID playerId,
        String playerName,
        String worldName,
        double x, double y, double z,
        String checkId,
        Set<String> tags
) {
    public ExemptionContext {
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }
}
