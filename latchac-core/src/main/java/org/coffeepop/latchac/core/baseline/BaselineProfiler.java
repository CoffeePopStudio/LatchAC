package org.coffeepop.latchac.core.baseline;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dual-layer behavioral baseline profiler.
 * Layer 1 (Population): shared baseline for cold-start. Falls back to hardcoded seeds.
 * Layer 2 (Individual): per-player EWMA baseline. Loaded from SQLite on join, saved on quit.
 *
 * Individual baseline data is stored locally ONLY — never exported.
 * Population aggregates (mean/std/count, no UUIDs) may be exported for community sharing.
 */
public class BaselineProfiler {
    private final BaselineStorage storage;
    private final PopulationBaseline population;
    private final Map<UUID, Map<String, BaselineMetric>> individualBaselines = new ConcurrentHashMap<>();

    public BaselineProfiler(File dataFolder) {
        this.storage = new BaselineStorage(dataFolder);
        this.population = new PopulationBaseline(storage);
    }

    public void onPlayerJoin(UUID playerId) {
        var saved = storage.loadBaselines(playerId);
        var metrics = new ConcurrentHashMap<String, BaselineMetric>();

        for (var entry : saved.entrySet()) {
            double[] vals = entry.getValue();
            metrics.put(entry.getKey(), new BaselineMetric(entry.getKey(), vals[0], vals[1]));
        }

        var popBaselines = storage.loadPopulationBaselines();
        for (var entry : popBaselines.entrySet()) {
            metrics.putIfAbsent(entry.getKey(),
                new BaselineMetric(entry.getKey(), entry.getValue()[0], entry.getValue()[1]));
        }

        individualBaselines.put(playerId, metrics);
    }

    public void onPlayerQuit(UUID playerId, int sessionVL, long onlineMs,
                              Map<String, Double> metricSnapshots) {
        var metrics = individualBaselines.get(playerId);
        if (metrics != null) {
            for (var entry : metrics.entrySet()) {
                storage.saveBaseline(playerId, entry.getKey(),
                        entry.getValue().getBaseline(), entry.getValue().getBaselineStd());
            }
        }
        if (metricSnapshots != null && !metricSnapshots.isEmpty()) {
            population.recordSession(playerId, sessionVL, onlineMs, metricSnapshots);
        }
        individualBaselines.remove(playerId);
    }

    public double update(UUID playerId, String metricName, double value) {
        var metrics = individualBaselines.get(playerId);
        if (metrics == null) return 0;

        var metric = metrics.computeIfAbsent(metricName, k -> {
            double[] pop = population.getBaseline(metricName);
            return new BaselineMetric(metricName, pop[0], pop[1]);
        });

        metric.update(value);
        return metric.zScore();
    }

    public boolean isAnomalous(UUID playerId, String metricName, double value) {
        double z = update(playerId, metricName, value);
        return z > 3.0;
    }

    public BaselineMetric getMetric(UUID playerId, String metricName) {
        var metrics = individualBaselines.get(playerId);
        return metrics != null ? metrics.get(metricName) : null;
    }

    public BaselineStorage getStorage() { return storage; }
    public PopulationBaseline getPopulation() { return population; }

    public void setTrainingMode(boolean enabled) {
        population.setTrainingMode(enabled);
    }
}
