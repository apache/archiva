package org.apache.archiva.rest.v2.interceptor;
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

import org.apache.archiva.admin.model.AuditInformation;
import org.apache.archiva.event.AbstractEventManager;
import org.apache.archiva.event.Event;
import org.apache.archiva.event.EventContextBuilder;
import org.apache.archiva.event.EventHandler;
import org.apache.archiva.event.EventSource;
import org.apache.archiva.event.EventType;
import org.apache.archiva.event.context.RestContext;
import org.apache.archiva.redback.rest.services.RedbackAuthenticationThreadLocal;
import org.apache.archiva.redback.rest.services.RedbackRequestInformation;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.rest.api.v2.event.RestRequestEvent;
import org.apache.archiva.rest.api.v2.event.RestResponseEvent;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import javax.annotation.Priority;


/**
 * @author Martin Schreier <martin_s@apache.org>
 */
@Provider
@Service( "restInterceptor#audit" )
@Priority( Priorities.AUDIT )
public class AuditInterceptor extends AbstractEventManager implements ContainerRequestFilter, ContainerResponseFilter, EventSource
{
    @Context
    ResourceInfo resourceInfo;

    protected void addAuditInformation( Event<RestContext> evt )
    {
        RedbackRequestInformation redbackRequestInformation = RedbackAuthenticationThreadLocal.get();
        String user = redbackRequestInformation == null ? "" : redbackRequestInformation.getUser().getUsername();
        String remoteAddr = redbackRequestInformation == null ? "" : redbackRequestInformation.getRemoteAddr();
        EventContextBuilder.withEvent( evt ).withUser( user, remoteAddr ).apply();
    }

    @Override
    public void filter( ContainerRequestContext requestContext, ContainerResponseContext responseContext ) throws IOException
    {
        RestResponseEvent evt = new RestResponseEvent( RestResponseEvent.AFTER,
            this, requestContext.getUriInfo( ).getPath( ), resourceInfo.getResourceClass( ).getName( ), resourceInfo.getResourceMethod( ).getName( )
            , requestContext.getMethod( ), responseContext.getStatus( ), requestContext.getUriInfo( ).getPathParameters( ) );
        addAuditInformation( evt );
        fireEvent( evt, this );
    }

    @Override
    public void filter( ContainerRequestContext requestContext ) throws IOException
    {
        RestRequestEvent evt = new RestRequestEvent( RestRequestEvent.BEFORE, this,
            requestContext.getUriInfo().getPath(), resourceInfo.getResourceClass().getName(),  resourceInfo.getResourceMethod().getName()
            , requestContext.getMethod(), requestContext.getUriInfo().getPathParameters() );
        addAuditInformation( evt );
        fireEvent( evt, this );
    }

}
