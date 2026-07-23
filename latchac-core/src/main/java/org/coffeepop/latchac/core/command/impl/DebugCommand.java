package org.coffeepop.latchac.core.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.command.SubCommand;

import java.util.List;

/**
 * /latchac debug — toggles debug mode.
 */
public class DebugCommand implements SubCommand {

    @Override
    public String name() { return "debug"; }

    @Override
    public String permission() { return "latchac.debug"; }

    @Override
    public List<Component> execute(String senderId, String[] args) {
        var cfg = LatchAC.get().getConfigManager();
        boolean was = cfg.isDebug();
        cfg.setDebug(!was);
        return List.of(Component.text()
                .append(Component.text("Debug mode: ", NamedTextColor.GRAY))
                .append(Component.text(was ? "OFF" : "ON", was ? NamedTextColor.RED : NamedTextColor.GREEN))
                .build());
    }

    @Override
    public List<String> tabComplete(String[] args, List<String> online) {
        return List.of();
    }
}
