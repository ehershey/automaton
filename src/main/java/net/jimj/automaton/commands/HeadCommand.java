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

import net.jimj.automaton.events.MessageEvent;
import net.jimj.automaton.model.User;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeadCommand extends Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeadCommand.class);
    private HttpClient httpClient;

    public HeadCommand() {
        httpClient = new DefaultHttpClient();
    }

    @Override
    public String getCommandName() {
        return "head";
    }

    @Override
    public void execute(User user, String args) {
        if(!args.startsWith("http://") && !args.startsWith("https://")) {
            args = "http://" + args;
        }

        HttpHead headMethod = new HttpHead(args);
        try {
            HttpResponse response = httpClient.execute(headMethod);
            StatusLine status = response.getStatusLine();
            notifyObserver(new MessageEvent(user, status.getStatusCode() + " " + status.getReasonPhrase()));

            Header[] serverHeaders = response.getHeaders("Server");
            if(serverHeaders != null) {
                notifyObserver(new MessageEvent(user, "Server: " + serverHeaders[0].getValue()));
            }
            headMethod.releaseConnection();
        }catch(Exception e) {
            LOGGER.error("Error in HEAD", e);
        }
    }

    @Override
    public boolean authorized(User user) {
        return true;
    }
}
