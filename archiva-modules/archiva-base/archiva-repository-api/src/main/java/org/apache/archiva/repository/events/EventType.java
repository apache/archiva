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

import java.util.ArrayList;
import java.util.List;

public class EventType<T extends Event> {

    private final String name;
    private final EventType<? super T> superType;

    public EventType(EventType<? super T> superType, String name) {
        this.name = name;
        this.superType = superType;
    }

    public String name() {
        return name;
    }

    public EventType<? super T> getSuperType() {
        return superType;
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
}
