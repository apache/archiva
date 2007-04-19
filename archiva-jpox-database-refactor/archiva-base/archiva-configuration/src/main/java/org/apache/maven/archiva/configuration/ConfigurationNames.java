package org.apache.maven.archiva.configuration;

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

import java.util.HashSet;
import java.util.Set;

/**
 * Utility methods for testing the configuration property name. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ConfigurationNames
{
    private static final Set networkProxies = new HashSet();

    private static final Set repositoryScanning = new HashSet();

    private static final Set repositories = new HashSet();

    static
    {
        repositories.add( "repositories" );
        repositories.add( "repository" );
        repositories.add( "id" );
        repositories.add( "name" );
        repositories.add( "url" );
        repositories.add( "layout" );
        repositories.add( "releases" );
        repositories.add( "snapshots" );
        repositories.add( "indexed" );
        repositories.add( "refreshCronExpression" );

        networkProxies.add( "networkProxies" );
        networkProxies.add( "networkProxy" );
        networkProxies.add( "id" );
        networkProxies.add( "protocol" );
        networkProxies.add( "host" );
        networkProxies.add( "port" );
        networkProxies.add( "username" );
        networkProxies.add( "password" );

        repositoryScanning.add( "repositoryScanning" );
        repositoryScanning.add( "fileTypes" );
        repositoryScanning.add( "fileType" );
        repositoryScanning.add( "patterns" );
        repositoryScanning.add( "pattern" );
        repositoryScanning.add( "goodConsumers" );
        repositoryScanning.add( "goodConsumer" );
        repositoryScanning.add( "badConsumers" );
        repositoryScanning.add( "badConsumer" );
    }

    public static boolean isNetworkProxy( String propertyName )
    {
        if ( empty( propertyName ) )
        {
            return false;
        }

        return networkProxies.contains( propertyName );
    }

    public static boolean isRepositoryScanning( String propertyName )
    {
        if ( empty( propertyName ) )
        {
            return false;
        }

        return repositoryScanning.contains( propertyName );
    }
    
    public static boolean isRepositories( String propertyName )
    {
        if( empty(propertyName))
        {
            return false;
        }
        
        return repositories.contains( propertyName );
    }

    private static boolean empty( String name )
    {
        if ( name == null )
        {
            return false;
        }

        return ( name.trim().length() <= 0 );
    }
}
