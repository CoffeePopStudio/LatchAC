package org.coffeepop.latchac.core.exempt;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExemptionMatrixTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static ExemptionContext ctx(String world, double x, double y, double z, Set<String> tags) {
        return new ExemptionContext(PLAYER, "Steve", world, x, y, z, "movement.flight", tags);
    }

    @Test
    void 区域内命中豁免_区域外不命中() {
        RegionRule rule = new RegionRule("machine-hall", "world", -10, 0, -10, 10, 320, 10);
        assertTrue(rule.exempts(ctx("world", 0, 64, 0, Set.of())).isPresent());
        assertTrue(rule.exempts(ctx("world", 11, 64, 0, Set.of())).isEmpty());
        assertTrue(rule.exempts(ctx("world_nether", 0, 64, 0, Set.of())).isEmpty());
    }

    @Test
    void 玩家名单按名字或UUID命中() {
        PlayerRule byName = new PlayerRule(Set.of(), Set.of("Steve"));
        PlayerRule byUuid = new PlayerRule(Set.of(PLAYER), Set.of());
        assertTrue(byName.exempts(ctx("world", 0, 0, 0, Set.of())).isPresent());
        assertTrue(byUuid.exempts(ctx("world", 0, 0, 0, Set.of())).isPresent());
    }

    @Test
    void 标签规则命中假人授信() {
        TagRule rule = new TagRule("fake-player", "Carpet 类假人授信");
        assertEquals(Optional.of("Carpet 类假人授信"),
                rule.exempts(ctx("world", 0, 0, 0, Set.of("fake-player"))));
        assertTrue(rule.exempts(ctx("world", 0, 0, 0, Set.of())).isEmpty());
    }

    @Test
    void 矩阵返回第一条命中原因_全不命中返回空() {
        ExemptionMatrix matrix = new ExemptionMatrix(List.of(
                new RegionRule("hall", "world", -10, 0, -10, 10, 320, 10),
                new TagRule("fake-player", "假人授信")));
        assertEquals(Optional.of("区域豁免: hall"),
                matrix.firstExemption(ctx("world", 0, 64, 0, Set.of("fake-player"))));
        assertTrue(matrix.firstExemption(ctx("world", 999, 64, 999, Set.of())).isEmpty());
    }

    @Test
    void 空矩阵永不豁免() {
        ExemptionMatrix matrix = new ExemptionMatrix(List.of());
        assertTrue(matrix.firstExemption(ctx("world", 0, 0, 0, Set.of("fake-player"))).isEmpty());
    }
}
