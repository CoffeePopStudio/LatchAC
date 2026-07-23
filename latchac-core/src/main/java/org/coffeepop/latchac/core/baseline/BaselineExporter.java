package org.coffeepop.latchac.core.baseline;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Community baseline network: export/import population baselines as JSON.
 * Anonymous — only contains mean/std/count, never player UUIDs.
 * Individual baselines are NEVER exported.
 */
public class BaselineExporter {
    private final BaselineStorage storage;

    public BaselineExporter(BaselineStorage storage) {
        this.storage = storage;
    }

    public String exportJson() {
        var baselines = storage.loadPopulationBaselines();
        var sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"version\": 1,\n");
        sb.append("  \"generated_at\": ").append(System.currentTimeMillis()).append(",\n");
        sb.append("  \"metrics\": {\n");
        var it = baselines.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            sb.append("    \"").append(entry.getKey()).append("\": {\n");
            sb.append("      \"mean\": ").append(entry.getValue()[0]).append(",\n");
            sb.append("      \"std\": ").append(entry.getValue()[1]).append("\n");
            sb.append("    }");
            if (it.hasNext()) sb.append(",");
            sb.append("\n");
        }
        sb.append("  }\n");
        sb.append("}\n");
        return sb.toString();
    }

    public File exportToFile(File dataFolder) throws IOException {
        var file = new File(dataFolder, "baseline-export.json");
        Files.writeString(file.toPath(), exportJson(), StandardCharsets.UTF_8);
        return file;
    }

    public void importFromJson(String json) {
        var lines = json.replace("{", "").replace("}", "").replace("\"", "").split("\n");
        String currentMetric = null;
        Double currentMean = null, currentStd = null;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("version") || line.startsWith("generated_at")
                    || line.startsWith("metrics")) continue;

            if (line.endsWith(":")) {
                currentMetric = line.substring(0, line.length() - 1).trim();
            } else if (line.contains("mean") && currentMetric != null) {
                currentMean = Double.parseDouble(line.split(":")[1].trim().replace(",", ""));
            } else if (line.contains("std") && currentMetric != null && currentMean != null) {
                currentStd = Double.parseDouble(line.split(":")[1].trim().replace(",", ""));
                var local = storage.loadPopulationBaselines();
                double[] localVals = local.get(currentMetric);
                if (localVals != null) {
                    double merged = (localVals[0] * 5 + currentMean * 3) / 8;
                    double mergedStd = (localVals[1] * 5 + currentStd * 3) / 8;
                    storage.savePopulationBaseline(currentMetric, merged, mergedStd, 10);
                } else {
                    storage.savePopulationBaseline(currentMetric, currentMean, currentStd, 3);
                }
                currentMetric = null; currentMean = null; currentStd = null;
            }
        }
    }

    public void importFromUrl(String url) throws IOException, InterruptedException {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            importFromJson(response.body());
        }
    }
}
