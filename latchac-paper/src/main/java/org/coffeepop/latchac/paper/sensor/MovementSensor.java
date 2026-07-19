package org.coffeepop.latchac.paper.sensor;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;
import org.coffeepop.latchac.core.LatchEngine;
import org.coffeepop.latchac.core.exempt.ExemptionContext;
import org.coffeepop.latchac.core.model.Outcome;
import org.coffeepop.latchac.core.model.Suspicion;
import org.coffeepop.latchac.paper.LatchAntiCheat;
import org.coffeepop.latchac.paper.fake.FakePlayerRegistry;

import java.util.Map;
import java.util.Set;

/**
 * 移动传感器（M2 流水线演示）。
 * 职责：演示 Sensor → Engine → 决策日志 全链路；真实移动检测器在 M3 实现。
 * 演示规则：排除所有已知合法上升上下文后，对超过跳跃初速的上升提交低分 Suspicion。
 * 设计红线：MONITOR 优先级只读事件，绝不修改玩家位置/速度（默认无回弹）。
 */
public final class MovementSensor implements Listener {

    private static final String CHECK_ID = "movement.flight";
    /** 原版跳跃初速约 0.42 格/tick，低于此值的上升直接早退。 */
    private static final double VANILLA_JUMP_VELOCITY = 0.42;

    private final LatchAntiCheat plugin;
    private final LatchEngine engine;
    private final FakePlayerRegistry fakePlayers;

    public MovementSensor(LatchAntiCheat plugin, LatchEngine engine, FakePlayerRegistry fakePlayers) {
        this.plugin = plugin;
        this.engine = engine;
        this.fakePlayers = fakePlayers;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        double dy = event.getTo().getY() - event.getFrom().getY();

        // 廉价前置过滤：明显合法状态直接早退（性能法则：事件驱动 + 最短路径）
        if (dy <= VANILLA_JUMP_VELOCITY) {
            return;
        }
        Player player = event.getPlayer();
        if (player.isGliding() || player.isFlying() || player.getAllowFlight()) {
            return;
        }
        if (player.isInsideVehicle() || player.isClimbing() || player.isSwimming()) {
            return;
        }
        if (player.hasPotionEffect(PotionEffectType.LEVITATION)
                || player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            return;
        }

        Set<String> tags = fakePlayers.identify(player).isPresent()
                ? Set.of("fake-player")
                : Set.of();

        ExemptionContext ctx = new ExemptionContext(
                player.getUniqueId(), player.getName(), player.getWorld().getName(),
                event.getTo().getX(), event.getTo().getY(), event.getTo().getZ(),
                CHECK_ID, tags);

        Suspicion suspicion = new Suspicion(CHECK_ID, player.getUniqueId(), 0.2,
                System.currentTimeMillis(), Map.of("dy", "%.3f".formatted(dy)));

        switch (engine.submit(suspicion, ctx)) {
            case Outcome.Exempted e -> plugin.getLogger().fine(
                    () -> "[豁免] %s -> %s".formatted(player.getName(), e.reason()));
            case Outcome.Scored s -> {
                if (s.verdict().tripped()) {
                    plugin.getLogger().warning(
                            "[LATCH] %s 触发 %s | %s | 证据 dy=%s | 仅记录，不干预".formatted(
                                    player.getName(), CHECK_ID, s.verdict().reason(),
                                    suspicion.evidence().get("dy")));
                }
            }
            case Outcome.Disabled d -> { /* 检查项关闭，无动作 */ }
        }
    }
}
