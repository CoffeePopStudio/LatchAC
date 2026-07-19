package org.coffeepop.latchac.core.model;

import java.util.UUID;

/** 锁存聚合后的判定结果。tripped=true 表示累计值已越过阈值（锁存翻转）。 */
public record Verdict(
        UUID playerId,
        String checkId,
        double aggregateScore,
        boolean tripped,
        String reason
) {}
