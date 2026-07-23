package org.coffeepop.latchac.core.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * In-memory configuration manager.
 * <p>
 * Currently uses defaults defined in {@link #loadDefaults()}.
 * Can be extended to load from YAML / JSON files.
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

    // ---- Generic access ----

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        Object v = config.get(key);
        return v != null ? (T) v : defaultValue;
    }

    public void set(String key, Object value) { config.put(key, value); }

    // ---- Convenience ----

    /** Whether debug logging is enabled. */
    public boolean isDebug() { return get("debug", false); }

    /** Plugin message prefix for chat output. */
    public String getPrefix() { return get("prefix", "&8[&bLatchAC&8]"); }

    /** VL threshold for staff alerts. */
    public int getAlertThreshold() { return get("alert-threshold", 20); }

    /** VL threshold for automatic punishment. */
    public int getPunishThreshold() { return get("punish-threshold", 50); }
}
