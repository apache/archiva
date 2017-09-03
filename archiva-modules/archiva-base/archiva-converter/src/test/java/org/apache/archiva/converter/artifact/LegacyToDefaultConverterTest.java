package org.apache.archiva.converter.artifact;

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

import junit.framework.TestCase;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

/**
 * LegacyToDefaultConverterTest
 */
@RunWith (ArchivaSpringJUnit4ClassRunner.class)
@ContextConfiguration (locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" })
public class LegacyToDefaultConverterTest
    extends TestCase
{
    private ArtifactRepository sourceRepository;

    private ArtifactRepository targetRepository;

    private ArtifactConverter artifactConverter;

    private ArtifactFactory artifactFactory;

    @Inject
    private PlexusSisuBridge plexusSisuBridge;

    @Inject
    private ApplicationContext applicationContext;

    private static final int SLEEP_MILLIS = 100;

    @Before
    public void init()
        throws Exception
    {
        super.setUp();

        ArtifactRepositoryFactory factory = plexusSisuBridge.lookup( ArtifactRepositoryFactory.class );

        Map<String, ArtifactRepositoryLayout> layoutsMap = plexusSisuBridge.lookupMap( ArtifactRepositoryLayout.class );

        System.out.println( "hints " + layoutsMap.keySet().toString() );

        ArtifactRepositoryLayout layout = plexusSisuBridge.lookup( ArtifactRepositoryLayout.class, "legacy" );

        Path sourceBase = getTestFile( "src/test/source-repository" );
        sourceRepository =
            factory.createArtifactRepository( "source", sourceBase.toUri().toURL().toString(), layout, null, null );

        layout = plexusSisuBridge.lookup( ArtifactRepositoryLayout.class, "default" );

        Path targetBase = getTestFile( "target/test-target-repository" );
        copyDirectoryStructure( getTestFile( "src/test/target-repository" ), targetBase );

        targetRepository =
            factory.createArtifactRepository( "target", targetBase.toUri().toURL().toString(), layout, null, null );

        artifactConverter =
            applicationContext.getBean( "artifactConverter#legacy-to-default", ArtifactConverter.class );

        artifactConverter.clearWarnings();
        artifactFactory = (ArtifactFactory) plexusSisuBridge.lookup( ArtifactFactory.class );
    }

    public static Path getTestFile( String path )
    {
        return Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), path );
    }

    private void copyDirectoryStructure( Path sourceDirectory, Path destinationDirectory )
        throws IOException
    {
        if ( !Files.exists(sourceDirectory) )
        {
            throw new IOException( "Source directory doesn't exists (" + sourceDirectory.toAbsolutePath()+ ")." );
        }

        Path[] files = Files.list( sourceDirectory ).toArray( Path[]::new );

        String sourcePath = sourceDirectory.toAbsolutePath().toString();

        for ( int i = 0; i < files.length; i++ )
        {
            Path file = files[i];

            String dest = file.toAbsolutePath().toString();

            dest = dest.substring( sourcePath.length() + 1 );

            Path destination = destinationDirectory.resolve( dest );

            if ( Files.isRegularFile( file ) )
            {
                destination = destination.getParent();

                FileUtils.copyFileToDirectory( file.toFile(), destination.toFile() );
            }
            else if ( Files.isDirectory( file ) )
            {
                if ( !".svn".equals( file.getFileName().toString() ) )
                {
                    if ( !Files.exists(destination))
                    {
                        Files.createDirectories( destination );
                    }
                    copyDirectoryStructure( file, destination );
                }
            }
            else
            {
                throw new IOException( "Unknown file type: " + file.toAbsolutePath() );
            }
        }
    }

    @Test
    public void testV4PomConvert()
        throws Exception
    {
        // test that it is copied as is

        Artifact artifact = createArtifact( "test", "v4artifact", "1.0.0" );
        ArtifactMetadata artifactMetadata = new ArtifactRepositoryMetadata( artifact );
        Path artifactMetadataFile = Paths.get( targetRepository.getBasedir(),
                                              targetRepository.pathOfRemoteRepositoryMetadata( artifactMetadata ) );
        Files.deleteIfExists( artifactMetadataFile);

        ArtifactMetadata versionMetadata = new SnapshotArtifactRepositoryMetadata( artifact );
        Path versionMetadataFile = Paths.get( targetRepository.getBasedir(),
                                             targetRepository.pathOfRemoteRepositoryMetadata( versionMetadata ) );
        Files.deleteIfExists(versionMetadataFile);

        Path artifactFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        Files.deleteIfExists(artifactFile);

        artifactConverter.convert( artifact, targetRepository );
        checkSuccess( artifactConverter );

        assertTrue( "Check artifact created", Files.exists(artifactFile) );
        assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile.toFile(), artifact.getFile() ) );

        artifact = createPomArtifact( artifact );
        Path pomFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        Path sourcePomFile = Paths.get( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );
        assertTrue( "Check POM created", Files.exists(pomFile) );

        compareFiles( sourcePomFile, pomFile );

        assertTrue( "Check artifact metadata created", Files.exists(artifactMetadataFile) );

        Path expectedMetadataFile = getTestFile( "src/test/expected-files/v4-artifact-metadata.xml" );

        compareFiles( expectedMetadataFile, artifactMetadataFile );

        assertTrue( "Check snapshot metadata created", Files.exists(versionMetadataFile) );

        expectedMetadataFile = getTestFile( "src/test/expected-files/v4-version-metadata.xml" );

        compareFiles( expectedMetadataFile, versionMetadataFile );
    }

    @Test
    public void testV3PomConvert()
        throws Exception
    {
        // test that the pom is coverted

        Artifact artifact = createArtifact( "test", "v3artifact", "1.0.0" );
        ArtifactMetadata artifactMetadata = new ArtifactRepositoryMetadata( artifact );
        Path artifactMetadataFile = Paths.get( targetRepository.getBasedir(),
                                              targetRepository.pathOfRemoteRepositoryMetadata( artifactMetadata ) );
        Files.deleteIfExists(artifactMetadataFile);

        ArtifactMetadata versionMetadata = new SnapshotArtifactRepositoryMetadata( artifact );
        Path versionMetadataFile = Paths.get( targetRepository.getBasedir(),
                                             targetRepository.pathOfRemoteRepositoryMetadata( versionMetadata ) );
        Files.deleteIfExists(versionMetadataFile);

        artifactConverter.convert( artifact, targetRepository );
        checkSuccess( artifactConverter );

        Path artifactFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        assertTrue( "Check artifact created", Files.exists(artifactFile) );
        assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile.toFile(), artifact.getFile() ) );

        artifact = createPomArtifact( artifact );
        Path pomFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        Path expectedPomFile = getTestFile( "src/test/expected-files/converted-v3.pom" );
        assertTrue( "Check POM created", Files.exists(pomFile) );

        compareFiles( expectedPomFile, pomFile );

        assertTrue( "Check artifact metadata created", Files.exists(artifactMetadataFile) );

        Path expectedMetadataFile = getTestFile( "src/test/expected-files/v3-artifact-metadata.xml" );

        compareFiles( expectedMetadataFile, artifactMetadataFile );

        assertTrue( "Check snapshot metadata created", Files.exists(versionMetadataFile) );

        expectedMetadataFile = getTestFile( "src/test/expected-files/v3-version-metadata.xml" );

        compareFiles( expectedMetadataFile, versionMetadataFile );
    }

    @Test
    public void testV3PomConvertWithRelocation()
        throws Exception
    {
        Artifact artifact = createArtifact( "test", "relocated-v3artifact", "1.0.0" );
        ArtifactMetadata artifactMetadata = new ArtifactRepositoryMetadata( artifact );
        Path artifactMetadataFile = Paths.get( targetRepository.getBasedir(),
                                              targetRepository.pathOfRemoteRepositoryMetadata( artifactMetadata ) );
        Files.deleteIfExists(artifactMetadataFile);

        ArtifactMetadata versionMetadata = new SnapshotArtifactRepositoryMetadata( artifact );
        Path versionMetadataFile = Paths.get( targetRepository.getBasedir(),
                                             targetRepository.pathOfRemoteRepositoryMetadata( versionMetadata ) );
        Files.deleteIfExists(versionMetadataFile);

        artifactConverter.convert( artifact, targetRepository );
        //checkSuccess();  --> commented until MNG-2100 is fixed

        Path artifactFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        assertTrue( "Check if relocated artifact created", Files.exists(artifactFile) );
        assertTrue( "Check if relocated artifact matches",
                    FileUtils.contentEquals( artifactFile.toFile(), artifact.getFile() ) );
        Artifact pomArtifact = createArtifact( "relocated-test", "relocated-v3artifact", "1.0.0", "1.0.0", "pom" );
        Path pomFile = getTestFile( "src/test/expected-files/" + targetRepository.pathOf( pomArtifact ) );
        Path testFile = getTestFile( "target/test-target-repository/" + targetRepository.pathOf( pomArtifact ) );
        compareFiles( pomFile, testFile );

        Artifact orig = createArtifact( "test", "relocated-v3artifact", "1.0.0", "1.0.0", "pom" );
        artifactFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( orig ) );
        assertTrue( "Check if relocation artifact pom is created", Files.exists(artifactFile) );
        testFile = getTestFile( "src/test/expected-files/" + targetRepository.pathOf( orig ) );
        compareFiles( artifactFile, testFile );
    }

    @Test
    public void testV3PomWarningsOnConvert()
        throws Exception
    {
        // test that the pom is converted but that warnings are reported

        Artifact artifact = createArtifact( "test", "v3-warnings-artifact", "1.0.0" );
        ArtifactMetadata artifactMetadata = new ArtifactRepositoryMetadata( artifact );
        Path artifactMetadataFile = Paths.get( targetRepository.getBasedir(),
                                              targetRepository.pathOfRemoteRepositoryMetadata( artifactMetadata ) );
        Files.deleteIfExists(artifactMetadataFile);

        ArtifactMetadata versionMetadata = new SnapshotArtifactRepositoryMetadata( artifact );
        Path versionMetadataFile = Paths.get( targetRepository.getBasedir(),
                                             targetRepository.pathOfRemoteRepositoryMetadata( versionMetadata ) );
        Files.deleteIfExists(versionMetadataFile);

        artifactConverter.convert( artifact, targetRepository );
        checkWarnings( artifactConverter, 2 );

        Path artifactFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        assertTrue( "Check artifact created", Files.exists(artifactFile) );
        assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile.toFile(), artifact.getFile() ) );

        artifact = createPomArtifact( artifact );
        Path pomFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        Path expectedPomFile = getTestFile( "src/test/expected-files/converted-v3-warnings.pom" );
        assertTrue( "Check POM created", Files.exists(pomFile) );

        compareFiles( expectedPomFile, pomFile );

        // TODO: check 2 warnings (extend and versions) matched on i18n key
    }

    private void doTestV4SnapshotPomConvert( String version, String expectedMetadataFileName )
        throws Exception
    {
        // test that it is copied as is

        Artifact artifact = createArtifact( "test", "v4artifact", version );
        ArtifactMetadata artifactMetadata = new ArtifactRepositoryMetadata( artifact );
        Path artifactMetadataFile = Paths.get( targetRepository.getBasedir(),
                                              targetRepository.pathOfRemoteRepositoryMetadata( artifactMetadata ) );
        Files.deleteIfExists(artifactMetadataFile);

        ArtifactMetadata snapshotMetadata = new SnapshotArtifactRepositoryMetadata( artifact );
        Path snapshotMetadataFile = Paths.get( targetRepository.getBasedir(),
                                              targetRepository.pathOfRemoteRepositoryMetadata( snapshotMetadata ) );
        Files.deleteIfExists(snapshotMetadataFile);

        artifactConverter.convert( artifact, targetRepository );
        checkSuccess( artifactConverter );

        Path artifactFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        assertTrue( "Check artifact created", Files.exists(artifactFile) );
        assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile.toFile(), artifact.getFile() ) );

        artifact = createPomArtifact( artifact );
        Path pomFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        Path sourcePomFile = Paths.get( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );
        assertTrue( "Check POM created", Files.exists(pomFile) );

        compareFiles( sourcePomFile, pomFile );

        assertTrue( "Check artifact metadata created", Files.exists(artifactMetadataFile) );

        Path expectedMetadataFile = getTestFile( "src/test/expected-files/v4-snapshot-artifact-metadata.xml" );

        compareFiles( expectedMetadataFile, artifactMetadataFile );

        assertTrue( "Check snapshot metadata created", Files.exists(snapshotMetadataFile) );

        expectedMetadataFile = getTestFile( expectedMetadataFileName );

        compareFiles( expectedMetadataFile, snapshotMetadataFile );
    }

    @Test
    public void testV3SnapshotPomConvert()
        throws Exception
    {
        // test that the pom is coverted

        Artifact artifact = createArtifact( "test", "v3artifact", "1.0.0-SNAPSHOT" );
        ArtifactMetadata artifactMetadata = new ArtifactRepositoryMetadata( artifact );
        Path artifactMetadataFile = Paths.get( targetRepository.getBasedir(),
                                              targetRepository.pathOfRemoteRepositoryMetadata( artifactMetadata ) );
        Files.deleteIfExists(artifactMetadataFile);

        ArtifactMetadata snapshotMetadata = new SnapshotArtifactRepositoryMetadata( artifact );
        Path snapshotMetadataFile = Paths.get( targetRepository.getBasedir(),
                                              targetRepository.pathOfRemoteRepositoryMetadata( snapshotMetadata ) );
        Files.deleteIfExists(snapshotMetadataFile);

        artifactConverter.convert( artifact, targetRepository );
        checkSuccess( artifactConverter );

        Path artifactFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        assertTrue( "Check artifact created", Files.exists(artifactFile) );
        assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile.toFile(), artifact.getFile() ) );

        artifact = createPomArtifact( artifact );
        Path pomFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        Path expectedPomFile = getTestFile( "src/test/expected-files/converted-v3-snapshot.pom" );
        assertTrue( "Check POM created", Files.exists(pomFile) );

        compareFiles( expectedPomFile, pomFile );

        assertTrue( "Check artifact metadata created", Files.exists(artifactMetadataFile) );

        Path expectedMetadataFile = getTestFile( "src/test/expected-files/v3-snapshot-artifact-metadata.xml" );

        compareFiles( expectedMetadataFile, artifactMetadataFile );

        assertTrue( "Check snapshot metadata created", Files.exists(snapshotMetadataFile) );

        expectedMetadataFile = getTestFile( "src/test/expected-files/v3-snapshot-metadata.xml" );

        compareFiles( expectedMetadataFile, snapshotMetadataFile );
    }

    @Test
    public void testV4SnapshotPomConvert()
        throws Exception
    {
        doTestV4SnapshotPomConvert( "1.0.0-SNAPSHOT", "src/test/expected-files/v4-snapshot-metadata.xml" );

        assertTrue( true );
    }

    @Test
    public void testV4TimestampedSnapshotPomConvert()
        throws Exception
    {
        doTestV4SnapshotPomConvert( "1.0.0-20060111.120115-1",
                                    "src/test/expected-files/v4-timestamped-snapshot-metadata.xml" );

        assertTrue( true );
    }

    @Test
    public void testMavenOnePluginConversion()
        throws Exception
    {
        Artifact artifact =
            createArtifact( "org.apache.maven.plugins", "maven-foo-plugin", "1.0", "1.0", "maven-plugin" );
        artifact.setFile(
            Paths.get( org.apache.archiva.common.utils.FileUtils.getBasedir(), "src/test/source-repository/test/plugins/maven-foo-plugin-1.0.jar" ).toFile() );
        artifactConverter.convert( artifact, targetRepository );
        // There is a warning but I can't figure out how to look at it. Eyeballing the results it appears
        // the plugin is being coverted correctly.
        //checkSuccess();

        Path artifactFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        assertTrue( "Check artifact created", Files.exists(artifactFile) );
        assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile.toFile(), artifact.getFile() ) );

        /*
         The POM isn't needed for Maven 1.x plugins but the raw conversion for  

         artifact = createPomArtifact( artifact );
         Path pomFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
         File expectedPomFile = getTestFile( "src/test/expected-files/maven-foo-plugin-1.0.pom" );
         assertTrue( "Check POM created", Files.exists(pomFile) );
         compareFiles( expectedPomFile, pomFile );
         */
    }

    @Test
    public void testV3TimestampedSnapshotPomConvert()
        throws Exception
    {
        // test that the pom is coverted

        Artifact artifact = createArtifact( "test", "v3artifact", "1.0.0-20060105.130101-3" );
        ArtifactMetadata artifactMetadata = new ArtifactRepositoryMetadata( artifact );
        Path artifactMetadataFile = Paths.get( targetRepository.getBasedir(),
                                              targetRepository.pathOfRemoteRepositoryMetadata( artifactMetadata ) );
        Files.deleteIfExists(artifactMetadataFile);

        ArtifactMetadata snapshotMetadata = new SnapshotArtifactRepositoryMetadata( artifact );
        Path snapshotMetadataFile = Paths.get( targetRepository.getBasedir(),
                                              targetRepository.pathOfRemoteRepositoryMetadata( snapshotMetadata ) );
        Files.deleteIfExists(snapshotMetadataFile);

        artifactConverter.convert( artifact, targetRepository );
        checkSuccess( artifactConverter );

        Path artifactFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        assertTrue( "Check artifact created", Files.exists(artifactFile) );
        assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile.toFile(), artifact.getFile() ) );

        artifact = createPomArtifact( artifact );
        Path pomFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        Path expectedPomFile = getTestFile( "src/test/expected-files/converted-v3-timestamped-snapshot.pom" );
        assertTrue( "Check POM created", Files.exists(pomFile) );

        compareFiles( expectedPomFile, pomFile );

        assertTrue( "Check artifact snapshotMetadata created", Files.exists(artifactMetadataFile) );

        Path expectedMetadataFile = getTestFile( "src/test/expected-files/v3-snapshot-artifact-metadata.xml" );

        compareFiles( expectedMetadataFile, artifactMetadataFile );

        assertTrue( "Check snapshot snapshotMetadata created", Files.exists(snapshotMetadataFile) );

        expectedMetadataFile = getTestFile( "src/test/expected-files/v3-timestamped-snapshot-metadata.xml" );

        compareFiles( expectedMetadataFile, snapshotMetadataFile );
    }

    @Test
    public void testNoPomConvert()
        throws Exception
    {
        // test that a POM is not created when there was none at the source

        Artifact artifact = createArtifact( "test", "noPomArtifact", "1.0.0" );
        artifactConverter.convert( artifact, targetRepository );
        checkWarnings( artifactConverter, 1 );

        assertHasWarningReason( artifactConverter, Messages.getString( "warning.missing.pom" ) );

        Path artifactFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        assertTrue( "Check artifact created", Files.exists(artifactFile) );
        assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile.toFile(), artifact.getFile() ) );

        artifact = createPomArtifact( artifact );
        Path pomFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        Path sourcePomFile = Paths.get( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );

        assertFalse( "Check no POM created", Files.exists(pomFile) );
        assertFalse( "No source POM", Files.exists(sourcePomFile) );
    }

    @Test
    public void testIncorrectSourceChecksumMd5()
        throws Exception
    {
        // test that it fails when the source md5 is wrong

        Artifact artifact = createArtifact( "test", "incorrectMd5Artifact", "1.0.0" );
        Path file = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        Files.deleteIfExists(file);

        artifactConverter.convert( artifact, targetRepository );
        checkWarnings( artifactConverter, 2 );

        assertHasWarningReason( artifactConverter, Messages.getString( "failure.incorrect.md5" ) );

        assertFalse( "Check artifact not created", Files.exists(file) );

        ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact );
        Path metadataFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOfRemoteRepositoryMetadata( metadata ) );
        assertFalse( "Check metadata not created", Files.exists(metadataFile) );
    }

    @Test
    public void testIncorrectSourceChecksumSha1()
        throws Exception
    {
        // test that it fails when the source sha1 is wrong

        Artifact artifact = createArtifact( "test", "incorrectSha1Artifact", "1.0.0" );
        Path file = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        Files.deleteIfExists(file);

        artifactConverter.convert( artifact, targetRepository );
        checkWarnings( artifactConverter, 2 );

        assertHasWarningReason( artifactConverter, Messages.getString( "failure.incorrect.sha1" ) );

        assertFalse( "Check artifact not created", Files.exists(file) );

        ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact );
        Path metadataFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOfRemoteRepositoryMetadata( metadata ) );
        assertFalse( "Check metadata not created", Files.exists(metadataFile) );
    }

    @Test
    public void testUnmodifiedArtifact()
        throws Exception, InterruptedException
    {
        // test the unmodified artifact is untouched

        Artifact artifact = createArtifact( "test", "unmodified-artifact", "1.0.0" );
        Artifact pomArtifact = createPomArtifact( artifact );

        Path sourceFile = Paths.get( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );
        Path sourcePomFile = Paths.get( sourceRepository.getBasedir(), sourceRepository.pathOf( pomArtifact ) );
        Path targetFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        Path targetPomFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( pomArtifact ) );

        assertTrue( "Check target file exists", Files.exists(targetFile) );
        assertTrue( "Check target POM exists", Files.exists(targetPomFile) );

        Files.setLastModifiedTime( sourceFile, FileTime.from(System.currentTimeMillis(), TimeUnit.MILLISECONDS) );
        Files.setLastModifiedTime( sourcePomFile, FileTime.from(System.currentTimeMillis(), TimeUnit.MILLISECONDS) );

        long origTime = Files.getLastModifiedTime( targetFile ).toMillis();
        long origPomTime = Files.getLastModifiedTime( targetPomFile ).toMillis();

        // Need to guarantee last modified is not equal
        Thread.sleep( SLEEP_MILLIS );

        artifactConverter.convert( artifact, targetRepository );
        checkSuccess( artifactConverter );

        compareFiles( sourceFile, targetFile );
        compareFiles( sourcePomFile, targetPomFile );

        assertEquals( "Check artifact unmodified", origTime, Files.getLastModifiedTime( targetFile ).toMillis() );
        assertEquals( "Check POM unmodified", origPomTime, Files.getLastModifiedTime( targetPomFile ).toMillis() );
    }

    @Test
    public void testModifedArtifactFails()
        throws Exception
    {
        // test that it fails when the source artifact has changed and is different to the existing artifact in the
        // target repository

        Artifact artifact = createArtifact( "test", "modified-artifact", "1.0.0" );
        Artifact pomArtifact = createPomArtifact( artifact );

        Path sourceFile = Paths.get( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );
        Path sourcePomFile = Paths.get( sourceRepository.getBasedir(), sourceRepository.pathOf( pomArtifact ) );
        Path targetFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        Path targetPomFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( pomArtifact ) );

        assertTrue( "Check target file exists", Files.exists(targetFile) );
        assertTrue( "Check target POM exists", Files.exists(targetPomFile) );

        Files.setLastModifiedTime(sourceFile, FileTime.from(System.currentTimeMillis() , TimeUnit.MILLISECONDS));
        Files.setLastModifiedTime(sourcePomFile, FileTime.from(System.currentTimeMillis() , TimeUnit.MILLISECONDS));

        long origTime = Files.getLastModifiedTime(targetFile).toMillis();
        long origPomTime = Files.getLastModifiedTime(targetPomFile).toMillis();

        // Need to guarantee last modified is not equal
        Thread.sleep( SLEEP_MILLIS );

        artifactConverter.convert( artifact, targetRepository );
        checkWarnings( artifactConverter, 2 );

        assertHasWarningReason( artifactConverter, Messages.getString( "failure.target.already.exists" ) );

        assertEquals( "Check unmodified", origTime, Files.getLastModifiedTime(targetFile).toMillis() );
        assertEquals( "Check unmodified", origPomTime, Files.getLastModifiedTime(targetPomFile).toMillis() );

        ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact );
        Path metadataFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOfRemoteRepositoryMetadata( metadata ) );
        assertFalse( "Check metadata not created", Files.exists(metadataFile) );
    }

    @Test
    public void testForcedUnmodifiedArtifact()
        throws Exception
    {
        // test unmodified artifact is still converted when set to force

        artifactConverter =
            applicationContext.getBean( "artifactConverter#force-repository-converter", ArtifactConverter.class );

        Artifact artifact = createArtifact( "test", "unmodified-artifact", "1.0.0" );
        Artifact pomArtifact = createPomArtifact( artifact );

        Path sourceFile = Paths.get( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );
        Path sourcePomFile = Paths.get( sourceRepository.getBasedir(), sourceRepository.pathOf( pomArtifact ) );
        Path targetFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        Path targetPomFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( pomArtifact ) );

        SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd", Locale.getDefault() );
        long origTime = dateFormat.parse( "2006-03-03" ).getTime();
        Files.setLastModifiedTime(targetFile, FileTime.from(origTime , TimeUnit.MILLISECONDS));
        Files.setLastModifiedTime(targetPomFile, FileTime.from(origTime , TimeUnit.MILLISECONDS));

        Files.setLastModifiedTime(sourceFile, FileTime.from(dateFormat.parse( "2006-01-01" ).getTime() , TimeUnit.MILLISECONDS));
        Files.setLastModifiedTime(sourcePomFile, FileTime.from(dateFormat.parse( "2006-02-02" ).getTime() , TimeUnit.MILLISECONDS));

        artifactConverter.convert( artifact, targetRepository );
        checkSuccess( artifactConverter );

        compareFiles( sourceFile, targetFile );
        compareFiles( sourcePomFile, targetPomFile );

        assertFalse( "Check modified", origTime == Files.getLastModifiedTime(targetFile).toMillis() );
        assertFalse( "Check modified", origTime == Files.getLastModifiedTime(targetPomFile).toMillis() );

        ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact );
        Path metadataFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOfRemoteRepositoryMetadata( metadata ) );
        assertTrue( "Check metadata created", Files.exists(metadataFile) );
    }

    @Test
    public void testDryRunSuccess()
        throws Exception
    {
        // test dry run does nothing on a run that will be successful, and returns success

        artifactConverter =
            applicationContext.getBean( "artifactConverter#dryrun-repository-converter", ArtifactConverter.class );

        Artifact artifact = createArtifact( "test", "dryrun-artifact", "1.0.0" );
        Artifact pomArtifact = createPomArtifact( artifact );

        Path sourceFile = Paths.get( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );
        Path sourcePomFile = Paths.get( sourceRepository.getBasedir(), sourceRepository.pathOf( pomArtifact ) );
        Path targetFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        Path targetPomFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( pomArtifact ) );

        // clear warning before test related to MRM-1638
        artifactConverter.clearWarnings();
        artifactConverter.convert( artifact, targetRepository );
        checkSuccess( artifactConverter );

        assertTrue( "Check source file exists", Files.exists(sourceFile) );
        assertTrue( "Check source POM exists", Files.exists(sourcePomFile) );

        assertFalse( "Check target file doesn't exist", Files.exists(targetFile) );
        assertFalse( "Check target POM doesn't exist", Files.exists(targetPomFile) );

        ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact );
        Path metadataFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOfRemoteRepositoryMetadata( metadata ) );
        assertFalse( "Check metadata not created", Files.exists(metadataFile) );
    }

    @Test
    public void testDryRunFailure()
        throws Exception
    {
        // test dry run does nothing on a run that will fail, and returns failure

        artifactConverter =
            applicationContext.getBean( "artifactConverter#dryrun-repository-converter", ArtifactConverter.class );

        Artifact artifact = createArtifact( "test", "modified-artifact", "1.0.0" );
        Artifact pomArtifact = createPomArtifact( artifact );

        Path sourceFile = Paths.get( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );
        Path sourcePomFile = Paths.get( sourceRepository.getBasedir(), sourceRepository.pathOf( pomArtifact ) );
        Path targetFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        Path targetPomFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( pomArtifact ) );

        assertTrue( "Check target file exists", Files.exists(targetFile) );
        assertTrue( "Check target POM exists", Files.exists(targetPomFile) );

        Files.setLastModifiedTime(sourceFile, FileTime.from(System.currentTimeMillis() , TimeUnit.MILLISECONDS));
        Files.setLastModifiedTime(sourcePomFile, FileTime.from(System.currentTimeMillis() , TimeUnit.MILLISECONDS));

        long origTime = Files.getLastModifiedTime(targetFile).toMillis();
        long origPomTime = Files.getLastModifiedTime(targetPomFile).toMillis();

        // Need to guarantee last modified is not equal
        Thread.sleep( SLEEP_MILLIS );

        // clear warning before test related to MRM-1638
        artifactConverter.clearWarnings();
        artifactConverter.convert( artifact, targetRepository );
        checkWarnings( artifactConverter, 2 );

        assertHasWarningReason( artifactConverter, Messages.getString( "failure.target.already.exists" ) );

        assertEquals( "Check unmodified", origTime, Files.getLastModifiedTime(targetFile).toMillis() );
        assertEquals( "Check unmodified", origPomTime, Files.getLastModifiedTime(targetPomFile).toMillis() );

        ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact );
        Path metadataFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOfRemoteRepositoryMetadata( metadata ) );
        assertFalse( "Check metadata not created", Files.exists(metadataFile) );
    }

    @Test
    public void testRollbackArtifactCreated()
        throws Exception
    {
        // test rollback can remove a created artifact, including checksums

        Artifact artifact = createArtifact( "test", "rollback-created-artifact", "1.0.0" );
        ArtifactMetadata artifactMetadata = new ArtifactRepositoryMetadata( artifact );
        Path artifactMetadataFile = Paths.get( targetRepository.getBasedir(),
                                              targetRepository.pathOfRemoteRepositoryMetadata( artifactMetadata ) );
        org.apache.archiva.common.utils.FileUtils.deleteDirectory( artifactMetadataFile.getParent() );

        ArtifactMetadata versionMetadata = new SnapshotArtifactRepositoryMetadata( artifact );
        Path versionMetadataFile = Paths.get( targetRepository.getBasedir(),
                                             targetRepository.pathOfRemoteRepositoryMetadata( versionMetadata ) );

        Path artifactFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );

        artifactConverter.convert( artifact, targetRepository );
        checkWarnings( artifactConverter, 2 );

        boolean found = false;
        String pattern = "^" + Messages.getString( "invalid.source.pom" ).replaceFirst( "\\{0\\}", ".*" ) + "$";
        for ( List<String> messages : artifactConverter.getWarnings().values() )
        {
            for ( String message : messages )
            {
                if ( message.matches( pattern ) )
                {
                    found = true;
                    break;
                }
            }

            if ( found )
            {
                break;
            }
        }

        assertTrue( "Check failure message.", found );

        assertFalse( "check artifact rolled back", Files.exists(artifactFile) );
        assertFalse( "check metadata rolled back", Files.exists(artifactMetadataFile) );
        assertFalse( "check metadata rolled back", Files.exists(versionMetadataFile) );
    }

    @Test
    public void testMultipleArtifacts()
        throws Exception
    {
        // test multiple artifacts are converted

        List<Artifact> artifacts = new ArrayList<>();
        artifacts.add( createArtifact( "test", "artifact-one", "1.0.0" ) );
        artifacts.add( createArtifact( "test", "artifact-two", "1.0.0" ) );
        artifacts.add( createArtifact( "test", "artifact-three", "1.0.0" ) );

        for ( Artifact artifact : artifacts )
        {
            artifactConverter.convert( artifact, targetRepository );
            checkSuccess( artifactConverter );
        }

        for ( Artifact artifact : artifacts )
        {
            Path artifactFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
            assertTrue( "Check artifact created", Files.exists(artifactFile) );
            assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile.toFile(), artifact.getFile() ) );

            artifact = createPomArtifact( artifact );
            Path pomFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
            Path expectedPomFile =
                getTestFile( "src/test/expected-files/converted-" + artifact.getArtifactId() + ".pom" );
            assertTrue( "Check POM created", Files.exists(pomFile) );

            compareFiles( expectedPomFile, pomFile );
        }
    }

    @Test
    public void testInvalidSourceArtifactMetadata()
        throws Exception
    {
        // test artifact is not converted when source metadata is invalid, and returns failure

        createModernSourceRepository();

        Artifact artifact = createArtifact( "test", "incorrectArtifactMetadata", "1.0.0" );
        Path file = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        Files.deleteIfExists(file);

        artifactConverter.convert( artifact, targetRepository );
        checkWarnings( artifactConverter, 2 );

        assertHasWarningReason( artifactConverter,
                                Messages.getString( "failure.incorrect.artifactMetadata.versions" ) );

        assertFalse( "Check artifact not created", Files.exists(file) );

        ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact );
        Path metadataFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOfRemoteRepositoryMetadata( metadata ) );
        assertFalse( "Check metadata not created", Files.exists(metadataFile) );
    }

    @Test
    public void testInvalidSourceSnapshotMetadata()
        throws Exception
    {
        // test artifact is not converted when source snapshot metadata is invalid and returns failure

        createModernSourceRepository();

        Artifact artifact = createArtifact( "test", "incorrectSnapshotMetadata", "1.0.0-20060102.030405-6" );
        Path file = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        Files.deleteIfExists(file);

        artifactConverter.convert( artifact, targetRepository );
        checkWarnings( artifactConverter, 2 );

        assertHasWarningReason( artifactConverter,
                                Messages.getString( "failure.incorrect.snapshotMetadata.snapshot" ) );

        assertFalse( "Check artifact not created", Files.exists(file) );

        ArtifactRepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact );
        Path metadataFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOfRemoteRepositoryMetadata( metadata ) );
        assertFalse( "Check metadata not created", Files.exists(metadataFile) );
    }

    @Test
    public void testMergeArtifactMetadata()
        throws Exception
    {
        // test artifact level metadata is merged when it already exists on successful conversion

        Artifact artifact = createArtifact( "test", "newversion-artifact", "1.0.1" );
        artifactConverter.convert( artifact, targetRepository );
        checkSuccess( artifactConverter );

        Path artifactFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        assertTrue( "Check artifact created", Files.exists(artifactFile) );
        assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile.toFile(), artifact.getFile() ) );

        artifact = createPomArtifact( artifact );
        Path pomFile = Paths.get( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        Path sourcePomFile = Paths.get( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );
        assertTrue( "Check POM created", Files.exists(pomFile) );

        compareFiles( sourcePomFile, pomFile );

        ArtifactMetadata artifactMetadata = new ArtifactRepositoryMetadata( artifact );
        Path artifactMetadataFile = Paths.get( targetRepository.getBasedir(),
                                              targetRepository.pathOfRemoteRepositoryMetadata( artifactMetadata ) );
        assertTrue( "Check artifact metadata created", Files.exists(artifactMetadataFile) );

        Path expectedMetadataFile = getTestFile( "src/test/expected-files/newversion-artifact-metadata.xml" );

        compareFiles( expectedMetadataFile, artifactMetadataFile );
    }

    @Test
    public void testSourceAndTargetRepositoriesMatch()
        throws Exception
    {
        // test that it fails if the same

        ArtifactRepositoryFactory factory = plexusSisuBridge.lookup( ArtifactRepositoryFactory.class );

        sourceRepository =
            factory.createArtifactRepository( "source", targetRepository.getUrl(), targetRepository.getLayout(), null,
                                              null );

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

    private Artifact createArtifact( String groupId, String artifactId, String baseVersion, String version,
                                     String type )
    {
        Artifact artifact = artifactFactory.createArtifact( groupId, artifactId, version, null, type );
        artifact.setBaseVersion( baseVersion );
        artifact.setRepository( sourceRepository );
        artifact.setFile( Paths.get( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) ).toFile() );
        return artifact;
    }

    private Artifact createPomArtifact( Artifact artifact )
    {
        return createArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getBaseVersion(),
                               artifact.getVersion(), "pom" );
    }

    private static void compareFiles( Path expectedPomFile, Path pomFile )
        throws IOException
    {
        String expectedContent = normalizeString(
            org.apache.archiva.common.utils.FileUtils.readFileToString( expectedPomFile, Charset.defaultCharset() ) );
        String targetContent =
            normalizeString( org.apache.archiva.common.utils.FileUtils.readFileToString( pomFile, Charset.defaultCharset() ) );
        assertEquals( "Check file match between " + expectedPomFile + " and " + pomFile, expectedContent,
                      targetContent );
    }

    private static String normalizeString( String path )
    {
        return path.trim().replaceAll( "\r\n", "\n" ).replace( '\r', '\n' ).replaceAll( "<\\?xml .+\\?>",
                                                                                        "" ).replaceAll( "^\\s+", "" );
    }

    private void checkSuccess( ArtifactConverter converter )
    {
        assertNotNull( "Warnings should never be null.", converter.getWarnings() );
        assertEquals( "Should have no warnings. " + converter.getWarnings(), 0, countWarningMessages( converter ) );
    }

    private void checkWarnings( ArtifactConverter converter, int count )
    {
        assertNotNull( "Warnings should never be null.", converter.getWarnings() );
        assertEquals( "Should have some warnings.", count, countWarningMessages( converter ) );
    }

    private int countWarningMessages( ArtifactConverter converter )
    {
        int count = 0;
        for ( List<String> values : converter.getWarnings().values() )
        {
            count += values.size();
        }
        return count;
    }

    private void assertHasWarningReason( ArtifactConverter converter, String reason )
    {
        assertNotNull( "Warnings should never be null.", converter.getWarnings() );
        assertTrue( "Expecting 1 or more Warnings", countWarningMessages( converter ) > 0 );

        for ( List<String> messages : converter.getWarnings().values() )
        {
            if ( messages.contains( reason ) )
            {
                /* No need to check any further */
                return;
            }
        }

        /* didn't find it. */

        for ( Map.Entry<Artifact, List<String>> entry : converter.getWarnings().entrySet() )
        {
            Artifact artifact = (Artifact) entry.getKey();
            System.out.println(
                "-Artifact: " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion() );
            List<String> messages = entry.getValue();
            for ( String message : messages )
            {
                System.out.println( "  " + message );
            }
        }
        fail( "Unable to find message <" + reason + "> in warnings." );
    }

    private void createModernSourceRepository()
        throws Exception
    {
        ArtifactRepositoryFactory factory = plexusSisuBridge.lookup( ArtifactRepositoryFactory.class );

        ArtifactRepositoryLayout layout = plexusSisuBridge.lookup( ArtifactRepositoryLayout.class, "default" );

        Path sourceBase = getTestFile( "src/test/source-modern-repository" );
        sourceRepository =
            factory.createArtifactRepository( "source", sourceBase.toUri().toURL().toString(), layout, null, null );
    }
}
