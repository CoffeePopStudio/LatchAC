package org.coffeepop.latchac.paper.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.data.PlayerData;
import org.coffeepop.latchac.core.player.LatchPlayer;

import java.util.Set;
import java.util.UUID;

/**
 * Platform adapter — translates raw packets into {@link LatchPlayer} state updates.
 */
public class PacketCheckListener extends PacketListenerAbstract {

    private static final Set<PacketType.Play.Client> MOVEMENT = Set.of(
            PacketType.Play.Client.PLAYER_POSITION,
            PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION,
            PacketType.Play.Client.PLAYER_ROTATION,
            PacketType.Play.Client.PLAYER_FLYING
    );

    private final JavaPlugin plugin;

    public PacketCheckListener(JavaPlugin plugin) {
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.isCancelled()) return;
        Player player = (Player) event.getPlayer();
        if (player == null) return;

        UUID id = player.getUniqueId();
        PacketTypeCommon type = event.getPacketType();
        if (!(type instanceof PacketType.Play.Client ct)) return;

        if (MOVEMENT.contains(ct)) {
            // Extract data ON THIS THREAD (Netty) — the buffer will be released after
            MoveSnapshot snap = capture(ct, event);
            runOnMain(() -> {
                PlayerData data = LatchAC.get().getDataManager().get(id);
                if (data != null) {
                    snap.apply(data.getPlayer());
                    updateLiquidState(player, data);
                }
            });
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        // No-op: container tracking moved to Bukkit InventoryOpenEvent/InventoryCloseEvent
    }

    private void runOnMain(Runnable r) {
        if (Bukkit.isPrimaryThread()) r.run();
        else Bukkit.getScheduler().runTask(plugin, r);
    }

    private void updateLiquidState(Player player, PlayerData data) {
        var block = player.getLocation().getBlock();
        data.getPlayer().setInLiquid(block.isLiquid());
    }

    // ---- Movement snapshot ----

    private static MoveSnapshot capture(PacketType.Play.Client type, PacketReceiveEvent event) {
        return switch (type) {
            case PLAYER_POSITION_AND_ROTATION -> {
                var w = new WrapperPlayClientPlayerPositionAndRotation(event);
                yield new MoveSnapshot(type,
                        w.getLocation().getX(), w.getLocation().getY(), w.getLocation().getZ(),
                        w.getYaw(), w.getPitch(), w.isOnGround());
            }
            case PLAYER_POSITION -> {
                var w = new WrapperPlayClientPlayerPosition(event);
                yield new MoveSnapshot(type,
                        w.getLocation().getX(), w.getLocation().getY(), w.getLocation().getZ(),
                        0, 0, w.isOnGround());
            }
            case PLAYER_ROTATION -> {
                var w = new WrapperPlayClientPlayerRotation(event);
                yield new MoveSnapshot(type, 0, 0, 0, w.getYaw(), w.getPitch(), w.isOnGround());
            }
            default -> {
                var w = new WrapperPlayClientPlayerFlying(event);
                yield new MoveSnapshot(type, 0, 0, 0, 0, 0, w.isOnGround());
            }
        };
    }

    private record MoveSnapshot(PacketType.Play.Client type,
                                 double x, double y, double z,
                                 float yaw, float pitch, boolean onGround) {
        void apply(LatchPlayer lp) {
            switch (type) {
                case PLAYER_POSITION_AND_ROTATION ->
                        lp.updatePosition(x, y, z, yaw, pitch, onGround);
                case PLAYER_POSITION ->
                        lp.updatePosition(x, y, z, lp.getYaw(), lp.getPitch(), onGround);
                case PLAYER_ROTATION ->
                        lp.updateRotation(yaw, pitch, onGround);
                default ->
                        lp.updateFlying(onGround);
            }
        }
    }
}
