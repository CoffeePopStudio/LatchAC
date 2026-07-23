package org.coffeepop.latchac.core.check;

import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.data.PlayerData;
import org.coffeepop.latchac.core.player.LatchPlayer;
import org.coffeepop.latchac.core.violation.Violation;

import java.util.logging.Level;

/**
 * Abstract base class for all anti-cheat checks.
 * <p>
 * Metadata is read from the {@link CheckInfo} annotation.
 * Override {@link #onCheck} to react to state updates — it is called automatically
 * after every movement packet sync (handled internally by {@link LatchPlayer}).
 *
 * <pre>{@code
 *   @CheckInfo(name = "Speed", type = MOVEMENT)
 *   public class SpeedCheck extends Check {
 *       @Override public void onCheck(LatchPlayer p) { ... }
 *   }
 * }</pre>
 */
public abstract class Check {

    private final String name;
    private final CheckType type;
    private final String description;
    private final int maxVL;
    private boolean enabled = true;

    protected Check() {
        CheckInfo info = getClass().getAnnotation(CheckInfo.class);
        if (info == null) throw new IllegalStateException(
                getClass().getName() + " is missing @CheckInfo annotation");
        this.name = info.name();
        this.type = info.type();
        this.description = info.description();
        this.maxVL = info.maxVL();
    }

    protected Check(String name, CheckType type, String description, int maxVL) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.maxVL = maxVL;
    }

    public String getName() { return name; }
    public CheckType getType() { return type; }
    public String getDescription() { return description; }
    public int getMaxVL() { return maxVL; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /**
     * Called automatically after every movement packet sync.
     * Default no-op — override to add detection logic.
     */
    public void onCheck(LatchPlayer player) {}

    protected void flag(PlayerData data, String detail) {
        Violation v = new Violation(data.getPlayerId(), this.name, this.type, detail);
        LatchAC.get().getViolationHandler().handle(v);
        if (LatchAC.get().getConfigManager().isDebug()) {
            LatchAC.get().getLogger().log(Level.INFO,
                    "[{0}] {1} flagged {2}: {3}",
                    new Object[]{type.name(), name, data.getPlayerId(), detail});
        }
    }

    protected void flag(LatchPlayer player, String detail) {
        Violation v = new Violation(player.getUniqueId(), this.name, this.type, detail);
        LatchAC.get().getViolationHandler().handle(v);
        if (LatchAC.get().getConfigManager().isDebug()) {
            LatchAC.get().getLogger().log(Level.INFO,
                    "[{0}] {1} flagged {2}: {3}",
                    new Object[]{type.name(), name, player.getUniqueId(), detail});
        }
    }

    /** Teleports player back to last valid position. Does not increment VL. */
    protected void setback(LatchPlayer player) {
        player.setback();
    }

    /** Flag + setback in one call. Increments VL AND teleports back. */
    protected void flagAndSetback(LatchPlayer player, String detail) {
        flag(player, detail);
        player.setback();
    }

    protected void debug(PlayerData data, String detail) {
        if (LatchAC.get().getConfigManager().isDebug()) {
            LatchAC.get().getLogger().log(Level.INFO,
                    "[DEBUG] [{0}] {1} on {2}: {3}",
                    new Object[]{type.name(), name, data.getPlayerId(), detail});
        }
    }
}
