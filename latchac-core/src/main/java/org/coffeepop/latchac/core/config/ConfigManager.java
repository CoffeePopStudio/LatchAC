package org.coffeepop.latchac.core.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * In-memory configuration manager.
 * <p>
 * Call {@link #loadDefaults()} first, then {@link #load(Map)} with
 * platform-provided values (e.g. from YAML) to override.
 */
public class ConfigManager {

    private final Logger logger;
    private final Map<String, Object> config = new ConcurrentHashMap<>();

    public ConfigManager(Logger logger) { this.logger = logger; }

    /** Loads default configuration values. */
    public void loadDefaults() {
        config.put("debug", false);
        config.put("prefix", "&8[&bLatchAC&8]");
        config.put("alert-threshold", 20);
        config.put("punish-threshold", 50);
        logger.info("ConfigManager loaded defaults.");
    }

    /**
     * Loads configuration from a map (e.g. from platform YAML parsing).
     * Call after {@link #loadDefaults()}; values not in the map keep defaults.
     */
    public void load(Map<String, Object> values) {
        config.putAll(values);
        logger.info("ConfigManager loaded " + values.size() + " values from platform.");
    }

    /** Returns an immutable snapshot of all configuration entries. */
    public Map<String, Object> getAll() {
        return Map.copyOf(config);
    }

    // ---- Generic access ----

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        Object v = config.get(key);
        return v != null ? (T) v : defaultValue;
    }

    public void set(String key, Object value) { config.put(key, value); }

    // ---- Convenience ----

    public boolean isDebug() { return get("debug", false); }
    public String getPrefix() { return get("prefix", "&8[&bLatchAC&8]"); }
    public int getAlertThreshold() { return get("alert-threshold", 20); }
    public int getPunishThreshold() { return get("punish-threshold", 50); }
}
