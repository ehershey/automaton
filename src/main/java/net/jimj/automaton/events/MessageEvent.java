package net.jimj.automaton.events;

import net.jimj.automaton.model.User;

public class MessageEvent implements Event {
    private User user;
    private String message;

    public MessageEvent(User user, String message) {
        this.user = user;
        this.message = message;
    }

    public String getTarget() {
        return user.getChannel() == null ? user.getNick() : user.getChannel();
    }

    public String getMessage() {
        return message;
    }
}
