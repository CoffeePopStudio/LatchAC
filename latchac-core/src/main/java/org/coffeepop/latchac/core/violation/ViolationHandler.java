package org.coffeepop.latchac.core.violation;

import org.coffeepop.latchac.core.LatchAC;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tracks Violation Levels (VL) per player per check and dispatches ladder-based actions.
 * <p>
 * VL decays by 1 every 60s of inactivity.
 * Placeholder replacement for commands is done here before calling the platform callback.
 */
public class ViolationHandler {

    private static final long DECAY_MS = 60_000;
    private static final long STALE_MS = 600_000;
    private static final int MAX_HISTORY = 20;

    private final Logger logger;
    private final Map<UUID, Map<String, Integer>> playerVLs = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastDecay = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Violation>> history = new ConcurrentHashMap<>();

    private VerboseCallback verboseCallback = (pid, cn, vl, d) -> {};
    private LadderCallback ladderCallback = (pid, cn, vl, cmds, rst, v) -> {};
    private boolean trainingMode;

    public ViolationHandler(Logger logger) { this.logger = logger; }

    @FunctionalInterface
    public interface VerboseCallback {
        void onFlag(UUID playerId, String checkName, int totalVL, String detail);
    }

    @FunctionalInterface
    public interface LadderCallback {
        /** @param commands placeholder-replaced command list, empty if none */
        void onAction(UUID playerId, String checkName, int totalVL,
                      List<String> commands, boolean resetVl, Violation v);
    }

    public void setVerboseCallback(VerboseCallback c) { this.verboseCallback = c; }
    public void setLadderCallback(LadderCallback c) { this.ladderCallback = c; }
    public void setTrainingMode(boolean trainingMode) { this.trainingMode = trainingMode; }
    public boolean isTrainingMode() { return trainingMode; }

    public void handle(Violation v) {
        UUID pid = v.getPlayerId();

        // Store in history ring buffer
        history.computeIfAbsent(pid, k -> new ArrayDeque<>()).addLast(v);
        if (history.get(pid).size() > MAX_HISTORY) history.get(pid).removeFirst();

        // Training mode: only verbose logging
        if (trainingMode) {
            verboseCallback.onFlag(pid, v.getCheckName(), -1, v.getDetail());
            return;
        }

        decayVL(pid);

        Map<String, Integer> ckVLs = playerVLs.computeIfAbsent(pid, k -> new ConcurrentHashMap<>());
        int newVL = ckVLs.merge(v.getCheckName(), 1, Integer::sum);

        // Always fire verbose to platform (platform decides whether to show based on debug)
        verboseCallback.onFlag(pid, v.getCheckName(), newVL, v.getDetail());

        // Ladder check
        var ladder = LatchAC.get().getConfigManager().getPunishLadder();
        for (var step : ladder) {
            if (newVL == step.vl() || (newVL > step.vl() && newVL % step.vl() == 0)) {
                // Replace placeholders in commands
                List<String> resolved = step.commands().stream()
                        .map(cmd -> replacePlaceholders(cmd, v, newVL))
                        .toList();

                logger.log(Level.WARNING, "Ladder VL {0} → {1}: {2} {3}",
                        new Object[]{newVL, step.action() != null ? step.action() : "COMMANDS",
                                v.getCheckName(), v.getDetail()});

                ladderCallback.onAction(pid, v.getCheckName(), newVL,
                        resolved, step.resetVl(), v);

                // Reset VL if configured
                if (step.resetVl()) {
                    ckVLs.put(v.getCheckName(), 0);
                }
                return;
            }
        }
    }

    private static String replacePlaceholders(String cmd, Violation v, int vl) {
        return cmd
                .replace("{player}", v.getPlayerName() != null ? v.getPlayerName() : "?")
                .replace("{uuid}", v.getPlayerId().toString())
                .replace("{check}", v.getCheckName())
                .replace("{vl}", String.valueOf(vl))
                .replace("{detail}", v.getDetail())
                .replace("{x}", fmt(v.getX()))
                .replace("{y}", fmt(v.getY()))
                .replace("{z}", fmt(v.getZ()))
                .replace("{dx}", fmt(v.getDeltaX()))
                .replace("{dy}", fmt(v.getDeltaY()))
                .replace("{dz}", fmt(v.getDeltaZ()))
                .replace("{ground}", String.valueOf(v.isOnGround()))
                .replace("{vehicle}", String.valueOf(v.isInVehicle()))
                .replace("{gamemode}", String.valueOf(v.getGameMode()))
                .replace("{cps}", fmt(v.getCps()))
                .replace("{ping}", String.valueOf(v.getPing()));
    }

    private static String fmt(double d) { return String.format("%.3f", d); }

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

    public List<Violation> getHistory(UUID pid) {
        Deque<Violation> h = history.get(pid);
        return h != null ? List.copyOf(h) : List.of();
    }

    public void reset(UUID pid) {
        playerVLs.remove(pid);
        lastDecay.remove(pid);
        history.remove(pid);
    }

    public void cleanupStaleEntries() {
        long cutoff = System.currentTimeMillis() - STALE_MS;
        lastDecay.entrySet().removeIf(e -> {
            if (e.getValue() < cutoff) {
                playerVLs.remove(e.getKey());
                history.remove(e.getKey());
                return true;
            }
            return false;
        });
    }
}
