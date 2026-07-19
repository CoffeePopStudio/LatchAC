package org.coffeepop.latchac.core.check;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class CheckRegistry {

    private final Map<String, Check> checksByName = new ConcurrentHashMap<>();
    private final Map<CheckType, List<Check>> checksByType = new ConcurrentHashMap<>();

    public CheckRegistry() {
        for (CheckType t : CheckType.values()) checksByType.put(t, new CopyOnWriteArrayList<>());
    }

    public void register(Check check) {
        checksByName.put(check.getName().toLowerCase(), check);
        checksByType.get(check.getType()).add(check);
    }

    public Optional<Check> getCheck(String name) {
        return Optional.ofNullable(checksByName.get(name.toLowerCase()));
    }

    public List<Check> getChecksByType(CheckType type) {
        return Collections.unmodifiableList(checksByType.getOrDefault(type, List.of()));
    }

    public Collection<Check> getAllChecks() {
        return Collections.unmodifiableCollection(checksByName.values());
    }

    public void enable(String name) { getCheck(name).ifPresent(c -> c.setEnabled(true)); }
    public void disable(String name) { getCheck(name).ifPresent(c -> c.setEnabled(false)); }

    public void unregisterAll() {
        checksByName.clear();
        checksByType.values().forEach(List::clear);
    }
}
