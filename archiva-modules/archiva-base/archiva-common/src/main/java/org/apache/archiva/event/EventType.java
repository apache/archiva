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

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.*;

/**
 * Event types define a hierarchical structure of events. Each event is bound to a certain event type.
 * All event types have a super type, only the root event type {@link EventType#ROOT} has no super type.
 *
 * Event types should be stored as static fields on the events itself.
 *
 * @param <T> The type class parameter allows to define the types in a type safe way and represents a event class,
 *           where the type is associated to.
 */
public class EventType<T extends Event> implements Serializable  {


    public static final EventType<Event> ROOT = new EventType<>();

    private final String name;
    private final EventType<? super T> superType;
    private WeakHashMap<EventType<? extends T>, Void> subTypes;

    /**
     * Creates a type with the given name and the root type as parent.
     * @param name the name of the new type
     */
    public EventType(String name) {
        this.superType = ROOT;
        this.name = name;
    }

    /**
     * Creates a event type instance with the given super type and name.
     *
     * @param superType The super type or <code>null</code>, if this is the root type.
     * @param name
     */
    public EventType(EventType<? super T> superType, String name) {
        if (superType==null) {
            throw new NullPointerException("Super Type may not be null");
        }
        this.name = name;
        this.superType = superType;
        superType.register(this);
    }

    /**
     * Creates the root type
     */
    private EventType() {
        this.name="ROOT";
        this.superType=null;
    }

    public String name() {
        return name;
    }

    public EventType<? super T> getSuperType() {
        return superType;
    }

    private void register(EventType<? extends T> subType) {
        if (subTypes == null) {
            subTypes = new WeakHashMap<>();
        }
        for (EventType<? extends T> t : subTypes.keySet()) {
            if (((t.name == null && subType.name == null) || (t.name != null && t.name.equals(subType.name)))) {
                throw new IllegalArgumentException("EventType \"" + subType + "\""
                        + "with parent \"" + subType.getSuperType()+"\" already exists");
            }
        }
        subTypes.put(subType, null);
    }


    public static List<EventType<?>> fetchSuperTypes(EventType<?> type) {
        List<EventType<?>> typeList = new ArrayList<>();
        EventType<?> cType = type;
        while (cType!=null) {
            typeList.add(cType);
            cType = cType.getSuperType();
        }
        return typeList;
    }

    public static boolean isInstanceOf(EventType<?> type, EventType<?> baseType) {
        EventType<?> cType = type;
        while(cType!=null) {
            if (cType == baseType) {
                return true;
            }
            cType = cType.getSuperType();
        }
        return false;
    }


    private Object writeReplace() throws ObjectStreamException {
        Deque<String> path = new LinkedList<>();
        EventType<?> t = this;
        while (t != ROOT) {
            path.addFirst(t.name);
            t = t.superType;
        }
        return new EventTypeSerialization(new ArrayList<>(path));
    }

    static class EventTypeSerialization implements Serializable {
        private static final long serialVersionUID = 1841649460281865547L;
        private List<String> path;

        public EventTypeSerialization(List<String> path) {
            this.path = path;
        }

        private Object readResolve() throws ObjectStreamException {
            EventType t = ROOT;
            for (int i = 0; i < path.size(); ++i) {
                String p = path.get(i);
                if (t.subTypes != null) {
                    EventType<?> s = findSubType(t.subTypes.keySet(), p);
                    if (s == null) {
                        throw new InvalidObjectException("Cannot find event type \"" + p + "\" (of " + t + ")");
                    }
                    t = s;
                } else {
                    throw new InvalidObjectException("Cannot find event type \"" + p + "\" (of " + t + ")");
                }
            }
            return t;
        }

        private EventType<?> findSubType(Set<EventType> subTypes, String name) {
            for (EventType t : subTypes) {
                if (((t.name == null && name == null) || (t.name != null && t.name.equals(name)))) {
                    return t;
                }
            }
            return null;
        }

    }
}
