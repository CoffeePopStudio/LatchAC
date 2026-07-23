package org.coffeepop.latchac.core.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.baseline.BaselineExporter;
import org.coffeepop.latchac.core.command.SubCommand;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * /latchac baseline export | import <path|url> | status
 */
public class BaselineCommand implements SubCommand {

    @Override
    public String name() { return "baseline"; }

    @Override
    public String permission() { return "latchac.baseline"; }

    @Override
    public List<Component> execute(String senderId, String[] args) {
        if (args.length == 0) return usage();

        return switch (args[0].toLowerCase()) {
            case "export" -> doExport();
            case "import" -> doImport(sliceArg(args));
            case "status"  -> doStatus();
            default -> usage();
        };
    }

    @Override
    public List<String> tabComplete(String[] args, List<String> online) {
        if (args.length == 1) return List.of("export", "import", "status");
        return List.of();
    }

    private List<Component> usage() {
        return List.of(
                Component.text("/latchac baseline export", NamedTextColor.GRAY),
                Component.text("/latchac baseline import <path|url>", NamedTextColor.GRAY),
                Component.text("/latchac baseline status", NamedTextColor.GRAY));
    }

    private List<Component> doExport() {
        var storage = LatchAC.get().getBaselineProfiler().getStorage();
        var exporter = new BaselineExporter(storage);
        try {
            File dataFolder = LatchAC.get().getBaselineProfiler().getStorage().getDataFolder();
            File file = exporter.exportToFile(dataFolder);
            return List.of(Component.text()
                    .append(Component.text("Exported to ", NamedTextColor.GREEN))
                    .append(Component.text(file.getAbsolutePath(), NamedTextColor.WHITE))
                    .build());
        } catch (IOException e) {
            return List.of(Component.text("Export failed: " + e.getMessage(), NamedTextColor.RED));
        }
    }

    private List<Component> doImport(String path) {
        if (path == null) {
            return List.of(Component.text("Usage: /latchac baseline import <path|url>", NamedTextColor.RED));
        }
        var storage = LatchAC.get().getBaselineProfiler().getStorage();
        var exporter = new BaselineExporter(storage);
        try {
            if (path.startsWith("http://") || path.startsWith("https://")) {
                exporter.importFromUrl(path);
            } else {
                String json = java.nio.file.Files.readString(java.nio.file.Path.of(path));
                exporter.importFromJson(json);
            }
            var baselines = storage.loadPopulationBaselines();
            return List.of(Component.text()
                    .append(Component.text("Imported ", NamedTextColor.GREEN))
                    .append(Component.text(String.valueOf(baselines.size()), NamedTextColor.WHITE))
                    .append(Component.text(" metrics, merged with local", NamedTextColor.GRAY))
                    .build());
        } catch (Exception e) {
            return List.of(Component.text("Import failed: " + e.getMessage(), NamedTextColor.RED));
        }
    }

    private List<Component> doStatus() {
        var storage = LatchAC.get().getBaselineProfiler().getStorage();
        var baselines = storage.loadPopulationBaselines();
        int metrics = baselines.size();
        // Individual profiles: count saved player files in the storage directory
        File dbFolder = storage.getDataFolder();
        File[] dbFiles = dbFolder.listFiles((dir, name) -> name.endsWith(".db"));
        return List.of(
                Component.text()
                        .append(Component.text(String.valueOf(metrics), NamedTextColor.WHITE))
                        .append(Component.text(" metrics in population baseline", NamedTextColor.GRAY))
                        .build(),
                Component.text()
                        .append(Component.text(dbFiles != null ? String.valueOf(dbFiles.length) : "0", NamedTextColor.WHITE))
                        .append(Component.text(" DB files in ", NamedTextColor.GRAY))
                        .append(Component.text(dbFolder.getAbsolutePath(), NamedTextColor.WHITE))
                        .build());
    }

    private static String sliceArg(String[] args) {
        return args.length < 2 ? null : args[1];
    }
}
