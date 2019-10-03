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

import java.net.URI;

/**
 * These events are thrown, when index information has changed.
 */
public class RepositoryIndexEvent extends RepositoryValueEvent<URI> {

    private static final long serialVersionUID = -7801989699524776524L;

    public static EventType<RepositoryIndexEvent> ANY = new EventType<>(RepositoryValueEvent.ANY, "REPOSITORY.VALUE.INDEX");
    public static EventType<RepositoryIndexEvent> INDEX_URI_CHANGED = new EventType<>(ANY, "REPOSITORY.VALUE.INDEX.URI_CHANGED");
    public static EventType<RepositoryIndexEvent> PACKED_INDEX_URI_CHANGED = new EventType<>(ANY, "REPOSITORY.VALUE.INDEX.PACKED_URI_CHANGED");

    RepositoryIndexEvent(EventType<? extends RepositoryIndexEvent> type, Object origin, Repository repo, URI oldValue, URI value) {
        super(type, origin, repo, oldValue, value, "index.uri");
    }

    public static final <O> RepositoryIndexEvent indexUriChange(O origin, Repository repo, URI oldValue, URI newValue) {
        return new RepositoryIndexEvent(INDEX_URI_CHANGED, origin, repo, oldValue, newValue);
    }

    public static final <O> RepositoryIndexEvent packedIndexUriChange(O origin, Repository repo, URI oldValue, URI newValue) {
        return new RepositoryIndexEvent(PACKED_INDEX_URI_CHANGED, origin, repo, oldValue, newValue);
    }
}
