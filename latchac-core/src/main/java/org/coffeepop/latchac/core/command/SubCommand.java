package org.coffeepop.latchac.core.command;

import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * Core subcommand interface — platform-agnostic.
 * <p>
 * {@link #execute} returns {@link Component} lines; the platform adapter sends them.
 * {@link #tabComplete} receives online player names for player-targeting completions.
 */
public interface SubCommand {

    String name();

    /** Returns message components to be sent to the sender. */
    List<Component> execute(String senderId, String[] args);

    /** Returns tab completions for the given args. */
    List<String> tabComplete(String[] args, List<String> onlinePlayerNames);

    /**
     * Permission node required to execute this subcommand.
     * Returns {@code null} by default, meaning no permission is required.
     */
    default String permission() { return null; }
}
