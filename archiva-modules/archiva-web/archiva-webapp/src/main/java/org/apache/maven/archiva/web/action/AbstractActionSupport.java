package org.apache.maven.archiva.web.action;

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

import com.google.common.collect.Lists;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;
import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.audit.AuditListener;
import org.apache.archiva.audit.Auditable;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.security.ArchivaXworkUser;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.interceptor.SessionAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LogEnabled and SessionAware ActionSupport
 */
public abstract class AbstractActionSupport
    extends ActionSupport
    implements SessionAware, Auditable
{
    protected Map<?, ?> session;

    protected Logger log = LoggerFactory.getLogger( getClass() );

    /**
     * plexus.requirement role="org.apache.archiva.audit.AuditListener"
     */
    private List<AuditListener> auditListeners = new ArrayList<AuditListener>();

    /**
     * plexus.requirement
     */
    @Inject
    protected RepositorySessionFactory repositorySessionFactory;

    @Inject
    protected ApplicationContext applicationContext;

    private String principal;

    @PostConstruct
    public void initialize()
    {
        // TODO some caching here
        this.auditListeners = Lists.newArrayList( applicationContext.getBeansOfType( AuditListener.class ).values() );
    }

    @SuppressWarnings( "unchecked" )
    public void setSession( Map map )
    {
        this.session = map;
    }

    public void addAuditListener( AuditListener listener )
    {
        this.auditListeners.add( listener );
    }

    public void clearAuditListeners()
    {
        this.auditListeners.clear();
    }

    public void removeAuditListener( AuditListener listener )
    {
        this.auditListeners.remove( listener );
    }

    protected void triggerAuditEvent( String repositoryId, String resource, String action )
    {
        AuditEvent event = new AuditEvent( repositoryId, getPrincipal(), resource, action );
        event.setRemoteIP( getRemoteAddr() );

        for ( AuditListener listener : auditListeners )
        {
            listener.auditEvent( event );
        }
    }

    protected void triggerAuditEvent( String resource, String action )
    {
        AuditEvent event = new AuditEvent( null, getPrincipal(), resource, action );
        event.setRemoteIP( getRemoteAddr() );

        for ( AuditListener listener : auditListeners )
        {
            listener.auditEvent( event );
        }
    }

    protected void triggerAuditEvent( String action )
    {
        AuditEvent event = new AuditEvent( null, getPrincipal(), null, action );
        event.setRemoteIP( getRemoteAddr() );

        for ( AuditListener listener : auditListeners )
        {
            listener.auditEvent( event );
        }
    }

    private String getRemoteAddr()
    {
        HttpServletRequest request = ServletActionContext.getRequest();
        return request != null ? request.getRemoteAddr() : null;
    }

    @SuppressWarnings( "unchecked" )
    protected String getPrincipal()
    {
        if ( principal != null )
        {
            return principal;
        }
        return ArchivaXworkUser.getActivePrincipal( ActionContext.getContext().getSession() );
    }

    void setPrincipal( String principal )
    {
        this.principal = principal;
    }

    public void setAuditListeners( List<AuditListener> auditListeners )
    {
        this.auditListeners = auditListeners;
    }

    public void setRepositorySessionFactory( RepositorySessionFactory repositorySessionFactory )
    {
        this.repositorySessionFactory = repositorySessionFactory;
    }

    protected <T> Map<String, T> getBeansOfType( Class<T> clazz )
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
}
