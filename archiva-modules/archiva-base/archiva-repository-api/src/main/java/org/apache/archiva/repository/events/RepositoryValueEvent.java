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

import org.apache.archiva.repository.Repository;

/**
 * Repository event. Repository events are used for providing information about repository changes.
 *
 * @param <V>
 */
public class RepositoryValueEvent<V> extends RepositoryEvent {

    public static final EventType<RepositoryValueEvent<?>> ANY = new EventType(RepositoryEvent.ANY, "REPOSITORY.VALUE.UPDATED");

    final V value;
    final V oldValue;

    public RepositoryValueEvent(EventType<? extends RepositoryValueEvent<V>> type, Object origin, Repository repo, V oldValue, V value) {
        super(type, origin, repo);
        this.value = value;
        this.oldValue = oldValue;
    }

    public V getValue() {
        return value;
    }

    public V getOldValue() {
        return oldValue;
    }

}
