package org.coffeepop.latchac.core.baseline;

/**
 * Metrics tracked by the Baseline Profiler.
 * Uses EWMA (α=0.05) for slow, stable adaptation.
 */
public class BaselineMetric {
    private final String name;
    private double baseline;
    private double baselineStd;
    private double currentValue;

    private static final double ALPHA = 0.05;

    public BaselineMetric(String name, double initialValue, double initialStd) {
        this.name = name;
        this.baseline = initialValue;
        this.baselineStd = initialStd;
        this.currentValue = initialValue;
    }

    public void update(double value) {
        this.currentValue = value;
        this.baseline = ALPHA * value + (1 - ALPHA) * baseline;
        this.baselineStd = ALPHA * Math.abs(value - baseline) + (1 - ALPHA) * baselineStd;
    }

    public double zScore() {
        if (baselineStd < 0.0001) return 0;
        return Math.abs(currentValue - baseline) / baselineStd;
    }

    public boolean isAnomalous() {
        return zScore() > 3.0;
    }

    public String getName() { return name; }
    public double getBaseline() { return baseline; }
    public double getBaselineStd() { return baselineStd; }
    public double getCurrentValue() { return currentValue; }
}
