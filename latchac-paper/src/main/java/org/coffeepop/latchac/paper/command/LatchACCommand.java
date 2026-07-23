package org.coffeepop.latchac.paper.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.coffeepop.latchac.core.LatchAC;

import java.util.List;

/**
 * Paper adapter — translates Bukkit {@link CommandSender} into core subcommand calls.
 */
public class LatchACCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String senderId = sender instanceof Player p ? p.getUniqueId().toString() : "CONSOLE";
        List<String> result = LatchAC.get().getCommandManager().dispatch(senderId, args);
        for (String line : result) sender.sendMessage(line);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> online = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return LatchAC.get().getCommandManager().tabComplete(args, online);
    }
}
