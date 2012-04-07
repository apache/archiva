package org.apache.archiva.rest.services;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.admin.model.AuditInformation;
import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.audit.AuditListener;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.security.AccessDeniedException;
import org.apache.archiva.security.ArchivaSecurityException;
import org.apache.archiva.security.PrincipalNotFoundException;
import org.apache.archiva.security.UserRepositories;
import org.apache.commons.lang.StringUtils;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.codehaus.redback.rest.services.RedbackAuthenticationThreadLocal;
import org.codehaus.redback.rest.services.RedbackRequestInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * abstract class with common utilities methods
 *
 * @author Olivier Lamy
 * @since 1.4-M1
 */
public abstract class AbstractRestService
{

    protected Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    private List<AuditListener> auditListeners = new ArrayList<AuditListener>();

    @Inject
    protected UserRepositories userRepositories;


    @Inject
    @Named( value = "repositorySessionFactory" )
    protected RepositorySessionFactory repositorySessionFactory;

    @Context
    protected HttpServletRequest httpServletRequest;

    protected AuditInformation getAuditInformation()
    {
        RedbackRequestInformation redbackRequestInformation = RedbackAuthenticationThreadLocal.get();
        User user = redbackRequestInformation == null ? null : redbackRequestInformation.getUser();
        String remoteAddr = redbackRequestInformation == null ? null : redbackRequestInformation.getRemoteAddr();
        return new AuditInformation( user, remoteAddr );
    }

    public List<AuditListener> getAuditListeners()
    {
        return auditListeners;
    }

    public void setAuditListeners( List<AuditListener> auditListeners )
    {
        this.auditListeners = auditListeners;
    }

    protected List<String> getObservableRepos()
    {
        try
        {
            List<String> ids = userRepositories.getObservableRepositoryIds( getPrincipal() );
            return ids == null ? Collections.<String>emptyList() : ids;
        }
        catch ( PrincipalNotFoundException e )
        {
            log.warn( e.getMessage(), e );
        }
        catch ( AccessDeniedException e )
        {
            log.warn( e.getMessage(), e );
        }
        catch ( ArchivaSecurityException e )
        {
            log.warn( e.getMessage(), e );
        }
        return Collections.emptyList();
    }

    protected String getPrincipal()
    {
        RedbackRequestInformation redbackRequestInformation = RedbackAuthenticationThreadLocal.get();

        return redbackRequestInformation == null
            ? UserManager.GUEST_USERNAME
            : ( redbackRequestInformation.getUser() == null
                ? UserManager.GUEST_USERNAME
                : redbackRequestInformation.getUser().getUsername() );
    }

    protected String getBaseUrl( HttpServletRequest req )
    {
        return req.getScheme() + "://" + req.getServerName() + ( req.getServerPort() == 80
            ? ""
            : ":" + req.getServerPort() ) + req.getContextPath();
    }

    protected <T> Map<String, T> getBeansOfType( ApplicationContext applicationContext, Class<T> clazz )
    {
        //TODO do some caching here !!!
        // olamy : with plexus we get only roleHint
        // as per convention we named spring bean role#hint remove role# if exists
        Map<String, T> springBeans = applicationContext.getBeansOfType( clazz );

        Map<String, T> beans = new HashMap<String, T>( springBeans.size() );

        for ( Map.Entry<String, T> entry : springBeans.entrySet() )
        {
            String key = StringUtils.substringAfterLast( entry.getKey(), "#" );
            beans.put( key, entry.getValue() );
        }
        return beans;
    }

    protected void triggerAuditEvent( String repositoryId, String filePath, String action )
    {
        AuditEvent auditEvent = new AuditEvent( repositoryId, getPrincipal(), filePath, action );
        AuditInformation auditInformation = getAuditInformation();
        auditEvent.setUserId( auditInformation.getUser() == null ? "" : auditInformation.getUser().getUsername() );
        auditEvent.setRemoteIP( auditInformation.getRemoteAddr() );
        for ( AuditListener auditListener : getAuditListeners() )
        {
            auditListener.auditEvent( auditEvent );
        }
    }
}
