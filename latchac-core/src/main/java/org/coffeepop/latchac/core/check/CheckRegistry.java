package org.coffeepop.latchac.core.check;

import org.coffeepop.latchac.core.player.LatchPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe registry for {@link Check} instances.
 * <p>
 * Checks are indexed by name (case-insensitive) and {@link CheckType}.
 * {@link #runChecks} is called automatically from {@link LatchPlayer} after
 * each movement update — the platform layer never touches dispatch.
 */
public class CheckRegistry {

    private final Map<String, Check> checksByName = new ConcurrentHashMap<>();
    private final Map<CheckType, List<Check>> checksByType = new ConcurrentHashMap<>();

    public CheckRegistry() {
        for (CheckType t : CheckType.values()) checksByType.put(t, new CopyOnWriteArrayList<>());
        LatchPlayer.onDataUpdate = this::runChecks;
    }

    public void register(Check check) {
        checksByName.put(check.getName().toLowerCase(), check);
        checksByType.get(check.getType()).add(check);
    }

    // ---- Lookup ----

    public Optional<Check> getCheck(String name) {
        return Optional.ofNullable(checksByName.get(name.toLowerCase()));
    }

    public List<Check> getChecksByType(CheckType type) {
        return Collections.unmodifiableList(checksByType.getOrDefault(type, List.of()));
    }

    public Collection<Check> getAllChecks() {
        return Collections.unmodifiableCollection(checksByName.values());
    }

    public void enable(String name)  { getCheck(name).ifPresent(c -> c.setEnabled(true)); }
    public void disable(String name) { getCheck(name).ifPresent(c -> c.setEnabled(false)); }

    // ---- Dispatch ----

    public void runChecks(LatchPlayer player) {
        for (Check check : checksByName.values()) {
            if (check.isEnabled()) check.onCheck(player);
        }
    }

    public void unregisterAll() {
        checksByName.clear();
        checksByType.values().forEach(List::clear);
    }
}
