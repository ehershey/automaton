package net.jimj.automaton.commands;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class KarmaCommandTest {
    private KarmaCommand command;
    private DBCollection karma;

    @Before
    public void init() {
        karma = mock(DBCollection.class);
        command = new KarmaCommand(karma);
    }

    @Test
    public void testAddKarmaNew() {
        when(karma.find(any(BasicDBObject.class))).thenReturn(null);
        ArgumentCaptor<BasicDBObject> insert = ArgumentCaptor.forClass(BasicDBObject.class);
        command.addKarma("test");
        verify(karma).insert(insert.capture());
        Integer value = (Integer)insert.getValue().get("value");
        assertEquals(1, value.intValue());
    }

    @Test
    public void testAddKarmaUpdate() {
        DBCursor cur = mock(DBCursor.class);
        BasicDBObject existing = new BasicDBObject("value", 5);

        when(karma.find(any(BasicDBObject.class))).thenReturn(cur);
        when(cur.hasNext()).thenReturn(Boolean.TRUE);
        when(cur.next()).thenReturn(existing);
        ArgumentCaptor<BasicDBObject> insertArg = ArgumentCaptor.forClass(BasicDBObject.class);

        command.addKarma("test");

        verify(karma).insert(insertArg.capture());
        Integer value = (Integer)insertArg.getValue().get("value");
        assertEquals(6, value.intValue());
    }

    @Test
    public void testSubtractKarma() {
        when(karma.find(any(BasicDBObject.class))).thenReturn(null);
        ArgumentCaptor<BasicDBObject> insert = ArgumentCaptor.forClass(BasicDBObject.class);

        command.subtractKarma("test");

        verify(karma).insert(insert.capture());
        Integer value = (Integer)insert.getValue().get("value");
        assertEquals(-1, value.intValue());
    }

    @Test
    public void testSubtractKarmaUpdate() {
        DBCursor cur = mock(DBCursor.class);
        BasicDBObject existing = new BasicDBObject("value", 5);

        when(karma.find(any(BasicDBObject.class))).thenReturn(cur);
        when(cur.hasNext()).thenReturn(Boolean.TRUE);
        when(cur.next()).thenReturn(existing);
        ArgumentCaptor<BasicDBObject> insertArg = ArgumentCaptor.forClass(BasicDBObject.class);

        command.subtractKarma("test");

        verify(karma).insert(insertArg.capture());
        Integer value = (Integer)insertArg.getValue().get("value");
        assertEquals(4, value.intValue());
    }
}
