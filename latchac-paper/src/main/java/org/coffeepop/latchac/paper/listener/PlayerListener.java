package org.coffeepop.latchac.paper.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.player.LatchPlayer;
import org.coffeepop.latchac.paper.LatchACPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Player lifecycle + vehicle + gameMode state sync.
 */
public class PlayerListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        org.bukkit.entity.Player p = e.getPlayer();
        LatchPlayer lp = new LatchPlayer(p.getUniqueId(), p.getName(), p);
        lp.setInVehicle(p.isInsideVehicle());
        syncGameMode(lp, p);
        LatchACPlugin.syncBypassPermissions(lp, p);
        LatchAC.get().getDataManager().addPlayer(lp);
        LatchAC.get().getBaselineProfiler().onPlayerJoin(p.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        var id = e.getPlayer().getUniqueId();
        var data = LatchAC.get().getDataManager().get(id);
        if (data != null) {
            var vl = LatchAC.get().getViolationHandler().getAllVLs(id);
            int totalVL = vl.values().stream().mapToInt(Integer::intValue).sum();
            long onlineMs = System.currentTimeMillis() - data.getPlayer().getLastMoveTime();
            Map<String, Double> snapshots = new HashMap<>();
            snapshots.put("packetInterval", computePacketInterval(data.getPlayer()
                    .getPacketTimestamps()));
            LatchAC.get().getBaselineProfiler().onPlayerQuit(id, totalVL, onlineMs, snapshots);
        }
        LatchAC.get().getCheckRegistry().onPlayerQuit(id);
        LatchAC.get().getDataManager().remove(id);
        LatchAC.get().getViolationHandler().reset(id);
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent e) {
        var data = LatchAC.get().getDataManager().get(e.getPlayer().getUniqueId());
        if (data != null) {
            data.getPlayer().setGameMode(e.getNewGameMode().ordinal());
            data.getPlayer().setAllowFlight(e.getPlayer().getAllowFlight());
        }
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent e) {
        if (e.getEntered() instanceof org.bukkit.entity.Player p) {
            var data = LatchAC.get().getDataManager().get(p.getUniqueId());
            if (data != null) data.getPlayer().setInVehicle(true);
        }
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent e) {
        if (e.getExited() instanceof org.bukkit.entity.Player p) {
            var data = LatchAC.get().getDataManager().get(p.getUniqueId());
            if (data != null) data.getPlayer().setInVehicle(false);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        // 排除玩家自身物品栏和合成界面——仅追踪外部容器
        var type = e.getInventory().getType();
        if (type == InventoryType.PLAYER || type == InventoryType.CRAFTING || type == InventoryType.CREATIVE) return;
        var data = LatchAC.get().getDataManager().get(e.getPlayer().getUniqueId());
        if (data != null) {
            data.getPlayer().setInContainer(true, type.name());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        var data = LatchAC.get().getDataManager().get(e.getPlayer().getUniqueId());
        if (data != null) {
            data.getPlayer().setInContainer(false, null);
        }
    }

    private static void syncGameMode(LatchPlayer lp, org.bukkit.entity.Player p) {
        lp.setGameMode(p.getGameMode().ordinal());
        lp.setAllowFlight(p.getAllowFlight());
    }

    private static double computePacketInterval(java.util.Deque<Long> timestamps) {
        if (timestamps.size() < 2) return 50.0;
        var it = timestamps.descendingIterator();
        long a = it.next();
        long b = it.next();
        return (a - b) / 1_000_000.0;
    }
}
