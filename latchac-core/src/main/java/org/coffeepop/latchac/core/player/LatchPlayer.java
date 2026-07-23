package org.coffeepop.latchac.core.player;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Platform-agnostic player abstraction with position / movement / state data.
 * <p>
 * The platform module syncs raw packets into {@code update*} methods.
 * Each movement update automatically triggers the core check pipeline via the
 * static {@link #onDataUpdate} callback — wired once in {@code LatchAC.init()}.
 * Paper/Fabric never touches check dispatch.
 */
public class LatchPlayer {

    /** Set by LatchAC.init — called after every movement update. */
    public static Consumer<LatchPlayer> onDataUpdate = p -> {};

    /** Set by LatchAC.init — teleports player back to last position. */
    public static Consumer<LatchPlayer> onSetback = p -> {};

    // ---- Identity ----

    private final UUID uniqueId;
    private final String name;
    private final Object platformPlayer;

    // ---- Position ----

    private double x, y, z;
    private float yaw, pitch;
    private double lastX, lastY, lastZ;
    private float lastYaw, lastPitch;

    // ---- State ----

    private boolean onGround;
    private boolean sprinting;
    private boolean sneaking;
    private boolean hasPosition;
    private boolean inContainer;
    private String containerType;
    private boolean inVehicle;
    private long lastMoveTime;

    public LatchPlayer(UUID uniqueId, String name, Object platformPlayer) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.platformPlayer = platformPlayer;
    }

    // ========================
    //  Identity
    // ========================

    public UUID getUniqueId() { return uniqueId; }
    public String getName() { return name; }

    @SuppressWarnings("unchecked")
    public <T> T getPlatformPlayer() { return (T) platformPlayer; }

    // ========================
    //  State updates (called by platform layer)
    // ========================

    public void updatePosition(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        this.lastX = this.x; this.lastY = this.y; this.lastZ = this.z;
        this.lastYaw = this.yaw; this.lastPitch = this.pitch;
        this.x = x; this.y = y; this.z = z;
        this.yaw = yaw; this.pitch = pitch;
        this.onGround = onGround;
        this.lastMoveTime = System.currentTimeMillis();
        this.hasPosition = true;
        onDataUpdate.accept(this);
    }

    public void updateRotation(float yaw, float pitch, boolean onGround) {
        this.lastYaw = this.yaw; this.lastPitch = this.pitch;
        this.yaw = yaw; this.pitch = pitch;
        this.onGround = onGround;
        this.lastMoveTime = System.currentTimeMillis();
        onDataUpdate.accept(this);
    }

    public void updateFlying(boolean onGround) {
        this.onGround = onGround;
        this.lastMoveTime = System.currentTimeMillis();
        onDataUpdate.accept(this);
    }

    public void updateFlags(boolean sprinting, boolean sneaking) {
        this.sprinting = sprinting;
        this.sneaking = sneaking;
    }

    public void setInContainer(boolean inContainer, String type) {
        this.inContainer = inContainer;
        this.containerType = inContainer ? type : null;
    }

    public void setback() { onSetback.accept(this); }

    public void setInVehicle(boolean inVehicle) { this.inVehicle = inVehicle; }

    // ========================
    //  Position
    // ========================

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw()   { return yaw; }
    public float getPitch() { return pitch; }
    public double getLastX() { return lastX; }
    public double getLastY() { return lastY; }
    public double getLastZ() { return lastZ; }
    public float getLastYaw()   { return lastYaw; }
    public float getLastPitch() { return lastPitch; }

    // ========================
    //  Deltas
    // ========================

    public double getDeltaX() { return x - lastX; }
    public double getDeltaY() { return y - lastY; }
    public double getDeltaZ() { return z - lastZ; }
    public double getDeltaXZ() { return Math.hypot(getDeltaX(), getDeltaZ()); }
    public float getDeltaYaw()   { return yaw - lastYaw; }
    public float getDeltaPitch() { return pitch - lastPitch; }

    // ========================
    //  State
    // ========================

    public boolean isOnGround()  { return onGround; }
    public boolean isSprinting() { return sprinting; }
    public boolean isSneaking()  { return sneaking; }
    public boolean hasPosition() { return hasPosition; }
    public boolean isInContainer() { return inContainer; }
    public String getContainerType() { return containerType; }
    public boolean isInVehicle() { return inVehicle; }
    public long getLastMoveTime() { return lastMoveTime; }

    public boolean movedRecently(long ms) {
        return System.currentTimeMillis() - lastMoveTime < ms;
    }

    // ========================
    //  Utilities
    // ========================

    public double distanceXZ(LatchPlayer other) {
        return Math.hypot(this.x - other.x, this.z - other.z);
    }

    public double distance(LatchPlayer other) {
        return Math.sqrt(
                Math.pow(this.x - other.x, 2) +
                Math.pow(this.y - other.y, 2) +
                Math.pow(this.z - other.z, 2));
    }

    @Override
    public String toString() {
        return name + " (" + uniqueId + ")";
    }
}
