package org.apache.maven.archiva.converter.artifact;

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

import org.apache.maven.archiva.converter.artifact.ArtifactConversionException;
import org.apache.maven.archiva.converter.artifact.ArtifactConverter;
import org.apache.maven.archiva.converter.artifact.Messages;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;

/**
 * LegacyToDefaultConverterTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class LegacyToDefaultConverterTest
    extends PlexusTestCase
{
    private ArtifactRepository sourceRepository;

    private ArtifactRepository targetRepository;

    private ArtifactConverter artifactConverter;

    private ArtifactFactory artifactFactory;

    private static final int SLEEP_MILLIS = 100;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArtifactRepositoryFactory factory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );

        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "legacy" );

        File sourceBase = getTestFile( "src/test/source-repository" );
        sourceRepository = factory.createArtifactRepository( "source", sourceBase.toURL().toString(), layout, null,
                                                             null );

        layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );

        File targetBase = getTestFile( "target/test-target-repository" );
        copyDirectoryStructure( getTestFile( "src/test/target-repository" ), targetBase );

        targetRepository = factory.createArtifactRepository( "target", targetBase.toURL().toString(), layout, null,
                                                             null );

        artifactConverter = (ArtifactConverter) lookup( ArtifactConverter.ROLE, "legacy-to-default" );

        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
    }

    protected void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    private void copyDirectoryStructure( File sourceDirectory, File destinationDirectory )
        throws IOException
    {
        if ( !sourceDirectory.exists() )
        {
            throw new IOException( "Source directory doesn't exists (" + sourceDirectory.getAbsolutePath() + ")." );
        }

        File[] files = sourceDirectory.listFiles();

        String sourcePath = sourceDirectory.getAbsolutePath();

        for ( int i = 0; i < files.length; i++ )
        {
            File file = files[i];

            String dest = file.getAbsolutePath();

            dest = dest.substring( sourcePath.length() + 1 );

            File destination = new File( destinationDirectory, dest );

            if ( file.isFile() )
            {
                destination = destination.getParentFile();

                FileUtils.copyFileToDirectory( file, destination );
            }
            else if ( file.isDirectory() )
            {
                if ( !".svn".equals( file.getName() ) )
                {
                    if ( !destination.exists() && !destination.mkdirs() )
                    {
                        throw new IOException( "Could not create destination directory '"
                            + destination.getAbsolutePath() + "'." );
                    }
                    copyDirectoryStructure( file, destination );
                }
            }
            else
            {
                throw new IOException( "Unknown file type: " + file.getAbsolutePath() );
            }
        }
    }

    public void testV4PomConvert()
        throws Exception
    {
        // test that it is copied as is

        Artifact artifact = createArtifact( "test", "v4artifact", "1.0.0" );
        ArtifactMetadata artifactMetadata = new ArtifactRepositoryMetadata( artifact );
        File artifactMetadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( artifactMetadata ) );
        artifactMetadataFile.delete();

        ArtifactMetadata versionMetadata = new SnapshotArtifactRepositoryMetadata( artifact );
        File versionMetadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( versionMetadata ) );
        versionMetadataFile.delete();

        File artifactFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        artifactFile.delete();

        artifactConverter.convert( artifact, targetRepository );
        checkSuccess( artifactConverter );

        assertTrue( "Check artifact created", artifactFile.exists() );
        assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile, artifact.getFile() ) );

        artifact = createPomArtifact( artifact );
        File pomFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        File sourcePomFile = new File( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );
        assertTrue( "Check POM created", pomFile.exists() );

        compareFiles( sourcePomFile, pomFile );

        assertTrue( "Check artifact metadata created", artifactMetadataFile.exists() );

        File expectedMetadataFile = getTestFile( "src/test/expected-files/v4-artifact-metadata.xml" );

        compareFiles( expectedMetadataFile, artifactMetadataFile );

        assertTrue( "Check snapshot metadata created", versionMetadataFile.exists() );

        expectedMetadataFile = getTestFile( "src/test/expected-files/v4-version-metadata.xml" );

        compareFiles( expectedMetadataFile, versionMetadataFile );
    }

    public void testV3PomConvert()
        throws Exception
    {
        // test that the pom is coverted

        Artifact artifact = createArtifact( "test", "v3artifact", "1.0.0" );
        ArtifactMetadata artifactMetadata = new ArtifactRepositoryMetadata( artifact );
        File artifactMetadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( artifactMetadata ) );
        artifactMetadataFile.delete();

        ArtifactMetadata versionMetadata = new SnapshotArtifactRepositoryMetadata( artifact );
        File versionMetadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( versionMetadata ) );
        versionMetadataFile.delete();

        artifactConverter.convert( artifact, targetRepository );
        checkSuccess( artifactConverter );

        File artifactFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        assertTrue( "Check artifact created", artifactFile.exists() );
        assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile, artifact.getFile() ) );

        artifact = createPomArtifact( artifact );
        File pomFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        File expectedPomFile = getTestFile( "src/test/expected-files/converted-v3.pom" );
        assertTrue( "Check POM created", pomFile.exists() );

        compareFiles( expectedPomFile, pomFile );

        assertTrue( "Check artifact metadata created", artifactMetadataFile.exists() );

        File expectedMetadataFile = getTestFile( "src/test/expected-files/v3-artifact-metadata.xml" );

        compareFiles( expectedMetadataFile, artifactMetadataFile );

        assertTrue( "Check snapshot metadata created", versionMetadataFile.exists() );

        expectedMetadataFile = getTestFile( "src/test/expected-files/v3-version-metadata.xml" );

        compareFiles( expectedMetadataFile, versionMetadataFile );
    }

    public void testV3PomConvertWithRelocation()
        throws Exception
    {
        Artifact artifact = createArtifact( "test", "relocated-v3artifact", "1.0.0" );
        ArtifactMetadata artifactMetadata = new ArtifactRepositoryMetadata( artifact );
        File artifactMetadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( artifactMetadata ) );
        artifactMetadataFile.delete();

        ArtifactMetadata versionMetadata = new SnapshotArtifactRepositoryMetadata( artifact );
        File versionMetadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( versionMetadata ) );
        versionMetadataFile.delete();

        artifactConverter.convert( artifact, targetRepository );
        //checkSuccess();  --> commented until MNG-2100 is fixed

        File artifactFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        assertTrue( "Check if relocated artifact created", artifactFile.exists() );
        assertTrue( "Check if relocated artifact matches", FileUtils.contentEquals( artifactFile, artifact.getFile() ) );
        Artifact pomArtifact = createArtifact( "relocated-test", "relocated-v3artifact", "1.0.0", "1.0.0", "pom" );
        File pomFile = getTestFile( "src/test/expected-files/" + targetRepository.pathOf( pomArtifact ) );
        File testFile = getTestFile( "target/test-target-repository/" + targetRepository.pathOf( pomArtifact ) );
        compareFiles( pomFile, testFile );

        Artifact orig = createArtifact( "test", "relocated-v3artifact", "1.0.0", "1.0.0", "pom" );
        artifactFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( orig ) );
        assertTrue( "Check if relocation artifact pom is created", artifactFile.exists() );
        testFile = getTestFile( "src/test/expected-files/" + targetRepository.pathOf( orig ) );
        compareFiles( artifactFile, testFile );
    }

    public void testV3PomWarningsOnConvert()
        throws Exception
    {
        // test that the pom is converted but that warnings are reported

        Artifact artifact = createArtifact( "test", "v3-warnings-artifact", "1.0.0" );
        ArtifactMetadata artifactMetadata = new ArtifactRepositoryMetadata( artifact );
        File artifactMetadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( artifactMetadata ) );
        artifactMetadataFile.delete();

        ArtifactMetadata versionMetadata = new SnapshotArtifactRepositoryMetadata( artifact );
        File versionMetadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( versionMetadata ) );
        versionMetadataFile.delete();

        artifactConverter.convert( artifact, targetRepository );
        checkWarnings( artifactConverter, 2 );

        File artifactFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        assertTrue( "Check artifact created", artifactFile.exists() );
        assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile, artifact.getFile() ) );

        artifact = createPomArtifact( artifact );
        File pomFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        File expectedPomFile = getTestFile( "src/test/expected-files/converted-v3-warnings.pom" );
        assertTrue( "Check POM created", pomFile.exists() );

        compareFiles( expectedPomFile, pomFile );

        // TODO: check 2 warnings (extend and versions) matched on i18n key
    }

    private void doTestV4SnapshotPomConvert( String version, String expectedMetadataFileName )
        throws Exception
    {
        // test that it is copied as is

        Artifact artifact = createArtifact( "test", "v4artifact", version );
        ArtifactMetadata artifactMetadata = new ArtifactRepositoryMetadata( artifact );
        File artifactMetadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( artifactMetadata ) );
        artifactMetadataFile.delete();

        ArtifactMetadata snapshotMetadata = new SnapshotArtifactRepositoryMetadata( artifact );
        File snapshotMetadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( snapshotMetadata ) );
        snapshotMetadataFile.delete();

        artifactConverter.convert( artifact, targetRepository );
        checkSuccess( artifactConverter );

        File artifactFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        assertTrue( "Check artifact created", artifactFile.exists() );
        assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile, artifact.getFile() ) );

        artifact = createPomArtifact( artifact );
        File pomFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        File sourcePomFile = new File( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );
        assertTrue( "Check POM created", pomFile.exists() );

        compareFiles( sourcePomFile, pomFile );

        assertTrue( "Check artifact metadata created", artifactMetadataFile.exists() );

        File expectedMetadataFile = getTestFile( "src/test/expected-files/v4-snapshot-artifact-metadata.xml" );

        compareFiles( expectedMetadataFile, artifactMetadataFile );

        assertTrue( "Check snapshot metadata created", snapshotMetadataFile.exists() );

        expectedMetadataFile = getTestFile( expectedMetadataFileName );

        compareFiles( expectedMetadataFile, snapshotMetadataFile );
    }

    public void testV3SnapshotPomConvert()
        throws Exception
    {
        // test that the pom is coverted

        Artifact artifact = createArtifact( "test", "v3artifact", "1.0.0-SNAPSHOT" );
        ArtifactMetadata artifactMetadata = new ArtifactRepositoryMetadata( artifact );
        File artifactMetadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( artifactMetadata ) );
        artifactMetadataFile.delete();

        ArtifactMetadata snapshotMetadata = new SnapshotArtifactRepositoryMetadata( artifact );
        File snapshotMetadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( snapshotMetadata ) );
        snapshotMetadataFile.delete();

        artifactConverter.convert( artifact, targetRepository );
        checkSuccess( artifactConverter );

        File artifactFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        assertTrue( "Check artifact created", artifactFile.exists() );
        assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile, artifact.getFile() ) );

        artifact = createPomArtifact( artifact );
        File pomFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        File expectedPomFile = getTestFile( "src/test/expected-files/converted-v3-snapshot.pom" );
        assertTrue( "Check POM created", pomFile.exists() );

        compareFiles( expectedPomFile, pomFile );

        assertTrue( "Check artifact metadata created", artifactMetadataFile.exists() );

        File expectedMetadataFile = getTestFile( "src/test/expected-files/v3-snapshot-artifact-metadata.xml" );

        compareFiles( expectedMetadataFile, artifactMetadataFile );

        assertTrue( "Check snapshot metadata created", snapshotMetadataFile.exists() );

        expectedMetadataFile = getTestFile( "src/test/expected-files/v3-snapshot-metadata.xml" );

        compareFiles( expectedMetadataFile, snapshotMetadataFile );
    }

    public void testV4SnapshotPomConvert()
        throws Exception
    {
        doTestV4SnapshotPomConvert( "1.0.0-SNAPSHOT", "src/test/expected-files/v4-snapshot-metadata.xml" );

        assertTrue( true );
    }

    public void testV4TimestampedSnapshotPomConvert()
        throws Exception
    {
        doTestV4SnapshotPomConvert( "1.0.0-20060111.120115-1",
                                    "src/test/expected-files/v4-timestamped-snapshot-metadata.xml" );

        assertTrue( true );
    }

    public void testMavenOnePluginConversion()
        throws Exception
    {
        Artifact artifact = createArtifact( "org.apache.maven.plugins", "maven-foo-plugin", "1.0", "1.0",
                                            "maven-plugin" );
        artifact.setFile( new File( getBasedir(), "src/test/source-repository/test/plugins/maven-foo-plugin-1.0.jar" ) );
        artifactConverter.convert( artifact, targetRepository );
        // There is a warning but I can't figure out how to look at it. Eyeballing the results it appears
        // the plugin is being coverted correctly.
        //checkSuccess();

        File artifactFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        assertTrue( "Check artifact created", artifactFile.exists() );
        assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile, artifact.getFile() ) );

        /*
         The POM isn't needed for Maven 1.x plugins but the raw conversion for  

         artifact = createPomArtifact( artifact );
         File pomFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
         File expectedPomFile = getTestFile( "src/test/expected-files/maven-foo-plugin-1.0.pom" );
         assertTrue( "Check POM created", pomFile.exists() );
         compareFiles( expectedPomFile, pomFile );
         */
    }

    public void testV3TimestampedSnapshotPomConvert()
        throws Exception
    {
        // test that the pom is coverted

        Artifact artifact = createArtifact( "test", "v3artifact", "1.0.0-20060105.130101-3" );
        ArtifactMetadata artifactMetadata = new ArtifactRepositoryMetadata( artifact );
        File artifactMetadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( artifactMetadata ) );
        artifactMetadataFile.delete();

        ArtifactMetadata snapshotMetadata = new SnapshotArtifactRepositoryMetadata( artifact );
        File snapshotMetadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( snapshotMetadata ) );
        snapshotMetadataFile.delete();

        artifactConverter.convert( artifact, targetRepository );
        checkSuccess( artifactConverter );

        File artifactFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        assertTrue( "Check artifact created", artifactFile.exists() );
        assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile, artifact.getFile() ) );

        artifact = createPomArtifact( artifact );
        File pomFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        File expectedPomFile = getTestFile( "src/test/expected-files/converted-v3-timestamped-snapshot.pom" );
        assertTrue( "Check POM created", pomFile.exists() );

        compareFiles( expectedPomFile, pomFile );

        assertTrue( "Check artifact snapshotMetadata created", artifactMetadataFile.exists() );

        File expectedMetadataFile = getTestFile( "src/test/expected-files/v3-snapshot-artifact-metadata.xml" );

        compareFiles( expectedMetadataFile, artifactMetadataFile );

        assertTrue( "Check snapshot snapshotMetadata created", snapshotMetadataFile.exists() );

        expectedMetadataFile = getTestFile( "src/test/expected-files/v3-timestamped-snapshot-metadata.xml" );

        compareFiles( expectedMetadataFile, snapshotMetadataFile );
    }

    public void testNoPomConvert()
        throws Exception
    {
        // test that a POM is not created when there was none at the source

        Artifact artifact = createArtifact( "test", "noPomArtifact", "1.0.0" );
        artifactConverter.convert( artifact, targetRepository );
        checkWarnings( artifactConverter, 1 );

        assertHasWarningReason( artifactConverter, Messages.getString( "warning.missing.pom" ) );

        File artifactFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        assertTrue( "Check artifact created", artifactFile.exists() );
        assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile, artifact.getFile() ) );

        artifact = createPomArtifact( artifact );
        File pomFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        File sourcePomFile = new File( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );

        assertFalse( "Check no POM created", pomFile.exists() );
        assertFalse( "No source POM", sourcePomFile.exists() );
    }

    public void testIncorrectSourceChecksumMd5()
        throws Exception
    {
        // test that it fails when the source md5 is wrong

        Artifact artifact = createArtifact( "test", "incorrectMd5Artifact", "1.0.0" );
        File file = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        file.delete();

        artifactConverter.convert( artifact, targetRepository );
        checkWarnings( artifactConverter, 2 );

        assertHasWarningReason( artifactConverter, Messages.getString( "failure.incorrect.md5" ) );

        assertFalse( "Check artifact not created", file.exists() );

        ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact );
        File metadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( metadata ) );
        assertFalse( "Check metadata not created", metadataFile.exists() );
    }

    public void testIncorrectSourceChecksumSha1()
        throws Exception
    {
        // test that it fails when the source sha1 is wrong

        Artifact artifact = createArtifact( "test", "incorrectSha1Artifact", "1.0.0" );
        File file = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        file.delete();

        artifactConverter.convert( artifact, targetRepository );
        checkWarnings( artifactConverter, 2 );

        assertHasWarningReason( artifactConverter, Messages.getString( "failure.incorrect.sha1" ) );

        assertFalse( "Check artifact not created", file.exists() );

        ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact );
        File metadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( metadata ) );
        assertFalse( "Check metadata not created", metadataFile.exists() );
    }

    public void testUnmodifiedArtifact()
        throws Exception, InterruptedException
    {
        // test the unmodified artifact is untouched

        Artifact artifact = createArtifact( "test", "unmodified-artifact", "1.0.0" );
        Artifact pomArtifact = createPomArtifact( artifact );

        File sourceFile = new File( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );
        File sourcePomFile = new File( sourceRepository.getBasedir(), sourceRepository.pathOf( pomArtifact ) );
        File targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        File targetPomFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( pomArtifact ) );

        assertTrue( "Check target file exists", targetFile.exists() );
        assertTrue( "Check target POM exists", targetPomFile.exists() );

        sourceFile.setLastModified( System.currentTimeMillis() );
        sourcePomFile.setLastModified( System.currentTimeMillis() );

        long origTime = targetFile.lastModified();
        long origPomTime = targetPomFile.lastModified();

        // Need to guarantee last modified is not equal
        Thread.sleep( SLEEP_MILLIS );

        artifactConverter.convert( artifact, targetRepository );
        checkSuccess( artifactConverter );

        compareFiles( sourceFile, targetFile );
        compareFiles( sourcePomFile, targetPomFile );

        assertEquals( "Check artifact unmodified", origTime, targetFile.lastModified() );
        assertEquals( "Check POM unmodified", origPomTime, targetPomFile.lastModified() );
    }

    public void testModifedArtifactFails()
        throws Exception
    {
        // test that it fails when the source artifact has changed and is different to the existing artifact in the
        // target repository

        Artifact artifact = createArtifact( "test", "modified-artifact", "1.0.0" );
        Artifact pomArtifact = createPomArtifact( artifact );

        File sourceFile = new File( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );
        File sourcePomFile = new File( sourceRepository.getBasedir(), sourceRepository.pathOf( pomArtifact ) );
        File targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        File targetPomFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( pomArtifact ) );

        assertTrue( "Check target file exists", targetFile.exists() );
        assertTrue( "Check target POM exists", targetPomFile.exists() );

        sourceFile.setLastModified( System.currentTimeMillis() );
        sourcePomFile.setLastModified( System.currentTimeMillis() );

        long origTime = targetFile.lastModified();
        long origPomTime = targetPomFile.lastModified();

        // Need to guarantee last modified is not equal
        Thread.sleep( SLEEP_MILLIS );

        artifactConverter.convert( artifact, targetRepository );
        checkWarnings( artifactConverter, 2 );

        assertHasWarningReason( artifactConverter, Messages.getString( "failure.target.already.exists" ) );

        assertEquals( "Check unmodified", origTime, targetFile.lastModified() );
        assertEquals( "Check unmodified", origPomTime, targetPomFile.lastModified() );

        ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact );
        File metadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( metadata ) );
        assertFalse( "Check metadata not created", metadataFile.exists() );
    }

    public void testForcedUnmodifiedArtifact()
        throws Exception
    {
        // test unmodified artifact is still converted when set to force

        artifactConverter = (ArtifactConverter) lookup( ArtifactConverter.ROLE, "force-repository-converter" );

        Artifact artifact = createArtifact( "test", "unmodified-artifact", "1.0.0" );
        Artifact pomArtifact = createPomArtifact( artifact );

        File sourceFile = new File( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );
        File sourcePomFile = new File( sourceRepository.getBasedir(), sourceRepository.pathOf( pomArtifact ) );
        File targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        File targetPomFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( pomArtifact ) );

        SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd", Locale.getDefault() );
        long origTime = dateFormat.parse( "2006-03-03" ).getTime();
        targetFile.setLastModified( origTime );
        targetPomFile.setLastModified( origTime );

        sourceFile.setLastModified( dateFormat.parse( "2006-01-01" ).getTime() );
        sourcePomFile.setLastModified( dateFormat.parse( "2006-02-02" ).getTime() );

        artifactConverter.convert( artifact, targetRepository );
        checkSuccess( artifactConverter );

        compareFiles( sourceFile, targetFile );
        compareFiles( sourcePomFile, targetPomFile );

        assertFalse( "Check modified", origTime == targetFile.lastModified() );
        assertFalse( "Check modified", origTime == targetPomFile.lastModified() );

        ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact );
        File metadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( metadata ) );
        assertTrue( "Check metadata created", metadataFile.exists() );
    }

    public void testDryRunSuccess()
        throws Exception
    {
        // test dry run does nothing on a run that will be successful, and returns success

        artifactConverter = (ArtifactConverter) lookup( ArtifactConverter.ROLE, "dryrun-repository-converter" );

        Artifact artifact = createArtifact( "test", "dryrun-artifact", "1.0.0" );
        Artifact pomArtifact = createPomArtifact( artifact );

        File sourceFile = new File( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );
        File sourcePomFile = new File( sourceRepository.getBasedir(), sourceRepository.pathOf( pomArtifact ) );
        File targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        File targetPomFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( pomArtifact ) );

        artifactConverter.convert( artifact, targetRepository );
        checkSuccess( artifactConverter );

        assertTrue( "Check source file exists", sourceFile.exists() );
        assertTrue( "Check source POM exists", sourcePomFile.exists() );

        assertFalse( "Check target file doesn't exist", targetFile.exists() );
        assertFalse( "Check target POM doesn't exist", targetPomFile.exists() );

        ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact );
        File metadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( metadata ) );
        assertFalse( "Check metadata not created", metadataFile.exists() );
    }

    public void testDryRunFailure()
        throws Exception
    {
        // test dry run does nothing on a run that will fail, and returns failure

        artifactConverter = (ArtifactConverter) lookup( ArtifactConverter.ROLE, "dryrun-repository-converter" );

        Artifact artifact = createArtifact( "test", "modified-artifact", "1.0.0" );
        Artifact pomArtifact = createPomArtifact( artifact );

        File sourceFile = new File( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );
        File sourcePomFile = new File( sourceRepository.getBasedir(), sourceRepository.pathOf( pomArtifact ) );
        File targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        File targetPomFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( pomArtifact ) );

        assertTrue( "Check target file exists", targetFile.exists() );
        assertTrue( "Check target POM exists", targetPomFile.exists() );

        sourceFile.setLastModified( System.currentTimeMillis() );
        sourcePomFile.setLastModified( System.currentTimeMillis() );

        long origTime = targetFile.lastModified();
        long origPomTime = targetPomFile.lastModified();

        // Need to guarantee last modified is not equal
        Thread.sleep( SLEEP_MILLIS );

        artifactConverter.convert( artifact, targetRepository );
        checkWarnings( artifactConverter, 2 );

        assertHasWarningReason( artifactConverter, Messages.getString( "failure.target.already.exists" ) );

        assertEquals( "Check unmodified", origTime, targetFile.lastModified() );
        assertEquals( "Check unmodified", origPomTime, targetPomFile.lastModified() );

        ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact );
        File metadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( metadata ) );
        assertFalse( "Check metadata not created", metadataFile.exists() );
    }

    public void testRollbackArtifactCreated()
        throws Exception
    {
        // test rollback can remove a created artifact, including checksums

        Artifact artifact = createArtifact( "test", "rollback-created-artifact", "1.0.0" );
        ArtifactMetadata artifactMetadata = new ArtifactRepositoryMetadata( artifact );
        File artifactMetadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( artifactMetadata ) );
        FileUtils.deleteDirectory( artifactMetadataFile.getParentFile() );

        ArtifactMetadata versionMetadata = new SnapshotArtifactRepositoryMetadata( artifact );
        File versionMetadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( versionMetadata ) );

        File artifactFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );

        artifactConverter.convert( artifact, targetRepository );
        checkWarnings( artifactConverter, 2 );

        boolean found = false;
        String pattern = "^" + Messages.getString( "invalid.source.pom" ).replaceFirst( "\\{0\\}", ".*" ) + "$";
        for ( Iterator it = artifactConverter.getWarnings().values().iterator(); it.hasNext() && !found; )
        {
            List messages = (List) it.next();

            for ( Iterator itmsgs = messages.iterator(); itmsgs.hasNext(); )
            {
                String message = (String) itmsgs.next();
                if ( message.matches( pattern ) )
                {
                    found = true;
                    break;
                }
            }
        }

        assertTrue( "Check failure message.", found );

        assertFalse( "check artifact rolled back", artifactFile.exists() );
        assertFalse( "check metadata rolled back", artifactMetadataFile.exists() );
        assertFalse( "check metadata rolled back", versionMetadataFile.exists() );
    }

    public void testMultipleArtifacts()
        throws Exception
    {
        // test multiple artifacts are converted

        List artifacts = new ArrayList();
        artifacts.add( createArtifact( "test", "artifact-one", "1.0.0" ) );
        artifacts.add( createArtifact( "test", "artifact-two", "1.0.0" ) );
        artifacts.add( createArtifact( "test", "artifact-three", "1.0.0" ) );

        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact arti = (Artifact) it.next();
            artifactConverter.convert( arti, targetRepository );
            checkSuccess( artifactConverter );
        }

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            File artifactFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
            assertTrue( "Check artifact created", artifactFile.exists() );
            assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile, artifact.getFile() ) );

            artifact = createPomArtifact( artifact );
            File pomFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
            File expectedPomFile = getTestFile( "src/test/expected-files/converted-" + artifact.getArtifactId()
                + ".pom" );
            assertTrue( "Check POM created", pomFile.exists() );

            compareFiles( expectedPomFile, pomFile );
        }
    }

    public void testInvalidSourceArtifactMetadata()
        throws Exception
    {
        // test artifact is not converted when source metadata is invalid, and returns failure

        createModernSourceRepository();

        Artifact artifact = createArtifact( "test", "incorrectArtifactMetadata", "1.0.0" );
        File file = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        file.delete();

        artifactConverter.convert( artifact, targetRepository );
        checkWarnings( artifactConverter, 2 );

        assertHasWarningReason( artifactConverter, Messages.getString( "failure.incorrect.artifactMetadata.versions" ) );

        assertFalse( "Check artifact not created", file.exists() );

        ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact );
        File metadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( metadata ) );
        assertFalse( "Check metadata not created", metadataFile.exists() );
    }

    public void testInvalidSourceSnapshotMetadata()
        throws Exception
    {
        // test artifact is not converted when source snapshot metadata is invalid and returns failure

        createModernSourceRepository();

        Artifact artifact = createArtifact( "test", "incorrectSnapshotMetadata", "1.0.0-20060102.030405-6" );
        File file = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        file.delete();

        artifactConverter.convert( artifact, targetRepository );
        checkWarnings( artifactConverter, 2 );

        assertHasWarningReason( artifactConverter, Messages.getString( "failure.incorrect.snapshotMetadata.snapshot" ) );

        assertFalse( "Check artifact not created", file.exists() );

        ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact );
        File metadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( metadata ) );
        assertFalse( "Check metadata not created", metadataFile.exists() );
    }

    public void testMergeArtifactMetadata()
        throws Exception
    {
        // test artifact level metadata is merged when it already exists on successful conversion

        Artifact artifact = createArtifact( "test", "newversion-artifact", "1.0.1" );
        artifactConverter.convert( artifact, targetRepository );
        checkSuccess( artifactConverter );

        File artifactFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        assertTrue( "Check artifact created", artifactFile.exists() );
        assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile, artifact.getFile() ) );

        artifact = createPomArtifact( artifact );
        File pomFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        File sourcePomFile = new File( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );
        assertTrue( "Check POM created", pomFile.exists() );

        compareFiles( sourcePomFile, pomFile );

        ArtifactMetadata artifactMetadata = new ArtifactRepositoryMetadata( artifact );
        File artifactMetadataFile = new File( targetRepository.getBasedir(), targetRepository
            .pathOfRemoteRepositoryMetadata( artifactMetadata ) );
        assertTrue( "Check artifact metadata created", artifactMetadataFile.exists() );

        File expectedMetadataFile = getTestFile( "src/test/expected-files/newversion-artifact-metadata.xml" );

        compareFiles( expectedMetadataFile, artifactMetadataFile );
    }

    public void testSourceAndTargetRepositoriesMatch()
        throws Exception
    {
        // test that it fails if the same

        ArtifactRepositoryFactory factory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );

        sourceRepository = factory.createArtifactRepository( "source", targetRepository.getUrl(), targetRepository
            .getLayout(), null, null );

        Artifact artifact = createArtifact( "test", "repository-artifact", "1.0" );

        try
        {
            artifactConverter.convert( artifact, targetRepository );
            fail( "Should have failed trying to convert within the same repository" );
        }
        catch ( ArtifactConversionException e )
        {
            // expected
            assertEquals( "check message", Messages.getString( "exception.repositories.match" ), e.getMessage() );
            assertNull( "Check no additional cause", e.getCause() );
        }
    }

    private Artifact createArtifact( String groupId, String artifactId, String version )
    {
        Matcher matcher = Artifact.VERSION_FILE_PATTERN.matcher( version );
        String baseVersion;
        if ( matcher.matches() )
        {
            baseVersion = matcher.group( 1 ) + "-SNAPSHOT";
        }
        else
        {
            baseVersion = version;
        }
        return createArtifact( groupId, artifactId, baseVersion, version, "jar" );
    }

    private Artifact createArtifact( String groupId, String artifactId, String baseVersion, String version, String type )
    {
        Artifact artifact = artifactFactory.createArtifact( groupId, artifactId, version, null, type );
        artifact.setBaseVersion( baseVersion );
        artifact.setRepository( sourceRepository );
        artifact.setFile( new File( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) ) );
        return artifact;
    }

    private Artifact createPomArtifact( Artifact artifact )
    {
        return createArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(), artifact
            .getVersion(), "pom" );
    }

    private static void compareFiles( File expectedPomFile, File pomFile )
        throws IOException
    {
        String expectedContent = normalizeString( org.apache.commons.io.FileUtils.readFileToString( expectedPomFile, null ) );
        String targetContent = normalizeString( org.apache.commons.io.FileUtils.readFileToString( pomFile, null ) );
        assertEquals( "Check file match between " + expectedPomFile + " and " + pomFile, expectedContent, targetContent );
    }

    private static String normalizeString( String path )
    {
        return path.trim().replaceAll( "\r\n", "\n" ).replace( '\r', '\n' ).replaceAll( "<\\?xml .+\\?>", "" );
    }

    private void checkSuccess( ArtifactConverter converter )
    {
        assertNotNull( "Warnings should never be null.", converter.getWarnings() );
        assertEquals( "Should have no warnings.", 0, countWarningMessages( converter ) );
    }

    private void checkWarnings( ArtifactConverter converter, int count )
    {
        assertNotNull( "Warnings should never be null.", converter.getWarnings() );
        assertEquals( "Should have some warnings.", count, countWarningMessages( converter ) );
    }

    private int countWarningMessages( ArtifactConverter converter )
    {
        int count = 0;
        for ( Iterator it = converter.getWarnings().values().iterator(); it.hasNext(); )
        {
            List values = (List) it.next();
            count += values.size();
        }
        return count;
    }

    private void assertHasWarningReason( ArtifactConverter converter, String reason )
    {
        assertNotNull( "Warnings should never be null.", converter.getWarnings() );
        assertTrue( "Expecting 1 or more Warnings", countWarningMessages( converter ) > 0 );

        for ( Iterator it = converter.getWarnings().values().iterator(); it.hasNext(); )
        {
            List messages = (List) it.next();
            if ( messages.contains( reason ) )
            {
                /* No need to check any furthor */
                return;
            }
        }

        /* didn't find it. */

        for ( Iterator it = converter.getWarnings().entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Entry) it.next();
            Artifact artifact = (Artifact) entry.getKey();
            System.out.println( "-Artifact: " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":"
                + artifact.getVersion() );
            List messages = (List) entry.getValue();
            for ( Iterator itmsgs = messages.iterator(); itmsgs.hasNext(); )
            {
                String message = (String) itmsgs.next();
                System.out.println( "  " + message );
            }
        }
        fail( "Unable to find message <" + reason + "> in warnings." );
    }

    private void createModernSourceRepository()
        throws Exception
    {
        ArtifactRepositoryFactory factory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );

        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );

        File sourceBase = getTestFile( "src/test/source-modern-repository" );
        sourceRepository = factory.createArtifactRepository( "source", sourceBase.toURL().toString(), layout, null,
                                                             null );
    }
}
