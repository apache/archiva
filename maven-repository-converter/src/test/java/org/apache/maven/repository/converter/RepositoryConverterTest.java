package org.apache.maven.repository.converter;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Test the repository converter.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @todo what about deletions from the source repository?
 * @todo use artifact-test instead
 * @todo should reject if dependencies are missing - rely on reporting?
 */
public class RepositoryConverterTest
    extends PlexusTestCase
{
    private ArtifactRepository sourceRepository;

    private ArtifactRepository targetRepository;

    private RepositoryConverter repositoryConverter;

    private ArtifactFactory artifactFactory;

    private static final int SLEEP_MILLIS = 100;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArtifactRepositoryFactory factory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );

        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "legacy" );

        File sourceBase = getTestFile( "src/test/source-repository" );
        sourceRepository =
            factory.createArtifactRepository( "source", sourceBase.toURL().toString(), layout, null, null );

        layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );

        File targetBase = getTestFile( "target/test-target-repository" );
        FileUtils.copyDirectoryStructure( getTestFile( "src/test/target-repository" ), targetBase );

        targetRepository =
            factory.createArtifactRepository( "target", targetBase.toURL().toString(), layout, null, null );

        repositoryConverter = (RepositoryConverter) lookup( RepositoryConverter.ROLE, "default" );

        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
    }

    public void testV4PomConvert()
        throws IOException, RepositoryConversionException
    {
        // test that it is copied as is

        Artifact artifact = createArtifact( "test", "v4artifact", "1.0.0" );
        repositoryConverter.convert( artifact, targetRepository );

        File artifactFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        assertTrue( "Check artifact created", artifactFile.exists() );
        assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile, artifact.getFile() ) );

        artifact = createPomArtifact( artifact );
        File pomFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        File sourcePomFile = new File( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );
        assertTrue( "Check POM created", pomFile.exists() );

        String sourceContent = FileUtils.fileRead( sourcePomFile ).trim();
        String targetContent = FileUtils.fileRead( pomFile ).trim();
        assertEquals( "Check POM matches", sourceContent, targetContent );
    }

    public void testV3PomConvert()
        throws IOException, RepositoryConversionException
    {
        // test that the pom is coverted

        Artifact artifact = createArtifact( "test", "v3artifact", "1.0.0" );
        repositoryConverter.convert( artifact, targetRepository );

        File artifactFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        assertTrue( "Check artifact created", artifactFile.exists() );
        assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile, artifact.getFile() ) );

        artifact = createPomArtifact( artifact );
        File pomFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        File expectedPomFile = getTestFile( "src/test/expected-files/converted-v3.pom" );
        assertTrue( "Check POM created", pomFile.exists() );

        compareFiles( expectedPomFile, pomFile );

        // TODO: test warnings (separate test?)
    }

    private static void compareFiles( File expectedPomFile, File pomFile )
        throws IOException
    {
        String expectedContent = normalizeString( FileUtils.fileRead( expectedPomFile ) );
        String targetContent = normalizeString( FileUtils.fileRead( pomFile ) );
        assertEquals( "Check POM was converted", expectedContent, targetContent );
    }

    private static String normalizeString( String path )
    {
        return path.trim().replace( "\r\n", "\n" ).replace( '\r', '\n' );
    }

    public void testNoPomConvert()
        throws IOException, RepositoryConversionException
    {
        // test that a POM is created when there was none at the source

        Artifact artifact = createArtifact( "test", "noPomArtifact", "1.0.0" );
        repositoryConverter.convert( artifact, targetRepository );

        File artifactFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        assertTrue( "Check artifact created", artifactFile.exists() );
        assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile, artifact.getFile() ) );

        artifact = createPomArtifact( artifact );
        File pomFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        File sourcePomFile = new File( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );
        // TODO: should we fail? Warn?
        assertFalse( "Check no POM created", pomFile.exists() );
        assertFalse( "No source POM", sourcePomFile.exists() );
    }

    public void testInvalidSourceChecksumMd5()
        throws RepositoryConversionException
    {
        // test that it fails when the source md5 is not a valid md5

        Artifact artifact = createArtifact( "test", "invalidMd5Artifact", "1.0.0" );
        repositoryConverter.convert( artifact, targetRepository );

        // TODO: check for failure
    }

    public void testInvalidSourceChecksumSha1()
    {
        // test that it fails when the source sha1 is not a valid sha1

        // TODO: using exceptions at this level, or passing in reporter?
    }

    public void testIncorrectSourceChecksumMd5()
    {
        // test that it fails when the source md5 is wrong

        // TODO: using exceptions at this level, or passing in reporter?
    }

    public void testIncorrectSourceChecksumSha1()
    {
        // test that it fails when the source sha1 is wrong

        // TODO: using exceptions at this level, or passing in reporter?
    }

    public void testUnmodifiedArtifact()
        throws RepositoryConversionException, IOException, InterruptedException
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

        repositoryConverter.convert( artifact, targetRepository );

        String expectedContent = FileUtils.fileRead( sourceFile ).trim();
        String targetContent = FileUtils.fileRead( targetFile ).trim();
        assertEquals( "Check file matches", expectedContent, targetContent );

        expectedContent = FileUtils.fileRead( sourcePomFile ).trim();
        targetContent = FileUtils.fileRead( targetPomFile ).trim();
        assertEquals( "Check POM matches", expectedContent, targetContent );

        assertEquals( "Check unmodified", origTime, targetFile.lastModified() );
        assertEquals( "Check unmodified", origPomTime, targetPomFile.lastModified() );
    }

    public void testModifedArtifactFails()
    {
        // test that it fails when the source artifact has changed and is different to the existing artifact in the
        // target repository

        // TODO
    }

    public void testForcedUnmodifiedArtifact()
        throws Exception, IOException
    {
        // test unmodified artifact is still converted when set to force

        repositoryConverter = (RepositoryConverter) lookup( RepositoryConverter.ROLE, "force-repository-converter" );

        Artifact artifact = createArtifact( "test", "unmodified-artifact", "1.0.0" );
        Artifact pomArtifact = createPomArtifact( artifact );

        File sourceFile = new File( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );
        File sourcePomFile = new File( sourceRepository.getBasedir(), sourceRepository.pathOf( pomArtifact ) );
        File targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        File targetPomFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( pomArtifact ) );

        long origTime = targetFile.lastModified();
        long origPomTime = targetPomFile.lastModified();

        sourceFile.setLastModified( System.currentTimeMillis() );
        sourcePomFile.setLastModified( System.currentTimeMillis() );

        // Need to guarantee last modified is not equal
        Thread.sleep( SLEEP_MILLIS );

        repositoryConverter.convert( artifact, targetRepository );

        String expectedContent = FileUtils.fileRead( sourceFile ).trim();
        String targetContent = FileUtils.fileRead( targetFile ).trim();
        assertEquals( "Check file matches", expectedContent, targetContent );

        expectedContent = FileUtils.fileRead( sourcePomFile ).trim();
        targetContent = FileUtils.fileRead( targetPomFile ).trim();
        assertEquals( "Check POM matches", expectedContent, targetContent );

        assertFalse( "Check modified", origTime == targetFile.lastModified() );
        assertFalse( "Check modified", origPomTime == targetPomFile.lastModified() );
    }

    public void testDryRunSuccess()
        throws Exception
    {
        // test dry run does nothing on a run that will be successful, and returns success

        repositoryConverter = (RepositoryConverter) lookup( RepositoryConverter.ROLE, "dryrun-repository-converter" );

        Artifact artifact = createArtifact( "test", "dryrun-artifact", "1.0.0" );
        Artifact pomArtifact = createPomArtifact( artifact );

        File sourceFile = new File( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) );
        File sourcePomFile = new File( sourceRepository.getBasedir(), sourceRepository.pathOf( pomArtifact ) );
        File targetFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
        File targetPomFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( pomArtifact ) );

        repositoryConverter.convert( artifact, targetRepository );

        assertTrue( "Check source file exists", sourceFile.exists() );
        assertTrue( "Check source POM exists", sourcePomFile.exists() );

        assertFalse( "Check target file doesn't exist", targetFile.exists() );
        assertFalse( "Check target POM doesn't exist", targetPomFile.exists() );
    }

    public void testDryRunFailure()
    {
        // test dry run does nothing on a run that will fail, and returns failure

        // TODO
    }

    public void testRollbackArtifactCreated()
    {
        // test rollback can remove a created artifact, including checksums

        // TODO
    }

    public void testRollbackArtifactChanged()
    {
        // test rollback can undo changes to an artifact, including checksums

        // TODO
    }

    public void testRollbackMetadataCreated()
    {
        // test rollback can remove a created artifact's metadata, including checksums

        // TODO
    }

    public void testRollbackMetadataChanged()
    {
        // test rollback can undo changes to an artifact's metadata, including checksums

        // TODO
    }

    public void testMultipleArtifacts()
        throws RepositoryConversionException, IOException
    {
        // test multiple artifacts are converted

        List artifacts = new ArrayList();
        artifacts.add( createArtifact( "test", "artifact-one", "1.0.0" ) );
        artifacts.add( createArtifact( "test", "artifact-two", "1.0.0" ) );
        artifacts.add( createArtifact( "test", "artifact-three", "1.0.0" ) );
        repositoryConverter.convert( artifacts, targetRepository );

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            File artifactFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
            assertTrue( "Check artifact created", artifactFile.exists() );
            assertTrue( "Check artifact matches", FileUtils.contentEquals( artifactFile, artifact.getFile() ) );

            artifact = createPomArtifact( artifact );
            File pomFile = new File( targetRepository.getBasedir(), targetRepository.pathOf( artifact ) );
            File expectedPomFile =
                getTestFile( "src/test/expected-files/converted-" + artifact.getArtifactId() + ".pom" );
            assertTrue( "Check POM created", pomFile.exists() );

            compareFiles( expectedPomFile, pomFile );
        }
    }

    public void testInvalidSourceArtifactMetadata()
    {
        // test artifact is not converted when source metadata is invalid, and returns failure

        // TODO
    }

    public void testSnapshotArtifact()
    {
        // test snapshot artifact is converted

        // TODO
    }

    public void testInvalidSourceSnapshotMetadata()
    {
        // test artifact is not converted when source snapshot metadata is invalid and returns failure

        // TODO
    }

    public void testCreateArtifactMetadata()
    {
        // test artifact level metadata is created when it doesn't exist on successful conversion

        // TODO
    }

    public void testCreateSnapshotMetadata()
    {
        // test snapshot metadata is created when it doesn't exist on successful conversion

        // TODO
    }

    public void testMergeArtifactMetadata()
    {
        // test artifact level metadata is merged when it already exists on successful conversion

        // TODO
    }

    public void testMergeSnapshotMetadata()
    {
        // test snapshot metadata is merged when it already exists on successful conversion

        // TODO
    }

    public void testSourceAndTargetRepositoriesMatch()
    {
        // test that it fails if the same (initially - later we might allow this with extra checks)

        // TODO
    }

    private Artifact createArtifact( String groupId, String artifactId, String version )
    {
        return createArtifact( groupId, artifactId, version, "jar" );
    }

    private Artifact createArtifact( String groupId, String artifactId, String version, String type )
    {
        Artifact artifact = artifactFactory.createArtifact( groupId, artifactId, version, null, type );
        artifact.setRepository( sourceRepository );
        artifact.setFile( new File( sourceRepository.getBasedir(), sourceRepository.pathOf( artifact ) ) );
        return artifact;
    }

    private Artifact createPomArtifact( Artifact artifact )
    {
        return createArtifact( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), "pom" );
    }

}
