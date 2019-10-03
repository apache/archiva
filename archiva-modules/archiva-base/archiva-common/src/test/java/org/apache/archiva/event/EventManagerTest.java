package org.apache.archiva.event;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class EventManagerTest
{

    private class TestHandler implements EventHandler<Event> {

        private List<Event> eventList = new ArrayList<>( );
        @Override
        public void handle( Event event )
        {
            eventList.add( event );
        }

        public List<Event> getEventList() {
            return eventList;
        }
    }

    private EventType<Event> testType = new EventType<>( "TEST" );
    private EventType<Event> testTestType = new EventType<>( testType,"TEST.TEST" );
    private EventType<Event> otherType = new EventType( "OTHER" );

    @Test
    public void registerEventHandler( )
    {
        EventManager eventManager = new EventManager( this );
        TestHandler handler1 = new TestHandler( );
        TestHandler handler2 = new TestHandler( );
        TestHandler handler3 = new TestHandler( );
        TestHandler handler4 = new TestHandler( );

        eventManager.registerEventHandler( Event.ANY, handler1 );
        eventManager.registerEventHandler( testType, handler2 );
        eventManager.registerEventHandler( testTestType, handler3 );
        eventManager.registerEventHandler( otherType, handler4 );

        Event event1 = new Event( testType, this );
        eventManager.fireEvent( event1 );
        assertEquals( 1, handler1.eventList.size( ) );
        assertEquals( 1, handler2.eventList.size( ) );
        assertEquals( 0, handler3.eventList.size( ) );
        assertEquals( 0, handler4.eventList.size( ) );

        Event event2 = new Event( testTestType, event1 );
        eventManager.fireEvent( event2 );
        assertEquals( 2, handler1.eventList.size( ) );
        assertEquals( 2, handler2.eventList.size( ) );
        assertEquals( 1, handler3.eventList.size( ) );
        assertEquals( 0, handler4.eventList.size( ) );

        Event event3 = new Event( otherType, event1 );
        eventManager.fireEvent( event3 );
        assertEquals( 3, handler1.eventList.size( ) );
        assertEquals( 2, handler2.eventList.size( ) );
        assertEquals( 1, handler3.eventList.size( ) );
        assertEquals( 1, handler4.eventList.size( ) );



    }

    @Test
    public void unregisterEventHandler( )
    {
        EventManager eventManager = new EventManager( this );
        TestHandler handler1 = new TestHandler( );
        TestHandler handler2 = new TestHandler( );
        TestHandler handler3 = new TestHandler( );
        TestHandler handler4 = new TestHandler( );

        eventManager.registerEventHandler( Event.ANY, handler1 );
        eventManager.registerEventHandler( testType, handler2 );
        eventManager.registerEventHandler( testTestType, handler3 );
        eventManager.registerEventHandler( otherType, handler4 );

        eventManager.unregisterEventHandler( Event.ANY, handler1 );
        Event event1 = new Event( testType, this );
        eventManager.fireEvent( event1 );
        assertEquals( 0, handler1.eventList.size( ) );
        assertEquals( 1, handler2.eventList.size( ) );
        assertEquals( 0, handler3.eventList.size( ) );
        assertEquals( 0, handler4.eventList.size( ) );

        eventManager.unregisterEventHandler( otherType, handler2 );
        Event event2 = new Event( testType, this );
        eventManager.fireEvent( event2 );
        assertEquals( 0, handler1.eventList.size( ) );
        assertEquals( 2, handler2.eventList.size( ) );
        assertEquals( 0, handler3.eventList.size( ) );
        assertEquals( 0, handler4.eventList.size( ) );
    }

    @Test
    public void fireEvent( )
    {
        Object other = new Object( );
        EventManager eventManager = new EventManager( this );
        assertThrows( NullPointerException.class, ( ) -> eventManager.fireEvent( null ) );
        Event event = new Event( EventType.ROOT, other );
        assertEquals( other, event.getSource( ) );
        TestHandler handler = new TestHandler( );
        eventManager.registerEventHandler( EventType.ROOT, handler );
        eventManager.fireEvent( event );
        assertEquals( 1, handler.getEventList( ).size( ) );
        Event newEvent = handler.getEventList( ).get( 0 );
        assertNotEquals( event, newEvent );
        assertEquals( this, newEvent.getSource( ) );

    }
}