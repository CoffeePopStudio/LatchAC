package org.coffeepop.latchac.paper.fake;

import org.bukkit.entity.Player;

/**
 * 假人识别 SPI。
 * 第三方插件可通过 LatchAntiCheat#fakePlayers().register(...) 注册自定义识别器，
 * 为私有假人方案提供授信通道（高可扩展性需求）。
 */
public interface FakePlayerProvider {

    /** 识别器名称，进入审计日志。 */
    String name();

    boolean isFakePlayer(Player player);
}
