package org.coffeepop.latchac.core.data;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData {

    private final UUID playerId;
    private final Map<String, Object> attrs = new ConcurrentHashMap<>();

    public PlayerData(UUID playerId) { this.playerId = playerId; }

    public UUID getPlayerId() { return playerId; }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) { return (T) attrs.get(key); }

    public void setAttribute(String key, Object value) { attrs.put(key, value); }
    public boolean hasAttribute(String key) { return attrs.containsKey(key); }
    public void removeAttribute(String key) { attrs.remove(key); }
}
