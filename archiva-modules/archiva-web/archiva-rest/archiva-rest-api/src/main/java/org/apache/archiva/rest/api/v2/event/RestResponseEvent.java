package org.apache.archiva.rest.api.v2.event;
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


import org.apache.archiva.event.Event;
import org.apache.archiva.event.EventContextBuilder;
import org.apache.archiva.event.EventType;
import org.apache.archiva.event.context.RestContext;

import java.util.List;
import java.util.Map;

/**
 * @author Martin Schreier <martin_s@apache.org>
 */
public class RestResponseEvent extends RestEvent
{
    public static EventType<RestResponseEvent> AFTER = new EventType<>( RestEvent.ANY, "REST.RESPONSE.AFTER" );

    public RestResponseEvent( EventType<? extends Event> type, Object originator,
                              String path, String service, String operation, String requestMethod, int resultCode,
                              Map<String, List<String>> pathParameter )
    {
        super( type, originator );
        EventContextBuilder builder = EventContextBuilder.withEvent( this );
        builder.witRest( path, service, operation, requestMethod, resultCode, pathParameter );
        builder.apply( );
    }

    @Override
    public RestContext getContext( )
    {
        return getContext( RestContext.class );
    }
}
