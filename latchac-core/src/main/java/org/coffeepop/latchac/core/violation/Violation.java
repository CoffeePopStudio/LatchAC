package org.coffeepop.latchac.core.violation;

import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.player.LatchPlayer;

import java.util.UUID;

/**
 * A single violation record produced when a check flags a player.
 * Carries a snapshot of the player's state at the moment of flagging.
 */
public class Violation {

    private final UUID playerId;
    private final String checkName;
    private final CheckType checkType;
    private final String detail;
    private final long timestamp;
    private final String playerName;

    // ---- Snapshot ----
    private final double x, y, z;
    private final double deltaX, deltaY, deltaZ;
    private final boolean onGround;
    private final boolean inVehicle;
    private final int gameMode;
    private final double cps;
    private final int ping;

    public Violation(UUID playerId, String checkName, CheckType checkType, String detail) {
        this.playerId = playerId;
        this.checkName = checkName;
        this.checkType = checkType;
        this.detail = detail;
        this.timestamp = System.currentTimeMillis();
        this.playerName = null;
        this.x = this.y = this.z = 0;
        this.deltaX = this.deltaY = this.deltaZ = 0;
        this.onGround = false;
        this.inVehicle = false;
        this.gameMode = 0;
        this.cps = 0;
        this.ping = 0;
    }

    public Violation(UUID playerId, String checkName, CheckType checkType, String detail,
                     LatchPlayer player) {
        this.playerId = playerId;
        this.checkName = checkName;
        this.checkType = checkType;
        this.detail = detail;
        this.timestamp = System.currentTimeMillis();
        this.playerName = player.getName();
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
        this.deltaX = player.getDeltaX();
        this.deltaY = player.getDeltaY();
        this.deltaZ = player.getDeltaZ();
        this.onGround = player.isOnGround();
        this.inVehicle = player.isInVehicle();
        this.gameMode = player.getGameMode();
        this.cps = player.getCPS();
        this.ping = player.getPing();
    }

    // ---- Getters ----

    public UUID getPlayerId() { return playerId; }
    public String getCheckName() { return checkName; }
    public CheckType getCheckType() { return checkType; }
    public String getDetail() { return detail; }
    public long getTimestamp() { return timestamp; }
    public String getPlayerName() { return playerName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getDeltaX() { return deltaX; }
    public double getDeltaY() { return deltaY; }
    public double getDeltaZ() { return deltaZ; }
    public boolean isOnGround() { return onGround; }
    public boolean isInVehicle() { return inVehicle; }
    public int getGameMode() { return gameMode; }
    public double getCps() { return cps; }
    public int getPing() { return ping; }
}
