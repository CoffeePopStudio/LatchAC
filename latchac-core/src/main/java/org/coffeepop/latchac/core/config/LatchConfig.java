package org.coffeepop.latchac.core.config;

import org.coffeepop.latchac.core.exempt.ExemptionRule;

import java.util.List;
import java.util.Map;

/** 一次热重载产生的完整不可变配置。 */
public record LatchConfig(
        long halfLifeMillis,
        Map<String, CheckConfig> checks,
        List<ExemptionRule> exemptionRules
) {
    public LatchConfig {
        checks = Map.copyOf(checks);
        exemptionRules = List.copyOf(exemptionRules);
    }

    /** 单个检查项配置。action 在 M2 仅支持 LOG（静默日志），M3 扩展 NOTIFY/KICK/BAN。 */
    public record CheckConfig(boolean enabled, double tripThreshold, String action) {}
}
