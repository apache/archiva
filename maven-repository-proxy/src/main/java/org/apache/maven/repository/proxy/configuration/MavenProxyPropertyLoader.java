package org.apache.maven.repository.proxy.configuration;

/*
 * Copyright 2003-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.repository.proxy.repository.ProxyRepository;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * @author Ben Walding
 */
public class MavenProxyPropertyLoader
{
    private static final String REPO_LOCAL_STORE = "repo.local.store";

    private static final String PROXY_LIST = "proxy.list";

    private static final String REPO_LIST = "repo.list";

    public ProxyConfiguration load( Properties props )
        throws ValidationException
    {
        ProxyConfiguration config = new ProxyConfiguration();

        config.setLayout( "default" );

        config.setRepositoryCachePath( getMandatoryProperty( props, REPO_LOCAL_STORE ) );

        {//just get the first proxy and break
            String propertyList = props.getProperty( PROXY_LIST );
            if ( propertyList != null )
            {
                StringTokenizer tok = new StringTokenizer( propertyList, "," );
                while ( tok.hasMoreTokens() )
                {
                    String key = tok.nextToken();
                    if ( StringUtils.isNotEmpty( key ) )
                    {
                        String host = getMandatoryProperty( props, "proxy." + key + ".host" );
                        int port = Integer.parseInt( getMandatoryProperty( props, "proxy." + key + ".port" ) );

                        // the username and password isn't required
                        String username = props.getProperty( "proxy." + key + ".username" );
                        String password = props.getProperty( "proxy." + key + ".password" );

                        if ( StringUtils.isNotEmpty( username ) )
                        {
                            config.setHttpProxy( host, port, username, password );
                        }
                        else
                        {
                            config.setHttpProxy( host, port );
                        }

                        //accept only one proxy configuration
                        break;
                    }
                }
            }
        }

        List repositories = new ArrayList();
        { //get the remote repository list
            String repoList = getMandatoryProperty( props, REPO_LIST );

            StringTokenizer tok = new StringTokenizer( repoList, "," );
            while ( tok.hasMoreTokens() )
            {
                String key = tok.nextToken();

                Properties repoProps = getSubset( props, "repo." + key + "." );
                String url = getMandatoryProperty( props, "repo." + key + ".url" );
                String proxyKey = repoProps.getProperty( "proxy" );

                boolean cacheFailures = Boolean.valueOf( repoProps.getProperty( "cache.failures", "false" ) ).booleanValue();
                boolean hardFail = Boolean.valueOf( repoProps.getProperty( "hardfail", "true" ) ).booleanValue();
                long cachePeriod = Long.parseLong( repoProps.getProperty( "cache.period", "0" ) );

                ProxyRepository repository =
                    new ProxyRepository( key, url, new DefaultRepositoryLayout(), cacheFailures, cachePeriod );

                repository.setHardfail( hardFail );

                if ( StringUtils.isNotEmpty( proxyKey ) )
                {
                    repository.setProxied( true );
                }

                repositories.add( repository );
            }
        }
        config.setRepositories( repositories );

        validateDirectories( config );
        validateRemoteRepo( config );

        return config;
    }

    private void validateRemoteRepo( ProxyConfiguration configuration )
        throws ValidationException
    {
        //Verify remote repository set
        //only warn if missing
        if ( configuration.getRepositories().size() < 1 )
        {
            throw new ValidationException( "At least one remote repository must be configured." );
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

    public ProxyConfiguration load( InputStream is )
        throws IOException, ValidationException
    {
        Properties props = new Properties();
        props.load( is );
        return load( props );
    }

    private String getMandatoryProperty( Properties props, String key )
        throws ValidationException
    {
        final String value = props.getProperty( key );

        if ( value == null )
        {
            throw new ValidationException( "Missing property: " + key );
        }

        return value;
    }

    private void validateDirectories( ProxyConfiguration configuration )
        throws ValidationException
    {
        File f = new File( configuration.getRepositoryCachePath() );
        if ( !f.exists() )
        {
            throw new ValidationException( "Specified directory does not exist: " + f.getAbsolutePath() );
        }

        for ( Iterator repos = configuration.getRepositories().iterator(); repos.hasNext(); )
        {
            ProxyRepository repo = (ProxyRepository) repos.next();
            if ( repo.getUrl().startsWith( "file://" ) )
            {
                File f2 = new File( repo.getBasedir() );
                if ( !f2.exists() )
                {
                    throw new ValidationException( "Specified directory does not exist: " + f2.getAbsolutePath() );
                }
            }
        }
    }

}