package org.apache.archiva.repository.event;

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

import org.apache.archiva.event.EventType;
import org.apache.archiva.repository.Repository;

/**
 * Repository value events are used for providing information about repository attribute changes.
 * The value event gives information of the attribute value before and after the change.
 *
 * @param <V> The type of the changed attribute
 */
public class RepositoryValueEvent<V> extends RepositoryEvent {

    private static final long serialVersionUID = 4176597620699304794L;

    public static final EventType<RepositoryValueEvent<?>> ANY = new EventType(RepositoryEvent.ANY, "REPOSITORY.VALUE");

    final V value;
    final V oldValue;
    final String attributeName;

    public RepositoryValueEvent(EventType<? extends RepositoryValueEvent<V>> type, Object origin, Repository repo, V oldValue, V value,
                                String attributeName) {
        super(type, origin, repo);
        this.value = value;
        this.oldValue = oldValue;
        this.attributeName = attributeName;
    }

    public V getValue() {
        return value;
    }

    public V getOldValue() {
        return oldValue;
    }

    public String getAttributeName() {
        return attributeName;
    }

}
