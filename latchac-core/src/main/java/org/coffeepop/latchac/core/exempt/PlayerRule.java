package org.coffeepop.latchac.core.exempt;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 玩家豁免：UUID 或名字名单（四维矩阵之"玩家"维）。
 * LuckPerms 权限节点豁免由 paper 层在采集时转换为 tag 注入，不在 core 耦合权限系统。
 */
public record PlayerRule(Set<UUID> uuids, Set<String> names) implements ExemptionRule {

    public PlayerRule {
        uuids = Set.copyOf(uuids);
        names = Set.copyOf(names);
    }

    @Override
    public Optional<String> exempts(ExemptionContext ctx) {
        if (uuids.contains(ctx.playerId()) || names.contains(ctx.playerName())) {
            return Optional.of("玩家豁免名单: " + ctx.playerName());
        }
        return Optional.empty();
    }
}
