package org.coffeepop.latchac.core.latch;

/**
 * 锁存累计器——LatchAC 的核心语义。
 * 累计值随时间指数衰减（半衰期 halfLifeMillis），新评分线性叠加。
 * 瞬时毛刺（网络抖动、单次误报）被时间稀释，只有持续证据才能推动累计值
 * 越过 tripThreshold——如同锁存器只对持续信号翻转。
 * 非线程安全：调用方需保证同一实例串行访问（LatchEngine 对实例加锁）。
 */
public final class LatchAccumulator {

    private final double tripThreshold;
    private final long halfLifeMillis;

    private double value;
    private long lastTimestamp = Long.MIN_VALUE;

    public LatchAccumulator(double tripThreshold, long halfLifeMillis) {
        if (tripThreshold <= 0) {
            throw new IllegalArgumentException("tripThreshold 必须为正数");
        }
        if (halfLifeMillis <= 0) {
            throw new IllegalArgumentException("halfLifeMillis 必须为正数");
        }
        this.tripThreshold = tripThreshold;
        this.halfLifeMillis = halfLifeMillis;
    }

    /** 衰减到 nowMillis 后叠加评分，返回当前累计值。 */
    public double accumulate(double score, long nowMillis) {
        decayTo(nowMillis);
        value += score;
        return value;
    }

    /** 只衰减不叠加，读取当前累计值。 */
    public double currentValue(long nowMillis) {
        decayTo(nowMillis);
        return value;
    }

    public boolean isTripped(long nowMillis) {
        return currentValue(nowMillis) >= tripThreshold;
    }

    private void decayTo(long nowMillis) {
        if (lastTimestamp == Long.MIN_VALUE) {
            lastTimestamp = nowMillis;
            return;
        }
        long dt = nowMillis - lastTimestamp;
        if (dt > 0) {
            value *= Math.pow(0.5, (double) dt / halfLifeMillis);
            lastTimestamp = nowMillis;
        }
    }
}
