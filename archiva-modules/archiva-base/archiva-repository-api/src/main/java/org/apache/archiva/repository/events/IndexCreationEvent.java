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

import java.net.URI;

public class IndexCreationEvent extends RepositoryValueEvent<URI> {

    public static EventType<IndexCreationEvent> ANY = new EventType<>(RepositoryValueEvent.ANY, "REPOSITORY.VALUE.INDEX");
    public static EventType<IndexCreationEvent> INDEX_URI_CHANGED = new EventType<>(ANY, "REPOSITORY.VALUE.INDEX.URI_CHANGED");
    public static EventType<IndexCreationEvent> PACKED_INDEX_URI_CHANGED = new EventType<>(ANY, "REPOSITORY.VALUE.INDEX.PACKED_URI_CHANGED");

    IndexCreationEvent(EventType<? extends IndexCreationEvent> type, Object origin, Repository repo, URI oldValue, URI value) {
        super(type, origin, repo, oldValue, value);
    }

    public static final <O> IndexCreationEvent indexUriChange(O origin, Repository repo, URI oldValue, URI newValue) {
        return new IndexCreationEvent(INDEX_URI_CHANGED, origin, repo, oldValue, newValue);
    }

    public static final <O> IndexCreationEvent packedIndexUriChange(O origin, Repository repo, URI oldValue, URI newValue) {
        return new IndexCreationEvent(PACKED_INDEX_URI_CHANGED, origin, repo, oldValue, newValue);
    }
}
