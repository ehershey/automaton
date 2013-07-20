package net.jimj.automaton.commands;

import net.jimj.automaton.events.Event;

public interface CommandObserver {
    public void observe(Event event);
}
