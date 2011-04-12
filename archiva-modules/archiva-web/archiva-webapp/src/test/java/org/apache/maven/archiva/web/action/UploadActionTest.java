package org.apache.maven.archiva.web.action;

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

import com.opensymphony.xwork2.Action;
import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.audit.AuditListener;
import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RepositoryScanningConfiguration;
import org.apache.maven.archiva.model.ArchivaRepositoryMetadata;
import org.apache.maven.archiva.model.SnapshotVersion;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.apache.maven.archiva.repository.content.ManagedDefaultRepositoryContent;
import org.apache.maven.archiva.repository.metadata.MetadataTools;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataReader;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * UploadActionTest
 */
public class UploadActionTest
    extends AbstractActionTestCase
{
    private UploadAction uploadAction;

    private ArchivaConfiguration archivaConfig;

    private MockControl archivaConfigControl;

    private RepositoryContentFactory repoFactory;

    private MockControl repoFactoryControl;

    private static final String REPOSITORY_ID = "test-repo";

    private Configuration config;

    public void setUp()
        throws Exception
    {
        super.setUp();

        MockControl schedulerControl = MockControl.createControl( ArchivaTaskScheduler.class );
        ArchivaTaskScheduler scheduler = (ArchivaTaskScheduler) schedulerControl.getMock();

        archivaConfigControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfig = (ArchivaConfiguration) archivaConfigControl.getMock();

        repoFactoryControl = MockClassControl.createControl( RepositoryContentFactory.class );
        repoFactory = (RepositoryContentFactory) repoFactoryControl.getMock();

        uploadAction = new UploadAction();
        uploadAction.setScheduler( scheduler );
        uploadAction.setConfiguration( archivaConfig );
        uploadAction.setRepositoryFactory( repoFactory );

        File testRepo = new File( getBasedir(), "target/test-classes/test-repo" );
        testRepo.mkdirs();

        assertTrue( testRepo.exists() );

        config = new Configuration();
        ManagedRepositoryConfiguration repoConfig = new ManagedRepositoryConfiguration();
        repoConfig.setId( REPOSITORY_ID );
        repoConfig.setLayout( "default" );
        repoConfig.setLocation( testRepo.getPath() );
        repoConfig.setName( REPOSITORY_ID );
        repoConfig.setBlockRedeployments( true );
        config.addManagedRepository( repoConfig );

        RepositoryScanningConfiguration repoScanning = new RepositoryScanningConfiguration();
        repoScanning.setKnownContentConsumers( new ArrayList<String>() );
        config.setRepositoryScanning( repoScanning );
    }

    public void tearDown()
        throws Exception
    {
        File testRepo = new File( config.findManagedRepositoryById( REPOSITORY_ID ).getLocation() );
        FileUtils.deleteDirectory( testRepo );

        assertFalse( testRepo.exists() );

        super.tearDown();
    }

    private void setUploadParameters( String version, String classifier, File artifact, File pomFile,
                                      boolean generatePom )
    {
        uploadAction.setRepositoryId( REPOSITORY_ID );
        uploadAction.setGroupId( "org.apache.archiva" );
        uploadAction.setArtifactId( "artifact-upload" );
        uploadAction.setVersion( version );
        uploadAction.setPackaging( "jar" );

        uploadAction.setClassifier( classifier );
        uploadAction.setArtifact( artifact );
        uploadAction.setPom( pomFile );
        uploadAction.setGeneratePom( generatePom );
    }

    private void assertAllArtifactsIncludingSupportArtifactsArePresent( String repoLocation, String artifact,
                                                                        String version )
    {
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/" + artifact + ".jar" ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/" + artifact
            + ".jar.sha1" ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/" + artifact
            + ".jar.md5" ).exists() );

        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/" + artifact + ".pom" ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/" + artifact
            + ".pom.sha1" ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/" + artifact
            + ".pom.md5" ).exists() );

        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA
            + ".sha1" ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA
            + ".md5" ).exists() );
    }

    private void verifyVersionMetadataChecksums( String repoLocation, String version )
        throws IOException
    {
        ChecksummedFile checksum =
            new ChecksummedFile( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/"
                + MetadataTools.MAVEN_METADATA ) );
        String sha1 = checksum.calculateChecksum( ChecksumAlgorithm.SHA1 );
        String md5 = checksum.calculateChecksum( ChecksumAlgorithm.MD5 );

        String contents =
            FileUtils.readFileToString( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/"
                + MetadataTools.MAVEN_METADATA + ".sha1" ) );
        assertTrue( StringUtils.contains( contents, sha1 ) );

        contents =
            FileUtils.readFileToString( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/"
                + MetadataTools.MAVEN_METADATA + ".md5" ) );
        assertTrue( StringUtils.contains( contents, md5 ) );
    }

    private void verifyProjectMetadataChecksums( String repoLocation )
        throws IOException
    {
        ChecksummedFile checksum =
            new ChecksummedFile( new File( repoLocation, "/org/apache/archiva/artifact-upload/"
                + MetadataTools.MAVEN_METADATA ) );
        String sha1 = checksum.calculateChecksum( ChecksumAlgorithm.SHA1 );
        String md5 = checksum.calculateChecksum( ChecksumAlgorithm.MD5 );

        String contents =
            FileUtils.readFileToString( new File( repoLocation, "/org/apache/archiva/artifact-upload/"
                + MetadataTools.MAVEN_METADATA + ".sha1" ) );
        assertTrue( StringUtils.contains( contents, sha1 ) );

        contents =
            FileUtils.readFileToString( new File( repoLocation, "/org/apache/archiva/artifact-upload/"
                + MetadataTools.MAVEN_METADATA + ".md5" ) );
        assertTrue( StringUtils.contains( contents, md5 ) );
    }

    private void verifyPomChecksums( String repoLocation, String artifact, String version )
        throws IOException
    {
        ChecksummedFile checksum;
        String sha1;
        String md5;
        String contents;
        checksum =
            new ChecksummedFile( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/"
                + artifact + ".pom" ) );
        sha1 = checksum.calculateChecksum( ChecksumAlgorithm.SHA1 );
        md5 = checksum.calculateChecksum( ChecksumAlgorithm.MD5 );

        contents =
            FileUtils.readFileToString( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/"
                + artifact + ".pom.sha1" ) );
        assertTrue( StringUtils.contains( contents, sha1 ) );

        contents =
            FileUtils.readFileToString( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/"
                + artifact + ".pom.md5" ) );
        assertTrue( StringUtils.contains( contents, md5 ) );
    }

    private void verifyArtifactChecksums( String repoLocation, String artifact, String version )
        throws IOException
    {
        ChecksummedFile checksum =
            new ChecksummedFile( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/"
                + artifact + ".jar" ) );
        String sha1 = checksum.calculateChecksum( ChecksumAlgorithm.SHA1 );
        String md5 = checksum.calculateChecksum( ChecksumAlgorithm.MD5 );

        String contents =
            FileUtils.readFileToString( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/"
                + artifact + ".jar.sha1" ) );
        assertTrue( StringUtils.contains( contents, sha1 ) );

        contents =
            FileUtils.readFileToString( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + version + "/"
                + artifact + ".jar.md5" ) );
        assertTrue( StringUtils.contains( contents, md5 ) );
    }

    private String getTimestamp( String[] artifactsList, int startIndex, int index )
    {
        int endIndex = -1;
        String timestamp;

        if ( artifactsList[index].contains( "jar" ) )
        {
            endIndex = artifactsList[index].indexOf( ".jar" );
        }
        else
        {
            endIndex = artifactsList[index].indexOf( ".pom" );
        }
        
        timestamp = artifactsList[index].substring( startIndex, endIndex );

        return timestamp;
    }
    
    private MockControl mockAuditLogs( List<String> resources )
    {
        return mockAuditLogs( AuditEvent.UPLOAD_FILE, resources );
    }

    private MockControl mockAuditLogs( String action, List<String> resources )
    {
        MockControl control = MockControl.createControl( AuditListener.class );
        AuditListener listener = (AuditListener) control.getMock();
        boolean matcherSet = false;
        for ( String resource : resources )
        {
            listener.auditEvent( new AuditEvent( REPOSITORY_ID, "guest", resource, action ) );
            if ( !matcherSet )
            {
                control.setMatcher( new AuditEventArgumentsMatcher() );
                matcherSet = true;
            }
        }
        control.replay();

        uploadAction.setAuditListeners( Collections.singletonList( listener ) );
        return control;
    }
    
    public void testArtifactUploadWithPomSuccessful()
        throws Exception
    {
        setUploadParameters( "1.0", null, new File( getBasedir(),
                                                    "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             new File( getBasedir(), "target/test-classes/upload-artifact-test/pom.xml" ), false );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        content.setRepository( config.findManagedRepositoryById( REPOSITORY_ID ) );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );

        archivaConfigControl.replay();
        repoFactoryControl.replay();

        MockControl control = mockAuditLogs(
            Arrays.asList( "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar",
                           "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom" ) );

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        archivaConfigControl.verify();
        repoFactoryControl.verify();
        control.verify();

        String repoLocation = config.findManagedRepositoryById( REPOSITORY_ID ).getLocation();
        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0", "1.0" );

        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyProjectMetadataChecksums( repoLocation );
    }

    public void testArtifactUploadWithClassifier()
        throws Exception
    {
        setUploadParameters( "1.0", "tests", new File( getBasedir(),
                                                       "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             new File( getBasedir(), "target/test-classes/upload-artifact-test/pom.xml" ), false );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        content.setRepository( config.findManagedRepositoryById( REPOSITORY_ID ) );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );

        archivaConfigControl.replay();
        repoFactoryControl.replay();

        MockControl control = mockAuditLogs(
            Arrays.asList( "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0-tests.jar",
                           "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom" ) );

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        archivaConfigControl.verify();
        repoFactoryControl.verify();
        control.verify();

        String repoLocation = config.findManagedRepositoryById( REPOSITORY_ID ).getLocation();
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0-tests.jar" ).exists() );
        assertTrue( new File( repoLocation,
                              "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0-tests.jar.sha1" ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0-tests.jar.md5" ).exists() );

        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom" ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom.sha1" ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom.md5" ).exists() );

        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA
            + ".sha1" ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA
            + ".md5" ).exists() );

        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0-tests", "1.0" );
        verifyProjectMetadataChecksums( repoLocation );
    }

    public void testArtifactUploadGeneratePomSuccessful()
        throws Exception
    {
        setUploadParameters( "1.0", null, new File( getBasedir(),
                                                    "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             null, true );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        content.setRepository( config.findManagedRepositoryById( REPOSITORY_ID ) );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );

        archivaConfigControl.replay();
        repoFactoryControl.replay();

        MockControl control = mockAuditLogs(
            Arrays.asList( "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar",
                           "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom" ) );

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        archivaConfigControl.verify();
        repoFactoryControl.verify();
        control.verify();

        String repoLocation = config.findManagedRepositoryById( REPOSITORY_ID ).getLocation();
        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0", "1.0" );

        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyProjectMetadataChecksums( repoLocation );
    }

    public void testArtifactUploadNoPomSuccessful()
        throws Exception
    {
        setUploadParameters( "1.0", null, new File( getBasedir(),
                                                    "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             null, false );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        content.setRepository( config.findManagedRepositoryById( REPOSITORY_ID ) );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );

        archivaConfigControl.replay();
        repoFactoryControl.replay();

        MockControl control =
            mockAuditLogs( Arrays.asList( "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar" ) );

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        archivaConfigControl.verify();
        repoFactoryControl.verify();
        control.verify();

        String repoLocation = config.findManagedRepositoryById( REPOSITORY_ID ).getLocation();
        assertTrue(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar" ).exists() );
        assertTrue(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar.sha1" ).exists() );
        assertTrue(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar.md5" ).exists() );

        assertFalse(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom" ).exists() );
        assertFalse(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom.sha1" ).exists() );
        assertFalse(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom.md5" ).exists() );

        assertTrue(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA +
            ".sha1" ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA +
            ".md5" ).exists() );

        // verify checksums of jar file
        ChecksummedFile checksum = new ChecksummedFile(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar" ) );
        String sha1 = checksum.calculateChecksum( ChecksumAlgorithm.SHA1 );
        String md5 = checksum.calculateChecksum( ChecksumAlgorithm.MD5 );

        String contents = FileUtils.readFileToString(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar.sha1" ) );
        assertTrue( StringUtils.contains( contents, sha1 ) );

        contents = FileUtils.readFileToString(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar.md5" ) );
        assertTrue( StringUtils.contains( contents, md5 ) );

        // verify checksums of metadata file
        checksum = new ChecksummedFile(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA ) );
        sha1 = checksum.calculateChecksum( ChecksumAlgorithm.SHA1 );
        md5 = checksum.calculateChecksum( ChecksumAlgorithm.MD5 );

        contents = FileUtils.readFileToString(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA + ".sha1" ) );
        assertTrue( StringUtils.contains( contents, sha1 ) );

        contents = FileUtils.readFileToString(
            new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA + ".md5" ) );
        assertTrue( StringUtils.contains( contents, md5 ) );
    }

    public void testArtifactUploadFailedRepositoryNotFound()
        throws Exception
    {
        setUploadParameters( "1.0", null, new File( getBasedir(),
                                                    "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             null, false );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        repoFactoryControl.expectAndThrow( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ),
                                           new RepositoryNotFoundException() );

        archivaConfigControl.replay();
        repoFactoryControl.replay();

        String returnString = uploadAction.doUpload();
        assertEquals( Action.ERROR, returnString );

        archivaConfigControl.verify();
        repoFactoryControl.verify();

        String repoLocation = config.findManagedRepositoryById( REPOSITORY_ID ).getLocation();
        assertFalse( new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar" ).exists() );

        assertFalse( new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom" ).exists() );

        assertFalse( new File( repoLocation, "/org/apache/archiva/artifact-upload/" + MetadataTools.MAVEN_METADATA ).exists() );
    }

    public void testArtifactUploadSnapshots()
        throws Exception
    {
        setUploadParameters( "1.0-SNAPSHOT", null, new File( getBasedir(),
                                                             "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             null, true );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        content.setRepository( config.findManagedRepositoryById( REPOSITORY_ID ) );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );

        archivaConfigControl.replay();
        repoFactoryControl.replay();

        SimpleDateFormat fmt = new SimpleDateFormat( "yyyyMMdd.HHmmss" );
        fmt.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        String timestamp = fmt.format( new Date() );
        MockControl control = mockAuditLogs( Arrays.asList(
            "org/apache/archiva/artifact-upload/1.0-SNAPSHOT/artifact-upload-1.0-" + timestamp + "-1.jar",
            "org/apache/archiva/artifact-upload/1.0-SNAPSHOT/artifact-upload-1.0-" + timestamp + "-1.pom" ) );

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        archivaConfigControl.verify();
        repoFactoryControl.verify();
        control.verify();

        String repoLocation = config.findManagedRepositoryById( REPOSITORY_ID ).getLocation();
        String[] artifactsList = new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0-SNAPSHOT/" ).list();
        Arrays.sort( artifactsList );

        assertEquals( 9, artifactsList.length );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0-SNAPSHOT/"
            + MetadataTools.MAVEN_METADATA ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0-SNAPSHOT/"
            + MetadataTools.MAVEN_METADATA + ".sha1" ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0-SNAPSHOT/"
            + MetadataTools.MAVEN_METADATA + ".md5" ).exists() );

        int startIndex = "artifact-upload-1.0-".length();
        String timestampPath = getTimestamp( artifactsList, startIndex, 0 );

        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0-" + timestampPath,
                                                               "1.0-SNAPSHOT" );
        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0-" + timestampPath, "1.0-SNAPSHOT" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0-" + timestampPath, "1.0-SNAPSHOT" );
        verifyProjectMetadataChecksums( repoLocation );
        verifyVersionMetadataChecksums( repoLocation, "1.0-SNAPSHOT" );

        // verify build number
        File metadataFile =
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0-SNAPSHOT/" + MetadataTools.MAVEN_METADATA );
        ArchivaRepositoryMetadata artifactMetadata = RepositoryMetadataReader.read( metadataFile );

        SnapshotVersion snapshotVersion = artifactMetadata.getSnapshotVersion();
        assertEquals( "Incorrect build number set in artifact metadata.", 1, snapshotVersion.getBuildNumber() );

        String timestampPart = StringUtils.substringBeforeLast( timestampPath, "-" );
        assertEquals( "Incorrect timestamp set in artifact metadata.", timestampPart, snapshotVersion.getTimestamp() );

        String buildnumber = StringUtils.substringAfterLast( timestampPath, "-" );
        assertEquals( "Incorrect build number in filename.", "1", buildnumber );

        archivaConfigControl.reset();
        repoFactoryControl.reset();
        control.reset();

        control.setDefaultMatcher( MockControl.ALWAYS_MATCHER );

        // MRM-1353
        // upload snapshot artifact again and check if build number was incremented
        setUploadParameters( "1.0-SNAPSHOT", null,
                             new File( getBasedir(),
                                       "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ), null,
                             true );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );
        
        archivaConfigControl.replay();
        repoFactoryControl.replay();
        
        fmt = new SimpleDateFormat( "yyyyMMdd.HHmmss" );
        fmt.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        timestamp = fmt.format( new Date() );
        
        control = mockAuditLogs( Arrays.asList(
                       "org/apache/archiva/artifact-upload/1.0-SNAPSHOT/artifact-upload-1.0-" + timestamp + "-2.jar",
                       "org/apache/archiva/artifact-upload/1.0-SNAPSHOT/artifact-upload-1.0-" + timestamp + "-2.pom" ) );
        
        returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        archivaConfigControl.verify();
        repoFactoryControl.verify();
        control.verify();
        
        artifactsList = new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0-SNAPSHOT/" ).list();
        Arrays.sort( artifactsList );
        
        assertEquals( 15, artifactsList.length );

        timestampPath = getTimestamp( artifactsList, startIndex, 6 );

        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0-" + timestampPath,
                                                               "1.0-SNAPSHOT" );
        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0-" + timestampPath, "1.0-SNAPSHOT" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0-" + timestampPath, "1.0-SNAPSHOT" );
        verifyProjectMetadataChecksums( repoLocation );
        verifyVersionMetadataChecksums( repoLocation, "1.0-SNAPSHOT" );

        // verify build number set in metadata and in filename
        metadataFile =
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0-SNAPSHOT/" + MetadataTools.MAVEN_METADATA );
        artifactMetadata = RepositoryMetadataReader.read( metadataFile );

        snapshotVersion = artifactMetadata.getSnapshotVersion();
        assertEquals( "Incorrect build number set in artifact metadata.", 2, snapshotVersion.getBuildNumber() );

        timestampPart = StringUtils.substringBeforeLast( timestampPath, "-" );
        assertEquals( "Incorrect timestamp set in artifact metadata.", timestampPart, snapshotVersion.getTimestamp() );

        buildnumber = StringUtils.substringAfterLast( timestampPath, "-" );
        assertEquals( "Incorrect build number in filename.", "2", buildnumber );
    }

    public void testChecksumIsCorrectWhenArtifactIsReUploaded()
        throws Exception
    {
        setUploadParameters( "1.0", null, new File( getBasedir(),
                                                    "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             null, true );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        ManagedRepositoryConfiguration repoConfig = config.findManagedRepositoryById( REPOSITORY_ID );
        repoConfig.setBlockRedeployments( false );
        content.setRepository( repoConfig );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );

        archivaConfigControl.replay();
        repoFactoryControl.replay();

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        archivaConfigControl.verify();
        repoFactoryControl.verify();

        archivaConfigControl.reset();
        repoFactoryControl.reset();

        String repoLocation = config.findManagedRepositoryById( REPOSITORY_ID ).getLocation();
        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0", "1.0" );

        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyProjectMetadataChecksums( repoLocation );

        // RE-upload artifact
        setUploadParameters( "1.0", null, new File( getBasedir(),
                                                    "target/test-classes/upload-artifact-test/artifact-to-be-reuploaded.jar" ),
                             null, true );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );

        archivaConfigControl.replay();
        repoFactoryControl.replay();

        // TODO: track modifications?
//        MockControl control = mockAuditLogs( AuditEvent.MODIFY_FILE, Arrays.asList(
        MockControl control = mockAuditLogs(
            Arrays.asList( "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar",
                           "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom" ) );

        returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        archivaConfigControl.verify();
        repoFactoryControl.verify();
        control.verify();

        repoLocation = config.findManagedRepositoryById( REPOSITORY_ID ).getLocation();
        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0", "1.0" );

        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyProjectMetadataChecksums( repoLocation );
    }

    public void testUploadArtifactAlreadyExistingRedeploymentsBlocked()
        throws Exception
    {
        setUploadParameters( "1.0", null, new File( getBasedir(),
                                                    "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             null, true );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        content.setRepository( config.findManagedRepositoryById( REPOSITORY_ID ) );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config, 2 );
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content, 2 );

        archivaConfigControl.replay();
        repoFactoryControl.replay();

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        setUploadParameters( "1.0", null, new File( getBasedir(),
                                                    "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             null, true );

        MockControl control = mockAuditLogs( Collections.<String>emptyList() );

        returnString = uploadAction.doUpload();
        assertEquals( Action.ERROR, returnString );

        archivaConfigControl.verify();
        repoFactoryControl.verify();
        control.verify();

        String repoLocation = config.findManagedRepositoryById( REPOSITORY_ID ).getLocation();
        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0", "1.0" );

        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyProjectMetadataChecksums( repoLocation );
    }

    public void testUploadArtifactAlreadyExistingRedeploymentsAllowed()
        throws Exception
    {
        setUploadParameters( "1.0", null, new File( getBasedir(),
                                                    "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             null, true );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        ManagedRepositoryConfiguration repoConfig = config.findManagedRepositoryById( REPOSITORY_ID );
        repoConfig.setBlockRedeployments( false );
        content.setRepository( repoConfig );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config, 2 );
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content, 2 );

        archivaConfigControl.replay();
        repoFactoryControl.replay();

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        setUploadParameters( "1.0", null, new File( getBasedir(),
                                                    "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             null, true );

        // TODO: track modifications?
//        MockControl control = mockAuditLogs( AuditEvent.MODIFY_FILE, Arrays.asList(
        MockControl control = mockAuditLogs(
            Arrays.asList( "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.jar",
                           "org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom" ) );

        returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        archivaConfigControl.verify();
        repoFactoryControl.verify();
        control.verify();

        String repoLocation = config.findManagedRepositoryById( REPOSITORY_ID ).getLocation();
        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0", "1.0" );

        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyProjectMetadataChecksums( repoLocation );
    }
}
