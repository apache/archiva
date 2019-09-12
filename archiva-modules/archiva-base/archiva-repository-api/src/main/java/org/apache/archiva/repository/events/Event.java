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

public class Event<O> {

    Event previous;
    final O originator;
    final EventType type;
    final LocalDateTime instant;

    public Event(EventType type, O originator) {
        this.originator = originator;
        this.type = type;
        this.instant = LocalDateTime.now();
    }

    private <OO> Event(Event<OO> previous, O originator) {
        this.previous = previous;
        this.originator = originator;
        this.type = previous.getType();
        this.instant = previous.getInstant();
    }

    public EventType getType() {
        return type;
    };

    public LocalDateTime getInstant() {
        return instant;
    }

    public O getOriginator() {
        return originator;
    }

    public <NO> Event<NO> recreate(NO newOrigin) {
        return new Event(this, newOrigin);
    }

    public Event getPreviousEvent() {
        return previous;
    }

    public boolean hasPreviousEvent() {
        return previous!=null;
    }
}
