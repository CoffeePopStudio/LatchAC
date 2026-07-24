package org.coffeepop.latchac.core.config;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Core runtime configuration.
 */
public class ConfigManager {

    public enum ActionType { KICK, TEMPBAN, BAN, IPBAN }

    public record LadderStep(int vl, ActionType action, String duration, List<String> commands, boolean resetVl) {
        public LadderStep { if (commands == null) commands = List.of(); }
    }

    private final Logger logger;
    private boolean debug;
    private String prefix = "&8[&bLatchAC&8]";
    private List<LadderStep> punishLadder = defaultLadder();
    private String kickMessage = "<red>你被 LatchAC 踢出</red><newline><gray>检测: {check}</gray>";
    private String banMessage = "<red>你已被 LatchAC 封禁</red><newline><gray>检测: {check}</gray><newline><gray>如有疑问请联系管理</gray>";

    public ConfigManager(Logger logger) { this.logger = logger; }

    private static List<LadderStep> defaultLadder() {
        var list = new ArrayList<LadderStep>();
        list.add(new LadderStep(20, null, null, List.of(), false));
        list.add(new LadderStep(50, ActionType.KICK, null, List.of(), true));
        list.add(new LadderStep(80, ActionType.TEMPBAN, "24h", List.of(), true));
        list.add(new LadderStep(120, ActionType.BAN, null, List.of(), true));
        return list;
    }

    public void init(boolean debug, String prefix, List<LadderStep> ladder,
                     String kickMessage, String banMessage) {
        this.debug = debug;
        if (prefix != null) this.prefix = prefix;
        if (ladder != null && !ladder.isEmpty()) this.punishLadder = ladder;
        if (kickMessage != null) this.kickMessage = kickMessage;
        if (banMessage != null) this.banMessage = banMessage;
        logger.info("Config loaded: debug=" + debug + " ladderSteps=" + punishLadder.size());
    }

    public boolean isDebug() { return debug; }
    public String getPrefix() { return prefix; }
    public List<LadderStep> getPunishLadder() { return punishLadder; }
    public String getKickMessage() { return kickMessage; }
    public String getBanMessage() { return banMessage; }
    public void setDebug(boolean debug) { this.debug = debug; }
}
