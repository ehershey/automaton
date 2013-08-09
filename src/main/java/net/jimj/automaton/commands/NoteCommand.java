package net.jimj.automaton.commands;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import net.jimj.automaton.events.MessageEvent;
import net.jimj.automaton.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class NoteCommand extends Command implements Processor {
    private static final Logger logger = LoggerFactory.getLogger(NoteCommand.class);

    private static final String NOTE_FROM = "from";
    private static final String NOTE_TO = "to";
    private static final String NOTE_NOTE = "note";
    private static final String NOTE_WHEN = "when";
    private static final String NOTE_DELIVERED = "delivered";

    private DBCollection notes;

    private static final SimpleDateFormat WHEN_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm zzz");

    public NoteCommand(DBCollection notes) {
        this.notes = notes;
        notes.ensureIndex(new BasicDBObject(NOTE_TO, 1));
    }

    @Override
    public String getCommandName() {
        return "note";
    }

    @Override
    public void execute(User user, String args) {
        String[] argParts = ArgUtil.split(args);

        if(argParts[0].isEmpty()) {
            logger.info("Finding notes for " + user.getNick());
            findNotes(user);
            return;
        }

        String toNick = argParts[0].trim();
        String note = ArgUtil.squash(argParts, 1);
        if(note == null ||  note.isEmpty()) {
            //insultEvent
            return;
        }

        logger.info("Storing note for " + toNick + " (" + note + ")");
        storeNote(user.getNick(), toNick, note);
    }

    protected void storeNote(String from, String to, String note) {
        BasicDBObject noteObj = new BasicDBObject(NOTE_FROM, from);
        noteObj.append(NOTE_TO, to.toLowerCase());
        noteObj.append(NOTE_NOTE, note);
        noteObj.append(NOTE_DELIVERED, false);
        noteObj.append(NOTE_WHEN, System.currentTimeMillis());
        notes.insert(noteObj);
    }

    protected void findNotes(User to) {
        BasicDBObject query = new BasicDBObject(NOTE_TO, to.getNick().toLowerCase());
        query.append(NOTE_DELIVERED, false);
        DBCursor noteCursor = notes.find(query);
        if(noteCursor == null) {
            return;
        }

        while(noteCursor.hasNext()) {
            DBObject noteObj = noteCursor.next();
            String from = (String)noteObj.get(NOTE_FROM);
            String note = (String)noteObj.get(NOTE_NOTE);
            long when = (long)noteObj.get(NOTE_WHEN);

            StringBuilder noteMessage = new StringBuilder(to.getNick()).append(" you have a note from ");
            noteMessage.append(from).append(" at ").append(WHEN_FMT.format(new Date(when)));
            notifyObserver(new MessageEvent(to, noteMessage.toString()));
            notifyObserver(new MessageEvent(to, note));

            notes.update(new BasicDBObject("_id", noteObj.get("_id")),new BasicDBObject("$set",
                    new BasicDBObject(NOTE_DELIVERED, true)));
        }
    }

    @Override
    public boolean authorized(User user) {
        return true;
    }

    @Override
    public boolean shouldProcess(String message) {
        return true;
    }

    @Override
    public void process(User user, String message) {
        findNotes(user);
    }
}
