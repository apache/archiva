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

package org.apache.maven.archiva.webdav;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * DefaultDavServerManager 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id: DefaultDavServerManager.java 7009 2007-10-25 23:34:43Z joakime $
 * 
 * @plexus.component role="org.apache.maven.archiva.webdav.DavServerManager" role-hint="default"
 */
public class DefaultDavServerManager
    implements DavServerManager
{
    /**
     * @plexus.requirement role-hint="simple"
     */
    private DavServerComponent server;

    private Map servers;

    public DefaultDavServerManager()
    {
        servers = new HashMap();
    }

    public DavServerComponent createServer( String prefix, File rootDirectory )
        throws DavServerException
    {
        if ( servers.containsKey( prefix ) )
        {
            throw new DavServerException( "Unable to create a new server on a pre-existing prefix [" + prefix + "]" );
        }

        server.setPrefix( prefix );
        if ( rootDirectory != null )
        {
            server.setRootDirectory( rootDirectory );
        }

        servers.put( prefix, server );

        return server;
    }

    public DavServerComponent getServer( String prefix )
    {
        return (DavServerComponent) servers.get( prefix );
    }

    public void removeServer( String prefix )
    {
        servers.remove( prefix );
    }

    public Collection getServers()
    {
        return servers.values();
    }

    public void removeAllServers()
    {
        servers.clear();
    }
}
