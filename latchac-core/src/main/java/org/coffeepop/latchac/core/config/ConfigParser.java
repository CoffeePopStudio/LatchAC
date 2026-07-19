package org.coffeepop.latchac.core.config;

import org.coffeepop.latchac.core.exempt.ExemptionRule;
import org.coffeepop.latchac.core.exempt.PlayerRule;
import org.coffeepop.latchac.core.exempt.RegionRule;
import org.coffeepop.latchac.core.exempt.TagRule;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * YAML 文本 → 不可变 LatchConfig。
 * 任何解析失败抛 IllegalArgumentException，调用方保留旧配置继续运行（热重载安全法则）。
 */
public final class ConfigParser {

    private static final long DEFAULT_HALF_LIFE_MS = 4000;

    @SuppressWarnings("unchecked")
    public LatchConfig parse(String yamlText) {
        Object root = new Yaml().load(yamlText);
        if (!(root instanceof Map)) {
            throw new IllegalArgumentException("配置根节点必须是 YAML 映射");
        }
        Map<String, Object> map = (Map<String, Object>) root;
        try {
            return new LatchConfig(parseHalfLife(map), parseChecks(map), parseExemptions(map));
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("配置结构错误: " + ex.getMessage(), ex);
        }
    }

    private long parseHalfLife(Map<String, Object> map) {
        Map<String, Object> latch = section(map, "latch");
        return ((Number) latch.getOrDefault("half-life-ms", DEFAULT_HALF_LIFE_MS)).longValue();
    }

    @SuppressWarnings("unchecked")
    private Map<String, LatchConfig.CheckConfig> parseChecks(Map<String, Object> map) {
        Map<String, LatchConfig.CheckConfig> checks = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : section(map, "checks").entrySet()) {
            Map<String, Object> c = (Map<String, Object>) e.getValue();
            checks.put(e.getKey(), new LatchConfig.CheckConfig(
                    (Boolean) c.getOrDefault("enabled", Boolean.TRUE),
                    ((Number) c.getOrDefault("trip-threshold", 3.0)).doubleValue(),
                    String.valueOf(c.getOrDefault("action", "LOG"))));
        }
        return checks;
    }

    @SuppressWarnings("unchecked")
    private List<ExemptionRule> parseExemptions(Map<String, Object> map) {
        List<ExemptionRule> rules = new ArrayList<>();
        Map<String, Object> ex = section(map, "exemptions");

        for (Object o : (List<Object>) ex.getOrDefault("regions", List.of())) {
            Map<String, Object> r = (Map<String, Object>) o;
            List<Number> min = (List<Number>) r.get("min");
            List<Number> max = (List<Number>) r.get("max");
            rules.add(new RegionRule(
                    String.valueOf(r.getOrDefault("name", "unnamed")),
                    String.valueOf(r.get("world")),
                    min.get(0).doubleValue(), min.get(1).doubleValue(), min.get(2).doubleValue(),
                    max.get(0).doubleValue(), max.get(1).doubleValue(), max.get(2).doubleValue()));
        }

        Map<String, Object> players = section(ex, "players");
        Set<String> names = new HashSet<>((List<String>) players.getOrDefault("names", List.of()));
        Set<UUID> uuids = new HashSet<>();
        for (String u : (List<String>) players.getOrDefault("uuids", List.<String>of())) {
            uuids.add(UUID.fromString(u));
        }
        if (!names.isEmpty() || !uuids.isEmpty()) {
            rules.add(new PlayerRule(uuids, names));
        }

        for (Object o : (List<Object>) ex.getOrDefault("trusted-tags", List.of())) {
            Map<String, Object> t = (Map<String, Object>) o;
            String tag = String.valueOf(t.get("tag"));
            rules.add(new TagRule(tag, String.valueOf(t.getOrDefault("description", "标签授信: " + tag))));
        }
        return rules;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> section(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof Map ? (Map<String, Object>) v : Map.of();
    }
}
