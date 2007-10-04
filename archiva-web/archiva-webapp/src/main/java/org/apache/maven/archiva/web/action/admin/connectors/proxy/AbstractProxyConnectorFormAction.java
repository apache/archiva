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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.policies.DownloadPolicy;
import org.apache.maven.archiva.policies.PostDownloadPolicy;
import org.apache.maven.archiva.policies.PreDownloadPolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AbstractProxyConnectorFormAction - generic fields and methods for either add or edit actions related with the 
 * Proxy Connector. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractProxyConnectorFormAction
    extends AbstractProxyConnectorAction
    implements Preparable
{

    /**
     * @plexus.requirement role="org.apache.maven.archiva.policies.PreDownloadPolicy"
     */
    private Map<String, PreDownloadPolicy> preDownloadPolicyMap;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.policies.PostDownloadPolicy"
     */
    private Map<String, PostDownloadPolicy> postDownloadPolicyMap;

    /**
     * The list of network proxy ids that are available.
     */
    private List<String> proxyIdOptions;

    /**
     * The list of managed repository ids that are available.
     */
    private List<String> managedRepoIdList;

    /**
     * The list of remove repository ids that are available.
     */
    private List<String> remoteRepoIdList;

    /**
     * The map of policies that are available to be set.
     */
    private Map<String, DownloadPolicy> policyMap;

    /**
     * The property key to add or remove.
     */
    private String propertyKey;

    /**
     * The property value to add.
     */
    private String propertyValue;

    /**
     * The blacklist pattern to add.
     */
    private String blackListPattern;

    /**
     * The whitelist pattern to add.
     */
    private String whiteListPattern;

    /**
     * The pattern to add or remove (black or white).
     */
    private String pattern;

    /**
     * The model for this action.
     */
    protected ProxyConnectorConfiguration connector;

    public String addBlackListPattern()
    {
        String pattern = getBlackListPattern();

        if ( StringUtils.isBlank( pattern ) )
        {
            addActionError( "Cannot add a blank black list pattern." );
        }

        if ( !hasActionErrors() )
        {
            getConnector().getBlackListPatterns().add( pattern );
            setBlackListPattern( null );
        }

        return INPUT;
    }

    public String addProperty()
    {
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

    public String addWhiteListPattern()
    {
        String pattern = getWhiteListPattern();

        if ( StringUtils.isBlank( pattern ) )
        {
            addActionError( "Cannot add a blank white list pattern." );
        }

        if ( !hasActionErrors() )
        {
            getConnector().getWhiteListPatterns().add( pattern );
            setWhiteListPattern( null );
        }

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

    public List<String> getManagedRepoIdList()
    {
        return managedRepoIdList;
    }

    public String getPattern()
    {
        return pattern;
    }

    public Map<String, DownloadPolicy> getPolicyMap()
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

    public List<String> getProxyIdOptions()
    {
        return proxyIdOptions;
    }

    public List<String> getRemoteRepoIdList()
    {
        return remoteRepoIdList;
    }

    public String getWhiteListPattern()
    {
        return whiteListPattern;
    }

    public void prepare()
    {
        proxyIdOptions = createNetworkProxyOptions();
        managedRepoIdList = createManagedRepoOptions();
        remoteRepoIdList = createRemoteRepoOptions();
        policyMap = createPolicyMap();
    }

    public String removeBlackListPattern()
    {
        String pattern = getPattern();

        if ( StringUtils.isBlank( pattern ) )
        {
            addActionError( "Cannot remove a blank black list pattern." );
        }

        if ( !getConnector().getBlackListPatterns().contains( pattern ) )
        {
            addActionError( "Non-existant black list pattern [" + pattern + "], no black list pattern removed." );
        }

        if ( !hasActionErrors() )
        {
            getConnector().getBlackListPatterns().remove( pattern );
        }

        setBlackListPattern( null );
        setPattern( null );

        return INPUT;
    }

    public String removeProperty()
    {
        String key = getPropertyKey();

        if ( StringUtils.isBlank( key ) )
        {
            addActionError( "Unable to remove property with blank key." );
        }

        if ( !getConnector().getProperties().containsKey( key ) )
        {
            addActionError( "Non-existant property key [" + pattern + "], no property was removed." );
        }

        if ( !hasActionErrors() )
        {
            getConnector().getProperties().remove( key );
        }

        setPropertyKey( null );
        setPropertyValue( null );

        return INPUT;
    }

    public String removeWhiteListPattern()
    {
        String pattern = getPattern();

        if ( StringUtils.isBlank( pattern ) )
        {
            addActionError( "Cannot remove a blank white list pattern." );
        }

        if ( !getConnector().getWhiteListPatterns().contains( pattern ) )
        {
            addActionError( "Non-existant white list pattern [" + pattern + "], no white list pattern removed." );
        }

        if ( !hasActionErrors() )
        {
            getConnector().getWhiteListPatterns().remove( pattern );
        }

        setWhiteListPattern( null );
        setPattern( null );

        return INPUT;
    }

    public void setBlackListPattern( String blackListPattern )
    {
        this.blackListPattern = blackListPattern;
    }

    public void setConnector( ProxyConnectorConfiguration connector )
    {
        this.connector = connector;
    }

    public void setManagedRepoIdList( List<String> managedRepoIdList )
    {
        this.managedRepoIdList = managedRepoIdList;
    }

    public void setPattern( String pattern )
    {
        this.pattern = pattern;
    }

    public void setPolicyMap( Map<String, DownloadPolicy> policyMap )
    {
        this.policyMap = policyMap;
    }

    public void setPropertyKey( String propertyKey )
    {
        this.propertyKey = propertyKey;
    }

    public void setPropertyValue( String propertyValue )
    {
        this.propertyValue = propertyValue;
    }

    public void setProxyIdOptions( List<String> proxyIdOptions )
    {
        this.proxyIdOptions = proxyIdOptions;
    }

    public void setRemoteRepoIdList( List<String> remoteRepoIdList )
    {
        this.remoteRepoIdList = remoteRepoIdList;
    }

    public void setWhiteListPattern( String whiteListPattern )
    {
        this.whiteListPattern = whiteListPattern;
    }

    protected List<String> createManagedRepoOptions()
    {
        return new ArrayList<String>( getConfig().getManagedRepositoriesAsMap().keySet() );
    }

    protected List<String> createNetworkProxyOptions()
    {
        List<String> options = new ArrayList<String>();

        options.add( DIRECT_CONNECTION );
        options.addAll( getConfig().getNetworkProxiesAsMap().keySet() );

        return options;
    }

    protected Map<String, DownloadPolicy> createPolicyMap()
    {
        Map<String, DownloadPolicy> policyMap = new HashMap<String, DownloadPolicy>();

        policyMap.putAll( preDownloadPolicyMap );
        policyMap.putAll( postDownloadPolicyMap );

        return policyMap;
    }

    protected List<String> createRemoteRepoOptions()
    {
        return new ArrayList<String>( getConfig().getRemoteRepositoriesAsMap().keySet() );
    }

    protected void validateConnector()
    {
        if ( connector.getPolicies() == null )
        {
            addActionError( "Policies must be set." );
        }
        else
        {
            // Validate / Fix policy settings arriving from browser.
            for ( Map.Entry<String, DownloadPolicy> entry : getPolicyMap().entrySet() )
            {
                String policyId = (String) entry.getKey();
                DownloadPolicy policy = (DownloadPolicy) entry.getValue();
                List<String> options = policy.getOptions();

                if ( !connector.getPolicies().containsKey( policyId ) )
                {
                    addActionError( "Policy [" + policyId + "] must be set (missing id)." );
                    continue;
                }

                // Ugly hack to compensate for ugly browsers.
                Object o = connector.getPolicies().get( policyId );
                String value;
                if ( o.getClass().isArray() )
                {
                    String arr[] = (String[]) o;
                    value = arr[0];
                }
                else
                {
                    value = (String) o;
                }

                connector.getPolicies().put( policyId, value );

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
        }
    }
}
