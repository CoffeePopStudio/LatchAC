package org.coffeepop.latchac.paper.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.coffeepop.latchac.paper.check.inventory.CheckInvMove;

/**
 * PacketEvents 包监听分发器。
 * <p>
 * 统一接收所有数据包事件，按类型分发给对应的 Check 处理。
 * 负责处理 Netty 线程 -> 主线程的调度。
 */
public class PacketCheckListener implements PacketListener {

    private final JavaPlugin plugin;
    private final CheckInvMove checkInvMove;

    public PacketCheckListener(JavaPlugin plugin, CheckInvMove checkInvMove) {
        this.plugin = plugin;
        this.checkInvMove = checkInvMove;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.isCancelled()) return;

        Player player = (Player) event.getPlayer();
        if (player == null) return;

        // 提前提权到主线程，这样才能安全访问 Bukkit API（getOpenInventory 等）
        if (!Bukkit.isPrimaryThread()) {
            PacketType<?> type = event.getPacketType();
            Bukkit.getScheduler().runTask(plugin, () -> dispatch(player, type));
            return;
        }

        dispatch(player, event.getPacketType());
    }

    private void dispatch(Player player, PacketType<?> type) {
        // 必须是真正的玩家对象（重新获取，防止离线引用）
        Player online = Bukkit.getPlayer(player.getUniqueId());
        if (online == null) return;

        // 仅处理客户端 -> 服务端的数据包
        if (!(type instanceof PacketType.Play.Client clientType)) return;

        // ---- 分发给各个 Packet-level Check ----
        checkInvMove.process(online, clientType);
    }
}
