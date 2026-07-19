package org.coffeepop.latchac.core;

import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckRegistry;
import org.coffeepop.latchac.core.config.ConfigManager;
import org.coffeepop.latchac.core.data.DataManager;
import org.coffeepop.latchac.core.violation.ViolationHandler;

import java.util.logging.Logger;

public final class LatchAC {

    private static LatchAC instance;

    private final Logger logger;
    private final ConfigManager configManager;
    private final CheckRegistry checkRegistry;
    private final DataManager dataManager;
    private final ViolationHandler violationHandler;

    private LatchAC(Logger logger) {
        this.logger = logger;
        this.configManager = new ConfigManager(logger);
        this.checkRegistry = new CheckRegistry();
        this.dataManager = new DataManager();
        this.violationHandler = new ViolationHandler(logger);
    }

    public static void init(Logger logger) {
        if (instance != null) throw new IllegalStateException("LatchAC already initialized");
        instance = new LatchAC(logger);
        instance.configManager.loadDefaults();
    }

    public static LatchAC get() {
        if (instance == null) throw new IllegalStateException("LatchAC not initialized");
        return instance;
    }

    public Logger getLogger() { return logger; }
    public ConfigManager getConfigManager() { return configManager; }
    public CheckRegistry getCheckRegistry() { return checkRegistry; }
    public DataManager getDataManager() { return dataManager; }
    public ViolationHandler getViolationHandler() { return violationHandler; }

    public void registerCheck(Check check) { checkRegistry.register(check); }

    public void shutdown() {
        logger.info("LatchAC shutting down...");
        dataManager.clearAll();
        checkRegistry.unregisterAll();
        instance = null;
    }
}
