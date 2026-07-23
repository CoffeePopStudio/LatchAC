package org.coffeepop.latchac.core.baseline;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Three-layer population baseline with anti-pollution admission gating.
 * Layer 0: Hardcoded math-derived seeds
 * Layer 1: Admin training (config.yml whitelist)
 * Layer 2: Online adaptive (gated admission)
 *
 * Individual baseline data is NEVER exported — only population aggregates.
 */
public class PopulationBaseline {
    private final BaselineStorage storage;
    private final Map<String, double[]> hardcodedSeeds = new HashMap<>();
    private final Map<String, double[]> activeBaseline = new ConcurrentHashMap<>();
    private final Set<UUID> adminWhitelist = ConcurrentHashMap.newKeySet();
    private final Map<UUID, AdmissionState> admissionStates = new ConcurrentHashMap<>();
    private boolean trainingMode;

    public PopulationBaseline(BaselineStorage storage) {
        this.storage = storage;
        initHardcodedSeeds();
        loadFromStorage();
    }

    private void initHardcodedSeeds() {
        hardcodedSeeds.put("packetRate", new double[]{20.0, 3.0});
        hardcodedSeeds.put("rotationVariance", new double[]{3.0, 1.5});
        hardcodedSeeds.put("clickIntervalCV", new double[]{0.4, 0.15});
        hardcodedSeeds.put("swingAttackDelay", new double[]{80.0, 30.0});
    }

    private void loadFromStorage() {
        var stored = storage.loadPopulationBaselines();
        activeBaseline.putAll(stored);
        hardcodedSeeds.forEach((k, v) -> activeBaseline.putIfAbsent(k, v));
    }

    public double[] getBaseline(String metricName) {
        return activeBaseline.getOrDefault(metricName,
                hardcodedSeeds.getOrDefault(metricName, new double[]{0, 1.0}));
    }

    public void addAdminWhitelist(UUID playerId) {
        adminWhitelist.add(playerId);
    }

    public void setTrainingMode(boolean trainingMode) {
        this.trainingMode = trainingMode;
    }

    private boolean canAdmitToPool(UUID playerId, AdmissionState state) {
        if (state.totalVL >= 3) return false;
        if (state.onlineMinutes < 120) return false;
        if (state.sessionCount < 2) return false;
        if (state.sessionCount >= 2 && state.behaviorCV >= 0.3) return false;
        return true;
    }

    public void recordSession(UUID playerId, int sessionVL, long onlineMs,
                               Map<String, Double> metricCurrentValues) {
        if (adminWhitelist.contains(playerId)) return;

        // Training mode: bypass gating, feed directly into population baseline
        if (trainingMode) {
            metricCurrentValues.forEach((metric, value) ->
                storage.savePopulationBaseline(metric, value, 1.0, 1));
            return;
        }

        var state = admissionStates.computeIfAbsent(playerId, id -> new AdmissionState());
        state.totalVL += sessionVL;
        state.onlineMinutes += onlineMs / 60_000;
        state.sessionCount++;

        for (var entry : metricCurrentValues.entrySet()) {
            var tracker = state.metricTrackers.computeIfAbsent(entry.getKey(), k -> new double[2]);
            double delta = entry.getValue() - tracker[0];
            tracker[0] += delta / state.sessionCount;
            tracker[1] += delta * (entry.getValue() - tracker[0]);
        }

        double totalVar = state.metricTrackers.values().stream().mapToDouble(t -> t[1]).sum();
        double totalMean = state.metricTrackers.values().stream().mapToDouble(t -> t[0]).sum();
        state.behaviorCV = totalMean > 0 ? Math.sqrt(Math.max(0, totalVar)) / totalMean : 1.0;

        if (canAdmitToPool(playerId, state)) {
            state.metricTrackers.forEach((metric, stats) ->
                storage.savePopulationBaseline(metric, stats[0],
                        Math.sqrt(Math.max(0, stats[1])), 1));
            state.admitted = true;
        }
    }

    private static class AdmissionState {
        int totalVL;
        long onlineMinutes;
        int sessionCount;
        double behaviorCV = 1.0;
        boolean admitted;
        final Map<String, double[]> metricTrackers = new HashMap<>();
    }
}
