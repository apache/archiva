package org.apache.archiva.repository.maven2;

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

import org.apache.archiva.dependency.tree.maven2.ArchivaRepositoryConnectorFactory;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;

public class MavenUtil {

    public static RepositorySystemSession newRepositorySystemSession(String localRepoDir)
    {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession( );

        LocalRepository repo = new LocalRepository( localRepoDir );

        DependencySelector depFilter = new AndDependencySelector( new ExclusionDependencySelector() );
        session.setDependencySelector( depFilter );
        SimpleLocalRepositoryManagerFactory repFactory = new SimpleLocalRepositoryManagerFactory( );
        try
        {
            LocalRepositoryManager manager = repFactory.newInstance( session, repo );
            session.setLocalRepositoryManager(manager);
        }
        catch ( NoLocalRepositoryManagerException e )
        {
            e.printStackTrace( );
        }

        return session;
    }

    public static RepositorySystem newRepositorySystem()
    {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator( );
        locator.addService( RepositoryConnectorFactory.class,
                            ArchivaRepositoryConnectorFactory.class );// FileRepositoryConnectorFactory.class );
        locator.addService( VersionResolver.class, DefaultVersionResolver.class );
        locator.addService( VersionRangeResolver.class, DefaultVersionRangeResolver.class );
        locator.addService( ArtifactDescriptorReader.class, DefaultArtifactDescriptorReader.class );

        return locator.getService( RepositorySystem.class );
    }
}
