package org.coffeepop.latchac.paper.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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

        // Permission check for subcommands
        if (args.length > 0) {
            var subCmd = LatchAC.get().getCommandManager().getCommands().get(args[0].toLowerCase());
            String perm = subCmd != null ? subCmd.permission() : null;
            if (perm != null && !sender.hasPermission(perm)) {
                sender.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
                return true;
            }
        }

        List<Component> result = LatchAC.get().getCommandManager().dispatch(senderId, args);
        for (Component line : result) {
            sender.sendMessage(line);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> online = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return LatchAC.get().getCommandManager().tabComplete(args, online);
    }
}
