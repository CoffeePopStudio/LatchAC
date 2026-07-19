package org.coffeepop.latchac.paper.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.coffeepop.latchac.paper.LatchAntiCheat;

public final class LatchacCommand implements CommandExecutor {

    private final LatchAntiCheat plugin;

    public LatchacCommand(LatchAntiCheat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            boolean ok = plugin.reloadLatchConfig();
            sender.sendMessage(ok
                    ? "[LatchAC] 配置已热重载"
                    : "[LatchAC] 重载失败，已保留旧配置（详见控制台）");
            return true;
        }
        sender.sendMessage("[LatchAC] 用法: /" + label + " reload");
        return true;
    }
}
