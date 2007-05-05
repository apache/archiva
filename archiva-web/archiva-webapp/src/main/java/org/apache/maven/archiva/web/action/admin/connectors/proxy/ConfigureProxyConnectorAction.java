package org.apache.maven.archiva.web.action.admin.connectors.proxy;

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

import com.opensymphony.xwork.Preparable;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.functors.IfClosure;
import org.apache.commons.collections.functors.NotPredicate;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.InvalidConfigurationException;
import org.apache.maven.archiva.configuration.NetworkProxyConfiguration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.configuration.util.ProxyConnectorSelectionPredicate;
import org.apache.maven.archiva.configuration.util.RemoteRepositoryPredicate;
import org.apache.maven.archiva.configuration.util.RepositoryIdListClosure;
import org.apache.maven.archiva.policies.DownloadPolicy;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.plexus.security.rbac.Resource;
import org.codehaus.plexus.security.ui.web.interceptor.SecureAction;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionBundle;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionException;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * ConfigureProxyConnectorAction 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="configureProxyConnectorAction"
 */
public class ConfigureProxyConnectorAction
    extends PlexusActionSupport
    implements SecureAction, Preparable, Initializable
{
    private static final String DIRECT_CONNECTION = "(direct connection)";

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.policies.PreDownloadPolicy"
     */
    private Map preDownloadPolicyMap;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.policies.PostDownloadPolicy"
     */
    private Map postDownloadPolicyMap;

    /**
     * The model for this action.
     */
    private ProxyConnectorConfiguration connector;

    private Map policyMap;

    private String source;

    private String target;

    private String mode;

    private String propertyKey;

    private String propertyValue;

    private String pattern;

    /**
     * The list of possible proxy ids. 
     */
    private List proxyIdOptions = new ArrayList();

    /**
     * The list of local repository ids.
     */
    private List localRepoIdList = new ArrayList();

    /**
     * The list of remote repository ids.
     */
    private List remoteRepoIdList = new ArrayList();

    /**
     * The blacklist pattern to add.
     */
    private String blackListPattern;

    /**
     * The whitelist pattern to add.
     */
    private String whiteListPattern;

    public String add()
    {
        getLogger().info( ".add()" );
        this.mode = "add";
        return INPUT;
    }

    public String confirm()
    {
        getLogger().info( ".confirm()" );
        return INPUT;
    }

    public String delete()
    {
        getLogger().info( ".delete()" );
        return INPUT;
    }

    public String addProperty()
    {
        getLogger().info( ".addProperty()" );
        String key = getPropertyKey();
        String value = getPropertyValue();

        if ( StringUtils.isBlank( key ) )
        {
            addActionError( "Unable to add property with blank key." );
        }

        if ( StringUtils.isBlank( value ) )
        {
            addActionError( "Unable to add property with blank value." );
        }

        if ( !hasActionErrors() )
        {
            getConnector().getProperties().put( key, value );
            setPropertyKey( null );
            setPropertyValue( null );
        }

        return INPUT;
    }

    public String removeProperty()
    {
        getLogger().info( ".removeProperty()" );
        String key = getPropertyKey();

        if ( StringUtils.isBlank( key ) )
        {
            addActionError( "Unable to remove property with blank key." );
        }

        if ( !hasActionErrors() )
        {
            getConnector().getProperties().remove( key );
            setPropertyKey( null );
            setPropertyValue( null );
        }

        return INPUT;
    }

    public String addWhiteListPattern()
    {
        getLogger().info( ".addWhiteListPattern()" );
        String pattern = getWhiteListPattern();

        if ( StringUtils.isBlank( pattern ) )
        {
            addActionError( "Cannot add an blank white list pattern." );
        }

        if ( !hasActionErrors() )
        {
            getLogger().info(
                              "whitelist patterns: (" + getConnector().getWhiteListPatterns().size() + "): "
                                  + getConnector().getWhiteListPatterns() );

            getConnector().getWhiteListPatterns().add( pattern );
            setWhiteListPattern( null );
        }

        return INPUT;
    }

    public String removeWhiteListPattern()
    {
        String pattern = getPattern();
        getLogger().info( ".removeWhiteListPattern(" + pattern + ")" );

        if ( StringUtils.isBlank( pattern ) )
        {
            addActionError( "Cannot remove an blank white list pattern." );
        }

        if ( !hasActionErrors() )
        {
            getConnector().getWhiteListPatterns().remove( pattern );
            setWhiteListPattern( null );
        }

        return INPUT;
    }

    public String addBlackListPattern()
    {
        getLogger().info( ".addBlackListPattern()" );
        String pattern = getBlackListPattern();

        if ( StringUtils.isBlank( pattern ) )
        {
            addActionError( "Cannot add an blank black list pattern." );
        }

        if ( !hasActionErrors() )
        {
            getConnector().getBlackListPatterns().add( pattern );
            setBlackListPattern( null );
        }

        return INPUT;
    }

    public String removeBlackListPattern()
    {
        getLogger().info( ".removeBlackListPattern()" );
        String pattern = getBlackListPattern();

        if ( StringUtils.isBlank( pattern ) )
        {
            addActionError( "Cannot remove an blank black list pattern." );
        }

        if ( !hasActionErrors() )
        {
            getConnector().getBlackListPatterns().remove( pattern );
            setBlackListPattern( null );
        }

        return INPUT;
    }

    public String edit()
    {
        getLogger().info( ".edit()" );
        this.mode = "edit";
        return INPUT;
    }

    public String getBlackListPattern()
    {
        return blackListPattern;
    }

    public ProxyConnectorConfiguration getConnector()
    {
        return connector;
    }

    public List getLocalRepoIdList()
    {
        return localRepoIdList;
    }

    public String getMode()
    {
        return this.mode;
    }

    public Map getPolicyMap()
    {
        return policyMap;
    }

    public String getPropertyKey()
    {
        return propertyKey;
    }

    public String getPropertyValue()
    {
        return propertyValue;
    }

    public List getProxyIdOptions()
    {
        return proxyIdOptions;
    }

    public List getRemoteRepoIdList()
    {
        return remoteRepoIdList;
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }

    public String getSource()
    {
        return source;
    }

    public String getTarget()
    {
        return target;
    }

    public String getWhiteListPattern()
    {
        return whiteListPattern;
    }

    public void initialize()
        throws InitializationException
    {
        policyMap = new HashMap();
        policyMap.putAll( preDownloadPolicyMap );
        policyMap.putAll( postDownloadPolicyMap );
    }

    public String input()
    {
        getLogger().info( "input()" );
        return INPUT;
    }

    public void prepare()
        throws Exception
    {
        String sourceId = getSource();
        String targetId = getTarget();

        getLogger().info( ".prepare() - sourceId [" + sourceId + "], targetId [" + targetId + "]" );

        if ( StringUtils.isBlank( sourceId ) || StringUtils.isBlank( targetId ) )
        {
            if ( this.connector == null )
            {
                getLogger().info( "Creating new connector." );
                this.connector = new ProxyConnectorConfiguration();
            }
        }
        else
        {
            this.connector = findProxyConnector( sourceId, targetId );
        }
        getLogger().info( "Connector: " + connector );

        Configuration config = archivaConfiguration.getConfiguration();

        // Gather Network Proxy Ids.

        this.proxyIdOptions = new ArrayList();
        this.proxyIdOptions.add( DIRECT_CONNECTION );

        Closure addProxyIds = new Closure()
        {
            public void execute( Object input )
            {
                if ( input instanceof NetworkProxyConfiguration )
                {
                    NetworkProxyConfiguration netproxy = (NetworkProxyConfiguration) input;
                    proxyIdOptions.add( netproxy.getId() );
                }
            }
        };

        CollectionUtils.forAllDo( config.getNetworkProxies(), addProxyIds );

        // Gather Local & Remote Repo Ids.

        RepositoryIdListClosure remoteRepoIdList = new RepositoryIdListClosure( new ArrayList() );
        RepositoryIdListClosure localRepoIdList = new RepositoryIdListClosure( new ArrayList() );
        Closure repoIfClosure = IfClosure.getInstance( RemoteRepositoryPredicate.getInstance(), remoteRepoIdList,
                                                       localRepoIdList );

        CollectionUtils.forAllDo( config.getRepositories(), repoIfClosure );

        this.remoteRepoIdList = remoteRepoIdList.getList();
        this.localRepoIdList = localRepoIdList.getList();
    }

    public String save()
    {
        String mode = getMode();

        String sourceId = getConnector().getSourceRepoId();
        String targetId = getConnector().getTargetRepoId();

        getLogger().info( ".save(" + mode + ":" + sourceId + "->" + targetId + ")" );

        if ( !isValid( getConnector() ) )
        {
            return INPUT;
        }

        if ( StringUtils.equalsIgnoreCase( "edit", mode ) )
        {
            removeConnector( sourceId, targetId );
        }

        try
        {
            if ( StringUtils.equals( DIRECT_CONNECTION, getConnector().getProxyId() ) )
            {
                getConnector().setProxyId( null );
            }

            addProxyConnector( getConnector() );
            saveConfiguration();
        }
        catch ( IOException e )
        {
            addActionError( "I/O Exception: " + e.getMessage() );
        }
        catch ( InvalidConfigurationException e )
        {
            addActionError( "Invalid Configuration Exception: " + e.getMessage() );
        }
        catch ( RegistryException e )
        {
            addActionError( "Configuration Registry Exception: " + e.getMessage() );
        }

        return SUCCESS;
    }

    public void setBlackListPattern( String blackListPattern )
    {
        this.blackListPattern = blackListPattern;
    }

    public void setConnector( ProxyConnectorConfiguration connector )
    {
        this.connector = connector;
    }

    public void setLocalRepoIdList( List localRepoIdList )
    {
        this.localRepoIdList = localRepoIdList;
    }

    public void setMode( String mode )
    {
        this.mode = mode;
    }

    public void setPropertyKey( String propertyKey )
    {
        this.propertyKey = propertyKey;
    }

    public void setPropertyValue( String propertyValue )
    {
        this.propertyValue = propertyValue;
    }

    public void setRemoteRepoIdList( List remoteRepoIdList )
    {
        this.remoteRepoIdList = remoteRepoIdList;
    }

    public void setSource( String source )
    {
        this.source = source;
    }

    public void setTarget( String target )
    {
        this.target = target;
    }

    public void setWhiteListPattern( String whiteListPattern )
    {
        this.whiteListPattern = whiteListPattern;
    }

    private void addProxyConnector( ProxyConnectorConfiguration proxyConnector )
        throws IOException
    {
        archivaConfiguration.getConfiguration().addProxyConnector( proxyConnector );
    }

    private ProxyConnectorConfiguration findProxyConnector( String sourceId, String targetId )
    {
        Configuration config = archivaConfiguration.getConfiguration();

        ProxyConnectorSelectionPredicate selectedProxy = new ProxyConnectorSelectionPredicate( sourceId, targetId );
        return (ProxyConnectorConfiguration) CollectionUtils.find( config.getProxyConnectors(), selectedProxy );
    }

    private boolean isValid( ProxyConnectorConfiguration proxyConnector )
    {
        if ( proxyConnector.getPolicies() == null )
        {
            addActionError( "Policies must be set." );
            return false;
        }

        Iterator it = policyMap.entrySet().iterator();
        while ( it.hasNext() )
        {
            Map.Entry entry = (Entry) it.next();
            String policyId = (String) entry.getKey();
            DownloadPolicy policy = (DownloadPolicy) entry.getValue();
            List options = policy.getOptions();

            if ( !proxyConnector.getPolicies().containsKey( policyId ) )
            {
                addActionError( "Policy [" + policyId + "] must be set (missing id)." );
                continue;
            }

            String arr[] = (String[]) proxyConnector.getPolicies().get( policyId );
            String value = arr[0];

            proxyConnector.getPolicies().put( policyId, value );

            if ( StringUtils.isBlank( value ) )
            {
                addActionError( "Policy [" + policyId + "] must be set (missing value)." );
                continue;
            }

            if ( !options.contains( value ) )
            {
                addActionError( "Value of [" + value + "] is invalid for policy [" + policyId + "], valid values: "
                    + options );
                continue;
            }
        }

        if ( hasActionErrors() || hasActionMessages() )
        {
            return false;
        }

        return true;
    }

    private void removeConnector( String sourceId, String targetId )
    {
        ProxyConnectorSelectionPredicate selectedProxy = new ProxyConnectorSelectionPredicate( sourceId, targetId );
        NotPredicate notSelectedProxy = new NotPredicate( selectedProxy );
        CollectionUtils.filter( archivaConfiguration.getConfiguration().getProxyConnectors(), notSelectedProxy );
    }

    private String saveConfiguration()
        throws IOException, InvalidConfigurationException, RegistryException
    {
        archivaConfiguration.save( archivaConfiguration.getConfiguration() );

        addActionMessage( "Successfully saved configuration" );

        return SUCCESS;
    }

    public String getPattern()
    {
        return pattern;
    }

    public void setPattern( String pattern )
    {
        this.pattern = pattern;
    }
}
