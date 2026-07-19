package org.coffeepop.latchac.paper.fake;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/** 假人识别器注册表。CopyOnWriteArrayList：注册极少、读取高频，Folia 多线程读安全。 */
public final class FakePlayerRegistry {

    private final List<FakePlayerProvider> providers = new CopyOnWriteArrayList<>();

    public void register(FakePlayerProvider provider) {
        providers.add(provider);
    }

    /** @return 命中的识别器名；非假人返回 empty */
    public Optional<String> identify(Player player) {
        for (FakePlayerProvider p : providers) {
            if (p.isFakePlayer(player)) {
                return Optional.of(p.name());
            }
        }
        return Optional.empty();
    }
}
