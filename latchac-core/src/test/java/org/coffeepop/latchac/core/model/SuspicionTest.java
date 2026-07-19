package org.coffeepop.latchac.core.model;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuspicionTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void 合法评分可创建并携带证据() {
        Suspicion s = new Suspicion("movement.flight", PLAYER, 0.5, 1000L, Map.of("dy", "1.2"));
        assertEquals("movement.flight", s.checkId());
        assertEquals(0.5, s.score());
        assertEquals("1.2", s.evidence().get("dy"));
    }

    @Test
    void 评分超出01区间被拒绝() {
        assertThrows(IllegalArgumentException.class,
                () -> new Suspicion("movement.flight", PLAYER, 1.1, 1000L, Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new Suspicion("movement.flight", PLAYER, -0.1, 1000L, Map.of()));
    }

    @Test
    void 空白checkId被拒绝() {
        assertThrows(IllegalArgumentException.class,
                () -> new Suspicion("  ", PLAYER, 0.5, 1000L, Map.of()));
    }

    @Test
    void 证据允许为null且结果不可变() {
        Suspicion s = new Suspicion("x", PLAYER, 0.1, 0L, null);
        assertTrue(s.evidence().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> s.evidence().put("k", "v"));
    }
}
