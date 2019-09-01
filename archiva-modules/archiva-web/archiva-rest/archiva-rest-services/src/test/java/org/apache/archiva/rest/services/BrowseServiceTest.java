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
import org.apache.archiva.maven2.model.Artifact;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.redback.rest.api.model.Role;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.apache.archiva.rest.api.model.ArtifactContentEntry;
import org.apache.archiva.rest.api.model.BrowseResult;
import org.apache.archiva.rest.api.model.BrowseResultEntry;
import org.apache.archiva.rest.api.model.Entry;
import org.apache.archiva.rest.api.model.MetadataAddRequest;
import org.apache.archiva.rest.api.model.VersionsList;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.BrowseService;
import org.apache.cxf.jaxrs.client.WebClient;
import org.assertj.core.data.MapEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olivier Lamy
 */
public class BrowseServiceTest
    extends AbstractArchivaRestTest
{
    private static final String TEST_REPO_ID = "test-repo";

    Map<String, String> toMap( List<Entry> entries )
    {
        Map<String, String> map = new HashMap<>( entries.size() );

        for ( Entry entry : entries )
        {
            map.put( entry.getKey(), entry.getValue() );
        }

        return map;
    }

    @Test
    public void metadatagetthenadd()
        throws Exception
    {
        scanRepo( TEST_REPO_ID );
        waitForScanToComplete( TEST_REPO_ID );

        BrowseService browseService = getBrowseService( authorizationHeader, false );

        Map<String, String> metadatas =
            toMap( browseService.getMetadatas( "commons-cli", "commons-cli", "1.0", TEST_REPO_ID ) );

        assertThat( metadatas ).isNotNull().isEmpty();

        browseService.addMetadata( "commons-cli", "commons-cli", "1.0", "wine", "bordeaux", TEST_REPO_ID );

        metadatas = toMap( browseService.getMetadatas( "commons-cli", "commons-cli", "1.0", TEST_REPO_ID ) );

        assertThat( metadatas ).isNotNull().isNotEmpty().contains( MapEntry.entry( "wine", "bordeaux" ) );
    }


    @Test
    public void metadatagetthenaddthendelete()
        throws Exception
    {
        try
        {
            scanRepo( TEST_REPO_ID );
            waitForScanToComplete( TEST_REPO_ID );

            BrowseService browseService = getBrowseService( authorizationHeader, false );

            Map<String, String> metadatas =
                toMap( browseService.getMetadatas( "commons-cli", "commons-cli", "1.0", TEST_REPO_ID ) );

            assertThat( metadatas ).isNotNull().isEmpty();

            browseService.addMetadata( "commons-cli", "commons-cli", "1.0", "wine", "bordeaux", TEST_REPO_ID );

            metadatas = toMap( browseService.getMetadatas( "commons-cli", "commons-cli", "1.0", TEST_REPO_ID ) );

            assertThat( metadatas ).isNotNull().isNotEmpty().contains( MapEntry.entry( "wine", "bordeaux" ) );

            browseService.deleteMetadata( "commons-cli", "commons-cli", "1.0", "wine", TEST_REPO_ID );

            metadatas = toMap( browseService.getMetadatas( "commons-cli", "commons-cli", "1.0", TEST_REPO_ID ) );

            assertThat( metadatas ).isNotNull().isEmpty();
        }
        catch ( ArchivaRestServiceException e )
        {
            log.error( e.getMessage(), e );
            throw e;
        }
    }

    @Test
    public void browserootGroups()
        throws Exception
    {
        BrowseService browseService = getBrowseService( authorizationHeader, false );

        BrowseResult browseResult = browseService.getRootGroups( TEST_REPO_ID );
        assertThat( browseResult ).isNotNull();
        assertThat( browseResult.getBrowseResultEntries() ) //
            .isNotNull() //
            .isNotEmpty() //
            .hasSize( 3 ) //
            .contains( new BrowseResultEntry( "commons-cli", false ), //
                       new BrowseResultEntry( "commons-logging", false ), //
                       new BrowseResultEntry( "org.apache", false ) );
    }

    @Test
    public void browsegroupId()
        throws Exception
    {
        BrowseService browseService = getBrowseService( authorizationHeader, false );

        BrowseResult browseResult = browseService.browseGroupId( "org.apache", TEST_REPO_ID );
        assertThat( browseResult ).isNotNull();
        assertThat( browseResult.getBrowseResultEntries() ) //
            .isNotNull() //
            .isNotEmpty() //
            .hasSize( 2 ) //
            .contains( new BrowseResultEntry( "org.apache.felix", false ), //
                       new BrowseResultEntry( "org.apache.karaf.features", false ) );
    }

    @Test
    public void listUserRepositories()
            throws Exception
    {
        initSourceTargetRepo();
        BrowseService browseService = getBrowseService( authorizationHeader, false );

        List<ManagedRepository> browseResult = browseService.getUserRepositories();
        assertThat( browseResult )
                .isNotNull()
                .isNotEmpty()
                .hasSize(5);
        List<String> repIds = new ArrayList<>();
        for(ManagedRepository rep : browseResult) {
            repIds.add(rep.getId());
        }
        assertThat(repIds).contains("internal","snapshots","test-repo","test-copy-target","test-origin-repo");

    }


    @Test
    public void listUserManagableRepositories()
            throws Exception
    {
        initSourceTargetRepo();
        // Giving the guest user a manager role
        String name = "Repository Manager - internal";
        Role role = getRoleManagementService( authorizationHeader ).getRole( name );
        role.setUsers( Arrays.asList( getUserService( authorizationHeader ).getUser( "guest" ) ) );
        getRoleManagementService( authorizationHeader ).updateRoleUsers( role );

        // browseService with guest user
        BrowseService browseService = getBrowseService( "", false );

        List<ManagedRepository> browseResult = browseService.getUserManagableRepositories();
        assertThat( browseResult )
                .isNotNull()
                .isNotEmpty().hasSize(1);
        List<String> repIds = new ArrayList<>();
        for(ManagedRepository rep : browseResult) {
            repIds.add(rep.getId());
        }
        assertThat(repIds).contains("internal");

    }

    @Test
    public void browsegroupIdWithReleaseStartNumber()
        throws Exception
    {
        BrowseService browseService = getBrowseService( authorizationHeader, false );
        BrowseResult browseResult = browseService.browseGroupId( "commons-logging.commons-logging", TEST_REPO_ID );
        log.info( "browseResult: {}", browseResult );
    }

    @Test
    public void versionsList()
        throws Exception
    {
        BrowseService browseService = getBrowseService( authorizationHeader, false );

        VersionsList versions =
            browseService.getVersionsList( "org.apache.karaf.features", "org.apache.karaf.features.core",
                                           TEST_REPO_ID );
        assertThat( versions ).isNotNull();
        assertThat( versions.getVersions() ) //
            .isNotNull() //
            .isNotEmpty() //
            .hasSize( 2 ) //
            .contains( "2.2.1", "2.2.2" );
    }

    @Test
    public void getProjectVersionMetadata()
        throws Exception
    {
        BrowseService browseService = getBrowseService( authorizationHeader, true );

        ProjectVersionMetadata metadata =
            browseService.getProjectVersionMetadata( "org.apache.karaf.features", "org.apache.karaf.features.core",
                                                     TEST_REPO_ID );

        assertThat( metadata ).isNotNull();
    }

    @Test
    public void readArtifactContentEntries()
        throws Exception
    {
        BrowseService browseService = getBrowseService( authorizationHeader, true );

        List<ArtifactContentEntry> artifactContentEntries =
            browseService.getArtifactContentEntries( "commons-logging", "commons-logging", "1.1", null, null, null,
                                                     TEST_REPO_ID );

        log.info( "artifactContentEntries: {}", artifactContentEntries );

        assertThat( artifactContentEntries ).isNotNull() //
            .isNotEmpty() //
            .hasSize( 2 ) //
            .contains( new ArtifactContentEntry( "org", false, 0, TEST_REPO_ID ), //
                       new ArtifactContentEntry( "META-INF", false, 0, TEST_REPO_ID ) );
    }

    @Test
    public void readArtifactContentEntriesRootPath()
        throws Exception
    {
        BrowseService browseService = getBrowseService( authorizationHeader, true );

        List<ArtifactContentEntry> artifactContentEntries =
            browseService.getArtifactContentEntries( "commons-logging", "commons-logging", "1.1", null, null, "org/",
                                                     TEST_REPO_ID );

        log.info( "artifactContentEntries: {}", artifactContentEntries );

        assertThat( artifactContentEntries ).isNotNull() //
            .isNotEmpty() //
            .hasSize( 1 ) //
            .contains( new ArtifactContentEntry( "org/apache", false, 1, TEST_REPO_ID ) );
    }

    @Test
    public void readArtifactContentEntriesFilesAndDirectories()
        throws Exception
    {
        BrowseService browseService = getBrowseService( authorizationHeader, true );

        List<ArtifactContentEntry> artifactContentEntries =
            browseService.getArtifactContentEntries( "commons-logging", "commons-logging", "1.1", null, null,
                                                     "org/apache/commons/logging/", TEST_REPO_ID );

        log.info( "artifactContentEntries: {}", artifactContentEntries );

        assertThat( artifactContentEntries ).isNotNull().isNotEmpty().hasSize( 10 ).contains(
            new ArtifactContentEntry( "org/apache/commons/logging/impl", false, 4, TEST_REPO_ID ),
            new ArtifactContentEntry( "org/apache/commons/logging/LogSource.class", true, 4, TEST_REPO_ID ) );
    }

    @Test
    public void getArtifactDownloadInfos()
        throws Exception
    {
        BrowseService browseService = getBrowseService( authorizationHeader, true );

        List<Artifact> artifactDownloadInfos =
            browseService.getArtifactDownloadInfos( "commons-logging", "commons-logging", "1.1", TEST_REPO_ID );

        log.info( "artifactDownloadInfos {}", artifactDownloadInfos );
        assertThat( artifactDownloadInfos ).isNotNull().isNotEmpty().hasSize( 3 );
    }


    @Test
    public void getArtifactsByMetadata()
        throws Exception
    {
        // START SNIPPET: get-artifacts-by-metadata
        BrowseService browseService = getBrowseService( authorizationHeader, true );

        List<Artifact> artifactDownloadInfos = browseService.getArtifactsByMetadata( "type", "pom", TEST_REPO_ID );

        assertThat( artifactDownloadInfos ).isNotNull().isNotEmpty().hasSize( 11 );
        // END SNIPPET: get-artifacts-by-metadata
    }


    @Test
    public void getArtifactsByProjectVersionMetadata()
        throws Exception
    {
        // START SNIPPET: get-artifacts-by-project-version-metadata
        BrowseService browseService = getBrowseService( authorizationHeader, true );

        browseService.addMetadata( "commons-cli", "commons-cli", "1.0", "wine", "bordeaux", TEST_REPO_ID );

        tryAssert( ( ) -> {
            List<Artifact> artifactDownloadInfos =
                browseService.getArtifactsByProjectVersionMetadata( "wine", "bordeaux", TEST_REPO_ID );

            assertThat( artifactDownloadInfos ).isNotNull( ).isNotEmpty( ).hasSize( 3 );
            // END SNIPPET: get-artifacts-by-project-version-metadata
        } );
    }


    @Test
    public void getArtifactsByProjectVersionMetadataWithNoRepository()
        throws Exception
    {
        final BrowseService browseService = getBrowseService( authorizationHeader, true );

        browseService.addMetadata( "commons-cli", "commons-cli", "1.0", "wine", "bordeaux", TEST_REPO_ID );


        tryAssert( ( ) -> {
            List<Artifact> artifactDownloadInfos =
                null;
            try
            {
                artifactDownloadInfos = browseService.getArtifactsByProjectVersionMetadata( "wine", "bordeaux", null );
            }
            catch ( ArchivaRestServiceException e )
            {
                throw new AssertionError( "ArchivaRestServiceException", e );
            }
            assertThat( artifactDownloadInfos ).isNotNull( ).isNotEmpty( ).hasSize( 3 );
        });
    }


    @Test
    public void getArtifactsByProperty()
        throws Exception
    {
        // START SNIPPET: get-artifacts-by-property
        BrowseService browseService = getBrowseService( authorizationHeader, true );

        tryAssert( ( ) -> {
            List<Artifact> artifactDownloadInfos =
                browseService.getArtifactsByProperty( "org.name", "The Apache Software Foundation", TEST_REPO_ID );

            assertThat( artifactDownloadInfos ).isNotNull( ).isNotEmpty( ).hasSize( 7 );
            // END SNIPPET: get-artifacts-by-property
        } );
    }


    @Test
    public void searchArtifacts()
        throws Exception
    {
        // START SNIPPET: search-artifacts
        BrowseService browseService = getBrowseService( authorizationHeader, true );

        tryAssert( ( ) -> {
            List<Artifact> artifactDownloadInfos =
                browseService.searchArtifacts( "The Apache Software Foundation", TEST_REPO_ID, true );

            assertThat( artifactDownloadInfos ).isNotNull( ).isNotEmpty( ).hasSize( 7 );
        } );
        // END SNIPPET: search-artifacts
    }


    @Test
    public void searchArtifactsByField()
        throws Exception
    {
        // START SNIPPET: search-artifacts-by-field
        BrowseService browseService = getBrowseService( authorizationHeader, true );

        List<Artifact> artifactDownloadInfos =
            browseService.searchArtifacts( "org.name", "The Apache Software Foundation", TEST_REPO_ID, true );

        assertThat( artifactDownloadInfos ).isNotNull().isNotEmpty().hasSize( 7 );
        // END SNIPPET: search-artifacts-by-field
    }


    @Test
    public void readArtifactContentText()
        throws Exception
    {
        BrowseService browseService = getBrowseService( authorizationHeader, true );

        WebClient.client( browseService ).accept( MediaType.TEXT_PLAIN );

        String text =
            browseService.getArtifactContentText( "commons-logging", "commons-logging", "1.1", "sources", null,
                                                  "org/apache/commons/logging/LogSource.java",
                                                  TEST_REPO_ID ).getContent();

        log.debug( "text: {}", text );

        assertThat( text ).contains( "package org.apache.commons.logging;" ).contains( "public class LogSource {" );
    }


    @Test
    public void readArtifactContentTextPom()
        throws Exception
    {
        BrowseService browseService = getBrowseService( authorizationHeader, true );

        WebClient.client( browseService ).accept( MediaType.TEXT_PLAIN );

        String text =
            browseService.getArtifactContentText( "commons-logging", "commons-logging", "1.1", null, "pom", null,
                                                  TEST_REPO_ID ).getContent();

        log.info( "text: {}", text );

        assertThat( text ).contains(
            "<url>http://jakarta.apache.org/commons/${pom.artifactId.substring(8)}/</url>" ).contains(
            "<subscribe>commons-dev-subscribe@jakarta.apache.org</subscribe>" );
    }


    @Test
    public void artifactsNumber()
        throws Exception
    {
        BrowseService browseService = getBrowseService( authorizationHeader, true );

        //WebClient.client( browseService ).accept( MediaType.TEXT_PLAIN );

        int number = browseService.getArtifacts( TEST_REPO_ID ).size();

        log.info( "getArtifactsNumber: {}", number );

        assertTrue( number > 1 );
    }

    @Test
    public void metadatainbatchmode()
        throws Exception
    {
        scanRepo( TEST_REPO_ID );
        waitForScanToComplete( TEST_REPO_ID );

        BrowseService browseService = getBrowseService( authorizationHeader, false );

        Map<String, String> inputMetadata = new HashMap<>( 3 );
        inputMetadata.put( "buildNumber", "1" );
        inputMetadata.put( "author", "alecharp" );
        inputMetadata.put( "jenkins_version", "1.486" );

        MetadataAddRequest metadataAddRequest = new MetadataAddRequest();
        metadataAddRequest.setGroupId( "commons-cli" );
        metadataAddRequest.setArtifactId( "commons-cli" );
        metadataAddRequest.setVersion( "1.0" );
        metadataAddRequest.setMetadatas( inputMetadata );
        browseService.importMetadata( metadataAddRequest, TEST_REPO_ID );

        Map<String, String> metadatas =
            toMap( browseService.getMetadatas( "commons-cli", "commons-cli", "1.0", TEST_REPO_ID ) );

        assertThat( metadatas ).isNotNull().isNotEmpty().contains( MapEntry.entry( "buildNumber", "1" ) ).contains(
            MapEntry.entry( "author", "alecharp" ) ).contains( MapEntry.entry( "jenkins_version", "1.486" ) );
    }

    @Before
    public void initialiseTestRepo()
        throws RedbackServiceException, ArchivaRestServiceException, IOException, InterruptedException
    {
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( TEST_REPO_ID, getProjectDirectory().resolve( "src/test/repo-with-osgi" ),
                            null, false );

        waitForScanToComplete( TEST_REPO_ID );
    }

    @After
    public void deleteTestRepo()
        throws Exception
    {
        scanRepo( TEST_REPO_ID );
        waitForScanToComplete( TEST_REPO_ID );
        deleteTestRepo( TEST_REPO_ID );
    }
}
