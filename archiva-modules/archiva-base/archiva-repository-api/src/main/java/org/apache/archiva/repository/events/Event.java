package org.apache.archiva.repository.events;

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

import java.time.LocalDateTime;
import java.util.EventObject;

public class Event extends EventObject {

    public static final EventType<Event> ANY = new EventType(null, "ANY");

    Event previous;
    final Object originator;
    final EventType<? extends Event> type;
    final LocalDateTime instant;

    public Event(EventType<? extends Event> type, Object originator) {
        super(originator);
        this.originator = originator;
        this.type = type;
        this.instant = LocalDateTime.now();
    }

    private Event(Event previous, Object originator) {
        super(originator);
        this.previous = previous;
        this.originator = originator;
        this.type = previous.getType();
        this.instant = previous.getInstant();
    }

    public EventType<? extends Event> getType() {
        return type;
    };

    public LocalDateTime getInstant() {
        return instant;
    }

    public Object getOriginator() {
        return originator;
    }

    public Event recreate(Object newOrigin) {
        return new Event(this, newOrigin);
    }

    public Event getPreviousEvent() {
        return previous;
    }

    public boolean hasPreviousEvent() {
        return previous!=null;
    }
}
