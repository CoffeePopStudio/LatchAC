package org.coffeepop.latchac.core.command;

import java.util.*;

/**
 * Lightweight subcommand dispatcher — all logic in core, no platform dependencies.
 */
public class CommandManager {

    private final Map<String, SubCommand> commands = new LinkedHashMap<>();

    public void register(SubCommand cmd) {
        commands.put(cmd.name().toLowerCase(), cmd);
    }

    /**
     * Dispatches a command to the matching subcommand.
     * <p>
     * NOTE: This method does not check {@link SubCommand#permission()}.
     * Platform layers should check permissions via {@link SubCommand#permission()}
     * before calling this method.
     */
    public List<String> dispatch(String senderId, String[] args) {
        if (args.length == 0) return help();
        SubCommand cmd = commands.get(args[0].toLowerCase());
        if (cmd == null) return List.of("§cUnknown subcommand. " + helpLine());
        // Platform layer should check: if (cmd.permission() != null && !senderHasPerm(senderId, cmd.permission()))
        return cmd.execute(senderId, slice(args));
    }

    /**
     * Returns the registered commands map for external permission checking.
     * @return unmodifiable view of the commands map
     */
    public Map<String, SubCommand> getCommands() {
        return Collections.unmodifiableMap(commands);
    }

    public List<String> tabComplete(String[] args, List<String> onlinePlayers) {
        if (args.length <= 1) {
            return commands.keySet().stream()
                    .filter(k -> k.startsWith(args.length == 0 ? "" : args[0].toLowerCase()))
                    .toList();
        }
        SubCommand cmd = commands.get(args[0].toLowerCase());
        if (cmd == null) return List.of();
        return cmd.tabComplete(slice(args), onlinePlayers);
    }

    private List<String> help() {
        var lines = new ArrayList<String>();
        lines.add("§8§m                  §r §bLatchAC §8§m                  ");
        for (var cmd : commands.values()) {
            lines.add(" §7/latchac §f" + cmd.name());
        }
        lines.add("§8§m                                              ");
        return lines;
    }

    private String helpLine() {
        return "§7Try: " + String.join(" | ", commands.keySet());
    }

    private static String[] slice(String[] args) {
        return args.length <= 1 ? new String[0] : Arrays.copyOfRange(args, 1, args.length);
    }
}
