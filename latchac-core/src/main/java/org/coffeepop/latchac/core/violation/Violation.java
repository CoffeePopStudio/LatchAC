package org.coffeepop.latchac.core.violation;

import org.coffeepop.latchac.core.check.CheckType;

import java.util.UUID;

public class Violation {

    private final UUID playerId;
    private final String checkName;
    private final CheckType checkType;
    private final String detail;
    private final long timestamp;

    public Violation(UUID playerId, String checkName, CheckType checkType, String detail) {
        this.playerId = playerId;
        this.checkName = checkName;
        this.checkType = checkType;
        this.detail = detail;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getPlayerId() { return playerId; }
    public String getCheckName() { return checkName; }
    public CheckType getCheckType() { return checkType; }
    public String getDetail() { return detail; }
    public long getTimestamp() { return timestamp; }
}
