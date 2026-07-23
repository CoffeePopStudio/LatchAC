package org.coffeepop.latchac.core.command.impl;

import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.command.SubCommand;

import java.util.List;

/**
 * /latchac check list | enable <name> | disable <name>
 */
public class CheckCommand implements SubCommand {

    @Override
    public String name() { return "check"; }

    @Override
    public List<String> execute(String senderId, String[] args) {
        if (args.length == 0) return usage();

        return switch (args[0].toLowerCase()) {
            case "list"  -> listChecks();
            case "enable"  -> toggle(args, true);
            case "disable" -> toggle(args, false);
            default -> usage();
        };
    }

    @Override
    public List<String> tabComplete(String[] args, List<String> online) {
        if (args.length == 1) return List.of("list", "enable", "disable");
        if (args.length == 2 && (args[0].equals("enable") || args[0].equals("disable"))) {
            return LatchAC.get().getCheckRegistry().getAllChecks().stream()
                    .map(Check::getName).toList();
        }
        return List.of();
    }

    private List<String> usage() {
        return List.of(
                "§7/latchac check §flist",
                "§7/latchac check §fenable §7<name>",
                "§7/latchac check §fdisable §7<name>");
    }

    private List<String> listChecks() {
        var checks = LatchAC.get().getCheckRegistry().getAllChecks();
        if (checks.isEmpty()) return List.of("§7No checks loaded.");
        var lines = new java.util.ArrayList<>(List.of("§7Checks (" + checks.size() + "):"));
        for (Check c : checks) {
            String state = c.isEnabled() ? "§aON" : "§cOFF";
            lines.add("  " + state + " §7" + c.getName() + " §8[" + c.getType().getDisplayName() + "]");
        }
        return lines;
    }

    private List<String> toggle(String[] args, boolean enable) {
        if (args.length < 2) return List.of("§cUsage: /latchac check " + (enable ? "enable" : "disable") + " <name>");
        var opt = LatchAC.get().getCheckRegistry().getCheck(args[1]);
        if (opt.isEmpty()) return List.of("§cCheck not found: " + args[1]);
        Check c = opt.get();
        c.setEnabled(enable);
        return List.of((enable ? "§aEnabled" : "§cDisabled") + "§7: " + c.getName());
    }
}
