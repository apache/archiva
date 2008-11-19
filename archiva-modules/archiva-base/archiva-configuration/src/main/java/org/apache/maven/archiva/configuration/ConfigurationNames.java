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

/**
 * Utility methods for testing the configuration property name.
 *
 * @version $Id$
 */
public class ConfigurationNames
{
    public static boolean isNetworkProxy( String propertyName )
    {
        return startsWith( "networkProxies.", propertyName );
    }

    public static boolean isRepositoryScanning( String propertyName )
    {
        return startsWith( "repositoryScanning.", propertyName );
    }

    public static boolean isManagedRepositories( String propertyName )
    {
        return startsWith( "managedRepositories.", propertyName );
    }

    public static boolean isRemoteRepositories( String propertyName )
    {
        return startsWith( "remoteRepositories.", propertyName );
    }

    public static boolean isProxyConnector( String propertyName )
    {
        return startsWith( "proxyConnectors.", propertyName );
    }

    private static boolean startsWith( String prefix, String name )
    {
        if ( name == null )
        {
            return false;
        }

        if ( name.length() <= 0 )
        {
            return false;
        }

        return name.startsWith( prefix );
    }
}
