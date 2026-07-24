package org.coffeepop.latchac.paper;

import com.github.retrooper.packetevents.PacketEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.config.ConfigManager;
import org.coffeepop.latchac.core.engine.PredictionEngine;
import org.coffeepop.latchac.core.player.LatchPlayer;
import org.coffeepop.latchac.paper.command.LatchACCommand;
import org.coffeepop.latchac.paper.listener.PacketCheckListener;
import org.coffeepop.latchac.paper.listener.PlayerListener;
import org.coffeepop.latchac.paper.nms.v26_1.V26_1MotionSimulator;

import java.util.*;
import java.util.logging.Level;

/**
 * Paper plugin entry point — wires platform adapters and the command executor.
 */
public final class LatchACPlugin extends JavaPlugin {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private PacketCheckListener packetListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        LatchAC.init(getLogger());
        LatchAC.get().initBaselineProfiler(getDataFolder());
        PredictionEngine.setSimulator(new V26_1MotionSimulator());

        var yaml = getConfig();
        var coreCfg = LatchAC.get().getConfigManager();

        // ---- Parse punish ladder ----
        var ladder = new ArrayList<ConfigManager.LadderStep>();
        var ladderSection = yaml.getMapList("punish-ladder");
        if (ladderSection != null) {
            for (var step : ladderSection) {
                int vl = (int) step.get("vl");
                ConfigManager.ActionType action = null;
                String duration = null;
                List<String> commands = List.of();
                boolean resetVl = step.get("reset-vl") instanceof Boolean b ? b : true;

                if (step.containsKey("action") && step.get("action") != null) {
                    action = ConfigManager.ActionType.valueOf(
                            ((String) step.get("action")).toUpperCase());
                    if (action == ConfigManager.ActionType.TEMPBAN) {
                        duration = (String) step.get("duration");
                    }
                }
                if (step.containsKey("commands")) {
                    @SuppressWarnings("unchecked")
                    var rawCmds = (List<String>) step.get("commands");
                    if (rawCmds != null) commands = rawCmds;
                }
                ladder.add(new ConfigManager.LadderStep(vl, action, duration, commands, resetVl));
            }
        }

        // ---- Parse messages ----
        var msgsSection = yaml.getConfigurationSection("messages");
        String kickMsg = msgsSection != null ? msgsSection.getString("kick") : null;
        String banMsg = msgsSection != null ? msgsSection.getString("ban") : null;

        coreCfg.init(yaml.getBoolean("debug", false), yaml.getString("prefix"),
                ladder, kickMsg, banMsg);

        // ---- Baseline config ----
        var pop = LatchAC.get().getBaselineProfiler().getPopulation();
        var whitelist = yaml.getStringList("baseline.admin-whitelist");
        for (String uuid : whitelist) {
            try { pop.addAdminWhitelist(UUID.fromString(uuid)); }
            catch (IllegalArgumentException ignored) {}
        }
        boolean trainingMode = yaml.getBoolean("baseline.training-mode", false);
        LatchAC.get().getBaselineProfiler().setTrainingMode(trainingMode);
        LatchAC.get().getViolationHandler().setTrainingMode(trainingMode);
        if (trainingMode) getLogger().info("Training mode enabled");

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        for (Player p : Bukkit.getOnlinePlayers()) {
            LatchPlayer lp = new LatchPlayer(p.getUniqueId(), p.getName(), p);
            lp.setInVehicle(p.isInsideVehicle());
            lp.setGameMode(p.getGameMode().ordinal());
            lp.setAllowFlight(p.getAllowFlight());
            syncBypassPermissions(lp, p);
            LatchAC.get().getDataManager().addPlayer(lp);
        }

        var vh = LatchAC.get().getViolationHandler();

        // Verbose callback — every flag when debug on
        vh.setVerboseCallback((pid, check, vl, detail) -> {
            if (!LatchAC.get().getConfigManager().isDebug()) return;
            Player target = Bukkit.getPlayer(pid);
            if (target == null) return;
            var msg = Component.text().append(Component.text("[", NamedTextColor.GRAY))
                    .append(Component.text("L", NamedTextColor.AQUA))
                    .append(Component.text("] ", NamedTextColor.GRAY))
                    .append(Component.text(check, NamedTextColor.WHITE))
                    .append(Component.text(" flagged ", NamedTextColor.GRAY))
                    .append(Component.text(target.getName(), NamedTextColor.WHITE))
                    .append(Component.text(" [VL ", NamedTextColor.GRAY))
                    .append(Component.text(String.valueOf(vl), NamedTextColor.RED))
                    .append(Component.text("] ", NamedTextColor.GRAY))
                    .append(Component.text(detail, NamedTextColor.DARK_GRAY)).build();
            Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission("latchac.alerts"))
                    .forEach(p -> p.sendMessage(msg));
        });

        // Ladder callback — commands + action
        vh.setLadderCallback((pid, check, vl, commands, resetVl, v) -> {
            Player target = Bukkit.getPlayer(pid);
            String playerName = target != null ? target.getName() : pid.toString();

            // Auto alert broadcast
            var alertMsg = Component.text().append(Component.text("[", NamedTextColor.GRAY))
                    .append(Component.text("L", NamedTextColor.AQUA))
                    .append(Component.text("] ", NamedTextColor.GRAY))
                    .append(Component.text("\u26A0 ", NamedTextColor.RED))
                    .append(Component.text(check, NamedTextColor.WHITE))
                    .append(Component.text(" ", NamedTextColor.GRAY))
                    .append(Component.text(playerName, NamedTextColor.WHITE))
                    .append(Component.text(" VL " + vl, NamedTextColor.GRAY)).build();
            Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission("latchac.alerts"))
                    .forEach(p -> p.sendMessage(alertMsg));

            getLogger().log(Level.WARNING, "[LatchAC] {0} VL {1} {2} → cmds={3}",
                    new Object[]{playerName, vl, check, commands});

            // Action mode
            var step = findStep(vl);
            if (step != null && step.action() != null) {
                executeAction(target, step.action(), step.duration(), check, v);
            }

            // Commands mode
            for (String cmd : commands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        expandMessagePlaceholders(cmd, v));
            }
        });

        // Setback
        LatchPlayer.setOnSetback(lp -> {
            Player p = Bukkit.getPlayer(lp.getUniqueId());
            if (p == null) return;
            p.teleport(new Location(p.getWorld(), lp.getLastX(), lp.getLastY(), lp.getLastZ(),
                    lp.getLastYaw(), lp.getLastPitch()));
            lp.clearSetbackGuard();
        });

        packetListener = new PacketCheckListener(this);
        PacketEvents.getAPI().getEventManager().registerListener(packetListener);

        var cmd = new LatchACCommand();
        Objects.requireNonNull(getCommand("latchac")).setExecutor(cmd);
        Objects.requireNonNull(getCommand("latchac")).setTabCompleter(cmd);

        Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> LatchAC.get().getViolationHandler().cleanupStaleEntries(), 6000L, 6000L);

        getLogger().info("LatchAC v" + getPluginMeta().getVersion() + " enabled. "
                + LatchAC.get().getCheckRegistry().getAllChecks().size() + " checks loaded.");
    }

    private void executeAction(Player target, ConfigManager.ActionType action,
                                String duration, String check,
                                org.coffeepop.latchac.core.violation.Violation v) {
        if (target == null) return;
        var cfg = LatchAC.get().getConfigManager();
        Component reason = renderMessage(
                action == ConfigManager.ActionType.KICK ? cfg.getKickMessage() : cfg.getBanMessage(),
                target.getName(), check, 0, v);

        switch (action) {
            case KICK -> target.kick(reason);
            case BAN -> {
                target.kick(reason);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "ban " + target.getName() + " LatchAC: " + check);
            }
            case IPBAN -> {
                target.kick(reason);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "ban-ip " + target.getName() + " LatchAC: " + check);
            }
            case TEMPBAN -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "tempban " + target.getName() + " " + (duration != null ? duration : "24h")
                    + " LatchAC: " + check);
        }
    }

    private static Component renderMessage(String template, String player, String check,
                                            int vl, org.coffeepop.latchac.core.violation.Violation v) {
        if (template == null)
            return Component.text("You have been banned by Latch AntiCheat.", NamedTextColor.RED);
        String r = template.replace("{player}", player).replace("{check}", check)
                .replace("{vl}", String.valueOf(vl));
        if (v != null) {
            r = r.replace("{x}", fmt(v.getX())).replace("{y}", fmt(v.getY()))
                    .replace("{z}", fmt(v.getZ())).replace("{dx}", fmt(v.getDeltaX()))
                    .replace("{dy}", fmt(v.getDeltaY())).replace("{dz}", fmt(v.getDeltaZ()))
                    .replace("{ground}", String.valueOf(v.isOnGround()))
                    .replace("{vehicle}", String.valueOf(v.isInVehicle()))
                    .replace("{gamemode}", String.valueOf(v.getGameMode()))
                    .replace("{cps}", fmt(v.getCps()))
                    .replace("{ping}", String.valueOf(v.getPing()));
        }
        return MM.deserialize(r);
    }

    private String expandMessagePlaceholders(String cmd,
                                              org.coffeepop.latchac.core.violation.Violation v) {
        var cfg = LatchAC.get().getConfigManager();
        String kickText = cfg.getKickMessage().replace("{check}",
                v != null ? v.getCheckName() : "?");
        String banText = cfg.getBanMessage().replace("{check}",
                v != null ? v.getCheckName() : "?");
        return cmd.replace("{kick}", stripMiniMessage(kickText))
                .replace("{ban}", stripMiniMessage(banText));
    }

    private static String stripMiniMessage(String mm) {
        return mm.replaceAll("<[^>]+>", "");
    }

    private static String fmt(double d) { return String.format("%.3f", d); }

    private static ConfigManager.LadderStep findStep(int vl) {
        for (var s : LatchAC.get().getConfigManager().getPunishLadder()) {
            if (vl == s.vl() || (vl > s.vl() && vl % s.vl() == 0)) return s;
        }
        return null;
    }

    @Override
    public void onDisable() {
        if (packetListener != null)
            PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
        try { LatchAC.get().shutdown(); } catch (IllegalStateException ignored) {}
    }

    public static void syncBypassPermissions(LatchPlayer lp, Player p) {
        lp.clearBypasses();
        for (var check : LatchAC.get().getCheckRegistry().getAllChecks()) {
            String name = check.getName().toLowerCase();
            if (p.hasPermission("latchac.bypass." + name)) lp.addBypass(name);
            String cat = name.contains(".") ? name.substring(0, name.indexOf('.')) : name;
            if (p.hasPermission("latchac.bypass." + cat + ".*")) lp.addBypass(cat + ".*");
        }
        if (p.hasPermission("latchac.bypass.*")) lp.addBypass("*");
    }
}
