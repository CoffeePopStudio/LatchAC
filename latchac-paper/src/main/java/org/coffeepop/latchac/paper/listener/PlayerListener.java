package org.coffeepop.latchac.paper.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.data.PlayerData;

/**
 * 玩家生命周期管理。
 */
public class PlayerListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        PlayerData data = LatchAC.get().getDataManager().getOrCreate(e.getPlayer().getUniqueId());
        data.setAttribute("bukkit_player", e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        LatchAC.get().getDataManager().remove(e.getPlayer().getUniqueId());
        LatchAC.get().getViolationHandler().reset(e.getPlayer().getUniqueId());
    }
}
