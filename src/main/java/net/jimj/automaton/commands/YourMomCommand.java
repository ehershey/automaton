package net.jimj.automaton.commands;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import net.jimj.automaton.events.MessageEvent;
import net.jimj.automaton.model.User;

import java.util.List;
import java.util.Random;

public class YourMomCommand extends Command implements Processor {
    private static final String YOURMOM_INSULT = "insult";

    private DBCollection yourMoms;
    private Random random;

    public YourMomCommand(DBCollection yourMoms) {
        this.yourMoms = yourMoms;
        this.random = new Random();
    }

    @Override
    public String getCommandName() {
        return "yourmom";
    }

    @Override
    public void execute(User user, String args) {
        List<DBObject> yourMomIds = yourMoms.find(new BasicDBObject(),new BasicDBObject("_id", "1")).toArray();
        int totalYourMoms = yourMomIds.size();
        int quoteNum = random.nextInt(totalYourMoms);
        Object yourMomId = yourMomIds.get(quoteNum).get("_id");

        DBCursor yourMomCur = yourMoms.find(new BasicDBObject("_id", yourMomId), new BasicDBObject(YOURMOM_INSULT, 1));
        DBObject yourMomObj = yourMomCur.next();
        String insult = (String)yourMomObj.get(YOURMOM_INSULT);
        notifyObserver(new MessageEvent(user, insult));
    }

    @Override
    public boolean authorized(User user) {
        return true;
    }

    @Override
    public boolean shouldProcess(String message) {
        return message.toLowerCase().contains("your mom");
    }

    @Override
    public void process(User user, String message) {
        if(!putYourMom(message.toLowerCase())) {
            notifyObserver(new MessageEvent(user, "0/10 that is not a unique 'your mom'"));
        }
    }

    private boolean putYourMom(String insult) {
        boolean isUnique = true;

        BasicDBObject yourMomObj = new BasicDBObject(YOURMOM_INSULT, insult);
        DBCursor cur = yourMoms.find(yourMomObj);
        if(cur.hasNext()) {
            isUnique = false;
        }else {
            yourMoms.insert(yourMomObj);
        }

        return isUnique;
    }
}
