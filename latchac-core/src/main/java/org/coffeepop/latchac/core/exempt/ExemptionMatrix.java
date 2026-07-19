package org.coffeepop.latchac.core.exempt;

import java.util.List;
import java.util.Optional;

/**
 * 豁免矩阵：顺序询问所有规则，任一命中即豁免（评分直接清零）。
 * 不可变——热重载时由 LatchEngine 整体替换，天然线程安全。
 */
public final class ExemptionMatrix {

    private final List<ExemptionRule> rules;

    public ExemptionMatrix(List<ExemptionRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public Optional<String> firstExemption(ExemptionContext ctx) {
        for (ExemptionRule rule : rules) {
            Optional<String> hit = rule.exempts(ctx);
            if (hit.isPresent()) {
                return hit;
            }
        }
        return Optional.empty();
    }
}
