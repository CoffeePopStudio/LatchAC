package org.coffeepop.latchac.core;

import org.coffeepop.latchac.core.config.LatchConfig;
import org.coffeepop.latchac.core.exempt.ExemptionContext;
import org.coffeepop.latchac.core.exempt.ExemptionMatrix;
import org.coffeepop.latchac.core.latch.LatchAccumulator;
import org.coffeepop.latchac.core.model.Outcome;
import org.coffeepop.latchac.core.model.Suspicion;
import org.coffeepop.latchac.core.model.Verdict;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LatchAC 核心引擎（Arbiter 判定部分，平台无关）。
 * 流程：检查项开关 → 豁免矩阵（命中即清零）→ 锁存聚合 → 阈值判定。
 * 热重载：reload() 以"配置纪元"整体原子替换（volatile 引用），旧累计随旧纪元丢弃。
 * 线程安全：纪元不可变 + 累计器 ConcurrentHashMap + 实例锁，Folia 多区域线程并发投递安全。
 */
public final class LatchEngine {

    /** 一个配置纪元：配置 + 矩阵 + 该纪元的累计器。整体替换实现热重载。 */
    private record Epoch(LatchConfig config, ExemptionMatrix matrix,
                         ConcurrentHashMap<String, LatchAccumulator> accumulators) {

        static Epoch of(LatchConfig config) {
            return new Epoch(config, new ExemptionMatrix(config.exemptionRules()),
                    new ConcurrentHashMap<>());
        }
    }

    private volatile Epoch epoch;

    public LatchEngine(LatchConfig config) {
        this.epoch = Epoch.of(config);
    }

    /** 热重载：原子替换配置纪元。失败的配置根本到不了这里（解析期已拒绝）。 */
    public void reload(LatchConfig config) {
        this.epoch = Epoch.of(config);
    }

    /** 提交一次可疑评分，返回可审计的结局。 */
    public Outcome submit(Suspicion suspicion, ExemptionContext ctx) {
        Epoch e = this.epoch;

        LatchConfig.CheckConfig check = e.config().checks().get(suspicion.checkId());
        if (check == null || !check.enabled()) {
            return new Outcome.Disabled(suspicion.checkId());
        }

        Optional<String> exemption = e.matrix().firstExemption(ctx);
        if (exemption.isPresent()) {
            return new Outcome.Exempted(exemption.get());
        }

        String key = suspicion.playerId() + "|" + suspicion.checkId();
        LatchAccumulator acc = e.accumulators().computeIfAbsent(key,
                k -> new LatchAccumulator(check.tripThreshold(), e.config().halfLifeMillis()));

        double aggregate;
        synchronized (acc) {
            aggregate = acc.accumulate(suspicion.score(), suspicion.timestampMillis());
        }

        boolean tripped = aggregate >= check.tripThreshold();
        String reason = tripped
                ? "累计可疑度 %.2f 越过阈值 %.2f（锁存翻转）".formatted(aggregate, check.tripThreshold())
                : "累计中 %.2f / %.2f".formatted(aggregate, check.tripThreshold());
        return new Outcome.Scored(
                new Verdict(suspicion.playerId(), suspicion.checkId(), aggregate, tripped, reason));
    }
}
