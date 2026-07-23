package org.coffeepop.latchac.core.violation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tracks Violation Levels (VL) per player per check and dispatches events.
 *
 * <h3>Callbacks</h3>
 * <ul>
 *   <li>{@link #setVerboseCallback} — every single flag (for staff verbose alerts)</li>
 *   <li>{@link #setAlertCallback} — VL hits multiples of alert threshold (20, 40)</li>
 *   <li>{@link #setPunishCallback} — VL hits punish threshold (50) → reset</li>
 * </ul>
 * VL decays by 1 every 60s of inactivity.
 */
public class ViolationHandler {

    private static final int ALERT_THRESHOLD = 20;
    private static final int PUNISH_THRESHOLD = 50;
    private static final long DECAY_MS = 60_000;
    private static final long STALE_MS = 600_000; // 10 min — auto-cleanup stale entries

    private final Logger logger;
    private final Map<UUID, Map<String, Integer>> playerVLs = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastDecay = new ConcurrentHashMap<>();

    private FlagCallback verbose = (pid, cn, vl, d) -> {};
    private FlagCallback alert   = (pid, cn, vl, d) -> {};
    private FlagCallback punish  = (pid, cn, vl, d) -> {};
    private boolean trainingMode;

    public ViolationHandler(Logger logger) { this.logger = logger; }

    @FunctionalInterface
    public interface FlagCallback {
        void onFlag(UUID playerId, String checkName, int totalVL, String detail);
    }

    public void setVerboseCallback(FlagCallback c) { this.verbose = c; }
    public void setAlertCallback(FlagCallback c)   { this.alert = c; }
    public void setPunishCallback(FlagCallback c)  { this.punish = c; }
    public void setTrainingMode(boolean trainingMode) { this.trainingMode = trainingMode; }
    public boolean isTrainingMode() { return trainingMode; }

    public void handle(Violation v) {
        UUID pid = v.getPlayerId();

        // Training mode: only verbose logging, no VL accumulation or punishment
        if (trainingMode) {
            verbose.onFlag(pid, v.getCheckName(), -1, "[TRAINING] " + v.getDetail());
            return;
        }

        decayVL(pid);

        Map<String, Integer> ckVLs = playerVLs.computeIfAbsent(pid, k -> new ConcurrentHashMap<>());
        int newVL = ckVLs.merge(v.getCheckName(), 1, Integer::sum);

        // Every flag → verbose (debug-mode detail for staff)
        verbose.onFlag(pid, v.getCheckName(), newVL, v.getDetail());

        if (newVL >= PUNISH_THRESHOLD) {
            logger.log(Level.WARNING, "[PUNISH] {0} VL {1} {2}: {3}",
                    new Object[]{pid, newVL, v.getCheckName(), v.getDetail()});
            punish.onFlag(pid, v.getCheckName(), newVL, v.getDetail());
            ckVLs.put(v.getCheckName(), 0);
        } else if (newVL >= ALERT_THRESHOLD && newVL % ALERT_THRESHOLD == 0) {
            logger.log(Level.WARNING, "[ALERT] {0} VL {1} {2}: {3}",
                    new Object[]{pid, newVL, v.getCheckName(), v.getDetail()});
            alert.onFlag(pid, v.getCheckName(), newVL, v.getDetail());
        }
    }

    private void decayVL(UUID pid) {
        long now = System.currentTimeMillis();
        long last = lastDecay.getOrDefault(pid, now);
        int decay = (int) ((now - last) / DECAY_MS);
        if (decay > 0) {
            Map<String, Integer> ckVLs = playerVLs.get(pid);
            if (ckVLs != null) ckVLs.replaceAll((k, vl) -> Math.max(0, vl - decay));
            lastDecay.put(pid, now);
        }
    }

    public int getVL(UUID pid, String checkName) {
        Map<String, Integer> ckVLs = playerVLs.get(pid);
        return ckVLs != null ? ckVLs.getOrDefault(checkName, 0) : 0;
    }

    public Map<String, Integer> getAllVLs(UUID pid) {
        return Map.copyOf(playerVLs.getOrDefault(pid, Map.of()));
    }

    public void reset(UUID pid) {
        playerVLs.remove(pid);
        lastDecay.remove(pid);
    }

    /**
     * Removes VL data for players not seen in {@value #STALE_MS}ms.
     * Call periodically from the platform (e.g. every 5 min) or on quit.
     */
    public void cleanupStaleEntries() {
        long cutoff = System.currentTimeMillis() - STALE_MS;
        lastDecay.entrySet().removeIf(e -> {
            if (e.getValue() < cutoff) {
                playerVLs.remove(e.getKey());
                return true;
            }
            return false;
        });
    }
}
