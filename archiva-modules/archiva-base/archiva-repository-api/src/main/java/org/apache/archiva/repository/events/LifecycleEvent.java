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

public class LifecycleEvent extends RepositoryEvent {

    public static EventType<LifecycleEvent> ANY = new EventType<>(RepositoryEvent.ANY, "REPOSITORY.LIFECYCLE");
    public static EventType<LifecycleEvent> REGISTERED = new EventType<>(ANY, "REPOSITORY.LIFECYCLE.REGISTERED");
    public static EventType<LifecycleEvent> UNREGISTERED = new EventType<>(ANY, "REPOSITORY.LIFECYCLE.UNREGISTERED");
    public static EventType<LifecycleEvent> UPDATED = new EventType<>(ANY, "REPOSITORY.LIFECYCLE.UPDATED");

    public LifecycleEvent(EventType<? extends LifecycleEvent> type, Object origin, Repository repository) {
        super(type, origin, repository);
    }
}
