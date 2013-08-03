package net.jimj.automaton.commands;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import net.jimj.automaton.events.MessageEvent;
import net.jimj.automaton.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KarmaCommand extends Command implements Processor {
    private static final String KARMA_ITEM = "item";
    private static final String KARMA_VALUE = "value";

    private static final Logger LOGGER = LoggerFactory.getLogger(KarmaCommand.class);
    DBCollection karmaCollection;

    public KarmaCommand(DBCollection karmaCollection) {
        this.karmaCollection = karmaCollection;
    }

    @Override
    public String getCommandName() {
        return "karma";
    }

    @Override
    public void execute(User user, String args) {
        int value = 0;
        Karma karma = getKarma(args);
        if(karma != null) {
            value = karma.getValue();
        }
        String message = args + ": " + value;
        notifyObserver(new MessageEvent(user, message));
    }

    @Override
    public boolean authorized(User user) {
        return true;
    }

    @Override
    public boolean shouldProcess(String message) {
        return message.endsWith("++") || message.endsWith("--");
    }

    @Override
    public void process(User user, String message) {
        LOGGER.debug("Processing Karma message");
        if(message.endsWith("++")) {
            addKarma(message.substring(0, message.length()-2).toLowerCase());
        }else {
            subtractKarma(message.substring(0, message.length()-2).toLowerCase());
        }
    }

    protected void addKarma(String item) {
        Karma karma = getKarma(item);
        int newVal = (karma.getValue()+1);
        karma.setValue(newVal);
        putKarma(karma);
    }

    protected void subtractKarma(String item) {
        Karma karma = getKarma(item);
        int newVal = (karma.getValue()-1);
        karma.setValue(newVal);
        putKarma(karma);
    }

    protected Karma getKarma(String item) {
        LOGGER.trace("Looking for karma for " + item);
        Karma karma = new Karma(item.toLowerCase());

        BasicDBObject query = new BasicDBObject(KARMA_ITEM, item.toLowerCase());
        DBCursor cur = karmaCollection.find(query);

        if(cur != null && cur.hasNext()) {
            DBObject karmaObj = cur.next();
            karma.setId(karmaObj.get("_id"));
            karma.setItem((String)karmaObj.get(KARMA_ITEM));
            karma.setValue((Integer)karmaObj.get(KARMA_VALUE));
        }

        LOGGER.trace("Karma: " + karma);

        return karma;
    }

    private void putKarma(Karma karma) {
        if(karma.getId() == null) {
            //Insert
            BasicDBObject karmaObj = new BasicDBObject(KARMA_ITEM, karma.getItem());
            karmaObj.append(KARMA_VALUE, karma.getValue());
            karmaCollection.insert(karmaObj);
        }else {
            //Update
            BasicDBObject karmaObj = new BasicDBObject("_id", karma.getId());
            karmaCollection.update(karmaObj, new BasicDBObject("$set", new BasicDBObject(KARMA_VALUE, karma.getValue())));
        }
    }

    protected class Karma {
        private Object id;
        private String item;
        private int value;

        protected Karma() {
            value = 0;
        }

        protected Karma(String item) {
            value = 0;
            this.item = item;
        }
        public Object getId() {
            return id;
        }

        public void setId(Object id) {
            this.id = id;
        }

        public String getItem() {
            return item;
        }

        public void setItem(String item) {
            this.item = item;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.format("Karma item '%s' has karma %d", item, value);
        }
    }
}
