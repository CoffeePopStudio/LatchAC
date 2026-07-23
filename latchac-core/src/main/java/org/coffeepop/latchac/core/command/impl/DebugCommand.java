package org.coffeepop.latchac.core.command.impl;

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
    public List<String> execute(String senderId, String[] args) {
        var cfg = LatchAC.get().getConfigManager();
        boolean was = cfg.isDebug();
        cfg.set("debug", !was);
        return List.of("§7Debug mode: " + (!was ? "§aON" : "§cOFF"));
    }

    @Override
    public List<String> tabComplete(String[] args, List<String> online) {
        return List.of();
    }
}
