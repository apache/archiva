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
import org.apache.archiva.event.EventContextBuilder;
import org.apache.archiva.event.EventType;
import org.apache.archiva.event.context.RepositoryContext;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryGroup;

/**
 * A repository event is specific to a repository and holds a reference to the repository that
 * is related to this event.
 */
public class RepositoryEvent extends Event<RepositoryContext>
{

    private static final long serialVersionUID = 4676673476606414834L;

    public static final EventType<RepositoryEvent> ANY = new EventType<>(Event.ANY, "REPOSITORY");

    private final Repository repository;

    public RepositoryEvent(EventType<? extends RepositoryEvent> type, Object origin, Repository repository) {
        super(type, origin);
        this.repository = repository;
        EventContextBuilder builder = EventContextBuilder.withEvent( this );
        if (repository!=null)
        {
            builder.withRepository( repository.getId( ), repository.getType( ).name( ), getFlavour( repository ) );
        }
        builder.apply( );
    }

    public Repository getRepository() {
        return repository;
    }

    @Override
    public RepositoryContext getContext() {
        return getContext( RepositoryContext.class );
    }

    @Override
    public void setContext( RepositoryContext context )
    {
        setContext( RepositoryContext.class, context );
    }

    @Override
    public EventType<? extends RepositoryEvent> getType() {
        return (EventType<? extends RepositoryEvent>) super.getType();
    }

    private String getFlavour(Repository repository) {
        if (repository instanceof RemoteRepository ) {
            return RemoteRepository.class.getName( );
        } else if (repository instanceof ManagedRepository ) {
            return ManagedRepository.class.getName( );
        } else if ( repository instanceof RepositoryGroup ) {
            return RepositoryGroup.class.getName( );
        } else {
            return "UNKNOWN";
        }
    }
}
