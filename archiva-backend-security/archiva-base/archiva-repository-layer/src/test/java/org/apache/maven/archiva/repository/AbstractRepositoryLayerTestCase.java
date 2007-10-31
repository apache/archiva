package org.apache.maven.archiva.repository;

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

import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

/**
 * AbstractRepositoryLayerTestCase 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractRepositoryLayerTestCase
    extends PlexusTestCase
{
    protected ManagedRepositoryConfiguration createRepository( String id, String name, File location )
    {
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setLocation( location.getAbsolutePath() );
        return repo;
    }

    protected RemoteRepositoryConfiguration createRemoteRepository( String id, String name, String url )
    {
        RemoteRepositoryConfiguration repo = new RemoteRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setUrl( url );
        return repo;
    }
    
    protected ManagedRepositoryContent createManagedRepositoryContent( String id, String name, File location, String layout )
        throws Exception
    {
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setLocation( location.getAbsolutePath() );
        repo.setLayout( layout );

        ManagedRepositoryContent repoContent = (ManagedRepositoryContent) lookup( ManagedRepositoryContent.class, layout );
        repoContent.setRepository( repo );

        return repoContent;
    }

    protected RemoteRepositoryContent createRemoteRepositoryContent( String id, String name, String url, String layout )
        throws Exception
    {
        RemoteRepositoryConfiguration repo = new RemoteRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setUrl( url );
        repo.setLayout( layout );

        RemoteRepositoryContent repoContent = (RemoteRepositoryContent) lookup( RemoteRepositoryContent.class, layout );
        repoContent.setRepository( repo );

        return repoContent;
    }
}
