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

    /** Called after every movement update. Use {@link #setOnDataUpdate} to wire. */
    private static Consumer<LatchPlayer> onDataUpdate = p -> {};

    /** Teleports player back to last position. Use {@link #setOnSetback} to wire. */
    private static Consumer<LatchPlayer> onSetback = p -> {};

    /** Package-private setter — only LatchAC wiring should call this. */
    public static void setOnDataUpdate(Consumer<LatchPlayer> callback) {
        onDataUpdate = callback;
    }

    /** Package-private setter — only LatchAC wiring should call this. */
    public static void setOnSetback(Consumer<LatchPlayer> callback) {
        onSetback = callback;
    }

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
    private boolean inLiquid;
    private double footSlipperiness = 0.6;
    private long lastMoveTime;

    // ---- Combat ----
    private long lastAttackTime;
    private long lastSwingTime;
    private int lastTargetId = -1;
    private double targetX, targetY, targetZ;
    private double pendingVelocityX, pendingVelocityY, pendingVelocityZ;
    private long pendingVelocityTime;
    // ---- Timing ----
    private java.util.Deque<Long> packetTimestamps = new java.util.ArrayDeque<>();
    // ---- CPS / Combat metrics ----
    private java.util.Deque<Long> clickTimestamps = new java.util.ArrayDeque<>();
    private float lastDeltaYaw;
    // ---- Ground ratio ----
    private int tickCount, groundTickCount;
    // ---- Scaffold ----
    private java.util.Deque<Long> scaffoldTimestamps = new java.util.ArrayDeque<>();
    private java.util.Deque<Float> scaffoldAngles = new java.util.ArrayDeque<>();
    private java.util.Deque<Double> scaffoldDXZ = new java.util.ArrayDeque<>();

    // ---- Exemption ----

    /** GameMode ordinal: 0=Survival, 1=Creative, 2=Adventure, 3=Spectator. */
    private int gameMode;
    /** Plugin-granted flight (e.g. Essentials /fly). Does NOT include elytra. */
    private boolean allowFlight;
    /** Whether player is actively flying (checked periodically via Bukkit task). */
    private boolean flightToggled;
    /** Set true during setback teleport to skip next check cycle (re-entrancy guard). */
    private boolean setbackInProgress;

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
        if (!setbackInProgress) onDataUpdate.accept(this);
    }

    public void updateRotation(float yaw, float pitch, boolean onGround) {
        this.lastYaw = this.yaw; this.lastPitch = this.pitch;
        this.yaw = yaw; this.pitch = pitch;
        this.onGround = onGround;
        this.lastMoveTime = System.currentTimeMillis();
        if (!setbackInProgress) onDataUpdate.accept(this);
    }

    public void updateFlying(boolean onGround) {
        this.onGround = onGround;
        this.lastMoveTime = System.currentTimeMillis();
        if (!setbackInProgress) onDataUpdate.accept(this);
    }

    public void updateFlags(boolean sprinting, boolean sneaking) {
        this.sprinting = sprinting;
        this.sneaking = sneaking;
    }

    public void setInContainer(boolean inContainer, String type) {
        this.inContainer = inContainer;
        this.containerType = inContainer ? type : null;
    }

    public void setback() {
        setbackInProgress = true;
        onSetback.accept(this);
        // Reset guard after next packet arrives (handled in update* methods)
    }

    /** Called by platform when setback teleport confirmation packet arrives. */
    public void clearSetbackGuard() { setbackInProgress = false; }

    public void setInVehicle(boolean inVehicle) { this.inVehicle = inVehicle; }
    public void setInLiquid(boolean inLiquid) { this.inLiquid = inLiquid; }
    public void setFootSlipperiness(double s) { this.footSlipperiness = s; }

    // Combat setters/getters
    public long getLastAttackTime() { return lastAttackTime; }
    public void setLastAttackTime(long t) { this.lastAttackTime = t; }
    public long getLastSwingTime() { return lastSwingTime; }
    public void setLastSwingTime(long t) { this.lastSwingTime = t; }
    public int getLastTargetId() { return lastTargetId; }
    public void setLastTargetId(int id) { this.lastTargetId = id; }

    public void updateTargetPosition(double tx, double ty, double tz) {
        this.targetX = tx; this.targetY = ty; this.targetZ = tz;
    }
    public double getTargetX() { return targetX; }
    public double getTargetY() { return targetY; }
    public double getTargetZ() { return targetZ; }

    // Velocity
    public void setPendingVelocity(double vx, double vy, double vz) {
        this.pendingVelocityX = vx; this.pendingVelocityY = vy; this.pendingVelocityZ = vz;
        this.pendingVelocityTime = System.nanoTime();
    }
    public double getPendingVelocityX() { return pendingVelocityX; }
    public double getPendingVelocityY() { return pendingVelocityY; }
    public double getPendingVelocityZ() { return pendingVelocityZ; }
    public long getPendingVelocityTime() { return pendingVelocityTime; }

    // Timing
    public void addPacketTimestamp(long nanoTime) {
        packetTimestamps.addLast(nanoTime);
        if (packetTimestamps.size() > 120) packetTimestamps.removeFirst();
    }
    public java.util.Deque<Long> getPacketTimestamps() { return packetTimestamps; }

    /**
     * Counts movement packets received in the last second.
     * Normal: ~20 pkt/s. Stationary vanilla: ~1 pkt/s. Timer speed: >21 pkt/s.
     */
    public double getPacketRate() {
        if (packetTimestamps.size() < 2) return 20.0;
        long cutoff = packetTimestamps.peekLast() - 1_000_000_000L;
        int count = 0;
        var it = packetTimestamps.descendingIterator();
        while (it.hasNext() && it.next() > cutoff) count++;
        return (double) count;
    }

    public void addClickTimestamp(long nanoTime) {
        clickTimestamps.addLast(nanoTime);
        if (clickTimestamps.size() > 20) clickTimestamps.removeFirst();
    }

    public double getCPS() {
        if (clickTimestamps.size() < 2) return 0;
        long cutoff = clickTimestamps.peekLast() - 1_000_000_000L;
        int count = 0;
        var it = clickTimestamps.descendingIterator();
        while (it.hasNext() && it.next() > cutoff) count++;
        return (double) count;
    }

    public float getTurnJerk() {
        float jerk = Math.abs(getDeltaYaw() - lastDeltaYaw);
        lastDeltaYaw = getDeltaYaw();
        return jerk;
    }

    public void incrementTickCount(boolean onGround) {
        tickCount++;
        if (onGround) groundTickCount++;
        if (tickCount > 400) { // reset every ~20s to keep ratio fresh
            tickCount /= 2;
            groundTickCount /= 2;
        }
    }

    public double getGroundRatio() {
        return tickCount > 0 ? (double) groundTickCount / tickCount : 1.0;
    }

    public void setGameMode(int gameMode) { this.gameMode = gameMode; }
    public int getGameMode() { return gameMode; }

    public void setAllowFlight(boolean allowFlight) { this.allowFlight = allowFlight; }
    public boolean isAllowFlight() { return allowFlight; }

    public void setFlightToggled(boolean flightToggled) { this.flightToggled = flightToggled; }
    public boolean isFlightToggled() { return flightToggled; }

    /**
     * Whether this player should be fully exempt from all movement checks.
     * Only Creative and Spectator. Flight is handled by {@link #isFlightExempted()} instead —
     * allows Speed/Timer/AutoClick checks to still run while flying.
     */
    public boolean shouldExemptMovement() {
        return gameMode == 1 || gameMode == 3;
    }

    /**
     * Whether currently flying (plugin /fly or creative flight enabled).
     * Use this to skip FlyVertical/FlyAirStuck/FlyGroundSpoof only —
     * NOT Speed/Timer/AutoClick/BadPackets.
     */
    public boolean isFlightExempted() {
        return gameMode == 1 || gameMode == 3 || flightToggled;
    }

    // ---- Scaffold ----

    public void addScaffoldSample(long nanoTime, float pitch, double dxz) {
        scaffoldTimestamps.addLast(nanoTime);
        scaffoldAngles.addLast(pitch);
        scaffoldDXZ.addLast(dxz);
        if (scaffoldTimestamps.size() > 20) {
            scaffoldTimestamps.removeFirst();
            scaffoldAngles.removeFirst();
            scaffoldDXZ.removeFirst();
        }
    }

    public double getScaffoldInterval() {
        if (scaffoldTimestamps.size() < 2) return 0;
        var it = scaffoldTimestamps.descendingIterator();
        long latest = it.next();
        long prev = it.next();
        return (latest - prev) / 1_000_000.0;
    }

    public float getScaffoldPitch() {
        return scaffoldAngles.isEmpty() ? 0 : scaffoldAngles.peekLast();
    }

    public double getScaffoldDXZ() {
        return scaffoldDXZ.isEmpty() ? 0 : scaffoldDXZ.peekLast();
    }

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
    public boolean isInLiquid() { return inLiquid; }
    public double getFootSlipperiness() { return footSlipperiness; }
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
