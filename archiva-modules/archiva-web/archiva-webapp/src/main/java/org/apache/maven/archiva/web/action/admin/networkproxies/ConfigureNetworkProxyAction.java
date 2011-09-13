package org.apache.maven.archiva.web.action.admin.networkproxies;

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

import com.opensymphony.xwork2.Preparable;
import com.opensymphony.xwork2.Validateable;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.networkproxy.NetworkProxy;
import org.apache.archiva.admin.model.networkproxy.NetworkProxyAdmin;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.web.action.AbstractActionSupport;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.redback.integration.interceptor.SecureAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;

/**
 * ConfigureNetworkProxyAction
 *
 * @version $Id$
 */
@Controller( "configureNetworkProxyAction" )
@Scope( "prototype" )
public class ConfigureNetworkProxyAction
    extends AbstractActionSupport
    implements SecureAction, Preparable, Validateable
{

    @Inject
    private NetworkProxyAdmin networkProxyAdmin;

    private String mode;

    private String proxyid;

    private NetworkProxy proxy;

    public String add()
    {
        this.mode = "add";
        return INPUT;
    }

    public String confirm()
    {
        return INPUT;
    }

    public String delete()
        throws RepositoryAdminException
    {

        String id = getProxyid();
        if ( StringUtils.isBlank( id ) )
        {
            addActionError( "Unable to delete network proxy with blank id." );
            return SUCCESS;
        }

        NetworkProxy networkProxy = getNetworkProxyAdmin().getNetworkProxy( id );
        if ( networkProxy == null )
        {
            addActionError( "Unable to remove network proxy, proxy with id [" + id + "] not found." );
            return SUCCESS;
        }

        getNetworkProxyAdmin().deleteNetworkProxy( id, getAuditInformation() );
        addActionMessage( "Successfully removed network proxy [" + id + "]" );
        return SUCCESS;
    }

    public String edit()
    {
        this.mode = "edit";
        return INPUT;
    }

    public String getMode()
    {
        return mode;
    }

    public NetworkProxy getProxy()
    {
        return proxy;
    }

    public String getProxyid()
    {
        return proxyid;
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }

    public String input()
    {
        return INPUT;
    }

    public void prepare()
        throws Exception
    {
        String id = getProxyid();

        if ( StringUtils.isNotBlank( id ) )
        {
            proxy = findNetworkProxy( id );
        }

        if ( proxy == null )
        {
            proxy = new NetworkProxy();
        }
    }

    public String save()
        throws RepositoryAdminException
    {
        String mode = getMode();

        String id = getProxy().getId();

        if ( StringUtils.equalsIgnoreCase( "edit", mode ) )
        {
            getNetworkProxyAdmin().updateNetworkProxy( proxy, getAuditInformation() );
        }
        else
        {
            getNetworkProxyAdmin().addNetworkProxy( proxy, getAuditInformation() );
        }

        return SUCCESS;
    }

    public void validate()
    {
        // trim all unecessary trailing/leading white-spaces; always put this statement before the closing braces(after all validation).
        trimAllRequestParameterValues();
    }

    public void setMode( String mode )
    {
        this.mode = mode;
    }

    public void setProxy( NetworkProxy proxy )
    {
        this.proxy = proxy;
    }

    public void setProxyid( String proxyid )
    {
        this.proxyid = proxyid;
    }


    private NetworkProxy findNetworkProxy( String id )
        throws RepositoryAdminException
    {
        return getNetworkProxyAdmin().getNetworkProxy( id );
    }

    private void removeNetworkProxy( String id )
        throws RepositoryAdminException
    {
        getNetworkProxyAdmin().deleteNetworkProxy( id, getAuditInformation() );
    }


    private void trimAllRequestParameterValues()
    {
        if ( StringUtils.isNotEmpty( proxy.getId() ) )
        {
            proxy.setId( proxy.getId().trim() );
        }

        if ( StringUtils.isNotEmpty( proxy.getHost() ) )
        {
            proxy.setHost( proxy.getHost().trim() );
        }

        if ( StringUtils.isNotEmpty( proxy.getPassword() ) )
        {
            proxy.setPassword( proxy.getPassword().trim() );
        }

        if ( StringUtils.isNotEmpty( proxy.getProtocol() ) )
        {
            proxy.setProtocol( proxy.getProtocol().trim() );
        }

        if ( StringUtils.isNotEmpty( proxy.getUsername() ) )
        {
            proxy.setUsername( proxy.getUsername().trim() );
        }
    }

    public NetworkProxyAdmin getNetworkProxyAdmin()
    {
        return networkProxyAdmin;
    }

    public void setNetworkProxyAdmin( NetworkProxyAdmin networkProxyAdmin )
    {
        this.networkProxyAdmin = networkProxyAdmin;
    }
}

