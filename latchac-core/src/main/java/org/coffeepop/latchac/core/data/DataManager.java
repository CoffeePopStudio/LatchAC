package org.coffeepop.latchac.core.data;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {

    private final Map<UUID, PlayerData> map = new ConcurrentHashMap<>();

    public PlayerData getOrCreate(UUID playerId) {
        return map.computeIfAbsent(playerId, PlayerData::new);
    }

    public PlayerData get(UUID playerId) { return map.get(playerId); }
    public void remove(UUID playerId) { map.remove(playerId); }
    public void clearAll() { map.clear(); }
    public int getOnlineCount() { return map.size(); }
}
