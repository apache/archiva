package org.apache.archiva.repository.maven.content;

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

import org.apache.archiva.common.filelock.FileLockManager;
import org.apache.archiva.common.utils.VersionComparator;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.FileType;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.ProjectReference;
import org.apache.archiva.model.VersionedReference;
import org.apache.archiva.repository.EditableManagedRepository;
import org.apache.archiva.repository.LayoutException;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.content.ItemSelector;
import org.apache.archiva.repository.maven.MavenManagedRepository;
import org.apache.archiva.repository.maven.metadata.storage.ArtifactMappingProvider;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * ManagedDefaultRepositoryContentTest
 */
public class ManagedDefaultRepositoryContentTest
    extends AbstractDefaultRepositoryContentTestCase
{
    private ManagedDefaultRepositoryContent repoContent;

    @Inject
    FileTypes fileTypes;

    @Inject
    @Named ( "archivaConfiguration#default" )
    ArchivaConfiguration archivaConfiguration;

    @Inject
    List<? extends ArtifactMappingProvider> artifactMappingProviders;

    @Inject
    MavenContentHelper contentHelper;

    @Inject
    FileLockManager fileLockManager;

    private Path getRepositoryPath(String repoName) {
        try
        {
            return Paths.get( Thread.currentThread( ).getContextClassLoader( ).getResource( "repositories/" + repoName ).toURI( ) );
        }
        catch ( URISyntaxException e )
        {
            throw new RuntimeException( "Could not resolve repository path " + e.getMessage( ), e );
        }
    }

    @Before
    public void setUp()
        throws Exception
    {
        Path repoDir = getRepositoryPath( "default-repository" );

        MavenManagedRepository repository = createRepository( "testRepo", "Unit Test Repo", repoDir );

        FileType fileType = archivaConfiguration.getConfiguration().getRepositoryScanning().getFileTypes().get( 0 );
        fileType.addPattern( "**/*.xml" );
        assertEquals( FileTypes.ARTIFACTS, fileType.getId() );

        fileTypes.afterConfigurationChange( null, "fileType", null );

        repoContent = new ManagedDefaultRepositoryContent(repository, artifactMappingProviders, fileTypes, fileLockManager);
        repoContent.setMavenContentHelper( contentHelper );
        
        //repoContent = (ManagedRepositoryContent) lookup( ManagedRepositoryContent.class, "default" );
    }

    @Test
    public void testGetVersionsBadArtifact()
        throws Exception
    {
        assertGetVersions( "bad_artifact", Collections.emptyList() );
    }

    @Test
    public void testGetVersionsMissingMultipleVersions()
        throws Exception
    {
        assertGetVersions( "missing_metadata_b", Arrays.asList( "1.0", "1.0.1", "2.0", "2.0.1", "2.0-20070821-dev" ) );
    }

    @Test
    public void testGetVersionsSimple()
        throws Exception
    {
        assertVersions( "proxied_multi", "2.1", new String[]{ "2.1" } );
    }

    @Test
    public void testGetVersionsSimpleYetIncomplete()
        throws Exception
    {
        assertGetVersions( "incomplete_metadata_a", Collections.singletonList( "1.0" ) );
    }

    @Test
    public void testGetVersionsSimpleYetMissing()
        throws Exception
    {
        assertGetVersions( "missing_metadata_a", Collections.singletonList( "1.0" ) );
    }

    @Test
    public void testGetVersionsSnapshotA()
        throws Exception
    {
        assertVersions( "snap_shots_a", "1.0-alpha-11-SNAPSHOT",
                        new String[]{ "1.0-alpha-11-SNAPSHOT", "1.0-alpha-11-20070221.194724-2",
                            "1.0-alpha-11-20070302.212723-3", "1.0-alpha-11-20070303.152828-4",
                            "1.0-alpha-11-20070305.215149-5", "1.0-alpha-11-20070307.170909-6",
                            "1.0-alpha-11-20070314.211405-9", "1.0-alpha-11-20070316.175232-11" } );
    }

    @Test
    public void testToMetadataPathFromProjectReference()
    {
        ProjectReference reference = new ProjectReference();
        reference.setGroupId( "com.foo" );
        reference.setArtifactId( "foo-tool" );

        assertEquals( "com/foo/foo-tool/maven-metadata.xml", repoContent.toMetadataPath( reference ) );
    }

    @Test
    public void testToMetadataPathFromVersionReference()
    {
        VersionedReference reference = new VersionedReference();
        reference.setGroupId( "com.foo" );
        reference.setArtifactId( "foo-tool" );
        reference.setVersion( "1.0" );

        assertEquals( "com/foo/foo-tool/1.0/maven-metadata.xml", repoContent.toMetadataPath( reference ) );
    }

    @Test
    @Override
    public void testToPathOnNullArtifactReference()
    {
        try
        {
            ArtifactReference reference = null;
            repoContent.toPath( reference );
            fail( "Should have failed due to null artifact reference." );
        }
        catch ( IllegalArgumentException e )
        {
            /* expected path */
        }
    }

    @Test
    public void testExcludeMetadataFile()
        throws Exception
    {
        assertVersions( "include_xml", "1.0", new String[]{ "1.0" } );
    }

    private void assertGetVersions( String artifactId, List<String> expectedVersions )
        throws Exception
    {
        ProjectReference reference = new ProjectReference();
        reference.setGroupId( "org.apache.archiva.metadata.tests" );
        reference.setArtifactId( artifactId );

        // Use the test metadata-repository, which is already setup for
        // These kind of version tests.
        Path repoDir = getRepositoryPath( "metadata-repository" );
        (( EditableManagedRepository)repoContent.getRepository()).setLocation( repoDir.toAbsolutePath().toUri() );

        // Request the versions.
        Set<String> testedVersionSet = repoContent.getVersions( reference );

        // Sort the list (for asserts)
        List<String> testedVersions = new ArrayList<>();
        testedVersions.addAll( testedVersionSet );
        Collections.sort( testedVersions, new VersionComparator() );

        // Test the expected array of versions, to the actual tested versions
        assertEquals( "available versions", expectedVersions, testedVersions );
    }

    private void assertVersions( String artifactId, String version, String[] expectedVersions )
        throws Exception
    {
        VersionedReference reference = new VersionedReference();
        reference.setGroupId( "org.apache.archiva.metadata.tests" );
        reference.setArtifactId( artifactId );
        reference.setVersion( version );

        // Use the test metadata-repository, which is already setup for
        // These kind of version tests.
        Path repoDir = getRepositoryPath( "metadata-repository" );
        ((EditableManagedRepository)repoContent.getRepository()).setLocation( repoDir.toAbsolutePath().toUri() );

        // Request the versions.
        Set<String> testedVersionSet = repoContent.getVersions( reference );

        // Sort the list (for asserts later)
        List<String> testedVersions = new ArrayList<>();
        testedVersions.addAll( testedVersionSet );
        Collections.sort( testedVersions, new VersionComparator() );

        // Test the expected array of versions, to the actual tested versions
        assertEquals( "Assert Versions: length/size", expectedVersions.length, testedVersions.size() );

        for ( int i = 0; i < expectedVersions.length; i++ )
        {
            String actualVersion = testedVersions.get( i );
            assertEquals( "Versions[" + i + "]", expectedVersions[i], actualVersion );
        }
    }


    @Override
    protected ArtifactReference toArtifactReference( String path )
        throws LayoutException
    {
        return repoContent.toArtifactReference( path );
    }

    @Override
    protected ItemSelector toItemSelector( String path ) throws LayoutException
    {
        return repoContent.toItemSelector( path );
    }

    @Override
    protected String toPath( ArtifactReference reference )
    {
        return repoContent.toPath( reference );
    }

    @Override
    protected String toPath( ItemSelector selector ) {
        return repoContent.toPath( selector );
    }

    @Override
    protected ManagedRepositoryContent getManaged( )
    {
        return repoContent;
    }

    private Path setupRepoCopy( String source, String target) throws IOException
    {
        Path defaultRepo = getRepositoryPath( source );
        Path newRepo = defaultRepo.getParent( ).resolve( target );
        FileUtils.copyDirectory( defaultRepo.toFile( ), newRepo.toFile( ) );

        MavenManagedRepository repository = createRepository( "testRepo", "Unit Test Repo", newRepo );

        FileType fileType = archivaConfiguration.getConfiguration().getRepositoryScanning().getFileTypes().get( 0 );
        fileType.addPattern( "**/*.xml" );
        assertEquals( FileTypes.ARTIFACTS, fileType.getId() );

        fileTypes.afterConfigurationChange( null, "fileType", null );

        repoContent = new ManagedDefaultRepositoryContent(repository, artifactMappingProviders, fileTypes, fileLockManager);
        return newRepo;

    }

    @Test
    public void testDeleteArtifactWithType() throws IOException, org.apache.archiva.repository.ContentNotFoundException, org.apache.archiva.repository.ContentAccessException
    {
        Path deleteRepo = setupRepoCopy( "delete-repository", "delete-repository-2" );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0-source.jar" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar.md5" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar.sha1" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.pom" ) ) );

        ArtifactReference ref = new ArtifactReference( );
        ref.setGroupId( "org.apache.maven" );
        ref.setArtifactId( "samplejar" );
        ref.setVersion( "1.0" );
        ref.setType( "jar" );

        repoContent.deleteArtifact( ref );

        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0-source.jar" ) ) );
        assertFalse( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar.md5" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar.sha1" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.pom" ) ) );


    }


    @Test
    public void testDeleteArtifactWithClassifier() throws IOException, org.apache.archiva.repository.ContentNotFoundException, org.apache.archiva.repository.ContentAccessException
    {
        Path deleteRepo = setupRepoCopy( "default-repository", "default-repository-2" );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0-source.jar" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0-source.jar.sha1" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar.md5" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar.sha1" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.pom" ) ) );

        ArtifactReference ref = new ArtifactReference( );
        ref.setGroupId( "org.apache.maven" );
        ref.setArtifactId( "samplejar" );
        ref.setVersion( "1.0" );
        ref.setClassifier( "source" );
        ref.setType( "jar" );

        repoContent.deleteArtifact( ref );

        assertFalse( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0-source.jar" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0-source.jar.sha1" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar.md5" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar.sha1" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.pom" ) ) );

    }

    @Test
    public void testDeleteArtifactWithoutType() throws IOException, org.apache.archiva.repository.ContentNotFoundException, org.apache.archiva.repository.ContentAccessException
    {
        Path deleteRepo = setupRepoCopy( "default-repository", "default-repository-2" );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0-source.jar" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0-source.jar.sha1" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar.md5" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar.sha1" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.pom" ) ) );

        ArtifactReference ref = new ArtifactReference( );
        ref.setGroupId( "org.apache.maven" );
        ref.setArtifactId( "samplejar" );
        ref.setVersion( "1.0" );

        repoContent.deleteArtifact( ref );

        assertFalse( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar" ) ) );

    }


    @Test
    public void testDeleteVersion() throws IOException, org.apache.archiva.repository.ContentNotFoundException, org.apache.archiva.repository.ContentAccessException
    {
        Path deleteRepo = setupRepoCopy( "delete-repository", "delete-repository-2" );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0-source.jar" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar.md5" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar.sha1" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.pom" ) ) );

        VersionedReference ref = new VersionedReference( ).groupId( "org.apache.maven" ).artifactId( "samplejar" ).version( "1.0" );

        repoContent.deleteVersion( ref );

        assertFalse( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0" ) ) );

    }

    @Test
    public void testDeleteProject() throws IOException, org.apache.archiva.repository.ContentNotFoundException, org.apache.archiva.repository.ContentAccessException
    {
        Path deleteRepo = setupRepoCopy( "delete-repository", "delete-repository-2" );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0-source.jar" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar.md5" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.jar.sha1" ) ) );
        assertTrue( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0/samplejar-1.0.pom" ) ) );

        ProjectReference ref = new ProjectReference( ).groupId( "org.apache.maven" ).artifactId( "samplejar" );

        repoContent.deleteProject( ref );

        assertFalse( Files.exists( deleteRepo.resolve( "org/apache/maven/samplejar/1.0" ) ) );

    }
}
