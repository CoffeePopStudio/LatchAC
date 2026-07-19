package org.coffeepop.latchac.core.model;

import java.util.Map;
import java.util.UUID;

/**
 * 单次可疑行为评分（0.0 ~ 1.0）。
 * 检测器只提供证据，不做判决——判决权在 LatchEngine（Arbiter 层）。
 */
public record Suspicion(
        String checkId,
        UUID playerId,
        double score,
        long timestampMillis,
        Map<String, String> evidence
) {
    public Suspicion {
        if (checkId == null || checkId.isBlank()) {
            throw new IllegalArgumentException("checkId 不能为空");
        }
        if (playerId == null) {
            throw new IllegalArgumentException("playerId 不能为空");
        }
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("score 必须在 [0.0, 1.0] 区间，实际: " + score);
        }
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
    }
}
