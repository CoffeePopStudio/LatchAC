package org.coffeepop.latchac.core.config;

import java.util.logging.Logger;

/**
 * Core runtime configuration — debug mode, chat prefix, VL thresholds.
 * Simple flat key-value store. Does NOT handle nested config (baseline, etc.).
 */
public class ConfigManager {

    private final Logger logger;
    private boolean debug;
    private String prefix = "&8[&bLatchAC&8]";
    private int alertThreshold = 20;
    private int punishThreshold = 50;

    public ConfigManager(Logger logger) { this.logger = logger; }

    /** Called once at startup with platform-loaded values. */
    public void init(boolean debug, String prefix, int alertThreshold, int punishThreshold) {
        this.debug = debug;
        if (prefix != null) this.prefix = prefix;
        this.alertThreshold = alertThreshold;
        this.punishThreshold = punishThreshold;
        logger.info("Config loaded: debug=" + debug + " alert=" + alertThreshold + " punish=" + punishThreshold);
    }

    public boolean isDebug() { return debug; }
    public String getPrefix() { return prefix; }
    public int getAlertThreshold() { return alertThreshold; }
    public int getPunishThreshold() { return punishThreshold; }

    public void setDebug(boolean debug) { this.debug = debug; }
}
