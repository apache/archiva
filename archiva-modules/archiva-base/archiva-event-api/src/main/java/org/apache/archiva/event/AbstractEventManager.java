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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Martin Schreier <martin_s@apache.org>
 */
public class AbstractEventManager implements EventSource
{
    private static final Logger log = LoggerFactory.getLogger( AbstractEventManager.class );

    protected final ConcurrentHashMap<EventType<? extends Event>, Set<EventHandler>> handlerMap = new ConcurrentHashMap<>();

    @Override
    public <T extends Event> void registerEventHandler( EventType<T> type, EventHandler<? super T> eventHandler) {
        Set<EventHandler> handlers = handlerMap.computeIfAbsent(type, t -> new LinkedHashSet<>());
        if (!handlers.contains(eventHandler)) {
            handlers.add(eventHandler);
        }
        log.debug( "Event handler registered: " + eventHandler.getClass( ) );
    }

    @Override
    public <T extends Event> void unregisterEventHandler( EventType<T> type, EventHandler<? super T> eventHandler) {
        if (handlerMap.containsKey(type)) {
            handlerMap.get(type).remove(eventHandler);
            log.debug( "Event handler unregistered: " + eventHandler.getClass( ) );
        }
    }

    /**
     * Fires the given event for the given source. If the source of the provided event does not match the <code>source</code>
     * parameter the event will be chained.
     *
     * The event will be sent to all registered event handler. Exceptions during handling are not propagated to the
     * caller.
     *
     * @param fireEvent the event to fire
     * @param source the source object
     */
    public void fireEvent(Event fireEvent, Object source) {
        final EventType<? extends Event> type = fireEvent.getType();
        Event event;
        if (fireEvent.getSource()!=source) {
            event = fireEvent.copyFor(source);
        } else {
            event = fireEvent;
        }
        for (EventType<? extends Event> handlerType : handlerMap.keySet()) {
            if (EventType.isInstanceOf(type, handlerType)) {
                for (EventHandler handler : handlerMap.get(handlerType)) {
                    try {
                        handler.handle(event);
                    } catch (Throwable e) {
                        // We catch all errors from handlers
                        log.error("An error occured during event handling: {}", e.getMessage(), e);
                    }
                }
            }
        }
    }
}
