package org.coffeepop.latchac.core.command.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.coffeepop.latchac.core.LatchAC;
import org.coffeepop.latchac.core.command.SubCommand;

import java.util.List;

/**
 * /latchac training — toggles training mode at runtime (does not persist to config).
 */
public class TrainingCommand implements SubCommand {

    @Override
    public String name() { return "training"; }

    @Override
    public String permission() { return "latchac.training"; }

    @Override
    public List<Component> execute(String senderId, String[] args) {
        boolean was = LatchAC.get().getViolationHandler().isTrainingMode();
        boolean now = !was;
        LatchAC.get().getBaselineProfiler().setTrainingMode(now);
        LatchAC.get().getViolationHandler().setTrainingMode(now);
        return List.of(Component.text()
                .append(Component.text("Training mode: ", NamedTextColor.GRAY))
                .append(Component.text(now ? "ON" : "OFF", now ? NamedTextColor.GREEN : NamedTextColor.RED))
                .build());
    }

    @Override
    public List<String> tabComplete(String[] args, List<String> online) {
        return List.of();
    }
}
