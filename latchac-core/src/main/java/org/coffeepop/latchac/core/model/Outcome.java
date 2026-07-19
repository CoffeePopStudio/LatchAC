package org.coffeepop.latchac.core.model;

/** 一次评分提交的完整结局。三种情形全部可日志审计——决策要能回答"为什么"。 */
public sealed interface Outcome {

    /** 命中豁免，评分清零丢弃。 */
    record Exempted(String reason) implements Outcome {}

    /** 进入锁存聚合，产生判定。 */
    record Scored(Verdict verdict) implements Outcome {}

    /** 检查项未配置或已禁用。 */
    record Disabled(String checkId) implements Outcome {}
}
