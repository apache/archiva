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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksummedFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RepositoryScanningConfiguration;
import org.apache.maven.archiva.database.ArchivaAuditLogsDao;
import org.apache.maven.archiva.model.ArchivaAuditLogs;
import org.apache.maven.archiva.model.ArchivaRepositoryMetadata;
import org.apache.maven.archiva.model.SnapshotVersion;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.apache.maven.archiva.repository.content.ManagedDefaultRepositoryContent;
import org.apache.maven.archiva.repository.metadata.MetadataTools;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataReader;
import org.apache.maven.archiva.scheduled.ArchivaTaskScheduler;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import com.opensymphony.xwork2.Action;

/**
 * UploadActionTest
 */
public class UploadActionTest
    extends PlexusInSpringTestCase
{
    private ArchivaTaskScheduler scheduler;

    private MockControl schedulerControl;

    private UploadAction uploadAction;

    private ArchivaConfiguration archivaConfig;

    private MockControl archivaConfigControl;

    private RepositoryContentFactory repoFactory;

    private MockControl repoFactoryControl;

    private ArchivaAuditLogsDao auditLogsDao;

    private MockControl auditLogsDaoControl;

    private static final String REPOSITORY_ID = "test-repo";

    private Configuration config;

    public void setUp()
        throws Exception
    {
        super.setUp();

        schedulerControl = MockControl.createControl( ArchivaTaskScheduler.class );
        scheduler = (ArchivaTaskScheduler) schedulerControl.getMock();

        archivaConfigControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfig = (ArchivaConfiguration) archivaConfigControl.getMock();

        repoFactoryControl = MockClassControl.createControl( RepositoryContentFactory.class );
        repoFactory = (RepositoryContentFactory) repoFactoryControl.getMock();

        auditLogsDaoControl = MockControl.createControl( ArchivaAuditLogsDao.class );
        auditLogsDaoControl.setDefaultMatcher( MockControl.ALWAYS_MATCHER );
        auditLogsDao = (ArchivaAuditLogsDao) auditLogsDaoControl.getMock();

        uploadAction = new UploadAction();
        uploadAction.setScheduler( scheduler );
        uploadAction.setConfiguration( archivaConfig );
        uploadAction.setRepositoryFactory( repoFactory );
        uploadAction.setAuditLogsDao( auditLogsDao );

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

    public void testArtifactUploadWithPomSuccessful()
        throws Exception
    {
        setUploadParameters( "1.0", null,
                             new File( getBasedir(),
                                       "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ),
                             new File( getBasedir(), "target/test-classes/upload-artifact-test/pom.xml" ), false );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        content.setRepository( config.findManagedRepositoryById( REPOSITORY_ID ) );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );
        auditLogsDaoControl.expectAndReturn( auditLogsDao.saveAuditLogs( new ArchivaAuditLogs() ), null );

        archivaConfigControl.replay();
        repoFactoryControl.replay();
        auditLogsDaoControl.replay();

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        archivaConfigControl.verify();
        repoFactoryControl.verify();
        auditLogsDaoControl.verify();

        String repoLocation = config.findManagedRepositoryById( REPOSITORY_ID ).getLocation();
        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0", "1.0" );

        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyProjectMetadataChecksums( repoLocation );
    }

    public void testArtifactUploadWithClassifier()
        throws Exception
    {
        setUploadParameters( "1.0", "tests",
                             new File( getBasedir(),
                                       "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ), null,
                             false );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        content.setRepository( config.findManagedRepositoryById( REPOSITORY_ID ) );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );
        auditLogsDaoControl.expectAndReturn( auditLogsDao.saveAuditLogs( new ArchivaAuditLogs() ), null );

        archivaConfigControl.replay();
        repoFactoryControl.replay();
        auditLogsDaoControl.replay();

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        archivaConfigControl.verify();
        repoFactoryControl.verify();
        auditLogsDaoControl.verify();

        String repoLocation = config.findManagedRepositoryById( REPOSITORY_ID ).getLocation();
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0-tests.jar" ).exists() );
        assertTrue( new File( repoLocation,
                              "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0-tests.jar.sha1" ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0-tests.jar.md5" ).exists() );

        assertFalse( new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom" ).exists() );
        assertFalse( new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom.sha1" ).exists() );
        assertFalse( new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0/artifact-upload-1.0.pom.md5" ).exists() );

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
        setUploadParameters( "1.0", null,
                             new File( getBasedir(),
                                       "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ), null,
                             true );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        content.setRepository( config.findManagedRepositoryById( REPOSITORY_ID ) );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );
        auditLogsDaoControl.expectAndReturn( auditLogsDao.saveAuditLogs( new ArchivaAuditLogs() ), null );

        archivaConfigControl.replay();
        repoFactoryControl.replay();
        auditLogsDaoControl.replay();

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        archivaConfigControl.verify();
        repoFactoryControl.verify();
        auditLogsDaoControl.verify();

        String repoLocation = config.findManagedRepositoryById( REPOSITORY_ID ).getLocation();
        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0", "1.0" );

        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyProjectMetadataChecksums( repoLocation );
    }

    public void testArtifactUploadFailedRepositoryNotFound()
        throws Exception
    {
        setUploadParameters( "1.0", null,
                             new File( getBasedir(),
                                       "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ), null,
                             false );

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
        setUploadParameters( "1.0-SNAPSHOT", null,
                             new File( getBasedir(),
                                       "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ), null,
                             true );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        content.setRepository( config.findManagedRepositoryById( REPOSITORY_ID ) );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );
        auditLogsDaoControl.expectAndReturn( auditLogsDao.saveAuditLogs( new ArchivaAuditLogs() ), null );

        archivaConfigControl.replay();
        repoFactoryControl.replay();
        auditLogsDaoControl.replay();

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        archivaConfigControl.verify();
        repoFactoryControl.verify();
        auditLogsDaoControl.verify();

        String repoLocation = config.findManagedRepositoryById( REPOSITORY_ID ).getLocation();
        String[] artifactsList = new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0-SNAPSHOT/" ).list();

        assertEquals( 9, artifactsList.length );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0-SNAPSHOT/"
            + MetadataTools.MAVEN_METADATA ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0-SNAPSHOT/"
            + MetadataTools.MAVEN_METADATA + ".sha1" ).exists() );
        assertTrue( new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0-SNAPSHOT/"
            + MetadataTools.MAVEN_METADATA + ".md5" ).exists() );

        int startIndex = "artifact-upload-1.0-".length();
        String timestamp = getTimestamp( artifactsList, startIndex, 0 );

        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0-" + timestamp,
                                                               "1.0-SNAPSHOT" );
        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0-" + timestamp, "1.0-SNAPSHOT" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0-" + timestamp, "1.0-SNAPSHOT" );
        verifyProjectMetadataChecksums( repoLocation );
        verifyVersionMetadataChecksums( repoLocation, "1.0-SNAPSHOT" );

        // verify build number
        File metadataFile =
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0-SNAPSHOT/" + MetadataTools.MAVEN_METADATA );
        ArchivaRepositoryMetadata artifactMetadata = RepositoryMetadataReader.read( metadataFile );

        SnapshotVersion snapshotVersion = artifactMetadata.getSnapshotVersion();
        assertEquals( "Incorrect build number set in artifact metadata.", 1, snapshotVersion.getBuildNumber() );

        String timestampPart = StringUtils.substringBeforeLast( timestamp, "-" );
        assertEquals( "Incorrect timestamp set in artifact metadata.", timestampPart, snapshotVersion.getTimestamp() );

        String buildnumber = StringUtils.substringAfterLast( timestamp, "-" );
        assertEquals( "Incorrect build number in filename.", "1", buildnumber );

        archivaConfigControl.reset();
        repoFactoryControl.reset();
        auditLogsDaoControl.reset();

        auditLogsDaoControl.setDefaultMatcher( MockControl.ALWAYS_MATCHER );

        // MRM-1353
        // upload snapshot artifact again and check if build number was incremented
        setUploadParameters( "1.0-SNAPSHOT", null,
                             new File( getBasedir(),
                                       "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ), null,
                             true );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );
        auditLogsDaoControl.expectAndReturn( auditLogsDao.saveAuditLogs( new ArchivaAuditLogs() ), null );

        archivaConfigControl.replay();
        repoFactoryControl.replay();
        auditLogsDaoControl.replay();

        returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        archivaConfigControl.verify();
        repoFactoryControl.verify();
        auditLogsDaoControl.verify();

        artifactsList = new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0-SNAPSHOT/" ).list();

        assertEquals( 15, artifactsList.length );

        timestamp = getTimestamp( artifactsList, startIndex, 6 );

        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0-" + timestamp,
                                                               "1.0-SNAPSHOT" );
        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0-" + timestamp, "1.0-SNAPSHOT" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0-" + timestamp, "1.0-SNAPSHOT" );
        verifyProjectMetadataChecksums( repoLocation );
        verifyVersionMetadataChecksums( repoLocation, "1.0-SNAPSHOT" );

        // verify build number set in metadata and in filename
        metadataFile =
            new File( repoLocation, "/org/apache/archiva/artifact-upload/1.0-SNAPSHOT/" + MetadataTools.MAVEN_METADATA );
        artifactMetadata = RepositoryMetadataReader.read( metadataFile );

        snapshotVersion = artifactMetadata.getSnapshotVersion();
        assertEquals( "Incorrect build number set in artifact metadata.", 2, snapshotVersion.getBuildNumber() );

        timestampPart = StringUtils.substringBeforeLast( timestamp, "-" );
        assertEquals( "Incorrect timestamp set in artifact metadata.", timestampPart, snapshotVersion.getTimestamp() );

        buildnumber = StringUtils.substringAfterLast( timestamp, "-" );
        assertEquals( "Incorrect build number in filename.", "2", buildnumber );
    }

    public void testChecksumIsCorrectWhenArtifactIsReUploaded()
        throws Exception
    {
        setUploadParameters( "1.0", null,
                             new File( getBasedir(),
                                       "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ), null,
                             true );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        ManagedRepositoryConfiguration repoConfig = config.findManagedRepositoryById( REPOSITORY_ID );
        repoConfig.setBlockRedeployments( false );
        content.setRepository( repoConfig );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );
        auditLogsDaoControl.expectAndReturn( auditLogsDao.saveAuditLogs( new ArchivaAuditLogs() ), null );

        archivaConfigControl.replay();
        repoFactoryControl.replay();
        auditLogsDaoControl.replay();

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        archivaConfigControl.verify();
        repoFactoryControl.verify();
        auditLogsDaoControl.verify();

        archivaConfigControl.reset();
        repoFactoryControl.reset();
        auditLogsDaoControl.reset();
        auditLogsDaoControl.setDefaultMatcher( MockControl.ALWAYS_MATCHER );

        String repoLocation = config.findManagedRepositoryById( REPOSITORY_ID ).getLocation();
        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0", "1.0" );

        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyProjectMetadataChecksums( repoLocation );

        // RE-upload artifact
        setUploadParameters( "1.0", null,
                             new File( getBasedir(),
                                       "target/test-classes/upload-artifact-test/artifact-to-be-reuploaded.jar" ),
                             null, true );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content );
        auditLogsDaoControl.expectAndReturn( auditLogsDao.saveAuditLogs( new ArchivaAuditLogs() ), null );

        archivaConfigControl.replay();
        repoFactoryControl.replay();
        auditLogsDaoControl.replay();

        returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        archivaConfigControl.verify();
        repoFactoryControl.verify();
        auditLogsDaoControl.verify();

        repoLocation = config.findManagedRepositoryById( REPOSITORY_ID ).getLocation();
        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0", "1.0" );

        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyProjectMetadataChecksums( repoLocation );
    }

    public void testUploadArtifactAlreadyExistingRedeploymentsBlocked()
        throws Exception
    {
        setUploadParameters( "1.0", null,
                             new File( getBasedir(),
                                       "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ), null,
                             true );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        content.setRepository( config.findManagedRepositoryById( REPOSITORY_ID ) );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config, 2 );
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content, 2 );
        auditLogsDaoControl.expectAndReturn( auditLogsDao.saveAuditLogs( new ArchivaAuditLogs() ), null );

        archivaConfigControl.replay();
        repoFactoryControl.replay();
        auditLogsDaoControl.replay();

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        setUploadParameters( "1.0", null,
                             new File( getBasedir(),
                                       "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ), null,
                             true );

        returnString = uploadAction.doUpload();
        assertEquals( Action.ERROR, returnString );

        archivaConfigControl.verify();
        repoFactoryControl.verify();
        auditLogsDaoControl.verify();

        String repoLocation = config.findManagedRepositoryById( REPOSITORY_ID ).getLocation();
        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0", "1.0" );

        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyProjectMetadataChecksums( repoLocation );
    }

    public void testUploadArtifactAlreadyExistingRedeploymentsAllowed()
        throws Exception
    {
        setUploadParameters( "1.0", null,
                             new File( getBasedir(),
                                       "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ), null,
                             true );

        ManagedRepositoryContent content = new ManagedDefaultRepositoryContent();
        ManagedRepositoryConfiguration repoConfig = config.findManagedRepositoryById( REPOSITORY_ID );
        repoConfig.setBlockRedeployments( false );
        content.setRepository( repoConfig );

        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config, 2 );
        repoFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( REPOSITORY_ID ), content, 2 );
        auditLogsDaoControl.expectAndReturn( auditLogsDao.saveAuditLogs( new ArchivaAuditLogs() ), null, 2 );

        archivaConfigControl.replay();
        repoFactoryControl.replay();
        auditLogsDaoControl.replay();

        String returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        setUploadParameters( "1.0", null,
                             new File( getBasedir(),
                                       "target/test-classes/upload-artifact-test/artifact-to-be-uploaded.jar" ), null,
                             true );

        returnString = uploadAction.doUpload();
        assertEquals( Action.SUCCESS, returnString );

        archivaConfigControl.verify();
        repoFactoryControl.verify();
        auditLogsDaoControl.verify();

        String repoLocation = config.findManagedRepositoryById( REPOSITORY_ID ).getLocation();
        assertAllArtifactsIncludingSupportArtifactsArePresent( repoLocation, "artifact-upload-1.0", "1.0" );

        verifyArtifactChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyPomChecksums( repoLocation, "artifact-upload-1.0", "1.0" );
        verifyProjectMetadataChecksums( repoLocation );
    }
}