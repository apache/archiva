package org.apache.archiva.rest.services;

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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.common.utils.FileUtil;
import org.apache.archiva.maven2.model.Artifact;
import org.apache.archiva.rest.api.model.BrowseResult;
import org.apache.archiva.rest.api.model.BrowseResultEntry;
import org.apache.archiva.rest.api.model.VersionsList;
import org.apache.archiva.rest.api.services.BrowseService;
import org.apache.archiva.rest.api.services.ManagedRepositoriesService;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olivier Lamy
 */
public class RepositoriesServiceTest
    extends AbstractArchivaRestTest
{

    @Test( expected = ForbiddenException.class )
    public void scanRepoKarmaFailed()
        throws Exception
    {
        RepositoriesService service = getRepositoriesService();
        try
        {
            service.scanRepository( "id", true );
        }
        catch ( ForbiddenException e )
        {
            assertEquals( 403, e.getResponse().getStatus() );
            throw e;
        }
    }

    @Test
    public void scanRepo()
        throws Exception
    {
        RepositoriesService service = getRepositoriesService( authorizationHeader );

        ManagedRepositoriesService managedRepositoriesService = getManagedRepositoriesService( authorizationHeader );

        String repoId = managedRepositoriesService.getManagedRepositories().get( 0 ).getId();

        int timeout = 20000;
        while ( timeout > 0 && service.alreadyScanning( repoId ) )
        {
            Thread.sleep( 500 );
            timeout -= 500;
        }

        assertTrue( service.scanRepository( repoId, true ) );
    }

    @Test( expected = ForbiddenException.class )
    public void deleteArtifactKarmaFailed()
        throws Exception
    {
        try
        {
            Artifact artifact = new Artifact();
            artifact.setGroupId( "commons-logging" );
            artifact.setArtifactId( "commons-logging" );
            artifact.setVersion( "1.0.1" );
            artifact.setPackaging( "jar" );
            artifact.setContext( SOURCE_REPO_ID );

            RepositoriesService repositoriesService = getRepositoriesService( null );

            repositoriesService.deleteArtifact( artifact );
        }
        catch ( ForbiddenException e )
        {
            assertEquals( 403, e.getResponse().getStatus() );
            throw e;

        }
    }

    @Test( expected = BadRequestException.class )
    public void deleteWithRepoNull()
        throws Exception
    {
        try
        {

            RepositoriesService repositoriesService = getRepositoriesService( authorizationHeader );

            Artifact artifact = new Artifact();
            artifact.setGroupId( "commons-logging" );
            artifact.setArtifactId( "commons-logging" );
            artifact.setVersion( "1.0.1" );
            artifact.setPackaging( "jar" );

            repositoriesService.deleteArtifact( artifact );
        }
        catch ( BadRequestException e )
        {
            assertEquals( "not http " + Response.Status.BAD_REQUEST.getStatusCode() + " status",
                          Response.Status.BAD_REQUEST.getStatusCode(), e.getResponse().getStatus() );
            throw e;
        }
    }


    /**
     * delete a version of an artifact without packaging
     *
     * @throws Exception
     */
    @Test
    public void deleteArtifactVersion()
        throws Exception
    {
        initSourceTargetRepo();

        BrowseService browseService = getBrowseService( authorizationHeader, false );

        List<Artifact> artifacts =
            browseService.getArtifactDownloadInfos( "org.apache.karaf.features", "org.apache.karaf.features.core",
                                                    "2.2.2", SOURCE_REPO_ID );

        log.info( "artifacts: {}", artifacts );

        assertThat( artifacts ).isNotNull().isNotEmpty().hasSize( 2 );

        VersionsList versionsList =
            browseService.getVersionsList( "org.apache.karaf.features", "org.apache.karaf.features.core",
                                           SOURCE_REPO_ID );
        assertThat( versionsList.getVersions() ).isNotNull().isNotEmpty().hasSize( 2 );

        log.info( "artifacts.size: {}", artifacts.size() );

        try
        {
            File artifactFile = new File(
                "target/test-origin-repo/org/apache/karaf/features/org.apache.karaf.features.core/2.2.2/org.apache.karaf.features.core-2.2.2.jar" );

            assertTrue( "artifact not exists:" + artifactFile.getPath(), artifactFile.exists() );

            Artifact artifact = new Artifact();
            artifact.setGroupId( "org.apache.karaf.features" );
            artifact.setArtifactId( "org.apache.karaf.features.core" );
            artifact.setVersion( "2.2.2" );
            artifact.setContext( SOURCE_REPO_ID );

            RepositoriesService repositoriesService = getRepositoriesService( authorizationHeader );

            repositoriesService.deleteArtifact( artifact );

            assertFalse( "artifact not deleted exists:" + artifactFile.getPath(), artifactFile.exists() );

            artifacts =
                browseService.getArtifactDownloadInfos( "org.apache.karaf.features", "org.apache.karaf.features.core",
                                                        "2.2.2", SOURCE_REPO_ID );

            assertThat( artifacts ).isNotNull().isEmpty();

            versionsList = browseService.getVersionsList( "org.apache.karaf.features", "org.apache.karaf.features.core",
                                                          SOURCE_REPO_ID );

            assertThat( versionsList.getVersions() ).isNotNull().isNotEmpty().hasSize( 1 );

        }
        finally
        {
            cleanRepos();
        }
    }


    @Test
    public void deleteArtifact()
        throws Exception
    {
        initSourceTargetRepo();

        BrowseService browseService = getBrowseService( authorizationHeader, false );

        List<Artifact> artifacts =
            browseService.getArtifactDownloadInfos( "org.apache.karaf.features", "org.apache.karaf.features.core",
                                                    "2.2.2", SOURCE_REPO_ID );

        log.info( "artifacts: {}", artifacts );

        assertThat( artifacts ).isNotNull().isNotEmpty().hasSize( 2 );

        VersionsList versionsList =
            browseService.getVersionsList( "org.apache.karaf.features", "org.apache.karaf.features.core",
                                           SOURCE_REPO_ID );
        assertThat( versionsList.getVersions() ).isNotNull().isNotEmpty().hasSize( 2 );

        log.info( "artifacts.size: {}", artifacts.size() );

        try
        {
            File artifactFile = new File(
                "target/test-origin-repo/org/apache/karaf/features/org.apache.karaf.features.core/2.2.2/org.apache.karaf.features.core-2.2.2.jar" );

            assertTrue( "artifact not exists:" + artifactFile.getPath(), artifactFile.exists() );

            Artifact artifact = new Artifact();
            artifact.setGroupId( "org.apache.karaf.features" );
            artifact.setArtifactId( "org.apache.karaf.features.core" );
            artifact.setVersion( "2.2.2" );
            artifact.setPackaging( "jar" );
            artifact.setContext( SOURCE_REPO_ID );

            RepositoriesService repositoriesService = getRepositoriesService( authorizationHeader );

            repositoriesService.deleteArtifact( artifact );

            assertFalse( "artifact not deleted exists:" + artifactFile.getPath(), artifactFile.exists() );

            artifacts =
                browseService.getArtifactDownloadInfos( "org.apache.karaf.features", "org.apache.karaf.features.core",
                                                        "2.2.2", SOURCE_REPO_ID );

            assertThat( artifacts ).isNotNull().isEmpty();

            versionsList = browseService.getVersionsList( "org.apache.karaf.features", "org.apache.karaf.features.core",
                                                          SOURCE_REPO_ID );

            assertThat( versionsList.getVersions() ).isNotNull().isNotEmpty().hasSize( 1 );

        }
        finally
        {
            cleanRepos();
        }
    }

    @Test
    public void deleteArtifactWithClassifier()
        throws Exception
    {
        initSourceTargetRepo();

        BrowseService browseService = getBrowseService( authorizationHeader, false );

        List<Artifact> artifacts =
            browseService.getArtifactDownloadInfos( "commons-logging", "commons-logging", "1.0.1", SOURCE_REPO_ID );

        assertThat( artifacts ).isNotNull().isNotEmpty().hasSize( 3 );

        VersionsList versionsList =
            browseService.getVersionsList( "commons-logging", "commons-logging", SOURCE_REPO_ID );
        assertThat( versionsList.getVersions() ).isNotNull().isNotEmpty().hasSize( 6 );

        log.info( "artifacts.size: {}", artifacts.size() );

        try
        {
            File artifactFile = new File(
                "target/test-origin-repo/commons-logging/commons-logging/1.0.1/commons-logging-1.0.1-javadoc.jar" );

            File artifactFilemd5 = new File(
                "target/test-origin-repo/commons-logging/commons-logging/1.0.1/commons-logging-1.0.1-javadoc.jar.md5" );

            File artifactFilesha1 = new File(
                "target/test-origin-repo/commons-logging/commons-logging/1.0.1/commons-logging-1.0.1-javadoc.jar.sha1" );

            assertTrue( "artifact not exists:" + artifactFile.getPath(), artifactFile.exists() );

            assertTrue( "md5 not exists:" + artifactFilemd5.getPath(), artifactFilemd5.exists() );
            assertTrue( "sha1 not exists:" + artifactFilesha1.getPath(), artifactFilesha1.exists() );

            Artifact artifact = new Artifact();
            artifact.setGroupId( "commons-logging" );
            artifact.setArtifactId( "commons-logging" );
            artifact.setVersion( "1.0.1" );
            artifact.setClassifier( "javadoc" );
            artifact.setPackaging( "jar" );
            artifact.setContext( SOURCE_REPO_ID );

            RepositoriesService repositoriesService = getRepositoriesService( authorizationHeader );

            repositoriesService.deleteArtifact( artifact );

            assertFalse( "artifact not deleted exists:" + artifactFile.getPath(), artifactFile.exists() );
            assertFalse( "md5 still exists:" + artifactFilemd5.getPath(), artifactFilemd5.exists() );
            assertFalse( "sha1 still exists:" + artifactFilesha1.getPath(), artifactFilesha1.exists() );

            artifacts =
                browseService.getArtifactDownloadInfos( "commons-logging", "commons-logging", "1.0.1", SOURCE_REPO_ID );

            log.info( "artifact: {}", artifacts );

            assertThat( artifacts ).isNotNull().isNotEmpty().hasSize( 2 );

            versionsList = browseService.getVersionsList( "commons-logging", "commons-logging", SOURCE_REPO_ID );

            log.info( "versionsList: {}", versionsList );

            assertThat( versionsList.getVersions() ).isNotNull().isNotEmpty().hasSize( 6 );

        }
        finally
        {
            cleanRepos();
        }
    }


    @Test
    public void deleteGroupId()
        throws Exception
    {
        initSourceTargetRepo();
        try
        {
            BrowseService browseService = getBrowseService( authorizationHeader, false );

            BrowseResult browseResult = browseService.browseGroupId( "org.apache.karaf.features", SOURCE_REPO_ID );

            assertNotNull( browseResult );

            log.info( "browseResult: {}", browseResult );

            assertThat( browseResult.getBrowseResultEntries() ).isNotNull().isNotEmpty().contains(
                new BrowseResultEntry( "org.apache.karaf.features.org.apache.karaf.features.command", true ),
                new BrowseResultEntry( "org.apache.karaf.features.org.apache.karaf.features.core", true ) );

            File directory =
                new File( "target/test-origin-repo/org/apache/karaf/features/org.apache.karaf.features.command" );

            assertTrue( "directory not exists", directory.exists() );

            RepositoriesService repositoriesService = getRepositoriesService( authorizationHeader );
            repositoriesService.deleteGroupId( "org.apache.karaf", SOURCE_REPO_ID );

            assertFalse( "directory not exists", directory.exists() );

            browseResult = browseService.browseGroupId( "org.apache.karaf.features", SOURCE_REPO_ID );

            assertNotNull( browseResult );

            assertThat( browseResult.getBrowseResultEntries() ).isNotNull().isEmpty();

            browseResult = browseService.browseGroupId( "org.apache.karaf", SOURCE_REPO_ID );

            assertNotNull( browseResult );

            assertThat( browseResult.getBrowseResultEntries() ).isNotNull().isEmpty();

            log.info( "browseResult empty: {}", browseResult );
        }
        finally
        {
            cleanRepos();
        }
    }

    @Test
    public void authorizedToDeleteArtifacts()
        throws Exception
    {
        ManagedRepository managedRepository = getTestManagedRepository( "SOURCE_REPO_ID", "SOURCE_REPO_ID" );
        try
        {
            getManagedRepositoriesService( authorizationHeader ).addManagedRepository( managedRepository );
            RepositoriesService repositoriesService = getRepositoriesService( authorizationHeader );
            assertTrue( repositoriesService.isAuthorizedToDeleteArtifacts( managedRepository.getId() ) );
        }
        finally
        {
            cleanQuietlyRepo( managedRepository.getId() );
        }
    }

    @Test
    public void notAuthorizedToDeleteArtifacts()
        throws Exception
    {
        ManagedRepository managedRepository = getTestManagedRepository( "SOURCE_REPO_ID", "SOURCE_REPO_ID" );
        try
        {
            getManagedRepositoriesService( authorizationHeader ).addManagedRepository( managedRepository );
            RepositoriesService repositoriesService = getRepositoriesService( guestAuthzHeader );
            assertFalse( repositoriesService.isAuthorizedToDeleteArtifacts( managedRepository.getId() ) );
        }
        finally
        {
            cleanQuietlyRepo( managedRepository.getId() );
        }
    }

    protected void cleanQuietlyRepo( String id )
    {
        try
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( id, true );
        }
        catch ( Exception e )
        {
            log.info( "ignore issue deleting test repo: {}", e.getMessage() );
        }
    }

    @Test
    public void deleteSnapshot()
        throws Exception
    {
        File targetRepo = initSnapshotRepo();
        try
        {

            RepositoriesService repositoriesService = getRepositoriesService( authorizationHeader );
            //repositoriesService.scanRepositoryDirectoriesNow( SNAPSHOT_REPO_ID );

            BrowseService browseService = getBrowseService( authorizationHeader, false );
            List<Artifact> artifacts =
                browseService.getArtifactDownloadInfos( "org.apache.archiva.redback.components", "spring-quartz",
                                                        "2.0-SNAPSHOT", SNAPSHOT_REPO_ID );

            log.info( "artifacts: {}", artifacts );

            assertThat( artifacts ).isNotNull().isNotEmpty().hasSize( 10 );

            File artifactFile = new File( targetRepo,
                                          "org/apache/archiva/redback/components/spring-quartz/2.0-SNAPSHOT/spring-quartz-2.0-20120618.214127-1.jar" );

            File artifactFilemd5 = new File( targetRepo,
                                             "org/apache/archiva/redback/components/spring-quartz/2.0-SNAPSHOT/spring-quartz-2.0-20120618.214127-1.jar.md5" );

            File artifactFilepom = new File( targetRepo,
                                             "org/apache/archiva/redback/components/spring-quartz/2.0-SNAPSHOT/spring-quartz-2.0-20120618.214127-1.pom" );

            assertThat( artifactFile ).exists();
            assertThat( artifactFilemd5 ).exists();
            assertThat( artifactFilepom ).exists();

            // we delete only one snapshot
            Artifact artifact =
                new Artifact( "org.apache.archiva.redback.components", "spring-quartz", "2.0-20120618.214127-1" );
            artifact.setPackaging( "jar" );
            artifact.setRepositoryId( SNAPSHOT_REPO_ID );
            artifact.setContext( SNAPSHOT_REPO_ID );

            repositoriesService.deleteArtifact( artifact );

            artifacts =
                browseService.getArtifactDownloadInfos( "org.apache.archiva.redback.components", "spring-quartz",
                                                        "2.0-SNAPSHOT", SNAPSHOT_REPO_ID );

            log.info( "artifacts: {}", artifacts );

            assertThat( artifacts ).isNotNull().isNotEmpty().hasSize( 8 );

            assertThat( artifactFile ).doesNotExist();
            assertThat( artifactFilemd5 ).doesNotExist();
            assertThat( artifactFilepom ).doesNotExist();
        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            throw e;
        }
        finally
        {
            cleanSnapshotRepo();
        }
    }

    protected File initSnapshotRepo()
        throws Exception
    {
        File targetRepo = new File( getBasedir(), "target/repo-with-snapshots" );
        if ( targetRepo.exists() )
        {
            FileUtils.deleteDirectory( targetRepo );
        }
        assertFalse( targetRepo.exists() );

        FileUtils.copyDirectoryToDirectory( new File( getBasedir(), "src/test/repo-with-snapshots" ),
                                            targetRepo.getParentFile() );

        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( SNAPSHOT_REPO_ID ) != null )
        {
            getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( SNAPSHOT_REPO_ID, true );
            assertNull( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( SNAPSHOT_REPO_ID ) );
        }
        ManagedRepository managedRepository = getTestManagedRepository( SNAPSHOT_REPO_ID, "repo-with-snapshots" );
        /*managedRepository.setId( SNAPSHOT_REPO_ID );
        managedRepository.setLocation( );
        managedRepository.setCronExpression( "* * * * * ?" );*/
        getManagedRepositoriesService( authorizationHeader ).addManagedRepository( managedRepository );
        assertNotNull( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( SNAPSHOT_REPO_ID ) );

        return targetRepo;
    }

    protected void cleanSnapshotRepo()
        throws Exception
    {

        if ( getManagedRepositoriesService( authorizationHeader ).getManagedRepository( SNAPSHOT_REPO_ID ) != null )
        {
            try
            {
                getManagedRepositoriesService( authorizationHeader ).deleteManagedRepository( SNAPSHOT_REPO_ID, true );
                assertNull(
                    getManagedRepositoriesService( authorizationHeader ).getManagedRepository( SNAPSHOT_REPO_ID ) );
            }
            catch ( Exception e )
            {
                log.warn( "skip issue while cleaning test repository: this can cause test failure", e );
            }
        }

    }

    protected ManagedRepository getTestManagedRepository( String id, String path )
    {
        String location = new File( FileUtil.getBasedir(), "target/" + path ).getAbsolutePath();
        return new ManagedRepository( id, id, location, "default", true, true, true, "2 * * * * ?", null, false, 80, 80,
                                      true, false );
    }

    @Override
    protected ManagedRepository getTestManagedRepository()
    {
        return getTestManagedRepository( "TEST", "test-repo" );
    }


    static final String SNAPSHOT_REPO_ID = "snapshot-repo";


}
