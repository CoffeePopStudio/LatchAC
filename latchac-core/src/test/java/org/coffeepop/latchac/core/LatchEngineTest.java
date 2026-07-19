package org.coffeepop.latchac.core;

import org.coffeepop.latchac.core.config.LatchConfig;
import org.coffeepop.latchac.core.exempt.ExemptionContext;
import org.coffeepop.latchac.core.exempt.RegionRule;
import org.coffeepop.latchac.core.exempt.TagRule;
import org.coffeepop.latchac.core.model.Outcome;
import org.coffeepop.latchac.core.model.Suspicion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatchEngineTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String CHECK = "movement.flight";

    private static LatchConfig config(double threshold) {
        return new LatchConfig(4000,
                Map.of(CHECK, new LatchConfig.CheckConfig(true, threshold, "LOG")),
                List.of(new RegionRule("hall", "world", -10, 0, -10, 10, 320, 10),
                        new TagRule("fake-player", "假人授信")));
    }

    private static Suspicion sus(double score, long ts) {
        return new Suspicion(CHECK, PLAYER, score, ts, Map.of());
    }

    private static ExemptionContext ctx(double x, Set<String> tags) {
        return new ExemptionContext(PLAYER, "Steve", "world", x, 64, 0, CHECK, tags);
    }

    @Test
    void 豁免区域内评分清零并给出原因() {
        LatchEngine engine = new LatchEngine(config(3.0));
        Outcome out = engine.submit(sus(1.0, 0), ctx(0, Set.of()));
        Outcome.Exempted exempted = assertInstanceOf(Outcome.Exempted.class, out);
        assertEquals("区域豁免: hall", exempted.reason());
    }

    @Test
    void 假人标签直接授信() {
        LatchEngine engine = new LatchEngine(config(3.0));
        Outcome out = engine.submit(sus(1.0, 0), ctx(999, Set.of("fake-player")));
        assertInstanceOf(Outcome.Exempted.class, out);
    }

    @Test
    void 区域外单次低分不翻转_持续提交翻转() {
        LatchEngine engine = new LatchEngine(config(3.0));
        Outcome first = engine.submit(sus(1.0, 0), ctx(999, Set.of()));
        assertFalse(assertInstanceOf(Outcome.Scored.class, first).verdict().tripped());

        engine.submit(sus(1.0, 100), ctx(999, Set.of()));
        engine.submit(sus(1.0, 200), ctx(999, Set.of()));
        Outcome fourth = engine.submit(sus(1.0, 300), ctx(999, Set.of()));
        assertTrue(assertInstanceOf(Outcome.Scored.class, fourth).verdict().tripped());
    }

    @Test
    void 未配置或禁用的检查项返回Disabled() {
        LatchEngine engine = new LatchEngine(config(3.0));
        Suspicion unknown = new Suspicion("combat.killaura", PLAYER, 0.5, 0, Map.of());
        ExemptionContext c = new ExemptionContext(PLAYER, "Steve", "world", 999, 64, 0,
                "combat.killaura", Set.of());
        assertInstanceOf(Outcome.Disabled.class, engine.submit(unknown, c));
    }

    @Test
    void 热重载原子替换且旧累计清零() {
        LatchEngine engine = new LatchEngine(config(3.0));
        engine.submit(sus(1.0, 0), ctx(999, Set.of()));
        engine.submit(sus(1.0, 100), ctx(999, Set.of()));

        engine.reload(config(10.0)); // 新纪元：阈值 10，累计清零

        Outcome after = engine.submit(sus(1.0, 200), ctx(999, Set.of()));
        Outcome.Scored scored = assertInstanceOf(Outcome.Scored.class, after);
        assertEquals(1.0, scored.verdict().aggregateScore(), 1e-9);
        assertFalse(scored.verdict().tripped());
    }
}
