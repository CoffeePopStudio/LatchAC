package org.coffeepop.latchac.core.data;

import org.coffeepop.latchac.core.player.LatchPlayer;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages online player {@link PlayerData} instances.
 * <p>
 * The platform module constructs {@link LatchPlayer} on join and calls
 * {@link #addPlayer}. Core checks retrieve data via {@link #get}.
 */
public class DataManager {

    private final Map<UUID, PlayerData> map = new ConcurrentHashMap<>();

    /**
     * Registers a player on join.
     *
     * @param player platform-constructed LatchPlayer
     * @return the created or existing PlayerData
     */
    public PlayerData addPlayer(LatchPlayer player) {
        return map.computeIfAbsent(player.getUniqueId(), id -> new PlayerData(player));
    }

    public PlayerData get(UUID playerId) { return map.get(playerId); }

    /** Removes a player on quit. */
    public void remove(UUID playerId) { map.remove(playerId); }

    /** Clears all player data (called on shutdown). */
    public void clearAll() { map.clear(); }

    public int getOnlineCount() { return map.size(); }

    public Collection<PlayerData> getAll() { return map.values(); }
}
