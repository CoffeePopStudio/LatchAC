package org.coffeepop.latchac.core.data;

import org.coffeepop.latchac.core.player.LatchPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player data container holding a platform-agnostic {@link LatchPlayer} reference.
 * <p>
 * Also provides an open-ended attribute store for platform-specific data
 * (e.g. Bukkit Player reference, Fabric server entity, etc.).
 */
public class PlayerData {

    private final LatchPlayer player;
    private final Map<String, Object> attrs = new ConcurrentHashMap<>();

    public PlayerData(LatchPlayer player) { this.player = player; }

    public LatchPlayer getPlayer() { return player; }
    public UUID getPlayerId() { return player.getUniqueId(); }

    // ---- Attribute store ----

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) { return (T) attrs.get(key); }

    public void setAttribute(String key, Object value) { attrs.put(key, value); }
    public boolean hasAttribute(String key) { return attrs.containsKey(key); }
    public void removeAttribute(String key) { attrs.remove(key); }
}
