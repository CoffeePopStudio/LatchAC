package org.coffeepop.latchac.core.exempt;

import java.util.Optional;

/** 标签豁免：采集层打上的授信标签（假人、Mod 行为模板等）直接放行。 */
public record TagRule(String tag, String description) implements ExemptionRule {

    @Override
    public Optional<String> exempts(ExemptionContext ctx) {
        return ctx.tags().contains(tag) ? Optional.of(description) : Optional.empty();
    }
}
