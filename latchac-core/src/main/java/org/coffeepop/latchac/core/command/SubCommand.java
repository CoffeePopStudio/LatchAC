package org.coffeepop.latchac.core.command;

import java.util.List;

/**
 * Core subcommand interface — platform-agnostic.
 * <p>
 * {@link #execute} returns message lines; the platform adapter sends them.
 * {@link #tabComplete} receives online player names for player-targeting completions.
 */
public interface SubCommand {

    String name();

    /** Returns lines to be sent to the sender. */
    List<String> execute(String senderId, String[] args);

    /** Returns tab completions for the given args. */
    List<String> tabComplete(String[] args, List<String> onlinePlayerNames);
}
