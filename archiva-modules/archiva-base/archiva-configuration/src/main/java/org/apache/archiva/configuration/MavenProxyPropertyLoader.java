package org.apache.archiva.configuration;

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

import org.apache.archiva.policies.ReleasesPolicy;
import org.apache.archiva.policies.SnapshotsPolicy;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 */
public class MavenProxyPropertyLoader
{
    private static final String REPO_LOCAL_STORE = "repo.local.store";

    private static final String PROXY_LIST = "proxy.list";

    private static final String REPO_LIST = "repo.list";

    public void load( Properties props, Configuration configuration )
        throws InvalidConfigurationException
    {
        // set up the managed repository
        String localCachePath = getMandatoryProperty( props, REPO_LOCAL_STORE );

        ManagedRepositoryConfiguration config = new ManagedRepositoryConfiguration();
        config.setLocation( localCachePath );
        config.setName( "Imported Maven-Proxy Cache" );
        config.setId( "maven-proxy" );
        config.setScanned( false );
        config.setReleases( true );
        config.setSnapshots( false );
        configuration.addManagedRepository( config );

        // Add the network proxies.
        String propertyList = props.getProperty( PROXY_LIST );
        if ( propertyList != null )
        {
            StringTokenizer tok = new StringTokenizer( propertyList, "," );
            while ( tok.hasMoreTokens() )
            {
                String key = tok.nextToken();
                if ( StringUtils.isNotEmpty( key ) )
                {
                    NetworkProxyConfiguration proxy = new NetworkProxyConfiguration();
                    proxy.setHost( getMandatoryProperty( props, "proxy." + key + ".host" ) );
                    proxy.setPort( Integer.parseInt( getMandatoryProperty( props, "proxy." + key + ".port" ) ) );

                    // the username and password isn't required
                    proxy.setUsername( props.getProperty( "proxy." + key + ".username" ) );
                    proxy.setPassword( props.getProperty( "proxy." + key + ".password" ) );

                    configuration.addNetworkProxy( proxy );
                }
            }
        }

        // Add the remote repository list
        String repoList = getMandatoryProperty( props, REPO_LIST );

        StringTokenizer tok = new StringTokenizer( repoList, "," );
        while ( tok.hasMoreTokens() )
        {
            String key = tok.nextToken();

            Properties repoProps = getSubset( props, "repo." + key + "." );
            String url = getMandatoryProperty( props, "repo." + key + ".url" );
            String proxyKey = repoProps.getProperty( "proxy" );

//            int cachePeriod = Integer.parseInt( repoProps.getProperty( "cache.period", "60" ) );

            RemoteRepositoryConfiguration repository = new RemoteRepositoryConfiguration();
            repository.setId( key );
            repository.setName( "Imported Maven-Proxy Remote Proxy" );
            repository.setUrl( url );
            repository.setLayout( "legacy" );

            configuration.addRemoteRepository( repository );

            ProxyConnectorConfiguration proxyConnector = new ProxyConnectorConfiguration();
            proxyConnector.setSourceRepoId( "maven-proxy" );
            proxyConnector.setTargetRepoId( key );
            proxyConnector.setProxyId( proxyKey );
            // TODO: convert cachePeriod to closest "daily" or "hourly"
            proxyConnector.addPolicy( ProxyConnectorConfiguration.POLICY_SNAPSHOTS, SnapshotsPolicy.DAILY.getId() );
            proxyConnector.addPolicy( ProxyConnectorConfiguration.POLICY_RELEASES, ReleasesPolicy.ALWAYS.getId() );

            configuration.addProxyConnector( proxyConnector );
        }
    }

    @SuppressWarnings( "unchecked" )
    private Properties getSubset( Properties props, String prefix )
    {
        Enumeration keys = props.keys();
        Properties result = new Properties();
        while ( keys.hasMoreElements() )
        {
            String key = (String) keys.nextElement();
            String value = props.getProperty( key );
            if ( key.startsWith( prefix ) )
            {
                String newKey = key.substring( prefix.length() );
                result.setProperty( newKey, value );
            }
        }
        return result;
    }

    public void load( InputStream is, Configuration configuration )
        throws IOException, InvalidConfigurationException
    {
        Properties props = new Properties();
        props.load( is );
        load( props, configuration );
    }

    private String getMandatoryProperty( Properties props, String key )
        throws InvalidConfigurationException
    {
        String value = props.getProperty( key );

        if ( value == null )
        {
            throw new InvalidConfigurationException( key, "Missing required field: " + key );
        }

        return value;
    }
}
