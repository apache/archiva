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

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;
import org.apache.archiva.admin.AuditInformation;
import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.audit.AuditListener;
import org.apache.archiva.audit.Auditable;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.archiva.security.ArchivaXworkUser;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.interceptor.SessionAware;
import org.codehaus.plexus.redback.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
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

    @Inject
    private List<AuditListener> auditListeners = new ArrayList<AuditListener>();


    @Inject
    @Named( value = "repositorySessionFactory" )
    protected RepositorySessionFactory repositorySessionFactory;

    @Inject
    protected ApplicationContext applicationContext;

    private String principal;

    @PostConstruct
    public void initialize()
    {
        // no op
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


    protected AuditInformation getAuditInformation()
    {
        AuditInformation auditInformation = new AuditInformation( new SimpleUser( getPrincipal() ), getRemoteAddr() );

        return auditInformation;
    }

    /**
     * dummy information for audit events
     * @since 1.4
     */
    private static class SimpleUser
        implements User
    {

        private String principal;

        protected SimpleUser( String principal )
        {
            this.principal = principal;
        }

        public Object getPrincipal()
        {
            return this.principal;
        }

        public String getUsername()
        {
            return null;
        }

        public void setUsername( String name )
        {

        }

        public String getFullName()
        {
            return null;
        }

        public void setFullName( String name )
        {

        }

        public String getEmail()
        {
            return null;
        }

        public void setEmail( String address )
        {

        }

        public String getPassword()
        {
            return null;
        }

        public void setPassword( String rawPassword )
        {

        }

        public String getEncodedPassword()
        {
            return null;
        }

        public void setEncodedPassword( String encodedPassword )
        {

        }

        public Date getLastPasswordChange()
        {
            return null;
        }

        public void setLastPasswordChange( Date passwordChangeDate )
        {

        }

        public List<String> getPreviousEncodedPasswords()
        {
            return null;
        }

        public void setPreviousEncodedPasswords( List<String> encodedPasswordList )
        {

        }

        public void addPreviousEncodedPassword( String encodedPassword )
        {

        }

        public boolean isPermanent()
        {
            return false;
        }

        public void setPermanent( boolean permanent )
        {

        }

        public boolean isLocked()
        {
            return false;
        }

        public void setLocked( boolean locked )
        {

        }

        public boolean isPasswordChangeRequired()
        {
            return false;
        }

        public void setPasswordChangeRequired( boolean changeRequired )
        {

        }

        public boolean isValidated()
        {
            return false;
        }

        public void setValidated( boolean valid )
        {

        }

        public int getCountFailedLoginAttempts()
        {
            return 0;
        }

        public void setCountFailedLoginAttempts( int count )
        {

        }

        public Date getAccountCreationDate()
        {
            return null;
        }

        public void setAccountCreationDate( Date date )
        {

        }

        public Date getLastLoginDate()
        {
            return null;
        }

        public void setLastLoginDate( Date date )
        {

        }
    }


}
