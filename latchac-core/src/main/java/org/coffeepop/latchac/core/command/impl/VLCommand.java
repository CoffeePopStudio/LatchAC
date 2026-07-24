package org.coffeepop.latchac.core.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.command.SubCommand;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /latchac vl <player> [-history]
 */
public class VLCommand implements SubCommand {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");

    @Override
    public String name() { return "vl"; }

    @Override
    public List<Component> execute(String senderId, String[] args) {
        if (args.length == 0) return List.of(
                Component.text("Usage: /latchac vl <player> [-history]", NamedTextColor.RED));

        boolean history = args.length >= 2 && args[0].equals("-history");
        String playerArg = history ? args[1] : args[0];

        UUID target = findPlayer(playerArg);
        if (target == null) return List.of(
                Component.text("Player not found: " + playerArg, NamedTextColor.RED));

        if (history) {
            return historyView(target, playerArg);
        }
        return vlView(target, playerArg);
    }

    private List<Component> vlView(UUID target, String name) {
        Map<String, Integer> vls = LatchAC.get().getViolationHandler().getAllVLs(target);
        if (vls.isEmpty()) return List.of(
                Component.text(name + " has no violations.", NamedTextColor.GREEN));

        var lines = new ArrayList<Component>();
        lines.add(Component.text()
                .append(Component.text("VL for ", NamedTextColor.GRAY))
                .append(Component.text(name, NamedTextColor.WHITE))
                .append(Component.text(":", NamedTextColor.GRAY))
                .build());
        vls.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> lines.add(Component.text()
                        .append(Component.text("  " + e.getKey(), NamedTextColor.GRAY))
                        .append(Component.text(" → ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(String.valueOf(e.getValue()), NamedTextColor.WHITE))
                        .build()));
        lines.add(Component.text("  Use /latchac vl " + name + " -history for details",
                NamedTextColor.DARK_GRAY));
        return lines;
    }

    private List<Component> historyView(UUID target, String name) {
        var violations = LatchAC.get().getViolationHandler().getHistory(target);
        if (violations.isEmpty()) return List.of(
                Component.text("No flag history for " + name, NamedTextColor.GREEN));

        var lines = new ArrayList<Component>();
        lines.add(Component.text()
                .append(Component.text("Flag history for ", NamedTextColor.GRAY))
                .append(Component.text(name, NamedTextColor.WHITE))
                .append(Component.text(" (last " + violations.size() + "):", NamedTextColor.GRAY))
                .build());

        for (var v : violations) {
            String time = SDF.format(new Date(v.getTimestamp()));
            lines.add(Component.text()
                    .append(Component.text("  " + time + " ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(v.getCheckName(), NamedTextColor.RED))
                    .append(Component.text("  " + v.getDetail(), NamedTextColor.GRAY))
                    .build());
            // Movement snapshot detail for movement checks
            if (v.getCheckName().toLowerCase().startsWith("fly")) {
                lines.add(Component.text(
                        String.format("    pos=%.1f,%.1f,%.1f  delta=%.3f,%.3f,%.3f  ground=%s vehicle=%s",
                                v.getX(), v.getY(), v.getZ(),
                                v.getDeltaX(), v.getDeltaY(), v.getDeltaZ(),
                                v.isOnGround(), v.isInVehicle()),
                        NamedTextColor.DARK_GRAY));
            }
        }
        return lines;
    }

    @Override
    public List<String> tabComplete(String[] args, List<String> online) {
        if (args.length == 1) {
            var opts = new ArrayList<>(online);
            opts.add("-history");
            return opts;
        }
        if (args.length == 2 && args[0].equals("-history")) return online;
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
