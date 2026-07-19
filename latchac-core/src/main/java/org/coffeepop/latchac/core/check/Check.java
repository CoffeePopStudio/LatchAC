package org.coffeepop.latchac.core.check;

import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.data.PlayerData;
import org.coffeepop.latchac.core.violation.Violation;

import java.util.logging.Level;

/**
 * 所有检查的抽象基类。
 */
public abstract class Check {

    private final String name;
    private final CheckType type;
    private final String description;
    private boolean enabled = true;

    protected Check(String name, CheckType type, String description) {
        this.name = name;
        this.type = type;
        this.description = description;
    }

    public String getName() { return name; }
    public CheckType getType() { return type; }
    public String getDescription() { return description; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** 标记违规，累加 VL */
    protected void flag(PlayerData data, String detail) {
        Violation v = new Violation(data.getPlayerId(), this.name, this.type, detail);
        LatchAC.get().getViolationHandler().handle(v);
        if (LatchAC.get().getConfigManager().isDebug()) {
            LatchAC.get().getLogger().log(Level.INFO,
                    "[{0}] {1} flagged {2}: {3}",
                    new Object[]{type.name(), name, data.getPlayerId(), detail});
        }
    }

    /** debug 日志，不增加 VL */
    protected void debug(PlayerData data, String detail) {
        if (LatchAC.get().getConfigManager().isDebug()) {
            LatchAC.get().getLogger().log(Level.INFO,
                    "[DEBUG] [{0}] {1} on {2}: {3}",
                    new Object[]{type.name(), name, data.getPlayerId(), detail});
        }
    }
}
