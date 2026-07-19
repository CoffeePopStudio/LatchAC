package org.coffeepop.latchac.paper;

import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.paper.check.inventory.CheckInvMove;
import org.coffeepop.latchac.paper.listener.PacketCheckListener;
import org.coffeepop.latchac.paper.listener.PlayerListener;

public final class LatchACPlugin extends JavaPlugin {

    private PacketCheckListener packetListener;

    @Override
    public void onEnable() {
        // 1. 初始化核心
        LatchAC.init(getLogger());

        // 2. 注册 Bukkit 事件监听器
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        // 3. 注入活跃玩家数据
        for (Player player : Bukkit.getOnlinePlayers()) {
            LatchAC.get().getDataManager().getOrCreate(player.getUniqueId());
        }

        // 4. 设置惩罚回调
        LatchAC.get().getViolationHandler().setPunishmentCallback((playerId, checkName, vl, detail) -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.kickPlayer("您已被 LatchAC 反作弊系统踢出。\n原因: " + checkName);
            }
        });

        // 5. 注册 PacketEvents 层面的检查
        CheckInvMove checkInvMove = new CheckInvMove();
        LatchAC.get().registerCheck(checkInvMove);

        packetListener = new PacketCheckListener(this, checkInvMove);
        PacketEvents.getAPI().getEventManager().registerListener(packetListener);

        getLogger().info("LatchAC v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (packetListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
        }
        LatchAC.get().shutdown();
        getLogger().info("LatchAC disabled.");
    }
}
