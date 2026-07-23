package org.coffeepop.latchac.core.command.impl;

import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.command.SubCommand;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /latchac vl <player> — shows violation levels.
 */
public class VLCommand implements SubCommand {

    @Override
    public String name() { return "vl"; }

    @Override
    public List<String> execute(String senderId, String[] args) {
        if (args.length == 0) return List.of("§cUsage: /latchac vl <player>");

        UUID target = findPlayer(args[0]);
        if (target == null) return List.of("§cPlayer not found: " + args[0]);

        Map<String, Integer> vls = LatchAC.get().getViolationHandler().getAllVLs(target);
        if (vls.isEmpty()) return List.of("§a" + args[0] + " has no violations.");

        var lines = new java.util.ArrayList<String>();
        lines.add("§7VL for §f" + args[0] + "§7:");
        vls.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> lines.add("  §7" + e.getKey() + " §8→ §f" + e.getValue()));
        return lines;
    }

    @Override
    public List<String> tabComplete(String[] args, List<String> online) {
        if (args.length == 1) return online;
        return List.of();
    }

    private UUID findPlayer(String name) {
        var data = LatchAC.get().getDataManager();
        for (var entry : data.getAll()) {
            if (entry.getPlayer().getName().equalsIgnoreCase(name)) {
                return entry.getPlayerId();
            }
        }
        try {
            return UUID.fromString(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
