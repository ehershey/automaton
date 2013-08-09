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
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class QuoteCommandTest {
    QuoteCommand command = null;
    DBCollection dbc = null;

    @Before
    public void init() {
        dbc = mock(DBCollection.class);
        command = new QuoteCommand(dbc);
    }

    public static String[] split(String str) {
        return str.split(" ");
    }

    @Test
    public void testFindQuoteStartNormal() {
        assertEquals("Quote start should be first part", 0, command.findQuoteStart(split("<TEST> QUOTE HERE")));
    }

    @Test
    public void testFindQuoteStartWithArgs() {
        assertEquals("Quote start should be the second part", 1, command.findQuoteStart(split("test <TEST> QUOTE HERE")));
        assertEquals("Quote start should be second part", 1, command.findQuoteStart(split("test [12:34] <TEST> QUOTE HERE")));
    }

    @Test
    public void testFindQuoteStartNone() {
        assertEquals("Quote start should not be found", -1, command.findQuoteStart(new String[]{}));
        assertEquals("Quote start should not be found", -1, command.findQuoteStart(split("")));
        assertEquals("Quote start should not be found", -1, command.findQuoteStart(split("no start found")));
    }

    @Test
    public void testLooksLikeTimestamp() {
        assertTrue(command.looksLikeTimestamp("12:34"));
        assertTrue(command.looksLikeTimestamp("9:34"));
        assertTrue(command.looksLikeTimestamp("[12:34]"));
        assertTrue(command.looksLikeTimestamp("[9:34]"));
        assertTrue(command.looksLikeTimestamp("[ 9:34]"));
    }

    @Test
    public void testBuildQuoteSearch() {
        assertSearchDBObject("gnome", null, "gnome");
        assertSearchDBObject("gnome /foo/", "foo", "gnome");
        assertSearchDBObject("/foo/", "foo", null);
    }

    public void assertSearchDBObject(String args, String searchTerm, String nick) {
        BasicDBObject dbObj = command.buildQuoteSearch(args);
        if(searchTerm == null) {
            assertNull(String.format("%s - %s", args, dbObj.toString()), dbObj.get(QuoteCommand.QUOTE_QUOTE));
        }else {
            BasicDBObject searchTermObj = (BasicDBObject) dbObj.get(QuoteCommand.QUOTE_QUOTE);
            assertEquals("Search terms don't match in " + args, searchTerm, searchTermObj.getString("$regex"));
        }

        if(nick == null) {
            assertNull(String.format("%s - %s", args, dbObj.toString()), dbObj.get(QuoteCommand.QUOTE_NICK));
        }else {
            assertEquals("Nicks don't match in " + args, nick, dbObj.getString(QuoteCommand.QUOTE_NICK));
        }
    }

    @Test
    public void testFindNickCandidates() {
        assertCandidates("<foo> foo <bar> bar <foo> foo2", "foo", "bar");
        assertCandidates("< foo> foo <bar> bar", "foo", "bar");
        assertCandidates("[12:34] <foo> foo <bar> bar", "foo", "bar");
        assertCandidates("[12:34] <foo> foo [12:35] <bar> bar", "foo", "bar");
    }

    protected void assertCandidates(String input, String... candidates) {
        Set<String> found = command.findNickCandidates(split(input));
        assertEquals(String.format("Wrong number of candidates for %s", input), candidates.length, found.size());
        for(String candidate : candidates) {
            assertTrue(String.format("Expected to find %s", candidate), found.contains(candidate));
        }
    }

    @Test
    public void testGetNormalizedNick() {
        assertEquals("Normalized nick wrong", "gnome", command.getNormalizedNick("<Gnome>"));
        assertEquals("Normalized nick wrong", "gnome", command.getNormalizedNick("<@Gnome>"));
        assertEquals("Normalized nick wrong", "gnome", command.getNormalizedNick("<+Gnome>"));
        assertEquals("Normalized nick wrong", "gnome", command.getNormalizedNick("<+@Gnome>"));
        assertEquals("Normalized nick wrong", "gnome", command.getNormalizedNick("<@+Gnome>"));
        assertEquals("Normalized nick wrong", "gnome", command.getNormalizedNick("<+@Gnome<>+>"));
        assertEquals("Normalized nick wrong", "gnome", command.getNormalizedNick("< gnome >"));
    }
}
