package org.coffeepop.latchac.paper;

import com.github.retrooper.packetevents.PacketEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.player.LatchPlayer;
import org.coffeepop.latchac.core.engine.PredictionEngine;
import org.coffeepop.latchac.paper.command.LatchACCommand;
import org.coffeepop.latchac.paper.listener.PacketCheckListener;
import org.coffeepop.latchac.paper.listener.PlayerListener;
import org.coffeepop.latchac.paper.nms.v26_1.V26_1MotionSimulator;

import java.util.Objects;
import java.util.UUID;

/**
 * Paper plugin entry point — wires platform adapters and the command executor.
 * Uses Adventure API for modern Component-based messaging.
 */
public final class LatchACPlugin extends JavaPlugin {

    private PacketCheckListener packetListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        LatchAC.init(getLogger());
        LatchAC.get().initBaselineProfiler(getDataFolder());
        PredictionEngine.setSimulator(new V26_1MotionSimulator());

        // ---- Core config (debug, prefix, VL thresholds) ----
        var yaml = getConfig();
        var coreCfg = LatchAC.get().getConfigManager();
        coreCfg.init(
            yaml.getBoolean("debug", false),
            yaml.getString("prefix"),
            yaml.getInt("alert-threshold", 20),
            yaml.getInt("punish-threshold", 50)
        );

        // ---- Baseline config (population baseline, training mode) ----
        var pop = LatchAC.get().getBaselineProfiler().getPopulation();
        var whitelist = yaml.getStringList("baseline.admin-whitelist");
        for (String uuid : whitelist) {
            try { pop.addAdminWhitelist(UUID.fromString(uuid)); }
            catch (IllegalArgumentException ignored) {}
        }

        boolean trainingMode = yaml.getBoolean("baseline.training-mode", false);
        LatchAC.get().getBaselineProfiler().setTrainingMode(trainingMode);
        LatchAC.get().getViolationHandler().setTrainingMode(trainingMode);
        if (trainingMode) {
            getLogger().info("Training mode enabled — flags logged only, VL suppressed, data feeds into population baseline");
        }

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        for (Player p : Bukkit.getOnlinePlayers()) {
            LatchPlayer lp = new LatchPlayer(p.getUniqueId(), p.getName(), p);
            lp.setInVehicle(p.isInsideVehicle());
            lp.setGameMode(p.getGameMode().ordinal());
            lp.setAllowFlight(p.getAllowFlight());
            LatchAC.get().getDataManager().addPlayer(lp);
        }

        var vh = LatchAC.get().getViolationHandler();

        // Verbose: every flag (when debug mode on) → staff with latchac.alerts
        vh.setVerboseCallback((pid, check, vl, detail) -> {
            if (!LatchAC.get().getConfigManager().isDebug()) return;
            Player target = Bukkit.getPlayer(pid);
            if (target == null) return;
            Component msg = Component.text()
                    .append(Component.text("[", NamedTextColor.GRAY))
                    .append(Component.text("L", NamedTextColor.AQUA))
                    .append(Component.text("] ", NamedTextColor.GRAY))
                    .append(Component.text(check, NamedTextColor.WHITE))
                    .append(Component.text(" flagged ", NamedTextColor.GRAY))
                    .append(Component.text(target.getName(), NamedTextColor.WHITE))
                    .append(Component.text(" [VL ", NamedTextColor.GRAY))
                    .append(Component.text(String.valueOf(vl), NamedTextColor.RED))
                    .append(Component.text("] ", NamedTextColor.GRAY))
                    .append(Component.text(detail, NamedTextColor.DARK_GRAY))
                    .build();
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("latchac.alerts"))
                    .forEach(p -> p.sendMessage(msg));
        });

        // Alert: VL thresholds → extra notice
        vh.setAlertCallback((pid, check, vl, detail) -> {
            Player target = Bukkit.getPlayer(pid);
            if (target == null) return;
            Component msg = Component.text()
                    .append(Component.text("[", NamedTextColor.GRAY))
                    .append(Component.text("L", NamedTextColor.AQUA))
                    .append(Component.text("] ", NamedTextColor.GRAY))
                    .append(Component.text("\u26A0 ", NamedTextColor.RED))
                    .append(Component.text(check, NamedTextColor.WHITE))
                    .append(Component.text(" ", NamedTextColor.GRAY))
                    .append(Component.text(target.getName(), NamedTextColor.WHITE))
                    .append(Component.text(" reached VL ", NamedTextColor.GRAY))
                    .append(Component.text(String.valueOf(vl), NamedTextColor.RED))
                    .append(Component.text("! ", NamedTextColor.GRAY))
                    .append(Component.text(detail, NamedTextColor.DARK_GRAY))
                    .build();
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("latchac.alerts"))
                    .forEach(p -> p.sendMessage(msg));
            Bukkit.getLogger().warning("[LatchAC] " + target.getName()
                    + " VL " + vl + " on " + check + " — " + detail);
        });

        // Punish: VL 50 → kick
        vh.setPunishCallback((pid, check, vl, detail) -> {
            Player p = Bukkit.getPlayer(pid);
            if (p != null) {
                Component reason = Component.text()
                        .append(Component.text("Kicked by Latch AntiCheat.\nReason: ", NamedTextColor.RED))
                        .append(Component.text(check, NamedTextColor.WHITE))
                        .build();
                p.kick(reason);
            }
        });

        // Setback: teleport player to last valid position
        LatchPlayer.setOnSetback(lp -> {
            Player p = Bukkit.getPlayer(lp.getUniqueId());
            if (p == null) return;
            p.teleport(new Location(p.getWorld(),
                    lp.getLastX(), lp.getLastY(), lp.getLastZ(),
                    lp.getLastYaw(), lp.getLastPitch()));
            // Clear the re-entrancy guard after teleport completes
            lp.clearSetbackGuard();
        });

        packetListener = new PacketCheckListener(this);
        PacketEvents.getAPI().getEventManager().registerListener(packetListener);

        // Register /latchac command
        var cmd = new LatchACCommand();
        Objects.requireNonNull(getCommand("latchac")).setExecutor(cmd);
        Objects.requireNonNull(getCommand("latchac")).setTabCompleter(cmd);

        // Periodic stale VL cleanup (every 5 minutes)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> LatchAC.get().getViolationHandler().cleanupStaleEntries(),
                6000L, 6000L);

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
