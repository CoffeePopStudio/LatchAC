package org.coffeepop.latchac.paper;

import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.player.LatchPlayer;
import org.coffeepop.latchac.paper.command.LatchACCommand;
import org.coffeepop.latchac.paper.listener.PacketCheckListener;
import org.coffeepop.latchac.paper.listener.PlayerListener;

import java.util.Objects;

/**
 * Paper plugin entry point — wires platform adapters and the command executor.
 */
public final class LatchACPlugin extends JavaPlugin {

    private PacketCheckListener packetListener;

    @Override
    public void onEnable() {
        LatchAC.init(getLogger());

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        for (Player p : Bukkit.getOnlinePlayers()) {
            LatchPlayer lp = new LatchPlayer(p.getUniqueId(), p.getName(), p);
            LatchAC.get().getDataManager().addPlayer(lp);
        }

        var vh = LatchAC.get().getViolationHandler();

        // Verbose: every flag (when debug mode on) → staff with latchac.alerts
        vh.setVerboseCallback((pid, check, vl, detail) -> {
            if (!LatchAC.get().getConfigManager().isDebug()) return;
            String msg = "§7[§bL§7] §f" + check + " §7flagged §f" +
                    Bukkit.getPlayer(pid).getName() + " §7[VL §c" + vl + "§7] §8" + detail;
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("latchac.alerts"))
                    .forEach(p -> p.sendMessage(msg));
        });

        // Alert: VL thresholds → extra notice
        vh.setAlertCallback((pid, check, vl, detail) -> {
            String msg = "§7[§bL§7] §c⚠ §f" + check + " §7" +
                    Bukkit.getPlayer(pid).getName() + " §7reached VL §c" + vl + "§7! §8" + detail;
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("latchac.alerts"))
                    .forEach(p -> p.sendMessage(msg));
            Bukkit.getLogger().warning("[LatchAC] " + Bukkit.getPlayer(pid).getName()
                    + " VL " + vl + " on " + check + " — " + detail);
        });

        // Punish: VL 50 → kick
        vh.setPunishCallback((pid, check, vl, detail) -> {
            Player p = Bukkit.getPlayer(pid);
            if (p != null) p.kickPlayer("Kicked by Latch AntiCheat.\nReason: " + check);
        });

        // Setback: teleport player to last valid position
        LatchPlayer.onSetback = lp -> {
            Player p = Bukkit.getPlayer(lp.getUniqueId());
            if (p == null) return;
            p.teleport(new Location(p.getWorld(),
                    lp.getLastX(), lp.getLastY(), lp.getLastZ(),
                    lp.getLastYaw(), lp.getLastPitch()));
        };

        packetListener = new PacketCheckListener(this);
        PacketEvents.getAPI().getEventManager().registerListener(packetListener);

        // Register /latchac command
        var cmd = new LatchACCommand();
        Objects.requireNonNull(getCommand("latchac")).setExecutor(cmd);
        Objects.requireNonNull(getCommand("latchac")).setTabCompleter(cmd);

        getLogger().info("LatchAC v" + getPluginMeta().getVersion() + " enabled. "
                + LatchAC.get().getCheckRegistry().getAllChecks().size() + " checks loaded.");
    }

    @Override
    public void onDisable() {
        if (packetListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
        }
        try { LatchAC.get().shutdown(); } catch (IllegalStateException ignored) {}
    }
}
