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
import org.apache.archiva.repository.RepositoryCredentials;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryProvider;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.features.ArtifactCleanupFeature;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.features.RemoteIndexFeature;
import org.apache.archiva.repository.features.StagingRepositoryFeature;
import org.apache.commons.lang.StringUtils;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;
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

    private URI getURIFromString( String uriStr) throws RepositoryException {
        URI uri;
        try {
            if (uriStr.startsWith("/")) {
                // only absolute paths are prepended with file scheme
                uri = new URI("file://"+uriStr);
            } else
            {
                uri = new URI( uriStr );
            }
            if (uri.getScheme()!=null && !"file".equals(uri.getScheme())) {
                log.error("Bad URI scheme found: {}, URI={}", uri.getScheme(), uri);
                throw new RepositoryException("The uri "+uriStr+" is not valid. Only file:// URI is allowed for maven.");
            }
        } catch (URISyntaxException e) {
            String newCfg = "file://"+uriStr;
            try
            {
                uri = new URI(newCfg);
            }
            catch ( URISyntaxException e1 )
            {
                log.error("Could not create URI from {} -> ", uriStr, newCfg);
                throw new RepositoryException( "The config entry "+uriStr+" cannot be converted to URI." );
            }
        }
        return uri;
    }

    @Override
    public ManagedRepository createManagedInstance( ManagedRepositoryConfiguration cfg ) throws RepositoryException
    {
        MavenManagedRepository repo = new MavenManagedRepository(cfg.getId() ,cfg.getName());
        repo.setLocation( getURIFromString( cfg.getLocation() ) );
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

        IndexCreationFeature indexCreationFeature = repo.getFeature( IndexCreationFeature.class ).get( );
        indexCreationFeature.setSkipPackedIndexCreation( cfg.isSkipPackedIndexCreation() );
        indexCreationFeature.setIndexPath( getURIFromString( cfg.getIndexDir() ) );

        ArtifactCleanupFeature artifactCleanupFeature = repo.getFeature( ArtifactCleanupFeature.class ).get();

        artifactCleanupFeature.setDeleteReleasedSnapshots( cfg.isDeleteReleasedSnapshots() );
        artifactCleanupFeature.setRetentionCount( cfg.getRetentionCount() );
        artifactCleanupFeature.setRetentionTime( Period.ofDays( cfg.getRetentionTime() ) );

        return repo;
    }

    @Override
    public ManagedRepository createStagingInstance( ManagedRepositoryConfiguration baseConfiguration ) throws RepositoryException
    {
        return createManagedInstance( getStageRepoConfig( baseConfiguration ) );
    }


    @Override
    public RemoteRepository createRemoteInstance( RemoteRepositoryConfiguration cfg ) throws RepositoryException
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
        repo.setTimeout( Duration.ofSeconds( cfg.getTimeout() ) );
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
        if (cfg.getPassword()!=null && cfg.getUsername()!=null)
        {
            credentials.setPassword( cfg.getPassword( ).toCharArray( ) );
            credentials.setUsername( cfg.getUsername() );
            repo.setCredentials( credentials );
        } else {
            credentials.setPassword( new char[0] );
        }
        if (cfg.getIndexDir()!=null) {
            IndexCreationFeature indexCreationFeature = repo.getFeature( IndexCreationFeature.class ).get();
            indexCreationFeature.setIndexPath( getURIFromString( cfg.getIndexDir() ) );
        }

        return repo;
    }

    @Override
    public RemoteRepositoryConfiguration getRemoteConfiguration( RemoteRepository remoteRepository ) throws RepositoryException
    {
        if (!(remoteRepository instanceof MavenRemoteRepository)) {
            log.error("Wrong remote repository type "+remoteRepository.getClass().getName());
            throw new RepositoryException( "The given repository type cannot be handled by the maven provider: "+remoteRepository.getClass().getName() );
        }
        RemoteRepositoryConfiguration cfg = new RemoteRepositoryConfiguration();
        cfg.setType( remoteRepository.getType().toString() );
        cfg.setId( remoteRepository.getId() );
        cfg.setName( remoteRepository.getName() );
        cfg.setDescription( remoteRepository.getDescription() );
        cfg.setUrl(remoteRepository.getLocation().toString());
        cfg.setTimeout( (int)remoteRepository.getTimeout().toMillis()/1000 );
        cfg.setCheckPath( remoteRepository.getCheckPath() );
        RepositoryCredentials creds = remoteRepository.getLoginCredentials( );
        if (creds!=null)
        {
            if (creds instanceof PasswordCredentials) {
                PasswordCredentials pCreds = (PasswordCredentials) creds;
                cfg.setPassword( new String(pCreds.getPassword()) );
                cfg.setUsername( pCreds.getUsername() );
            }
        }
        cfg.setLayout( remoteRepository.getLayout() );
        cfg.setExtraParameters( remoteRepository.getExtraParameters() );
        cfg.setExtraHeaders( remoteRepository.getExtraHeaders() );
        cfg.setRefreshCronExpression( remoteRepository.getSchedulingDefinition() );

        IndexCreationFeature indexCreationFeature = remoteRepository.getFeature( IndexCreationFeature.class ).get();
        cfg.setIndexDir( indexCreationFeature.getIndexPath().toString());

        RemoteIndexFeature remoteIndexFeature = remoteRepository.getFeature( RemoteIndexFeature.class ).get();
        cfg.setRemoteIndexUrl( remoteIndexFeature.getIndexUri().toString() );
        cfg.setRemoteDownloadTimeout( (int)remoteIndexFeature.getDownloadTimeout().get( ChronoUnit.SECONDS ) );
        cfg.setDownloadRemoteIndexOnStartup( remoteIndexFeature.isDownloadRemoteIndexOnStartup() );
        cfg.setDownloadRemoteIndex( remoteIndexFeature.isDownloadRemoteIndex() );
        cfg.setRemoteDownloadNetworkProxyId( remoteIndexFeature.getProxyId() );




        return cfg;

    }

    @Override
    public ManagedRepositoryConfiguration getManagedConfiguration( ManagedRepository managedRepository ) throws RepositoryException
    {
        if (!(managedRepository instanceof MavenManagedRepository)) {
            log.error("Wrong remote repository type "+managedRepository.getClass().getName());
            throw new RepositoryException( "The given repository type cannot be handled by the maven provider: "+managedRepository.getClass().getName() );
        }
        ManagedRepositoryConfiguration cfg = new ManagedRepositoryConfiguration();
        cfg.setType( managedRepository.getType().toString() );
        cfg.setId( managedRepository.getId() );
        cfg.setName( managedRepository.getName() );
        cfg.setDescription( managedRepository.getDescription() );
        cfg.setLocation( managedRepository.getLocation().toString() );
        cfg.setLayout( managedRepository.getLayout() );
        cfg.setRefreshCronExpression( managedRepository.getSchedulingDefinition() );
        cfg.setScanned( managedRepository.isScanned() );
        cfg.setBlockRedeployments( managedRepository.blocksRedeployments() );
        StagingRepositoryFeature stagingRepositoryFeature = managedRepository.getFeature( StagingRepositoryFeature.class ).get();
        cfg.setStageRepoNeeded(stagingRepositoryFeature.isStageRepoNeeded());


        IndexCreationFeature indexCreationFeature = managedRepository.getFeature( IndexCreationFeature.class ).get();
        cfg.setIndexDir( indexCreationFeature.getIndexPath().toString());
        cfg.setSkipPackedIndexCreation( indexCreationFeature.isSkipPackedIndexCreation() );

        ArtifactCleanupFeature artifactCleanupFeature = managedRepository.getFeature( ArtifactCleanupFeature.class ).get();
        cfg.setRetentionCount( artifactCleanupFeature.getRetentionCount());
        cfg.setRetentionTime( artifactCleanupFeature.getRetentionTime().getDays() );
        cfg.setDeleteReleasedSnapshots(artifactCleanupFeature.isDeleteReleasedSnapshots());

        if (managedRepository.getActiveReleaseSchemes().contains( ReleaseScheme.RELEASE )) {
            cfg.setReleases( true );
        } else {
            cfg.setReleases( false );
        }
        if (managedRepository.getActiveReleaseSchemes().contains( ReleaseScheme.SNAPSHOT )) {
            cfg.setSnapshots( true );
        } else {
            cfg.setSnapshots( false );
        }
        return cfg;

    }

    private ManagedRepositoryConfiguration getStageRepoConfig( ManagedRepositoryConfiguration repository )
    {
        ManagedRepositoryConfiguration stagingRepository = new ManagedRepositoryConfiguration();
        stagingRepository.setId( repository.getId() + StagingRepositoryFeature.STAGING_REPO_POSTFIX );
        stagingRepository.setLayout( repository.getLayout() );
        stagingRepository.setName( repository.getName() + StagingRepositoryFeature.STAGING_REPO_POSTFIX );
        stagingRepository.setBlockRedeployments( repository.isBlockRedeployments() );
        stagingRepository.setRetentionTime( repository.getRetentionTime() );
        stagingRepository.setDeleteReleasedSnapshots( repository.isDeleteReleasedSnapshots() );

        String path = repository.getLocation();
        int lastIndex = path.replace( '\\', '/' ).lastIndexOf( '/' );
        stagingRepository.setLocation( path.substring( 0, lastIndex ) + "/" + stagingRepository.getId() );

        if ( StringUtils.isNotBlank( repository.getIndexDir() ) )
        {
            Path indexDir = Paths.get( repository.getIndexDir() );
            // in case of absolute dir do not use the same
            if ( indexDir.isAbsolute() )
            {
                stagingRepository.setIndexDir( stagingRepository.getLocation() + "/.index" );
            }
            else
            {
                stagingRepository.setIndexDir( repository.getIndexDir() );
            }
        }
        stagingRepository.setRefreshCronExpression( repository.getRefreshCronExpression() );
        stagingRepository.setReleases( repository.isReleases() );
        stagingRepository.setRetentionCount( repository.getRetentionCount() );
        stagingRepository.setScanned( repository.isScanned() );
        stagingRepository.setSnapshots( repository.isSnapshots() );
        stagingRepository.setSkipPackedIndexCreation( repository.isSkipPackedIndexCreation() );
        // do not duplicate description
        //stagingRepository.getDescription("")
        return stagingRepository;
    }

    private void setBaseConfig( EditableRepository repo, AbstractRepositoryConfiguration cfg) {
        repo.setDescription( repo.getPrimaryLocale(), cfg.getDescription() );
        repo.setLayout( cfg.getLayout() );

    }
}
