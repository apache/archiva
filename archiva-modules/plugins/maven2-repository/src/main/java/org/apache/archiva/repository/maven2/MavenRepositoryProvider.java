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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.configuration.AbstractRepositoryConfiguration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.archiva.repository.EditableRepository;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.PasswordCredentials;
import org.apache.archiva.repository.ReleaseScheme;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryProvider;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.features.ArtifactCleanupFeature;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.features.RemoteIndexFeature;
import org.apache.archiva.repository.features.StagingRepositoryFeature;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Period;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Provider for the maven2 repository implementations
 */
@Service("mavenRepositoryProvider")
public class MavenRepositoryProvider implements RepositoryProvider
{
    private static final Logger log = LoggerFactory.getLogger( MavenRepositoryProvider.class );

    static final Set<RepositoryType> TYPES = new HashSet<>(  );
    static {
        TYPES.add( RepositoryType.MAVEN);
    }

    @Override
    public Set<RepositoryType> provides( )
    {
        return TYPES;
    }

    @Override
    public ManagedRepository createManagedInstance( ManagedRepositoryConfiguration cfg )
    {
        MavenManagedRepository repo = new MavenManagedRepository(cfg.getId() ,cfg.getName());
        try
        {
            if (cfg.getLocation().startsWith("file:")) {
                    repo.setLocation( new URI(cfg.getLocation()) );
            } else {
                repo.setLocation( new URI("file://"+cfg.getLocation()) );
            }
        }
        catch ( URISyntaxException e )
        {
            log.error("Could not set repository uri "+cfg.getLocation());
        }
        setBaseConfig( repo, cfg );
        repo.setSchedulingDefinition(cfg.getRefreshCronExpression());
        repo.setBlocksRedeployment( cfg.isBlockRedeployments() );
        repo.setScanned( cfg.isScanned() );
        Set<ReleaseScheme> schemes = new HashSet<>(  );
        if (cfg.isReleases()) {
            repo.addActiveReleaseScheme(ReleaseScheme.RELEASE);
        }
        if (cfg.isSnapshots()) {
            repo.addActiveReleaseScheme(ReleaseScheme.SNAPSHOT);
        }

        StagingRepositoryFeature stagingRepositoryFeature = repo.getFeature( StagingRepositoryFeature.class ).get();
        stagingRepositoryFeature.setStageRepoNeeded( cfg.isStageRepoNeeded() );
        // TODO: staging repository  -> here or in repositoryregistry?


        IndexCreationFeature indexCreationFeature = repo.getFeature( IndexCreationFeature.class ).get( );
        indexCreationFeature.setSkipPackedIndexCreation( cfg.isSkipPackedIndexCreation() );

        ArtifactCleanupFeature artifactCleanupFeature = repo.getFeature( ArtifactCleanupFeature.class ).get();

        artifactCleanupFeature.setDeleteReleasedSnapshots( cfg.isDeleteReleasedSnapshots() );
        artifactCleanupFeature.setRetentionCount( cfg.getRetentionCount() );
        artifactCleanupFeature.setRetentionTime( Period.ofDays( cfg.getRetentionTime() ) );

        return repo;
    }

    @Override
    public RemoteRepository createRemoteInstance( RemoteRepositoryConfiguration cfg )
    {
        MavenRemoteRepository repo = new MavenRemoteRepository( cfg.getId( ), cfg.getName( ) );
        setBaseConfig( repo, cfg );
        repo.setCheckPath( cfg.getCheckPath() );
        repo.setSchedulingDefinition( cfg.getRefreshCronExpression() );
        try
        {
            repo.setLocation(new URI(cfg.getUrl()));
        }
        catch ( URISyntaxException e )
        {
            log.error("Could not set remote url "+cfg.getUrl());
        }
        RemoteIndexFeature remoteIndexFeature = repo.getFeature( RemoteIndexFeature.class ).get();
        remoteIndexFeature.setDownloadRemoteIndex( cfg.isDownloadRemoteIndex() );
        remoteIndexFeature.setDownloadRemoteIndexOnStartup( cfg.isDownloadRemoteIndexOnStartup() );
        remoteIndexFeature.setDownloadTimeout( Duration.ofSeconds( cfg.getRemoteDownloadTimeout()) );
        remoteIndexFeature.setProxyId( cfg.getRemoteDownloadNetworkProxyId() );
        if (cfg.isDownloadRemoteIndex())
        {
            try
            {
                remoteIndexFeature.setIndexUri( new URI( cfg.getRemoteIndexUrl( ) ) );
            }
            catch ( URISyntaxException e )
            {
                log.error( "Could not set remote index url " + cfg.getRemoteIndexUrl( ) );
                remoteIndexFeature.setDownloadRemoteIndex( false );
                remoteIndexFeature.setDownloadRemoteIndexOnStartup( false );
            }
        }
        repo.setExtraHeaders( cfg.getExtraHeaders() );
        repo.setExtraParameters( cfg.getExtraParameters() );
        PasswordCredentials credentials = new PasswordCredentials();
        credentials.setPassword( cfg.getPassword().toCharArray() );
        credentials.setUsername( cfg.getUsername() );
        repo.setCredentials( credentials );

        return repo;
    }

    private void setBaseConfig( EditableRepository repo, AbstractRepositoryConfiguration cfg) {
        repo.setDescription( Locale.getDefault( ), cfg.getDescription() );
        String indexDir = cfg.getIndexDir();
        try
        {
            if ( StringUtils.isEmpty( indexDir )) {
                repo.setIndex( false );
                repo.setIndexPath( null );
            } else
            {
                if ( indexDir.startsWith( "file://" ) )
                {
                    repo.setIndexPath( new URI( indexDir ) );
                }
                else
                {
                    repo.setIndexPath( new URI( "file://" + indexDir ) );
                }
            }
        }
        catch ( URISyntaxException e )
        {
            log.error("Could not set index path "+cfg.getIndexDir());
            repo.setIndex(false);
        }
        repo.setLayout( cfg.getLayout() );

    }
}
