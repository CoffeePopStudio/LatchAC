package org.coffeepop.latchac.paper.check.inventory;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.check.Check;
import org.coffeepop.latchac.core.check.CheckType;
import org.coffeepop.latchac.core.data.PlayerData;

/**
 * InvMove：检测玩家打开容器界面时发送移动包。
 * <p>
 * 正常原版行为：打开箱子/工作台/熔炉等外部容器后，客户端不应再发送移动包。
 * 如果收到移动包，说明玩家在使用 InvMove 作弊。
 */
public class CheckInvMove extends Check {

    public CheckInvMove() {
        super("InvMove", CheckType.INVENTORY, "检测打开容器时移动");
    }

    /**
     * 由 PacketCheckListener 在主线程调用。
     *
     * @param player 玩家
     * @param type   收到的数据包类型
     */
    public void process(Player player, PacketType.Play.Client type) {
        if (!isEnabled()) return;

        // 只有移动包才需要检查
        if (!isMovementPacket(type)) return;

        InventoryView view = player.getOpenInventory();
        if (view == null || view.getTopInventory() == null) return;

        InventoryType topType = view.getTopInventory().getType();
        // 玩家自己的背包/创造模式界面，可以正常移动
        if (topType == InventoryType.CRAFTING || topType == InventoryType.CREATIVE) {
            return;
        }

        PlayerData data = LatchAC.get().getDataManager().get(player.getUniqueId());
        if (data != null) {
            flag(data, "container=" + topType.name().toLowerCase());
        }
    }

    private boolean isMovementPacket(PacketType.Play.Client type) {
        return type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
                || type == PacketType.Play.Client.PLAYER_ROTATION
                || type == PacketType.Play.Client.PLAYER_FLYING;
    }
}
