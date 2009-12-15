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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.maven.archiva.database.ArchivaAuditLogsDao;
import org.apache.maven.archiva.model.ArchivaAuditLogs;
import org.apache.maven.archiva.repository.audit.AuditEvent;
import org.apache.maven.archiva.repository.audit.AuditListener;
import org.apache.maven.archiva.repository.audit.Auditable;
import org.apache.maven.archiva.security.ArchivaXworkUser;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.interceptor.SessionAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

/**
 * LogEnabled and SessionAware ActionSupport
 */
public abstract class PlexusActionSupport
    extends ActionSupport
    implements SessionAware, Auditable
{
    protected Map<?, ?> session;

    protected Logger log = LoggerFactory.getLogger( getClass() );

    /**
     * @plexus.requirement role="org.apache.maven.archiva.repository.audit.AuditListener"
     */
    private List<AuditListener> auditListeners = new ArrayList<AuditListener>();

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaAuditLogsDao auditLogsDao;    

    private String principal;

    @SuppressWarnings("unchecked")
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
        
        ArchivaAuditLogs auditLogs = new ArchivaAuditLogs();
        auditLogs.setArtifact( resource );
        auditLogs.setEvent( action );
        auditLogs.setEventDate( Calendar.getInstance().getTime() );
        auditLogs.setRepositoryId( repositoryId );
        auditLogs.setUsername( getPrincipal() );
        
        auditLogsDao.saveAuditLogs( auditLogs );
    }

    protected void triggerAuditEvent( String resource, String action )
    {
        AuditEvent event = new AuditEvent( getPrincipal(), resource, action );
        event.setRemoteIP( getRemoteAddr() );
        
        for ( AuditListener listener : auditListeners )
        {
            listener.auditEvent( event );
        }
    }

    protected void triggerAuditEvent( String action )
    {
        AuditEvent event = new AuditEvent( getPrincipal(), action );
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
    
    public void setAuditLogsDao( ArchivaAuditLogsDao auditLogsDao )
    {
        this.auditLogsDao = auditLogsDao;
    }
}
