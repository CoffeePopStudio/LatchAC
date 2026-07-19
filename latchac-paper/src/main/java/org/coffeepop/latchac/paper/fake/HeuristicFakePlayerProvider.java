package org.coffeepop.latchac.paper.fake;

import org.bukkit.entity.Player;

/**
 * 通用假人特征识别：不绑定任何具体假人插件版本（模糊匹配原则——行为特征优先于插件签名）。
 * 特征 1：假人没有真实网络连接（fakeplayer 类插件、Carpet 移植方案普遍如此）。
 * 特征 2：主流 NPC/假人插件的通用 metadata 标记。
 */
public final class HeuristicFakePlayerProvider implements FakePlayerProvider {

    @Override
    public String name() {
        return "heuristic";
    }

    @Override
    public boolean isFakePlayer(Player player) {
        if (player.getAddress() == null) {
            return true;
        }
        return player.hasMetadata("NPC") || player.hasMetadata("fakeplayer");
    }
}
