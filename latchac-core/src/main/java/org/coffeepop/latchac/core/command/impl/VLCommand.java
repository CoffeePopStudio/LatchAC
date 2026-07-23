package org.coffeepop.latchac.core.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.command.SubCommand;

import java.util.ArrayList;
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
    public List<Component> execute(String senderId, String[] args) {
        if (args.length == 0) return List.of(
                Component.text("Usage: /latchac vl <player>", NamedTextColor.RED));

        UUID target = findPlayer(args[0]);
        if (target == null) return List.of(
                Component.text("Player not found: " + args[0], NamedTextColor.RED));

        Map<String, Integer> vls = LatchAC.get().getViolationHandler().getAllVLs(target);
        if (vls.isEmpty()) return List.of(
                Component.text(args[0] + " has no violations.", NamedTextColor.GREEN));

        var lines = new ArrayList<Component>();
        lines.add(Component.text()
                .append(Component.text("VL for ", NamedTextColor.GRAY))
                .append(Component.text(args[0], NamedTextColor.WHITE))
                .append(Component.text(":", NamedTextColor.GRAY))
                .build());
        vls.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> lines.add(Component.text()
                        .append(Component.text("  " + e.getKey(), NamedTextColor.GRAY))
                        .append(Component.text(" → ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(String.valueOf(e.getValue()), NamedTextColor.WHITE))
                        .build()));
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
