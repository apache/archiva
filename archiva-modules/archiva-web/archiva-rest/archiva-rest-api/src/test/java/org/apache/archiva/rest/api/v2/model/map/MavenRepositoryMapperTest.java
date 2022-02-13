package org.apache.archiva.rest.api.v2.model.map;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.common.filelock.DefaultFileLockManager;
import org.apache.archiva.configuration.model.ManagedRepositoryConfiguration;
import org.apache.archiva.repository.EditableManagedRepository;
import org.apache.archiva.repository.ReleaseScheme;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.UnsupportedURIException;
import org.apache.archiva.repository.base.managed.BasicManagedRepository;
import org.apache.archiva.repository.features.ArtifactCleanupFeature;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.features.StagingRepositoryFeature;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.apache.archiva.rest.api.v2.model.MavenManagedRepository;
import org.apache.archiva.rest.api.v2.model.Repository;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Period;
import java.util.Arrays;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Martin Schreier <martin_s@apache.org>
 */
class MavenRepositoryMapperTest
{

    @Test
    void map( )
    {
        MavenRepositoryMapper mapper = new MavenRepositoryMapper( );
        MavenManagedRepository repo = new MavenManagedRepository( );
        repo.setId( "repo01" );
        repo.setName( "Repo 01" );
        repo.setDescription( "This is repo 01" );
        repo.setLocation( "/data/repo01" );
        repo.setHasStagingRepository( true );
        repo.setSchedulingDefinition( "0,1,2 * * * *" );
        repo.setPackedIndexPath( ".index" );
        repo.setIndexPath( ".indexer" );
        repo.setIndex( true );
        repo.setDeleteSnapshotsOfRelease( false );
        repo.setBlocksRedeployments( false );
        repo.setReleaseSchemes( Arrays.asList( ReleaseScheme.RELEASE.name(), ReleaseScheme.SNAPSHOT.name() ) );
        repo.setCharacteristic( Repository.CHARACTERISTIC_MANAGED );
        repo.setScanned( true );
        repo.setRetentionPeriod( Period.ofDays( 10 ) );
        repo.setRetentionCount( 15 );
        repo.setSkipPackedIndexCreation( false );
        repo.setStagingRepository( "stage-repo01" );
        ManagedRepositoryConfiguration result = mapper.map( repo );

        assertNotNull( result );
        assertEquals( "repo01", result.getId( ) );
        assertEquals( "Repo 01", result.getName( ) );
        assertEquals( "This is repo 01", result.getDescription( ) );
        assertEquals( "/data/repo01", result.getLocation( ) );
        assertTrue( result.isStageRepoNeeded( ) );
        assertEquals( "0,1,2 * * * *", result.getRefreshCronExpression( ) );
        assertEquals( ".indexer", result.getIndexDir( ) );
        assertEquals( ".index", result.getPackedIndexDir( ) );
        assertFalse( result.isDeleteReleasedSnapshots( ) );
        assertFalse( result.isBlockRedeployments( ) );
        assertTrue( result.isSnapshots( ) );
        assertTrue( result.isReleases( ) );
        assertTrue( result.isScanned( ) );
        assertEquals( 10, result.getRetentionPeriod( ) );
        assertEquals( 15, result.getRetentionCount( ) );
        assertFalse( result.isSkipPackedIndexCreation( ) );

    }

    @Test
    void update( )
    {
        MavenRepositoryMapper mapper = new MavenRepositoryMapper( );
        MavenManagedRepository repo = new MavenManagedRepository( );
        ManagedRepositoryConfiguration result = new ManagedRepositoryConfiguration( );
        repo.setId( "repo01" );
        repo.setName( "Repo 01" );
        repo.setDescription( "This is repo 01" );
        repo.setLocation( "/data/repo01" );
        repo.setHasStagingRepository( true );
        repo.setSchedulingDefinition( "0,1,2 * * * *" );
        repo.setPackedIndexPath( ".index" );
        repo.setIndexPath( ".indexer" );
        repo.setIndex( true );
        repo.setDeleteSnapshotsOfRelease( false );
        repo.setBlocksRedeployments( false );
        repo.setReleaseSchemes( Arrays.asList( ReleaseScheme.RELEASE.name(), ReleaseScheme.SNAPSHOT.name() ) );
        repo.setCharacteristic( Repository.CHARACTERISTIC_MANAGED );
        repo.setScanned( true );
        repo.setRetentionPeriod( Period.ofDays( 10 ) );
        repo.setRetentionCount( 15 );
        repo.setSkipPackedIndexCreation( false );
        repo.setStagingRepository( "stage-repo01" );
         mapper.update( repo, result );

        assertNotNull( result );
        assertEquals( "repo01", result.getId( ) );
        assertEquals( "Repo 01", result.getName( ) );
        assertEquals( "This is repo 01", result.getDescription( ) );
        assertEquals( "/data/repo01", result.getLocation( ) );
        assertTrue( result.isStageRepoNeeded( ) );
        assertEquals( "0,1,2 * * * *", result.getRefreshCronExpression( ) );
        assertEquals( ".indexer", result.getIndexDir( ) );
        assertEquals( ".index", result.getPackedIndexDir( ) );
        assertFalse( result.isDeleteReleasedSnapshots( ) );
        assertFalse( result.isBlockRedeployments( ) );
        assertTrue( result.isSnapshots( ) );
        assertTrue( result.isReleases( ) );
        assertTrue( result.isScanned( ) );
        assertEquals( 10, result.getRetentionPeriod( ) );
        assertEquals( 15, result.getRetentionCount( ) );
        assertFalse( result.isSkipPackedIndexCreation( ) );
    }

    @Test
    void updateWithNullValues( )
    {
        MavenRepositoryMapper mapper = new MavenRepositoryMapper( );
        MavenManagedRepository repo = new MavenManagedRepository( );
        ManagedRepositoryConfiguration result = new ManagedRepositoryConfiguration( );
        repo.setId( "repo01" );
        repo.setName( "Repo 01" );
        repo.setDescription( "This is repo 01" );
        repo.setLocation( null );
        repo.setHasStagingRepository( true );
        repo.setSchedulingDefinition( "0,1,2 * * * *" );
        repo.setPackedIndexPath( null );
        repo.setIndexPath( null );
        repo.setIndex( true );
        repo.setDeleteSnapshotsOfRelease( false );
        repo.setBlocksRedeployments( false );
        repo.setReleaseSchemes( Arrays.asList( ReleaseScheme.RELEASE.name(), ReleaseScheme.SNAPSHOT.name() ) );
        repo.setCharacteristic( Repository.CHARACTERISTIC_MANAGED );
        repo.setScanned( true );
        repo.setRetentionPeriod( null );
        repo.setRetentionCount( 15 );
        repo.setSkipPackedIndexCreation( false );
        repo.setStagingRepository( null );
        mapper.update( repo, result );

        assertNotNull( result );
        assertEquals( "repo01", result.getId( ) );
        assertEquals( "Repo 01", result.getName( ) );
        assertEquals( "This is repo 01", result.getDescription( ) );
        assertNotNull( result.getLocation( ) );
        assertTrue( result.isStageRepoNeeded( ) );
        assertEquals( "0,1,2 * * * *", result.getRefreshCronExpression( ) );
        assertEquals( "", result.getIndexDir( ) );
        assertEquals( "", result.getPackedIndexDir( ) );
        assertFalse( result.isDeleteReleasedSnapshots( ) );
        assertFalse( result.isBlockRedeployments( ) );
        assertTrue( result.isSnapshots( ) );
        assertTrue( result.isReleases( ) );
        assertTrue( result.isScanned( ) );
        assertEquals( 100, result.getRetentionPeriod( ) );
        assertEquals( 15, result.getRetentionCount( ) );
        assertFalse( result.isSkipPackedIndexCreation( ) );
    }

    @Test
    void reverseMap( ) throws IOException, URISyntaxException, UnsupportedURIException
    {
        MavenRepositoryMapper mapper = new MavenRepositoryMapper( );

        Path tmpDir = Files.createTempDirectory( "mapper-test" );
        FilesystemStorage fsStorage = new FilesystemStorage( tmpDir, new DefaultFileLockManager( ) );
        EditableManagedRepository repository = new BasicManagedRepository( Locale.getDefault(), RepositoryType.MAVEN, "repo02", "Repo 02", fsStorage );
        repository.setDescription( Locale.getDefault(), "This is repo 02" );
        repository.setBlocksRedeployment( false );
        repository.setLocation( new URI("test-path") );
        repository.setScanned( true );
        repository.setLayout( "maven2" );
        repository.setSchedulingDefinition( "* 3,5,10 * * *" );
        IndexCreationFeature icf = repository.getFeature( IndexCreationFeature.class );
        icf.setIndexPath( new URI( ".indexer" ) );
        icf.setPackedIndexPath( new URI( ".index" ) );
        icf.setSkipPackedIndexCreation( false );
        ArtifactCleanupFeature acf = repository.getFeature( ArtifactCleanupFeature.class );
        acf.setDeleteReleasedSnapshots( false );
        acf.setRetentionPeriod( Period.ofDays( 5 ) );
        acf.setRetentionCount( 17 );
        StagingRepositoryFeature srf = repository.getFeature( StagingRepositoryFeature.class );
        srf.setStageRepoNeeded( false );

        MavenManagedRepository result = mapper.reverseMap( repository );
        assertEquals( "repo02", result.getId( ) );
        assertEquals( "Repo 02", result.getName( ) );
        assertEquals( "This is repo 02", result.getDescription( ) );
        assertFalse( result.isBlocksRedeployments( ) );
        assertEquals( "test-path", result.getLocation( ) );
        assertTrue( result.isScanned( ) );
        assertEquals( "maven2", result.getLayout( ) );
        assertEquals( "* 3,5,10 * * *", result.getSchedulingDefinition( ) );
        assertEquals( ".indexer", result.getIndexPath( ) );
        assertEquals( ".index", result.getPackedIndexPath( ) );
        assertFalse( result.isSkipPackedIndexCreation( ) );
        assertFalse( result.isDeleteSnapshotsOfRelease( ) );
        assertEquals( Period.ofDays( 5 ), result.getRetentionPeriod( ) );
        assertEquals( 17, result.getRetentionCount( ) );
        assertFalse( result.hasStagingRepository( ) );

    }

    @Test
    void reverseUpdate( ) throws IOException, URISyntaxException, UnsupportedURIException
    {
        MavenRepositoryMapper mapper = new MavenRepositoryMapper( );
        MavenManagedRepository result = new MavenManagedRepository( );
        Path tmpDir = Files.createTempDirectory( "mapper-test" );
        FilesystemStorage fsStorage = new FilesystemStorage( tmpDir, new DefaultFileLockManager( ) );
        EditableManagedRepository repository = new BasicManagedRepository( Locale.getDefault(), RepositoryType.MAVEN, "repo02", "Repo 02", fsStorage );
        repository.setDescription( Locale.getDefault(), "This is repo 02" );
        repository.setBlocksRedeployment( false );
        repository.setLocation( new URI("test-path") );
        repository.setScanned( true );
        repository.setLayout( "maven2" );
        repository.setSchedulingDefinition( "* 3,5,10 * * *" );
        IndexCreationFeature icf = repository.getFeature( IndexCreationFeature.class );
        icf.setIndexPath( new URI( ".indexer" ) );
        icf.setPackedIndexPath( new URI( ".index" ) );
        icf.setSkipPackedIndexCreation( false );
        ArtifactCleanupFeature acf = repository.getFeature( ArtifactCleanupFeature.class );
        acf.setDeleteReleasedSnapshots( false );
        acf.setRetentionPeriod( Period.ofDays( 5 ) );
        acf.setRetentionCount( 17 );
        StagingRepositoryFeature srf = repository.getFeature( StagingRepositoryFeature.class );
        srf.setStageRepoNeeded( false );

        mapper.reverseUpdate( repository, result );

        assertEquals( "repo02", result.getId( ) );
        assertEquals( "Repo 02", result.getName( ) );
        assertEquals( "This is repo 02", result.getDescription( ) );
        assertFalse( result.isBlocksRedeployments( ) );
        assertEquals( "test-path", result.getLocation( ) );
        assertTrue( result.isScanned( ) );
        assertEquals( "maven2", result.getLayout( ) );
        assertEquals( "* 3,5,10 * * *", result.getSchedulingDefinition( ) );
        assertEquals( ".indexer", result.getIndexPath( ) );
        assertEquals( ".index", result.getPackedIndexPath( ) );
        assertFalse( result.isSkipPackedIndexCreation( ) );
        assertFalse( result.isDeleteSnapshotsOfRelease( ) );
        assertEquals( Period.ofDays( 5 ), result.getRetentionPeriod( ) );
        assertEquals( 17, result.getRetentionCount( ) );
        assertFalse( result.hasStagingRepository( ) );

    }
}