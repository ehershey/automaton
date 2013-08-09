/*
 * Copyright (c) <2013> <Jim Johnson jimj@jimj.net>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.jimj.automaton.commands;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import net.jimj.automaton.events.MessageEvent;
import net.jimj.automaton.model.User;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

public class QuoteCommand extends Command {
    protected static final String QUOTE_NICK = "nick";
    protected static final String QUOTE_QUOTE = "quote";
    protected static final String QUOTE_NETWORK = "network";
    private static final Pattern TIMESTAMP = Pattern.compile(".*([0-9]{1,2}:[0-9][0-9]).*");
    private static final String NO_QUOTES = "No quotes found.";

    private DBCollection quotes;
    private static final Logger LOGGER = LoggerFactory.getLogger(QuoteCommand.class);
    private static Random random = new Random();

    public QuoteCommand(DBCollection quotes) {
        this.quotes = quotes;
    }

    @Override
    public String getCommandName() {
        return "quote";
    }

    @Override
    public void execute(User user, String args) {
        if(args == null) {
            args = "";
        }

        //Is this a search?
        //e.g. quote foo /search string/
        //to find a quote by 'foo' containing 'search string'
        int searchStart = args.indexOf("/");
        int searchEnd = args.lastIndexOf("/");

        String[] argParts = args.split(" ");
        //If there's more than 1 argument, and no search term.
        if(argParts.length > 1 && searchStart == searchEnd) {
            LOGGER.debug(String.format("%d > 1 && %d == %d", argParts.length, searchStart, searchEnd));
            if(storeQuote(args, argParts)) {
                notifyObserver(new MessageEvent(user, "quote stored."));
            }else {
                notifyObserver(new MessageEvent(user, "I couldn't parse the quote correctly."));
            }
        }else {
            notifyObserver(new MessageEvent(user, getQuote(args)));
        }
    }

    protected boolean storeQuote(String args, String[] argParts) {
        boolean stored = false;

        //Try to determine the start of the quote based on the assumption that
        //pasted quotes will contain nicknames surrounded by some sort of 'special' character.
        int quoteStart = findQuoteStart(argParts);
        if(quoteStart == -1) {
            LOGGER.warn("Couldn't find nick in quote " + args);
        } else {
            Set<String> nicks = findNickCandidates(argParts);

            //Quote defaults to entire string
            String quote = args;

            if(quoteStart > 0) {
                //Add on any 'passed in' nicks for the quote as well
                //i.e. .quote foo <foobar> my quote
                //would get the nicks ["foo", "foobar"] associated w/ it.
                for(int i=0;i<quoteStart;i++) {
                    nicks.add(argParts[i]);
                }

                //Cut out the passed in nicks from the actual quote.
                quote = squash(argParts, quoteStart);
            }

            BasicDBObject quoteObj = new BasicDBObject(QUOTE_NICK, nicks);
            quoteObj.append(QUOTE_QUOTE, quote);
            quoteObj.append(QUOTE_NETWORK, "slashnet");
            quoteObj.append("QUOTE_VERSION", "1");
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("Storing quote: " + quoteObj);
            }
            quotes.save(quoteObj);
            stored = true;
        }
        return stored;
    }

    protected String squash(String[] parts, int from) {
        StringBuilder result = new StringBuilder();
        for(int i=from;i<parts.length;i++) {
            result.append(parts[i]).append(" ");
        }
        return result.toString().trim();
    }

    protected int findQuoteStart(String[] argParts) {
        for(int i=0;i<argParts.length;i++) {
            String argPart = argParts[i];
            //Does the argPart start w/ a legal char?  If not, probably a nickname.
            if(isMetaWord(argPart)) {
                return i;
            }
        }

        return -1;
    }

    protected HashSet<String> findNickCandidates(String[] argParts) {
        HashSet<String> candidates = new HashSet<>();
        for(int i=0;i<argParts.length;i++) {
            String argPart = argParts[i];
            //String splitting happens on a space
            //argParts could show up as ["<", "nick>"] due to irc client formatting.
            //If this is the case, append the next argPart onto the current string
            if(argPart.length() == 1) {
                argPart += argParts[i+1];
            }

            if(isMetaWord(argPart)) {
                String candidate = getNormalizedNick(argPart);
                if(!looksLikeTimestamp(candidate)) {
                    candidates.add(candidate);
                }
            }
        }
        return candidates;
    }

    protected boolean looksLikeTimestamp(String str) {
        return TIMESTAMP.matcher(str).matches();
    }

    protected String getNormalizedNick(String nick) {
        int startNick = 0;
        int endNick = nick.length();

        int j=nick.length();
        for(int i=0;i<nick.length();i++) {
            if(i >= j) {
                break;
            }
            if(!legalChar(nick.charAt(i))) {
                startNick = i+1;
            }
            j--;
            if(!legalChar(nick.charAt(j))) {
                endNick = j;
            }
        }

        return nick.substring(startNick, endNick).toLowerCase();
    }

    protected boolean isMetaWord(String word) {
        if(StringUtils.isBlank(word)) {
            return false;
        }
        return !(Character.isAlphabetic(word.codePointAt(0)) || Character.isDigit(word.codePointAt(0)));
    }

    protected boolean legalChar(char c) {
        switch(c) {
            case '<':
            case '@':
            case '+':
            case '>':
            case ' ':
                return false;
            default:
                return true;
        }
    }

    protected String getQuote(String arg) {
        BasicDBObject query = buildQuoteSearch(arg);
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Searching for quotes that match " + query);
        }

        BasicDBObject keysWanted = new BasicDBObject("_id", "1");
        List<DBObject> quoteIds = quotes.find(query,keysWanted).toArray();
        return getRandomQuote(quoteIds);
    }

    protected BasicDBObject buildQuoteSearch(String arg) {
        //todo: replace w/ real Network concept.
        BasicDBObject query = new BasicDBObject(QUOTE_NETWORK,"slashnet");

        if(arg != null) {
            //Regex search
            int searchStart = arg.indexOf("/");
            int searchEnd = arg.lastIndexOf("/");
            if(searchStart != -1 && searchStart < searchEnd) {
                //cut out the included / characters.
                String quoteSearch = arg.substring(searchStart+1, searchEnd);
                if(quoteSearch.length() > 0) {
                    query.append(QUOTE_QUOTE, new BasicDBObject("$regex", quoteSearch));
                }
            }

            String nick = arg;
            if(searchStart != -1) {
                nick = arg.substring(0, searchStart).trim();
            }

            if(!StringUtils.isBlank(nick)) {
                query.append(QUOTE_NICK, nick);
            }
        }

        LOGGER.debug(query.toString());
        return query;
    }

    protected String getRandomQuote(List<DBObject> quoteIds) {
        if(quoteIds == null) {
            return NO_QUOTES;
        }

        int totalQuotes = quoteIds.size();

        if(totalQuotes < 1) {
            return NO_QUOTES;
        } else {
            int quoteNum = random.nextInt(totalQuotes);
            Object quoteId = quoteIds.get(quoteNum).get("_id");
            DBCursor quoteCur = quotes.find(new BasicDBObject("_id", quoteId), new BasicDBObject(QUOTE_QUOTE, 1));
            DBObject quoteObj = quoteCur.next();
            return (String)quoteObj.get(QUOTE_QUOTE);
        }
    }

    @Override
    public boolean authorized(User user) {
        return true;
    }
}
