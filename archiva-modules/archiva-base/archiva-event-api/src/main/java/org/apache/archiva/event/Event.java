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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.time.LocalDateTime;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Base class for events. Events have a type and a source.
 * The source is the instance that raised the event.
 *
 * There are different event types for a given event. The types are represented in a hierarchical structure.
 *
 * Events can be chained, which means a event listener can catch events and rethrow them as its own event.
 *
 */
public class Event<C extends EventContext> extends EventObject implements Cloneable {

    private static final long serialVersionUID = -7171846575892044990L;

    public static final EventType<Event> ANY = EventType.ROOT;

    private Event previous;
    private final EventType<? extends Event> type;
    private final LocalDateTime createTime;

    private HashMap<Class<? extends EventContext>, EventContext> contextMap = new HashMap<>( );

    public Event(EventType<? extends Event> type, Object originator) {
        super(originator);
        this.type = type;
        this.createTime = LocalDateTime.now();
    }

    private Event(Event previous, Object originator) {
        super(originator);
        this.previous = previous;
        this.type = previous.getType();
        this.createTime = previous.getCreateTime();
        this.contextMap = previous.contextMap;
    }

    /**
     * Returns the event type that is associated with this event instance.
     * @return the event type
     */
    public EventType<? extends Event> getType() {
        return type;
    };

    /**
     * Returns the time, when the event was created.
     * @return
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public <T extends EventContext> T getContext(Class<T> contextClazz) throws IllegalArgumentException {
        if (contextMap.containsKey( contextClazz )) {
            return (T) contextMap.get( contextClazz );
        } else {
            T ctx = null;
            for ( Map.Entry<Class<? extends EventContext>, EventContext> clazzEntry : contextMap.entrySet()) {
                if ( contextClazz.isAssignableFrom( clazzEntry.getKey() ) )
                {
                    ctx = (T) clazzEntry.getValue( );
                    break;
                }
            }
            if (ctx!=null) {
                contextMap.put( contextClazz, ctx );
                return ctx;
            }
        }
        throw new IllegalArgumentException( "No matching event context registered for " + contextClazz );
    }

    public C getContext() {
        throw new UnsupportedOperationException( );
    }

    public Map<String, String> getContextData() {
        return contextMap.entrySet( ).stream( ).flatMap( ctx -> ctx.getValue( ).getData( ).entrySet( ).stream( ) )
            .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );
    }

    public <T extends EventContext> void setContext( Class<T> clazz, T context) {
        this.contextMap.put( clazz, context );
    }

    public void setContext( C context) {
        throw new UnsupportedOperationException( );
    }

    /**
     * Recreates the event with the given instance as the new source. The
     * current source is stored in the previous event.
     * @param newSource The new source
     * @return a new event instance, where <code>this</code> is stored as previous event
     */
    public Event copyFor(Object newSource) {
        Event newEvent = (Event) this.clone();
        newEvent.previous = this;
        newEvent.source = newSource;
        newEvent.contextMap = this.contextMap;
        return newEvent;
    }

    /**
     * Returns the previous event or <code>null</code>, if this is a root event.
     * @return the previous event or <code>null</code>, if it does not exist
     */
    public Event getPreviousEvent() {
        return previous;
    }

    /**
     * Returns <code>true</code>, if the event has a previous event.
     * @return <code>true</code>, if this has a previous event, otherwise <code>false</code>
     */
    public boolean hasPreviousEvent() {
        return previous!=null;
    }

    @Override
    protected Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // this should not happen
            throw new RuntimeException("Event is not clonable");
        }
    }
}
