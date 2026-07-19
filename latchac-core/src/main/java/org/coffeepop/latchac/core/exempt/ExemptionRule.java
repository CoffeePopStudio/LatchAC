package org.coffeepop.latchac.core.exempt;

import java.util.Optional;

/** 单条豁免规则。命中时必须返回人类可读的原因——透明审计是一等需求。 */
public interface ExemptionRule {

    /** @return 命中时为豁免原因，未命中为 empty */
    Optional<String> exempts(ExemptionContext ctx);
}
