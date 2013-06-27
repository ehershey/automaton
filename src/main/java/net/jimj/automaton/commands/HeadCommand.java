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

import net.jimj.automaton.BotAction;
import net.jimj.automaton.model.User;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class HeadCommand extends Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeadCommand.class);
    private HttpClient httpClient;

    public HeadCommand() {
        httpClient = new DefaultHttpClient();
    }

    @Override
    public List<BotAction> execute(User user, String channel, String args) {
        List<BotAction> actions = new ArrayList<BotAction>();

        if(!args.startsWith("http://") && !args.startsWith("https://")) {
            args = "http://" + args;
        }

        HttpHead headMethod = new HttpHead(args);
        try {
            HttpResponse response = httpClient.execute(headMethod);
            StatusLine status = response.getStatusLine();
            BotAction statusAction = new BotAction(BotAction.Type.MESSAGE);
            statusAction.setPayload(status.getStatusCode() + " " + status.getReasonPhrase());
            actions.add(statusAction);

            Header[] serverHeaders = response.getHeaders("Server");
            if(serverHeaders != null) {
                BotAction serverAction = new BotAction(BotAction.Type.MESSAGE);
                serverAction.setPayload("Server: " + serverHeaders[0].getValue());
                actions.add(serverAction);
            }
            headMethod.releaseConnection();
        }catch(Exception e) {
            BotAction errorAction = new BotAction(BotAction.Type.MESSAGE);
            errorAction.setPayload("fyf " + user.getNick());
            LOGGER.error("Error in HEAD", e);
        }

        return actions;
    }

    @Override
    public boolean authorized(User user) {
        return true;
    }
}
