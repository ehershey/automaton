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

package net.jimj.automaton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.*;
import net.jimj.automaton.commands.Command;
import net.jimj.automaton.commands.HeadCommand;
import net.jimj.automaton.commands.QuoteCommand;
import net.jimj.automaton.model.Config;
import net.jimj.automaton.model.User;
import org.apache.commons.lang.StringUtils;
import org.jibble.pircbot.PircBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

public class Bot extends PircBot {
    private static final Logger LOGGER = LoggerFactory.getLogger(Bot.class);
    private static final String USER_COLLECTION = "users";
    private ObjectMapper objectMapper;
    private HashMap<String, Command> commandMap = new HashMap<>();
    private Config config;

    private DB db;
    private DBCollection users;

    public Bot(DB db) throws Exception {
        this.db = db;
        this.objectMapper = new ObjectMapper();
        loadConfig();

        this.setLogin(config.getNick());
        this.setName(config.getNick());
        //TODO: Clean up logging
        this.setVerbose(LOGGER.isTraceEnabled());
        users = db.getCollection(USER_COLLECTION);
        loadCommands();
    }

    public void go() {
        try {
            this.connect(config.getServer());
        }catch(Exception e) {
            LOGGER.error("Exception in go method ", e);
        }
    }

    @Override
    protected void onConnect() {
        for(String channel : config.getChannels()) {
            this.joinChannel(channel);
        }
    }

    @Override
    protected void onDisconnect() {
        go();
    }

    @Override
    protected void onPrivateMessage(String sender, String login, String hostname, String message) {
        onMessage(null, sender, login, hostname, message);
    }

    @Override
    protected void onChannelInfo(String channel, int userCount, String topic) {
        super.onChannelInfo(channel, userCount, topic);    //To change body of overridden methods use File | Settings | File Templates.

    }

    @Override
    protected void onMessage(String channel, String sender, String login, String hostname, String message) {
        //TODO: Check ignore here.
        if(message.startsWith(config.getCommandChar())) {
            String commandName = message;

            //Try to chop off the first word
            int commandNameEnd = message.indexOf(" ");
            String args = null;
            if(commandNameEnd != -1) {
                commandName = message.substring(0, commandNameEnd);
                args = StringUtils.strip(message.substring(commandName.length()));
            }

            try {
                fireCommand(commandName, channel, sender, args);
            }catch(Exception e) {
                String target = channel == null? sender : channel;
                this.sendMessage(target, "fyf " + sender);

                StackTraceElement[] st = e.getStackTrace();
                int numMessages = Math.min(3, st.length);

                //TODO: put this in config or dig out from Users somehow.
                this.sendMessage("Gnome", sender + " caused " + e.getMessage());
                for(int i=0;i<numMessages;i++) {
                    this.sendMessage("Gnome", st[i].toString());
                }
            }
        }
    }

    protected void fireCommand(String commandName, String channel, String sender, String args) {
        LOGGER.debug("Firing command " + commandName);
        Command commandObj = commandMap.get(commandName);
        User user = getUser(sender);
        if(commandObj != null && commandObj.authorized(user)) {
            List<BotAction> actions = commandObj.execute(user, channel, args);
            if(actions != null) {
                for(BotAction action : actions) {
                    switch(action.getType()) {
                        case MESSAGE:
                            String target = channel == null? sender : channel;
                            this.sendMessage(target, action.getPayload());
                            break;
                        default:
                            LOGGER.error("Unknown action " + action);
                            break;
                    }
                }
            }
        }
    }

    private void loadCommands() {
        commandMap.put(config.getCommandChar() + "quote", new QuoteCommand(db.getCollection("quotes")));
        commandMap.put(config.getCommandChar() + "head", new HeadCommand());
    }

    private void loadConfig() throws Exception {
        //TODO: externalize this
        InputStream configStream = getClass().getResourceAsStream("/META-INF/config.json");
        config = objectMapper.readValue(configStream, Config.class);
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug(objectMapper.writeValueAsString(config));
        }
    }

    private User getUser(String userName) {
        User user = null;
        DBCursor userCur = users.find(new BasicDBObject("nick", userName));
        if(userCur.hasNext()) {
            DBObject userObj = userCur.next();
            user = objectMapper.convertValue(userObj.toString(), User.class);
        }else {
            user = new User(0);
            user.setNick(userName);
        }
        return user;
    }
}
