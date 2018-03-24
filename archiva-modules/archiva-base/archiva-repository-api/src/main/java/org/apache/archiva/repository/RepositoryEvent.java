package org.apache.archiva.repository;

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

/**
 * Repository event. Repository events are used for providing information about repository changes.
 *
 * @param <T>
 */
public class RepositoryEvent<T> {

    final EventType type;
    final String repo;
    final T value;
    final T oldValue;
    final LocalDateTime instant;

    public RepositoryEvent(EventType type, String repo, T oldValue, T value) {
        this.type = type;
        this.repo = repo;
        this.value = value;
        this.oldValue = oldValue;
        this.instant = LocalDateTime.now();
    }

    public interface EventType {
        String name();
    }


    EventType getType() {
        return type;
    };

    String getRepositoryId() {
        return repo;
    };

    T getValue() {
        return value;
    }

    T getOldValue() {
        return oldValue;
    }

    public LocalDateTime getInstant() {
        return instant;
    }
}
