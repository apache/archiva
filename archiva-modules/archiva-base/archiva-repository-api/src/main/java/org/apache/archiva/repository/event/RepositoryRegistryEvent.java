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

import org.apache.archiva.event.Event;
import org.apache.archiva.event.EventType;

/**
 * Repository registry events are raised by the repository registry itself.
 */
public class RepositoryRegistryEvent extends Event
{

    private static final long serialVersionUID = -4740127827269612094L;

    /**
     * All repository registry events
     */
    public static EventType<RepositoryRegistryEvent> ANY = new EventType(EventType.ROOT, "REGISTRY");
    /**
     * When the registry has reloaded the registry data from the configuration
     */
    public static EventType<RepositoryRegistryEvent> RELOADED = new EventType(ANY, "REGISTRY.RELOADED");
    /**
     * When the registry was destroyed. Repository instances may still be referenced, but are not updated.
     */
    public static EventType<RepositoryRegistryEvent> DESTROYED = new EventType(ANY, "REGISTRY.DESTROYED");
    /**
     * When the registry was initialized
     */
    public static EventType<RepositoryRegistryEvent> INITIALIZED = new EventType(ANY, "REGISTRY.INITIALIZED");

    public RepositoryRegistryEvent(EventType<? extends RepositoryRegistryEvent> type, Object origin) {
        super(type, origin);
    }
}
