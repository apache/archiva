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

import com.opensymphony.xwork2.Preparable;
import org.apache.archiva.admin.repository.RepositoryAdminException;
import org.apache.archiva.admin.repository.proxyconnector.ProxyConnector;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.policies.DownloadErrorPolicy;
import org.apache.maven.archiva.policies.Policy;
import org.apache.maven.archiva.policies.PostDownloadPolicy;
import org.apache.maven.archiva.policies.PreDownloadPolicy;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AbstractProxyConnectorFormAction - generic fields and methods for either add or edit actions related with the
 * Proxy Connector.
 *
 * @version $Id$
 */
public abstract class AbstractProxyConnectorFormAction
    extends AbstractProxyConnectorAction
    implements Preparable
{


    private Map<String, PreDownloadPolicy> preDownloadPolicyMap;

    private Map<String, PostDownloadPolicy> postDownloadPolicyMap;

    private Map<String, DownloadErrorPolicy> downloadErrorPolicyMap;

    private List<String> proxyIdOptions;

    private List<String> managedRepoIdList;

    private List<String> remoteRepoIdList;

    /**
     * The map of policies that are available to be set.
     */
    private Map<String, Policy> policyMap;

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
    protected ProxyConnector connector;

    @Inject
    private ArchivaConfiguration archivaConfiguration;

    @PostConstruct
    public void initialize()
    {
        super.initialize();
        this.preDownloadPolicyMap = getBeansOfType( PreDownloadPolicy.class );
        this.postDownloadPolicyMap = getBeansOfType( PostDownloadPolicy.class );
        this.downloadErrorPolicyMap = getBeansOfType( DownloadErrorPolicy.class );
    }

    protected List<String> escapePatterns( List<String> patterns )
    {
        List<String> escapedPatterns = new ArrayList<String>();
        if ( patterns != null )
        {
            for ( String pattern : patterns )
            {
                escapedPatterns.add( StringUtils.replace( pattern, "\\", "\\\\" ) );
            }
        }

        return escapedPatterns;
    }

    protected List<String> unescapePatterns( List<String> patterns )
    {
        List<String> rawPatterns = new ArrayList<String>();
        if ( patterns != null )
        {
            for ( String pattern : patterns )
            {
                rawPatterns.add( StringUtils.replace( pattern, "\\\\", "\\" ) );
            }
        }

        return rawPatterns;
    }

    private String escapePattern( String pattern )
    {
        return StringUtils.replace( pattern, "\\", "\\\\" );
    }

    public String addBlackListPattern()
    {
        String pattern = getBlackListPattern();

        if ( StringUtils.isBlank( pattern ) )
        {
            addActionError( "Cannot add a blank black list pattern." );
        }

        if ( !hasActionErrors() )
        {
            getConnector().getBlackListPatterns().add( escapePattern( pattern ) );
            setBlackListPattern( null );
        }

        return INPUT;
    }

    @SuppressWarnings( "unchecked" )
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
            getConnector().getWhiteListPatterns().add( escapePattern( pattern ) );
            setWhiteListPattern( null );
        }

        return INPUT;
    }

    public String getBlackListPattern()
    {
        return blackListPattern;
    }

    public ProxyConnector getConnector()
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

    public Map<String, Policy> getPolicyMap()
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
        throws RepositoryAdminException
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

        if ( !getConnector().getBlackListPatterns().contains( pattern )
            && !getConnector().getBlackListPatterns().contains( StringUtils.replace( pattern, "\\", "\\\\" ) ) )
        {
            addActionError( "Non-existant black list pattern [" + pattern + "], no black list pattern removed." );
        }

        if ( !hasActionErrors() )
        {
            getConnector().getBlackListPatterns().remove( escapePattern( pattern ) );
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

        if ( !getConnector().getWhiteListPatterns().contains( pattern )
            && !getConnector().getWhiteListPatterns().contains( StringUtils.replace( pattern, "\\", "\\\\" ) ) )
        {
            addActionError( "Non-existant white list pattern [" + pattern + "], no white list pattern removed." );
        }

        if ( !hasActionErrors() )
        {
            getConnector().getWhiteListPatterns().remove( escapePattern( pattern ) );
        }

        setWhiteListPattern( null );
        setPattern( null );

        return INPUT;
    }

    public void setBlackListPattern( String blackListPattern )
    {
        this.blackListPattern = blackListPattern;
    }

    public void setConnector( ProxyConnector connector )
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

    public void setPolicyMap( Map<String, Policy> policyMap )
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
        throws RepositoryAdminException
    {
        return new ArrayList<String>( getManagedRepositoryAdmin().getManagedRepositoriesAsMap().keySet() );
    }

    protected List<String> createNetworkProxyOptions()
    {
        List<String> options = new ArrayList<String>();

        options.add( DIRECT_CONNECTION );
        options.addAll( archivaConfiguration.getConfiguration().getNetworkProxiesAsMap().keySet() );

        return options;
    }

    protected Map<String, Policy> createPolicyMap()
    {
        Map<String, Policy> policyMap = new HashMap<String, Policy>();

        policyMap.putAll( preDownloadPolicyMap );
        policyMap.putAll( postDownloadPolicyMap );
        policyMap.putAll( downloadErrorPolicyMap );

        return policyMap;
    }

    protected List<String> createRemoteRepoOptions()
        throws RepositoryAdminException
    {
        return new ArrayList<String>( getRemoteRepositoryAdmin().getRemoteRepositoriesAsMap().keySet() );
    }

    @SuppressWarnings( "unchecked" )
    protected void validateConnector()
    {
        if ( connector.getPolicies() == null )
        {
            addActionError( "Policies must be set." );
        }
        else
        {
            // Validate / Fix policy settings arriving from browser.
            for ( Map.Entry<String, Policy> entry : getPolicyMap().entrySet() )
            {
                String policyId = entry.getKey();
                Policy policy = entry.getValue();
                List<String> options = policy.getOptions();

                if ( !connector.getPolicies().containsKey( policyId ) )
                {
                    addActionError( "Policy [" + policyId + "] must be set (missing id)." );
                    continue;
                }

                Map<String, String> properties = connector.getProperties();
                for ( Map.Entry<String, String> entry2 : properties.entrySet() )
                {
                    Object value = entry2.getValue();
                    if ( value.getClass().isArray() )
                    {
                        String[] arr = (String[]) value;
                        properties.put( entry2.getKey(), arr[0] );
                    }
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
                    addActionError(
                        "Value of [" + value + "] is invalid for policy [" + policyId + "], valid values: " + options );
                    continue;
                }
            }
        }
    }

    public ArchivaConfiguration getArchivaConfiguration()
    {
        return archivaConfiguration;
    }

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }
}
