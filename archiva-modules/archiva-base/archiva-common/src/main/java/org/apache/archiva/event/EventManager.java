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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EventManager implements EventSource
{

    private static final Logger LOG = LoggerFactory.getLogger(EventManager.class);

    private final ConcurrentHashMap<EventType<? extends Event>, Set<EventHandler>> handlerMap = new ConcurrentHashMap<>();

    private final Object source;

    public EventManager(Object source) {
        if (source==null) {
            throw new IllegalArgumentException("The source may not be null");
        }
        this.source = source;
    }

    @Override
    public <T extends Event> void registerEventHandler(EventType<T> type, EventHandler<? super T> eventHandler) {
        Set<EventHandler> handlers = handlerMap.computeIfAbsent(type, t -> new LinkedHashSet<>());
        if (!handlers.contains(eventHandler)) {
            handlers.add(eventHandler);
        }
    }

    @Override
    public <T extends Event> void unregisterEventHandler(EventType<T> type, EventHandler<? super T> eventHandler) {
        if (handlerMap.containsKey(type)) {
            handlerMap.get(type).remove(eventHandler);
        }
    }

    public void fireEvent(Event fireEvent) {
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
                        } catch (Exception e) {
                            // We catch all errors from handlers
                            LOG.error("An error occured during event handling: {}", e.getMessage(), e);
                        }
                    }
            }
        }
    }
}
