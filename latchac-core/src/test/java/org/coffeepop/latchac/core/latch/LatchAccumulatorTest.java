package org.coffeepop.latchac.core.latch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatchAccumulatorTest {

    @Test
    void 单次毛刺不触发() {
        LatchAccumulator acc = new LatchAccumulator(3.0, 4000);
        assertEquals(1.0, acc.accumulate(1.0, 0), 1e-9);
        assertFalse(acc.isTripped(0));
    }

    @Test
    void 持续高分推过阈值后锁存翻转() {
        LatchAccumulator acc = new LatchAccumulator(3.0, 4000);
        acc.accumulate(1.0, 0);
        acc.accumulate(1.0, 100);
        acc.accumulate(1.0, 200);
        acc.accumulate(1.0, 300);
        assertTrue(acc.isTripped(300));
    }

    @Test
    void 经过一个半衰期评分减半() {
        LatchAccumulator acc = new LatchAccumulator(3.0, 4000);
        acc.accumulate(2.0, 0);
        assertEquals(1.0, acc.currentValue(4000), 1e-9);
    }

    @Test
    void 长时间静默后毛刺被完全稀释() {
        LatchAccumulator acc = new LatchAccumulator(3.0, 4000);
        acc.accumulate(2.9, 0);
        // 10 个半衰期 ≈ 千分之一
        assertTrue(acc.currentValue(40_000) < 0.01);
        assertFalse(acc.isTripped(40_000));
    }

    @Test
    void 非法构造参数被拒绝() {
        assertThrows(IllegalArgumentException.class, () -> new LatchAccumulator(0, 4000));
        assertThrows(IllegalArgumentException.class, () -> new LatchAccumulator(3.0, 0));
    }
}
