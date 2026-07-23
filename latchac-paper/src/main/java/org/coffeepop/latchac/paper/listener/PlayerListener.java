package org.coffeepop.latchac.paper.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.player.LatchPlayer;

/**
 * Player lifecycle + vehicle state sync.
 */
public class PlayerListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        org.bukkit.entity.Player p = e.getPlayer();
        LatchPlayer lp = new LatchPlayer(p.getUniqueId(), p.getName(), p);
        lp.setInVehicle(p.isInsideVehicle());
        LatchAC.get().getDataManager().addPlayer(lp);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        LatchAC.get().getDataManager().remove(e.getPlayer().getUniqueId());
        LatchAC.get().getViolationHandler().reset(e.getPlayer().getUniqueId());
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
}
