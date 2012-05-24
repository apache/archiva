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

import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.rest.api.model.ArtifactContentEntry;
import org.apache.archiva.rest.api.model.ArtifactDownloadInfo;
import org.apache.archiva.rest.api.model.BrowseResult;
import org.apache.archiva.rest.api.model.BrowseResultEntry;
import org.apache.archiva.rest.api.model.Entry;
import org.apache.archiva.rest.api.model.VersionsList;
import org.apache.archiva.rest.api.services.BrowseService;
import org.apache.cxf.jaxrs.client.WebClient;
import org.fest.assertions.MapAssert;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Olivier Lamy
 */
public class BrowseServiceTest
    extends AbstractArchivaRestTest
{

    Map<String, String> toMap( List<Entry> entries )
    {
        Map<String, String> map = new HashMap<String, String>( entries.size() );

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

        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, new File( getBasedir(), "src/test/repo-with-osgi" ).getAbsolutePath() );

        BrowseService browseService = getBrowseService( authorizationHeader, false );

        Map<String, String> metadatas =
            toMap( browseService.getMetadatas( "commons-cli", "commons-cli", "1.0", testRepoId ) );

        assertThat( metadatas ).isNotNull().isEmpty();

        browseService.addMetadata( "commons-cli", "commons-cli", "1.0", "wine", "bordeaux", testRepoId );

        metadatas = toMap( browseService.getMetadatas( "commons-cli", "commons-cli", "1.0", testRepoId ) );

        assertThat( metadatas ).isNotNull().isNotEmpty().includes( MapAssert.entry( "wine", "bordeaux" ) );

        deleteTestRepo( testRepoId );

    }


    @Test
    public void metadatagetthenaddthendelete()
        throws Exception
    {

        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, new File( getBasedir(), "src/test/repo-with-osgi" ).getAbsolutePath() );

        BrowseService browseService = getBrowseService( authorizationHeader, false );

        Map<String, String> metadatas =
            toMap( browseService.getMetadatas( "commons-cli", "commons-cli", "1.0", testRepoId ) );

        assertThat( metadatas ).isNotNull().isEmpty();

        browseService.addMetadata( "commons-cli", "commons-cli", "1.0", "wine", "bordeaux", testRepoId );

        metadatas = toMap( browseService.getMetadatas( "commons-cli", "commons-cli", "1.0", testRepoId ) );

        assertThat( metadatas ).isNotNull().isNotEmpty().includes( MapAssert.entry( "wine", "bordeaux" ) );

        browseService.deleteMetadata( "commons-cli", "commons-cli", "1.0", "wine", testRepoId );

        metadatas = toMap( browseService.getMetadatas( "commons-cli", "commons-cli", "1.0", testRepoId ) );

        assertThat( metadatas ).isNotNull().isEmpty();

        deleteTestRepo( testRepoId );

    }

    @Test
    public void browserootGroups()
        throws Exception
    {

        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, new File( getBasedir(), "src/test/repo-with-osgi" ).getAbsolutePath(), false );

        BrowseService browseService = getBrowseService( authorizationHeader, false );

        BrowseResult browseResult = browseService.getRootGroups( testRepoId );
        assertThat( browseResult ).isNotNull();
        assertThat( browseResult.getBrowseResultEntries() ).isNotNull().isNotEmpty().hasSize( 3 ).contains(
            new BrowseResultEntry( "commons-cli", false ), new BrowseResultEntry( "commons-logging", false ),
            new BrowseResultEntry( "org.apache", false ) );

        deleteTestRepo( testRepoId );

    }

    @Test
    public void browsegroupId()
        throws Exception
    {

        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, new File( getBasedir(), "src/test/repo-with-osgi" ).getAbsolutePath(), false );

        BrowseService browseService = getBrowseService( authorizationHeader, false );

        BrowseResult browseResult = browseService.browseGroupId( "org.apache", testRepoId );
        assertThat( browseResult ).isNotNull();
        assertThat( browseResult.getBrowseResultEntries() ).isNotNull().isNotEmpty().hasSize( 2 ).contains(
            new BrowseResultEntry( "org.apache.felix", false ),
            new BrowseResultEntry( "org.apache.karaf.features", false ) );

        deleteTestRepo( testRepoId );

    }


    @Test
    public void browsegroupIdWithReleaseStartNumber()
        throws Exception
    {

        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, new File( getBasedir(), "src/test/repo-with-osgi" ).getAbsolutePath(), false );

        BrowseService browseService = getBrowseService( authorizationHeader, false );
        BrowseResult browseResult = browseService.browseGroupId( "commons-logging.commons-logging", testRepoId );
        log.info( "browseResult: {}", browseResult );

        deleteTestRepo( testRepoId );

    }

    @Test
    public void versionsList()
        throws Exception
    {

        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, new File( getBasedir(), "src/test/repo-with-osgi" ).getAbsolutePath(), false );

        BrowseService browseService = getBrowseService( authorizationHeader, false );

        VersionsList versions =
            browseService.getVersionsList( "org.apache.karaf.features", "org.apache.karaf.features.core", testRepoId );
        assertThat( versions ).isNotNull();
        assertThat( versions.getVersions() ).isNotNull().isNotEmpty().hasSize( 2 ).contains( "2.2.1", "2.2.2" );

        deleteTestRepo( testRepoId );

    }

    @Test
    public void getProjectVersionMetadata()
        throws Exception
    {
        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, new File( getBasedir(), "src/test/repo-with-osgi" ).getAbsolutePath(), false );

        BrowseService browseService = getBrowseService( authorizationHeader, true );

        ProjectVersionMetadata metadata =
            browseService.getProjectVersionMetadata( "org.apache.karaf.features", "org.apache.karaf.features.core",
                                                     testRepoId );

        assertThat( metadata ).isNotNull();

        deleteTestRepo( testRepoId );
    }

    @Test
    public void readArtifactContentEntries()
        throws Exception
    {
        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, new File( getBasedir(), "src/test/repo-with-osgi" ).getAbsolutePath(), false );

        BrowseService browseService = getBrowseService( authorizationHeader, true );

        List<ArtifactContentEntry> artifactContentEntries =
            browseService.getArtifactContentEntries( "commons-logging", "commons-logging", "1.1", null, null, null,
                                                     testRepoId );

        log.info( "artifactContentEntries: {}", artifactContentEntries );

        assertThat( artifactContentEntries ).isNotNull().isNotEmpty().hasSize( 2 ).contains(
            new ArtifactContentEntry( "org", false, 0, testRepoId ),
            new ArtifactContentEntry( "META-INF", false, 0, testRepoId ) );
        deleteTestRepo( testRepoId );
    }

    @Test
    public void readArtifactContentEntriesRootPath()
        throws Exception
    {
        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, new File( getBasedir(), "src/test/repo-with-osgi" ).getAbsolutePath(), false );

        BrowseService browseService = getBrowseService( authorizationHeader, true );

        List<ArtifactContentEntry> artifactContentEntries =
            browseService.getArtifactContentEntries( "commons-logging", "commons-logging", "1.1", null, null, "org/",
                                                     testRepoId );

        log.info( "artifactContentEntries: {}", artifactContentEntries );

        assertThat( artifactContentEntries ).isNotNull().isNotEmpty().hasSize( 1 ).contains(
            new ArtifactContentEntry( "org/apache", false, 1, testRepoId ) );
        deleteTestRepo( testRepoId );
    }

    @Test
    public void readArtifactContentEntriesFilesAndDirectories()
        throws Exception
    {
        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, new File( getBasedir(), "src/test/repo-with-osgi" ).getAbsolutePath(), false );

        BrowseService browseService = getBrowseService( authorizationHeader, true );

        List<ArtifactContentEntry> artifactContentEntries =
            browseService.getArtifactContentEntries( "commons-logging", "commons-logging", "1.1", null, null,
                                                     "org/apache/commons/logging/", testRepoId );

        log.info( "artifactContentEntries: {}", artifactContentEntries );

        assertThat( artifactContentEntries ).isNotNull().isNotEmpty().hasSize( 10 ).contains(
            new ArtifactContentEntry( "org/apache/commons/logging/impl", false, 4, testRepoId ),
            new ArtifactContentEntry( "org/apache/commons/logging/LogSource.class", true, 4, testRepoId ) );
        deleteTestRepo( testRepoId );
    }

    @Test
    public void getArtifactDownloadInfos()
        throws Exception
    {
        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, new File( getBasedir(), "src/test/repo-with-osgi" ).getAbsolutePath(), false );

        BrowseService browseService = getBrowseService( authorizationHeader, true );

        List<ArtifactDownloadInfo> artifactDownloadInfos =
            browseService.getArtifactDownloadInfos( "commons-logging", "commons-logging", "1.1", testRepoId );

        log.info( "artifactDownloadInfos {}", artifactDownloadInfos );
        assertThat( artifactDownloadInfos ).isNotNull().isNotEmpty().hasSize( 3 );
        deleteTestRepo( testRepoId );
    }


    @Test
    public void readArtifactContentText()
        throws Exception
    {
        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, new File( getBasedir(), "src/test/repo-with-osgi" ).getAbsolutePath(), false );

        BrowseService browseService = getBrowseService( authorizationHeader, true );

        WebClient.client( browseService ).accept( MediaType.TEXT_PLAIN );

        try
        {
            String text =
                browseService.getArtifactContentText( "commons-logging", "commons-logging", "1.1", "sources", null,
                                                      "org/apache/commons/logging/LogSource.java",
                                                      testRepoId ).getContent();

            log.debug( "text: {}", text );

            assertThat( text ).contains( "package org.apache.commons.logging;" ).contains( "public class LogSource {" );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            throw e;
        }
    }


    @Test
    public void readArtifactContentTextPom()
        throws Exception
    {
        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, new File( getBasedir(), "src/test/repo-with-osgi" ).getAbsolutePath(), false );

        BrowseService browseService = getBrowseService( authorizationHeader, true );

        WebClient.client( browseService ).accept( MediaType.TEXT_PLAIN );

        try
        {
            String text =
                browseService.getArtifactContentText( "commons-logging", "commons-logging", "1.1", null, "pom", null,
                                                      testRepoId ).getContent();

            log.info( "text: {}", text );

            assertThat( text ).contains(
                "<url>http://jakarta.apache.org/commons/${pom.artifactId.substring(8)}/</url>" ).contains(
                "<subscribe>commons-dev-subscribe@jakarta.apache.org</subscribe>" );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            throw e;
        }
    }


}
