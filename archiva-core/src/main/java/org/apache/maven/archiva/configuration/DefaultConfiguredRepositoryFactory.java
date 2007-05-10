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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.proxy.ProxiedArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Create artifact repositories from a configuration.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="org.apache.maven.archiva.configuration.ConfiguredRepositoryFactory"
 */
public class DefaultConfiguredRepositoryFactory
    implements ConfiguredRepositoryFactory
{
    /**
     * @plexus.requirement role="org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout"
     */
    private Map repositoryLayouts;

    /**
     * @plexus.requirement
     */
    private ArtifactRepositoryFactory repoFactory;

    public ArtifactRepository createRepository( RepositoryConfiguration configuration )
    {
        return createRepository( configuration.getLayout(), configuration.getId(), configuration.getDirectory());
    }

    public ProxiedArtifactRepository createProxiedRepository( ProxiedRepositoryConfiguration configuration )
    {
        boolean enabled = isEnabled( configuration.getSnapshotsPolicy() );
        String updatePolicy =
            getUpdatePolicy( configuration.getSnapshotsPolicy(), configuration.getSnapshotsInterval() );
        ArtifactRepositoryPolicy snapshotsPolicy =
            new ArtifactRepositoryPolicy( enabled, updatePolicy, configuration.getChecksumPolicy() );

        enabled = isEnabled( configuration.getReleasesPolicy() );
        updatePolicy = getUpdatePolicy( configuration.getReleasesPolicy(), configuration.getReleasesInterval() );
        ArtifactRepositoryPolicy releasesPolicy =
            new ArtifactRepositoryPolicy( enabled, updatePolicy, configuration.getChecksumPolicy() );

        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) repositoryLayouts.get( configuration.getLayout() );

        if ( layout == null )
        {
            throw new IllegalArgumentException( "Invalid layout: " + configuration.getLayout() );
        }

        ArtifactRepository artifactRepository = repoFactory.createArtifactRepository( configuration.getId(),
                                                                                      configuration.getUrl(), layout,
                                                                                      snapshotsPolicy, releasesPolicy );
        ProxiedArtifactRepository repository = new ProxiedArtifactRepository( artifactRepository );
        repository.setCacheFailures( configuration.isCacheFailures() );
        repository.setHardFail( configuration.isHardFail() );
        repository.setName( configuration.getName() );
        repository.setUseNetworkProxy( configuration.isUseNetworkProxy() );
        return repository;
    }

    public List createRepositories( Configuration configuration )
    {
        List managedRepositories = configuration.getRepositories();
        List repositories = new ArrayList( managedRepositories.size() );

        for ( Iterator i = managedRepositories.iterator(); i.hasNext(); )
        {
            repositories.add( createRepository( (RepositoryConfiguration) i.next() ) );
        }

        return repositories;
    }

    public List createProxiedRepositories( Configuration configuration )
    {
        List proxiedRepositories = configuration.getProxiedRepositories();
        List repositories = new ArrayList( proxiedRepositories.size() );

        for ( Iterator i = proxiedRepositories.iterator(); i.hasNext(); )
        {
            repositories.add( createProxiedRepository( (ProxiedRepositoryConfiguration) i.next() ) );
        }

        return repositories;
    }

    public ArtifactRepository createLocalRepository( Configuration configuration )
    {
        return createRepository( "default", "local", configuration.getLocalRepository() );
    }

    public ArtifactRepository createRepository( String layout, String id, String directory )
    {
        ArtifactRepositoryLayout repositoryLayout = (ArtifactRepositoryLayout) repositoryLayouts.get( layout );
        File repository = new File( directory );
        repository.mkdirs();

        String repoDir = repository.toURI().toString();
        //workaround for spaces non converted by PathUtils in wagon
        //TODO: remove it when PathUtils will be fixed
        if ( repoDir.indexOf( "%20" ) >= 0 )
        {
            repoDir = StringUtils.replace( repoDir, "%20", " " );
        }

        return repoFactory.createArtifactRepository( id, repoDir, repositoryLayout, null, null );
    }

    private static String getUpdatePolicy( String policy, int interval )
    {
        return "interval".equals( policy ) ? policy + ":" + interval : policy;
    }

    private static boolean isEnabled( String policy )
    {
        return !"disabled".equals( policy );
    }
}
