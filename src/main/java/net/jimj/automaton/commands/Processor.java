package net.jimj.automaton.commands;

import net.jimj.automaton.model.User;

/**
 * Processors look at every line of text that is not a command.
 */
public interface Processor {
    public boolean shouldProcess(String message);
    public void process(User user, String message);
}
