package org.coffeepop.latchac.core.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.command.SubCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * /latchac check list | enable <name> | disable <name>
 */
public class CheckCommand implements SubCommand {

    @Override
    public String name() { return "check"; }

    @Override
    public List<Component> execute(String senderId, String[] args) {
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

    private List<Component> usage() {
        return List.of(
                Component.text()
                        .append(Component.text("/latchac check ", NamedTextColor.GRAY))
                        .append(Component.text("list", NamedTextColor.WHITE))
                        .build(),
                Component.text()
                        .append(Component.text("/latchac check ", NamedTextColor.GRAY))
                        .append(Component.text("enable", NamedTextColor.WHITE))
                        .append(Component.text(" <name>", NamedTextColor.GRAY))
                        .build(),
                Component.text()
                        .append(Component.text("/latchac check ", NamedTextColor.GRAY))
                        .append(Component.text("disable", NamedTextColor.WHITE))
                        .append(Component.text(" <name>", NamedTextColor.GRAY))
                        .build());
    }

    private List<Component> listChecks() {
        var checks = LatchAC.get().getCheckRegistry().getAllChecks();
        if (checks.isEmpty()) return List.of(Component.text("No checks loaded.", NamedTextColor.GRAY));
        var lines = new ArrayList<Component>();
        lines.add(Component.text("Checks (" + checks.size() + "):", NamedTextColor.GRAY));
        for (Check c : checks) {
            NamedTextColor stateColor = c.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED;
            lines.add(Component.text()
                    .append(Component.text("  " + (c.isEnabled() ? "ON" : "OFF"), stateColor))
                    .append(Component.text(" " + c.getName(), NamedTextColor.GRAY))
                    .append(Component.text(" [" + c.getType().getDisplayName() + "]", NamedTextColor.DARK_GRAY))
                    .build());
        }
        return lines;
    }

    private List<Component> toggle(String[] args, boolean enable) {
        String action = enable ? "enable" : "disable";
        if (args.length < 2) return List.of(Component.text()
                .append(Component.text("Usage: /latchac check " + action + " <name>", NamedTextColor.RED))
                .build());
        var opt = LatchAC.get().getCheckRegistry().getCheck(args[1]);
        if (opt.isEmpty()) return List.of(
                Component.text("Check not found: " + args[1], NamedTextColor.RED));
        Check c = opt.get();
        c.setEnabled(enable);
        return List.of(Component.text()
                .append(Component.text(enable ? "Enabled" : "Disabled", enable ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(Component.text(": " + c.getName(), NamedTextColor.GRAY))
                .build());
    }
}
