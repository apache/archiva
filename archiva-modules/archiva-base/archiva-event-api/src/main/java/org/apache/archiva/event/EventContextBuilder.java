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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.event.context.RepositoryContext;
import org.apache.archiva.event.context.RestContext;
import org.apache.archiva.event.context.UserContext;

import java.util.List;
import java.util.Map;

/**
 * Static helper class that allows to set certain context data
 *
 * @author Martin Schreier <martin_s@apache.org>
 */
public class EventContextBuilder
{
    Event evt;

    public static void setUserContext(Event evt, String user, String remoteAddress) {
        evt.setContext( UserContext.class, new UserContext( user, remoteAddress ) );
    }

    public static void setRestContext( Event evt,  String path, String service, String operation,
                                       String requestMethod, int resultCode, Map<String, List<String>> pathParameter) {
        evt.setContext( RestContext.class, new RestContext( path, service, operation, requestMethod, resultCode, pathParameter ) );
    }

    public static void setRepositoryContext(Event evt, String id, String type, String flavour ) {
        evt.setContext( RepositoryContext.class, new RepositoryContext( id, type, flavour ) );
    }

    private EventContextBuilder( Event evt) {
        this.evt = evt;
    }

    public static EventContextBuilder withEvent( Event evt )
    {
        return new EventContextBuilder( evt );
    }

    public EventContextBuilder withUser( String user, String remoteAddress) {
        setUserContext( this.evt, user, remoteAddress );
        return this;
    }

    public EventContextBuilder witRest( String path, String service, String operation, String requestMethod,
        int resultCode, Map<String,List<String>> pathParameter) {
        setRestContext( this.evt, path, service,  operation, requestMethod, resultCode, pathParameter );
        return this;
    }

    public EventContextBuilder withRepository(String id, String type, String flavour) {
        setRepositoryContext( this.evt, id, type, flavour );
        return this;
    }

    public Event apply() {
        return this.evt;
    }
}
