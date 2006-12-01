package org.apache.maven.archiva.configuration;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * @author Ben Walding
 * @author Brett Porter
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

        RepositoryConfiguration config = new RepositoryConfiguration();
        config.setDirectory( localCachePath );
        config.setName( "Imported Maven-Proxy Cache" );
        config.setId( "maven-proxy" );
        configuration.addRepository( config );

        //just get the first HTTP proxy and break
        String propertyList = props.getProperty( PROXY_LIST );
        if ( propertyList != null )
        {
            StringTokenizer tok = new StringTokenizer( propertyList, "," );
            while ( tok.hasMoreTokens() )
            {
                String key = tok.nextToken();
                if ( StringUtils.isNotEmpty( key ) )
                {
                    Proxy proxy = new Proxy();
                    proxy.setHost( getMandatoryProperty( props, "proxy." + key + ".host" ) );
                    proxy.setPort( Integer.parseInt( getMandatoryProperty( props, "proxy." + key + ".port" ) ) );

                    // the username and password isn't required
                    proxy.setUsername( props.getProperty( "proxy." + key + ".username" ) );
                    proxy.setPassword( props.getProperty( "proxy." + key + ".password" ) );

                    configuration.setProxy( proxy );

                    //accept only one proxy configuration
                    break;
                }
            }
        }

        //get the remote repository list
        String repoList = getMandatoryProperty( props, REPO_LIST );

        StringTokenizer tok = new StringTokenizer( repoList, "," );
        while ( tok.hasMoreTokens() )
        {
            String key = tok.nextToken();

            Properties repoProps = getSubset( props, "repo." + key + "." );
            String url = getMandatoryProperty( props, "repo." + key + ".url" );
            String proxyKey = repoProps.getProperty( "proxy" );

            boolean cacheFailures =
                Boolean.valueOf( repoProps.getProperty( "cache.failures", "false" ) ).booleanValue();
            boolean hardFail = Boolean.valueOf( repoProps.getProperty( "hardfail", "true" ) ).booleanValue();
            int cachePeriod = Integer.parseInt( repoProps.getProperty( "cache.period", "60" ) );

            ProxiedRepositoryConfiguration repository = new ProxiedRepositoryConfiguration();
            repository.setId( key );
            repository.setLayout( "legacy" );
            repository.setManagedRepository( config.getId() );
            repository.setName( "Imported Maven-Proxy Remote Proxy" );
            repository.setSnapshotsInterval( cachePeriod );
            repository.setUrl( url );
            repository.setUseNetworkProxy( StringUtils.isNotEmpty( proxyKey ) );
            repository.setCacheFailures( cacheFailures );
            repository.setHardFail( hardFail );

            configuration.addProxiedRepository( repository );
        }
    }

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