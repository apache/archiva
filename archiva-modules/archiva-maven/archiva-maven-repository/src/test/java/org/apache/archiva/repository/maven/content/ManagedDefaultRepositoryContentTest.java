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
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RepositoryContent;
import org.apache.archiva.repository.content.Artifact;
import org.apache.archiva.repository.content.BaseArtifactTypes;
import org.apache.archiva.repository.content.ContentItem;
import org.apache.archiva.repository.content.ItemNotFoundException;
import org.apache.archiva.repository.content.ItemSelector;
import org.apache.archiva.repository.content.Namespace;
import org.apache.archiva.repository.content.Project;
import org.apache.archiva.repository.content.Version;
import org.apache.archiva.repository.content.base.ArchivaItemSelector;
import org.apache.archiva.repository.maven.MavenManagedRepository;
import org.apache.archiva.repository.maven.metadata.storage.ArtifactMappingProvider;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * ManagedDefaultRepositoryContentTest
 */
public class ManagedDefaultRepositoryContentTest
    extends AbstractManagedRepositoryContentTest
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


    private void assertVersions( String artifactId, String version, String[] expectedVersions )
        throws Exception
    {
        // Use the test metadata-repository, which is already setup for
        // These kind of version tests.
        Path repoDir = getRepositoryPath( "metadata-repository" );
        ((EditableManagedRepository)repoContent.getRepository()).setLocation( repoDir.toAbsolutePath().toUri() );

        // Request the versions.

        // Sort the list (for asserts later)
        final VersionComparator comparator = new VersionComparator( );

        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.archiva.metadata.tests" )
            .withProjectId( artifactId )
            .withVersion( version )
            .build( );
        List<String> versions = repoContent.getVersions( selector ).stream()
            .map(v -> v.getVersion()).sorted( comparator ).collect( Collectors.toList());
        assertArrayEquals( expectedVersions, versions.toArray( ) );


    }

    @Test
    public void getTestGetProjectWithIllegalArgs() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache" )
            .withVersion( "1.0" )
            .build();
        try
        {
            repoContent.getProject( selector );
            assertFalse( "Should throw IllegalArgumentException if no project id is given", true );
        } catch (IllegalArgumentException e) {
            // Everything fine
            assertTrue( e.getMessage( ).contains( "Project id must be set" ) );
        }
    }

    @Test
    public void getTestGetVersionWithIllegalArgs() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withVersion( "1.0" )
            .build();
        try
        {
            repoContent.getVersion( selector );
            assertFalse( "Should throw IllegalArgumentException if no project id is given", true );
        } catch (IllegalArgumentException e) {
            // Everything fine
            assertTrue( e.getMessage( ).contains( "Project id must be set" ) );
        }


        selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withProjectId( "shared" )
            .build();
        try
        {
            repoContent.getVersion( selector );
            assertFalse( "Should throw IllegalArgumentException if no version is given", true );
        } catch (IllegalArgumentException e) {
            // Everything fine
            assertTrue( e.getMessage( ).contains( "Version must be set" ) );
        }
    }

    @Test
    public void getTestGetArtifactWithIllegalArgs() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withVersion( "1.0" )
            .withArtifactId( "shared" )
            .withArtifactVersion("1.0")
            .build();
        try
        {
            repoContent.getArtifact( selector );
            assertFalse( "Should throw IllegalArgumentException if no project id is given", true );
        } catch (IllegalArgumentException e) {
            // Everything fine
            assertTrue( e.getMessage( ).contains( "Project id must be set" ) );
        }


        selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withProjectId( "shared" )
            .withArtifactId( "shared" )
            .withArtifactVersion("1.0")
            .build();
        try
        {
            repoContent.getArtifact( selector );
            assertFalse( "Should throw IllegalArgumentException if no version is given", true );
        } catch (IllegalArgumentException e) {
            // Everything fine
            assertTrue( e.getMessage( ).contains( "Version must be set" ) );
        }

        selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withProjectId( "shared" )
            .withVersion("1.0")
            .withArtifactVersion("1.0")
            .build();
        try
        {
            repoContent.getArtifact( selector );
            assertFalse( "Should throw IllegalArgumentException if no artifact id is given", true );
        } catch (IllegalArgumentException e) {
            // Everything fine
            assertTrue( e.getMessage( ).contains( "Artifact id must be set" ) );
        }


    }

    @Test
    public void testGetProjects() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" ).build();
        Namespace ns = repoContent.getNamespace( selector );
        assertNotNull( ns );
        List<? extends Project> projects = repoContent.getProjects( ns );
        assertEquals( 12, projects.size( ) );
        String[] expected = new String[]{
            "A", "B", "C", "archiva", "discovery", "maven-parent", "samplejar", "shared", "some-ejb", "test",
            "testing", "update"
        };
        Object[] actual = projects.stream( ).map( p -> p.getId( ) ).sorted( ).toArray( );
        assertArrayEquals( expected, actual);
    }

    @Test
    public void testGetProjectsWithSelector() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" ).build();
        List<? extends Project> projects = repoContent.getProjects( selector );
        assertEquals( 12, projects.size( ) );
        String[] expected = new String[]{
            "A", "B", "C", "archiva", "discovery", "maven-parent", "samplejar", "shared", "some-ejb", "test",
            "testing", "update"
        };
        Object[] actual = projects.stream( ).map( p -> p.getId( ) ).sorted( ).toArray( );
        assertArrayEquals( expected, actual);
    }

    @Test
    public void testGetVersionsWithIllegalSelector() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" ).build();
        try
        {
            List<? extends Version> versions = repoContent.getVersions( selector );
            assertFalse( "IllegalArgumentException expected, when project id not set", true );
        } catch (IllegalArgumentException e) {
            assertEquals( "Project id not set, while retrieving versions.", e.getMessage( ) );
        }
    }

    @Test
    public void testGetVersionsWithSelector() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withProjectId( "samplejar" ).build();
        List<? extends Version> versions = repoContent.getVersions( selector );
        assertNotNull( versions );
        assertEquals( 2, versions.size( ) );
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

    @Override
    protected RepositoryContent getContent( )
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

    @Test
    public void testGetArtifactStreamWithVersionSelector() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "javax.sql" )
            .withProjectId( "jdbc" )
            .withVersion( "2.0" ).build();
        try(Stream<? extends Artifact> stream = repoContent.newArtifactStream( selector ))
        {
            assertNotNull( stream );
            List<? extends Artifact> results = stream.collect( Collectors.toList( ) );
            checkArtifactListWithVersionSelector1( results );
        }
    }

    @Test
    public void testGetArtifactListWithVersionSelector() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "javax.sql" )
            .withProjectId( "jdbc" )
            .withVersion( "2.0" ).build();
        List<? extends Artifact> results = repoContent.getArtifacts( selector );
        checkArtifactListWithVersionSelector1( results );
    }

    private void checkArtifactListWithVersionSelector1( List<? extends Artifact> results )
    {
        assertNotNull( results );
        assertEquals( 2, results.size( ) );
        Artifact mainArtifact = results.stream( ).filter( a -> a.getFileName( ).equals( "jdbc-2.0.jar" ) ).findFirst( ).get( );
        assertNotNull( mainArtifact );
        assertEquals( BaseArtifactTypes.MAIN, mainArtifact.getArtifactType( ) );
        Artifact metaArtifact = results.stream( ).filter( a -> a.getFileName( ).equals( "maven-metadata-repository.xml" ) ).findFirst( ).get( );
        assertNotNull( metaArtifact );
        assertEquals( MavenTypes.REPOSITORY_METADATA, metaArtifact.getArtifactType( ) );
    }

    @Test
    public void testGetArtifactStreamWithVersionSelector2() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.axis2" )
            .withProjectId( "axis2" )
            .withVersion( "1.3-SNAPSHOT" ).build();
        try(Stream<? extends Artifact> stream = repoContent.newArtifactStream( selector ))
        {
            assertNotNull( stream );
            List<? extends Artifact> results = stream.collect( Collectors.toList( ) );
            checkArtifactListWithVersionSelector2( results );
        }
    }

    @Test
    public void testGetArtifactListWithVersionSelector2() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.axis2" )
            .withProjectId( "axis2" )
            .withVersion( "1.3-SNAPSHOT" ).build();
        List<? extends Artifact> results = repoContent.getArtifacts( selector );
        checkArtifactListWithVersionSelector2( results );
    }

    private void checkArtifactListWithVersionSelector2( List<? extends Artifact> results )
    {
        assertNotNull( results );
        assertEquals( 39, results.size( ) );

        Artifact artifact = results.stream( ).filter( a -> a.getFileName( ).equals( "axis2-1.3-20070725.210059-1.pom" ) )
            .findFirst( ).get( );

        assertNotNull( artifact );
        assertEquals( "pom", artifact.getExtension( ) );
        assertEquals( BaseArtifactTypes.MAIN, artifact.getArtifactType( ) );
        assertEquals( "1.3-SNAPSHOT", artifact.getVersion( ).getVersion( ) );
        assertEquals( "1.3-20070725.210059-1", artifact.getArtifactVersion( ) );
        assertEquals( ".pom", artifact.getRemainder( ) );
        assertEquals( "axis2", artifact.getId( ) );
        assertEquals( "axis2", artifact.getVersion( ).getProject( ).getId( ) );
        assertEquals( "org.apache.axis2", artifact.getVersion( ).getProject( ).getNamespace( ).getNamespace( ) );
        assertEquals( "", artifact.getClassifier( ) );
        assertEquals( "pom", artifact.getType( ) );

        artifact = null;
        artifact = results.stream( ).filter( a -> a.getFileName( ).equals( "axis2-1.3-20070725.210059-1.pom.md5" ) )
            .findFirst( ).get( );

        assertNotNull( artifact );
        assertEquals( "md5", artifact.getExtension( ) );
        assertEquals( BaseArtifactTypes.RELATED, artifact.getArtifactType( ) );
        assertEquals( "1.3-SNAPSHOT", artifact.getVersion( ).getVersion( ) );
        assertEquals( "1.3-20070725.210059-1", artifact.getArtifactVersion( ) );
        assertEquals( ".pom.md5", artifact.getRemainder( ) );
        assertEquals( "axis2", artifact.getId( ) );
        assertEquals( "axis2", artifact.getVersion( ).getProject( ).getId( ) );
        assertEquals( "org.apache.axis2", artifact.getVersion( ).getProject( ).getNamespace( ).getNamespace( ) );
        assertEquals( "", artifact.getClassifier( ) );
        assertEquals( "md5", artifact.getType( ) );


        artifact = null;
        artifact = results.stream( ).filter( a -> a.getFileName( ).equals( "maven-metadata.xml" ) )
            .findFirst( ).get( );
        assertNotNull( artifact );
        assertEquals( BaseArtifactTypes.METADATA, artifact.getArtifactType( ) );
        assertEquals( "1.3-SNAPSHOT", artifact.getVersion( ).getVersion( ) );
        assertEquals( "xml", artifact.getExtension( ) );
    }

    @Test
    public void testGetArtifactListWithArtifactSelector1() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.axis2" )
            .withProjectId( "axis2" )
            .withVersion( "1.3-SNAPSHOT" )
            .withArtifactVersion( "1.3-20070731.113304-21" )
            .withExtension( "pom" )
            .build( );
        List<? extends Artifact> results = repoContent.getArtifacts( selector );
        checkArtifactListWithArtifactSelector1( results );
    }

    @Test
    public void testGetArtifactStreamWithArtifactSelector1() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.axis2" )
            .withProjectId( "axis2" )
            .withVersion( "1.3-SNAPSHOT" )
            .withArtifactVersion( "1.3-20070731.113304-21" )
            .withExtension( "pom" )
            .build( );
        try(Stream<? extends Artifact> results = repoContent.newArtifactStream( selector ))
        {
            checkArtifactListWithArtifactSelector1( results.collect( Collectors.toList()) );
        }
    }

    private void checkArtifactListWithArtifactSelector1( List<? extends Artifact> results )
    {
        assertNotNull( results );
        assertEquals( 1, results.size( ) );
        Artifact artifact = results.get( 0 );
        assertEquals( "pom", artifact.getExtension( ) );
        assertEquals( BaseArtifactTypes.MAIN, artifact.getArtifactType( ) );
    }

    @Test
    public void testGetArtifactListWithArtifactSelector2() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.axis2" )
            .withProjectId( "axis2" )
            .withVersion( "1.3-SNAPSHOT" )
            .withArtifactVersion( "1.3-20070731.113304-21" )
            .withExtension( "pom" )
            .includeRelatedArtifacts()
            .build( );
        List<? extends Artifact> results = repoContent.getArtifacts( selector );

        checkArtifactListWithArtifactSelector2( results );

    }

    @Test
    public void testGetArtifactStreamWithArtifactSelector2() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.axis2" )
            .withProjectId( "axis2" )
            .withVersion( "1.3-SNAPSHOT" )
            .withArtifactVersion( "1.3-20070731.113304-21" )
            .withExtension( "pom" )
            .includeRelatedArtifacts()
            .build( );
        try(Stream<? extends Artifact> results = repoContent.newArtifactStream( selector ))
        {
            checkArtifactListWithArtifactSelector2( results.collect( Collectors.toList()) );
        }
    }

    private void checkArtifactListWithArtifactSelector2( List<? extends Artifact> results )
    {
        assertNotNull( results );
        assertEquals( 3, results.size( ) );
        Artifact artifact = results.stream( ).filter( a -> a.getFileName( ).equalsIgnoreCase( "axis2-1.3-20070731.113304-21.pom" ) )
            .findFirst( ).get( );
        assertNotNull( artifact );
        assertEquals( "pom", artifact.getExtension( ) );
        assertEquals( BaseArtifactTypes.MAIN, artifact.getArtifactType( ) );

        artifact = results.stream( ).filter( a -> a.getFileName( ).equalsIgnoreCase( "axis2-1.3-20070731.113304-21.pom.sha1" ) )
            .findFirst( ).get( );
        assertNotNull( artifact );
        assertEquals( "sha1", artifact.getExtension( ) );
        assertEquals( BaseArtifactTypes.RELATED, artifact.getArtifactType( ) );
    }


    @Test
    public void testArtifactListWithProjectSelector() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven.shared" )
            .withProjectId( "maven-downloader" )
            .build( );
        List<? extends Artifact> results = repoContent.getArtifacts( selector );
        checkArtifactListWithProjectSelector( results );

    }

    @Test
    public void testArtifactStreamWithProjectSelector() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven.shared" )
            .withProjectId( "maven-downloader" )
            .build( );
        Stream<? extends Artifact> results = repoContent.newArtifactStream( selector );
        checkArtifactListWithProjectSelector( results.collect( Collectors.toList()) );

    }

    private void checkArtifactListWithProjectSelector( List<? extends Artifact> results )
    {
        assertNotNull( results );
        assertEquals( 27, results.size( ) );

        Artifact artifact = results.stream( ).filter( a -> a.getFileName( ).equalsIgnoreCase( "maven-metadata.xml" ) )
            .findFirst( ).get( );
        assertNotNull( artifact );
        assertEquals( "xml", artifact.getExtension( ) );
        assertEquals( BaseArtifactTypes.METADATA, artifact.getArtifactType( ) );

        artifact = results.stream( ).filter( a -> a.getFileName( ).equalsIgnoreCase( "maven-downloader-1.0-sources.jar" ) )
            .findFirst( ).get( );
        assertNotNull( artifact );
        assertEquals( BaseArtifactTypes.MAIN, artifact.getArtifactType( ) );
        assertEquals( "sources", artifact.getClassifier( ) );
        assertEquals( "java-source", artifact.getType( ) );

        artifact = results.stream( ).filter( a -> a.getFileName( ).equalsIgnoreCase( "maven-downloader-1.0-sources.jar.sha1" ) )
            .findFirst( ).get( );
        assertNotNull( artifact );
        assertEquals( BaseArtifactTypes.RELATED, artifact.getArtifactType( ) );
        assertEquals( "sources", artifact.getClassifier( ) );
        assertEquals( "sha1", artifact.getType( ) );
        assertEquals( ".jar.sha1", artifact.getRemainder( ) );
    }

    @Test
    public void testArtifactListWithNamespaceSelector() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.multilevel" )
            .build( );
        List<? extends Artifact> results = repoContent.getArtifacts( selector );
        assertNotNull( results );
        assertEquals( 3, results.size( ) );
        assertTrue( results.get( 0 ).getFileName( ).startsWith( "testproj1" ) );
    }

    @Test
    public void testArtifactListWithNamespaceSelectorRecursive() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.multilevel" )
            .recurse()
            .build( );
        List<? extends Artifact> results = repoContent.getArtifacts( selector );
        checkArtifactListWithNamespaceSelectorRecursive( results );
    }

    @Test
    public void testArtifactStreamWithNamespaceSelectorRecursive() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.multilevel" )
            .recurse()
            .build( );
        Stream<? extends Artifact> results = repoContent.newArtifactStream( selector );
        checkArtifactListWithNamespaceSelectorRecursive( results.collect( Collectors.toList()) );
    }

    private void checkArtifactListWithNamespaceSelectorRecursive( List<? extends Artifact> results )
    {
        assertNotNull( results );
        assertEquals( 6, results.size( ) );

        Artifact artifact = results.stream( ).filter( a -> a.getFileName( ).equalsIgnoreCase( "testproj2-1.0.pom" ) )
            .findFirst( ).get( );
        assertNotNull( artifact );
        assertEquals( 6, artifact.getAsset( ).getParent( ).getPath( ).split( "/" ).length );

        artifact = results.stream( ).filter( a -> a.getFileName( ).equalsIgnoreCase( "testproj1-1.0.pom" ) )
            .findFirst( ).get( );
        assertNotNull( artifact );
        assertEquals( 5, artifact.getAsset( ).getParent( ).getPath( ).split( "/" ).length );
    }


    @Test
    public void testArtifactListWithArtifactSelector1() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withProjectId( "test" )
            .withVersion( "1.0-SNAPSHOT" )
            .withArtifactId( "test" )
            .withArtifactVersion( "1.0-20050611.112233-1" )
            .build( );

        List<? extends Artifact> results = repoContent.getArtifacts( selector );

        assertNotNull( results );
        assertEquals( 1, results.size( ) );

        Artifact artifact = results.stream( ).filter( a -> a.getFileName( ).equalsIgnoreCase( "test-1.0-20050611.112233-1.jar" ) )
            .findFirst().get();
        assertNotNull( artifact );
        assertEquals( "", artifact.getClassifier( ) );
    }

    @Test
    public void testArtifactListWithArtifactSelector2() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withProjectId( "test" )
            .withVersion( "1.0-SNAPSHOT" )
            .withClassifier( "*" )
            .withArtifactId( "test" )
            .withArtifactVersion( "1.0-20050611.112233-1" )
            .build( );

        List<? extends Artifact> results = repoContent.getArtifacts( selector );

        assertNotNull( results );
        assertEquals( 2, results.size( ) );

        Artifact artifact = results.stream( ).filter( a -> a.getFileName( ).equalsIgnoreCase( "test-1.0-20050611.112233-1-javadoc.jar" ) )
            .findFirst().get();
        assertNotNull( artifact );
        assertEquals( "javadoc", artifact.getClassifier( ) );
        assertEquals( "javadoc", artifact.getType( ) );
    }

    @Test
    public void testArtifactListWithArtifactSelector3() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withProjectId( "test" )
            .withVersion( "1.0-SNAPSHOT" )
            .withClassifier( "*" )
            .withArtifactVersion( "1.0-20050611.112233-1" )
            .build( );

        List<? extends Artifact> results = repoContent.getArtifacts( selector );

        assertNotNull( results );
        assertEquals( 3, results.size( ) );

        Artifact artifact = results.stream( ).filter( a -> a.getFileName( ).equalsIgnoreCase( "test-1.0-20050611.112233-1-javadoc.jar" ) )
            .findFirst().get();
        assertNotNull( artifact );
        assertEquals( "javadoc", artifact.getClassifier( ) );
        assertEquals( "javadoc", artifact.getType( ) );

        artifact = results.stream( ).filter( a -> a.getFileName( ).equalsIgnoreCase( "wrong-artifactId-1.0-20050611.112233-1.jar" ) )
            .findFirst().get();
        assertNotNull( artifact );
        assertEquals( "", artifact.getClassifier( ) );
        assertEquals( "wrong-artifactId", artifact.getId( ) );
    }

    @Test
    public void testArtifactListWithArtifactSelector4() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withProjectId( "test" )
            .withVersion( "1.0-SNAPSHOT" )
            .withClassifier( "" )
            .build( );

        List<? extends Artifact> results = repoContent.getArtifacts( selector );

        assertNotNull( results );
        assertEquals( 5, results.size( ) );

        Artifact artifact = results.stream( ).filter( a -> a.getFileName( ).equalsIgnoreCase( "test-1.0-20050611.112233-1-javadoc.jar" ) )
            .findFirst().get();
        assertNotNull( artifact );
        assertEquals( "javadoc", artifact.getClassifier( ) );
        assertEquals( "javadoc", artifact.getType( ) );

        artifact = results.stream( ).filter( a -> a.getFileName( ).equalsIgnoreCase( "wrong-artifactId-1.0-20050611.112233-1.jar" ) )
            .findFirst().get();
        assertNotNull( artifact );
        assertEquals( "", artifact.getClassifier( ) );
        assertEquals( "wrong-artifactId", artifact.getId( ) );

        artifact = results.stream( ).filter( a -> a.getFileName( ).equalsIgnoreCase( "wrong-artifactId-1.0-20050611.1122x-1.jar" ) )
            .findFirst().get();
        assertNotNull( artifact );
        assertEquals( "", artifact.getClassifier( ) );
        assertEquals( "wrong-artifactId", artifact.getId( ) );
        assertEquals( "", artifact.getArtifactVersion( ) );

        artifact = results.stream( ).filter( a -> a.getFileName( ).equalsIgnoreCase( "test-1.0-20050611.1122x-1.jar" ) )
            .findFirst().get();
        assertNotNull( artifact );
        assertEquals( "", artifact.getClassifier( ) );
        assertEquals( "test", artifact.getId( ) );
        assertEquals( "1.0-20050611.1122x-1", artifact.getArtifactVersion( ) );

    }

    @Test
    public void testArtifactListWithArtifactSelectorWithClassifier() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withProjectId( "test" )
            .withVersion( "1.0-SNAPSHOT" )
            .withArtifactId( "test" )
            .withClassifier( "javadoc" )
            .withArtifactVersion( "1.0-20050611.112233-1" )
            .build( );

        List<? extends Artifact> results = repoContent.getArtifacts( selector );

        assertNotNull( results );
        assertEquals( 1, results.size( ) );

        Artifact artifact = results.stream( ).filter( a -> a.getFileName( ).equalsIgnoreCase( "test-1.0-20050611.112233-1-javadoc.jar" ) )
            .findFirst().get();
        assertNotNull( artifact );
        assertEquals( "javadoc", artifact.getClassifier( ) );
        assertEquals( "javadoc", artifact.getType( ) );
    }

    @Test
    public void testArtifactListWithArtifactSelectorWrongArtifact() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withProjectId( "test" )
            .withVersion( "1.0-SNAPSHOT" )
            .withArtifactId( "wrong-artifactId" )
            .withArtifactVersion( "1.0-20050611.112233-1" )
            .build( );

        List<? extends Artifact> results = repoContent.getArtifacts( selector );

        assertNotNull( results );
        assertEquals( 1, results.size( ) );

        Artifact artifact = results.stream( ).filter( a -> a.getFileName( ).equalsIgnoreCase( "wrong-artifactId-1.0-20050611.112233-1.jar" ) )
            .findFirst().get();
        assertNotNull( artifact );
    }

    @Test
    public void testArtifactListWithArtifactSelectorVersionPattern() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withProjectId( "test" )
            .withVersion( "1.0-SNAPSHOT" )
            .withArtifactVersion( "1.0-*" )
            .build( );

        List<? extends Artifact> results = repoContent.getArtifacts( selector );

        assertNotNull( results );
        assertEquals( 5, results.size( ) );

        Artifact artifact = results.stream( ).filter( a -> a.getFileName( ).equalsIgnoreCase( "wrong-artifactId-1.0-20050611.112233-1.jar" ) )
            .findFirst().get();
        assertNotNull( artifact );
    }

    @Test
    public void testGetArtifactFromContentItem() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" ).build();
        Namespace ns = repoContent.getNamespace( selector );
        List<? extends Artifact> artifacts = repoContent.getArtifacts( ns );
        assertNotNull( artifacts );
        assertEquals( 39, artifacts.size( ) );
        List<? extends Artifact> artifacts2 = repoContent.getArtifacts( (ContentItem)ns );
        assertArrayEquals( artifacts.toArray(), artifacts2.toArray() );

        selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven.shared" )
            .withProjectId( "maven-downloader" )
            .build();
        Project project = repoContent.getProject( selector );
        artifacts = repoContent.getArtifacts( project );
        assertNotNull( artifacts );
        assertEquals( 27, artifacts.size( ) );
        artifacts2 = repoContent.getArtifacts( (ContentItem)project );
        assertArrayEquals( artifacts.toArray(), artifacts2.toArray() );

        selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven.shared" )
            .withProjectId( "maven-downloader" )
            .withVersion( "1.1" )
            .build( );
        Version version = repoContent.getVersion( selector );
        artifacts = repoContent.getArtifacts( version );
        assertNotNull( artifacts );
        assertEquals( 12, artifacts.size( ) );
        artifacts2 = repoContent.getArtifacts( (ContentItem)version );
        assertArrayEquals( artifacts.toArray(), artifacts2.toArray() );

    }

    @Test
    public void testGetRelatedArtifactsFromArtifact() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven.shared" )
            .withProjectId( "maven-downloader" )
            .withVersion( "1.1" )
            .withExtension( "jar" )
            .withArtifactId( "maven-downloader" ).build( );

        Artifact artifact = repoContent.getArtifact( selector );
        assertNotNull( artifact );
        List<? extends Artifact> artifacts = repoContent.getArtifacts( artifact );
        assertNotNull( artifacts );
        assertEquals( 2, artifacts.size( ) );

    }

    @Test
    public void testToItemFromPath() throws LayoutException
    {
        String path = "/org/apache/maven/shared";
        ContentItem item = repoContent.toItem( path );
        assertNotNull( item );
        assertTrue( item instanceof Namespace );

        path = "/org/apache/maven/shared/maven-downloader";
        item = repoContent.toItem( path );
        assertNotNull( item );
        assertTrue( item instanceof Project );

        path = "/org/apache/maven/shared/maven-downloader/1.1";
        item = repoContent.toItem( path );
        assertNotNull( item );
        assertTrue( item instanceof Version );

        path = "/org/apache/maven/shared/maven-downloader/1.1/maven-downloader-1.1.jar";
        item = repoContent.toItem( path );
        assertNotNull( item );
        assertTrue( item instanceof Artifact );

    }

    @Test
    public void testToItemFromAssetPath() throws LayoutException
    {
        StorageAsset path = repoContent.getRepository().getAsset("/org/apache/maven/shared");
        ContentItem item = repoContent.toItem( path );
        assertNotNull( item );
        assertTrue( item instanceof Namespace );

        path = repoContent.getRepository( ).getAsset( "/org/apache/maven/shared/maven-downloader" );
        item = repoContent.toItem( path );
        assertNotNull( item );
        assertTrue( item instanceof Project );

        path = repoContent.getRepository( ).getAsset( "/org/apache/maven/shared/maven-downloader/1.1" );
        item = repoContent.toItem( path );
        assertNotNull( item );
        assertTrue( item instanceof Version );

        path = repoContent.getRepository( ).getAsset( "/org/apache/maven/shared/maven-downloader/1.1/maven-downloader-1.1.jar" );
        item = repoContent.toItem( path );
        assertNotNull( item );
        assertTrue( item instanceof Artifact );

    }

    @Test
    public void testHasContent() throws LayoutException
    {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven.shared" )
            .withProjectId( "maven-downloader" )
            .withVersion( "1.1" )
            .withArtifactId( "maven-downloader" )
            .withExtension( "jar" )
            .build();

        assertTrue( repoContent.hasContent( selector ) );

        selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven.shared" )
            .withProjectId( "maven-downloader" )
            .withVersion( "1.1" )
            .withArtifactId( "maven-downloader" )
            .withExtension( "zip" )
            .build();

        assertFalse( repoContent.hasContent( selector ) );

    }

    @Test
    public void testGetItemWithNamespaceSelector() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .build( );
        ContentItem item = repoContent.getItem( selector );
        assertNotNull( item );
        assertTrue( item instanceof Namespace );
    }

    @Test
    public void testGetItemWithProjectSelector() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withProjectId( "shared" )
            .build( );
        ContentItem item = repoContent.getItem( selector );
        assertNotNull( item );
        assertTrue( item instanceof Project );
    }

    @Test
    public void testGetItemWithVersionSelector() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withProjectId( "samplejar" )
            .withVersion("2.0")
            .build( );
        ContentItem item = repoContent.getItem( selector );
        assertNotNull( item );
        assertTrue( item instanceof Version );
    }

    @Test
    public void testGetItemWithArtifactSelector() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withProjectId( "samplejar" )
            .withVersion("2.0")
            .withArtifactId( "samplejar" )
            .build( );
        ContentItem item = repoContent.getItem( selector );
        assertNotNull( item );
        assertTrue( item instanceof Artifact );
    }

    @Test
    public void testGetNamespaceFromPath() {
        StorageAsset path = repoContent.getRepository( ).getAsset( "/org/apache/axis2" );
        Namespace ns = repoContent.getNamespaceFromPath( path );
        assertNotNull( ns );
        assertEquals( "org.apache.axis2", ns.getNamespace( ) );

    }

    @Test
    public void testArtifactListWithArtifactSelectorAndRelated() {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withProjectId( "samplejar" )
            .withVersion( "1.0" )
            .withArtifactVersion( "1.0" )
            .withArtifactId( "samplejar" )
            .withExtension( "jar" )
            .includeRelatedArtifacts()
            .build( );

        List<? extends Artifact> results = repoContent.getArtifacts( selector );

        assertNotNull( results );
        assertEquals( 3, results.size( ) );

        Artifact artifact = results.stream( ).filter( a -> a.getFileName( ).equalsIgnoreCase( "samplejar-1.0.jar" ) )
            .findFirst().get();
        assertNotNull( artifact );
        assertEquals( BaseArtifactTypes.MAIN, artifact.getArtifactType( ) );

        artifact = results.stream( ).filter( a -> a.getFileName( ).equalsIgnoreCase( "samplejar-1.0.jar.md5" ) )
            .findFirst().get();
        assertNotNull( artifact );
        assertEquals( BaseArtifactTypes.RELATED, artifact.getArtifactType( ) );
        assertEquals( "md5", artifact.getExtension( ) );

        artifact = results.stream( ).filter( a -> a.getFileName( ).equalsIgnoreCase( "samplejar-1.0.jar.sha1" ) )
            .findFirst().get();
        assertNotNull( artifact );
        assertEquals( BaseArtifactTypes.RELATED, artifact.getArtifactType( ) );
        assertEquals( "sha1", artifact.getExtension( ) );

    }

    private Path copyRepository(String repoName) throws IOException, URISyntaxException
    {
        Path tempDir = Files.createTempDirectory( "archiva-repocontent" );
        Path repoSource = Paths.get( Thread.currentThread( ).getContextClassLoader( ).getResource( "repositories/" + repoName ).toURI( ) );
        assertTrue( Files.exists( repoSource ) );
        FileUtils.copyDirectory( repoSource.toFile( ), tempDir.toFile() );
        return tempDir;
    }

    private ManagedRepository createManagedRepoWithContent(String sourceRepoName) throws IOException, URISyntaxException
    {
        Path repoDir = copyRepository( sourceRepoName );
        MavenManagedRepository repo = createRepository( sourceRepoName, sourceRepoName, repoDir );
        ManagedDefaultRepositoryContent deleteRepoContent = new ManagedDefaultRepositoryContent( repo, artifactMappingProviders, fileTypes, fileLockManager );
        deleteRepoContent.setMavenContentHelper( contentHelper );
        return repo;
    }

    @Test
    public void deleteNamespaceItem() throws IOException, URISyntaxException, ItemNotFoundException
    {
        ManagedRepository repo = createManagedRepoWithContent( "delete-repository" );
        ManagedRepositoryContent myRepoContent = repo.getContent( );
        Path repoRoot = repo.getAsset( "" ).getFilePath( );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/maven" )) );
        ArchivaItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" ).build();
        ContentItem item = myRepoContent.getItem( selector );
        assertTrue( item instanceof Namespace );
        myRepoContent.deleteItem( item );
        assertFalse( Files.exists(repoRoot.resolve( "org/apache/maven" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache" )) );

        // Sub namespaces are deleted too
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/sub/samplejar" )) );
        selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.test" ).build();
        item = myRepoContent.getItem( selector );
        assertTrue( item instanceof Namespace );
        myRepoContent.deleteItem( item );
        assertFalse( Files.exists(repoRoot.resolve( "org/apache/test/samplejar" )) );
        assertFalse( Files.exists(repoRoot.resolve( "org/apache/test/sub/samplejar" )) );
    }

    @Test
    public void deleteProjectItem() throws IOException, URISyntaxException, ItemNotFoundException
    {
        ManagedRepository repo = createManagedRepoWithContent( "delete-repository" );
        ManagedRepositoryContent myRepoContent = repo.getContent( );
        Path repoRoot = repo.getAsset( "" ).getFilePath( );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/maven/A" )) );
        ArchivaItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withProjectId( "A" ).build();
        ContentItem item = myRepoContent.getItem( selector );
        assertTrue( item instanceof Project );
        myRepoContent.deleteItem( item );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/maven" )) );
        assertTrue( Files.exists( repoRoot.resolve( "org/apache/maven/samplejar/1.0" ) ) );
        assertTrue( Files.exists( repoRoot.resolve( "org/apache/maven/samplejar/2.0" ) ) );
        assertFalse( Files.exists(repoRoot.resolve( "org/apache/maven/A" )) );

        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/sub/samplejar" )) );
        selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.test" )
            .withProjectId( "samplejar" ).build();
        item = myRepoContent.getItem( selector );
        assertTrue( item instanceof Project );
        myRepoContent.deleteItem( item );
        assertFalse( Files.exists(repoRoot.resolve( "org/apache/test/samplejar" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/sub/samplejar" )) );
    }

    @Test
    public void deleteVersionItem() throws IOException, URISyntaxException, ItemNotFoundException
    {
        ManagedRepository repo = createManagedRepoWithContent( "delete-repository" );
        ManagedRepositoryContent myRepoContent = repo.getContent( );
        Path repoRoot = repo.getAsset( "" ).getFilePath( );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/maven/A/1.0" )) );
        ArchivaItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withProjectId( "A" )
            .withVersion( "1.0" ).build();
        ContentItem item = myRepoContent.getItem( selector );
        assertTrue( item instanceof Version );
        myRepoContent.deleteItem( item );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/maven/A" )) );
        assertTrue( Files.exists( repoRoot.resolve( "org/apache/maven/samplejar/1.0" ) ) );
        assertTrue( Files.exists( repoRoot.resolve( "org/apache/maven/samplejar/2.0" ) ) );
        assertFalse( Files.exists(repoRoot.resolve( "org/apache/maven/A/1.0" )) );

        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/sub/samplejar" )) );
        selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.test" )
            .withProjectId( "samplejar" )
            .withVersion( "2.0" ).build();
        item = myRepoContent.getItem( selector );
        assertTrue( item instanceof Version );
        myRepoContent.deleteItem( item );
        assertFalse( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/2.0" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/sub/samplejar/1.0" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/sub/samplejar/2.0" )) );
    }

    @Test
    public void deleteArtifactItem() throws IOException, URISyntaxException, ItemNotFoundException
    {
        ManagedRepository repo = createManagedRepoWithContent( "delete-repository" );
        ManagedRepositoryContent myRepoContent = repo.getContent( );
        Path repoRoot = repo.getAsset( "" ).getFilePath( );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/maven/A/1.0/A-1.0.pom" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/maven/A/1.0/A-1.0.war" )) );
        ArchivaItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withProjectId( "A" )
            .withVersion( "1.0" )
            .withArtifactId( "A" )
            .withArtifactVersion( "1.0" )
            .withExtension( "pom" )
            .build();
        ContentItem item = myRepoContent.getItem( selector );
        assertTrue( item instanceof Artifact );
        myRepoContent.deleteItem( item );
        assertTrue( Files.exists( repoRoot.resolve( "org/apache/maven/samplejar/1.0" ) ) );
        assertTrue( Files.exists( repoRoot.resolve( "org/apache/maven/samplejar/2.0" ) ) );
        assertFalse( Files.exists(repoRoot.resolve( "org/apache/maven/A/1.0/A-1.0.pom" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/maven/A/1.0/A-1.0.war" )) );


        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0.jar" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0.jar.md5" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0.jar.sha1" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0.pom" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0-source.jar" )) );
        selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.test" )
            .withProjectId( "samplejar" )
            .withVersion( "1.0" )
            .withArtifactId( "samplejar" )
            .withArtifactVersion( "1.0" )
            .withExtension( "jar" )
            .build();
        item = myRepoContent.getItem( selector );
        assertTrue( item instanceof Artifact );
        myRepoContent.deleteItem( item );
        assertFalse( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0.jar" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0.jar.md5" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0.jar.sha1" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0.pom" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0-source.jar" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/2.0" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/sub/samplejar/1.0" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/sub/samplejar/2.0" )) );

        selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.test" )
            .withProjectId( "samplejar" )
            .withVersion( "1.0" )
            .withArtifactId( "samplejar" )
            .withArtifactVersion( "1.0" )
            .withClassifier( "source" )
            .withExtension( "jar" )
            .build();
        item = myRepoContent.getItem( selector );
        assertTrue( item instanceof Artifact );
        myRepoContent.deleteItem( item );
        assertFalse( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0.jar" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0.jar.md5" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0.jar.sha1" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0.pom" )) );
        assertFalse( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0-source.jar" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0-source.jar.sha1" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/2.0" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/sub/samplejar/1.0" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/sub/samplejar/2.0" )) );

        selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.test" )
            .withProjectId( "samplejar" )
            .withVersion( "1.0" )
            .withArtifactId( "samplejar" )
            .withArtifactVersion( "1.0" )
            .withExtension( "jar.md5" )
            .build();
        item = myRepoContent.getItem( selector );
        assertTrue( item instanceof Artifact );
        myRepoContent.deleteItem( item );
        assertFalse( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0.jar" )) );
        assertFalse( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0.jar.md5" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0.jar.sha1" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0.pom" )) );
        assertFalse( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0-source.jar" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/1.0/samplejar-1.0-source.jar.sha1" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/samplejar/2.0" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/sub/samplejar/1.0" )) );
        assertTrue( Files.exists(repoRoot.resolve( "org/apache/test/sub/samplejar/2.0" )) );


    }

    @Test
    public void deleteItemNotFound() throws IOException, URISyntaxException, ItemNotFoundException
    {
        ManagedRepository repo = createManagedRepoWithContent( "delete-repository" );
        ManagedRepositoryContent myRepoContent = repo.getContent( );
        Path repoRoot = repo.getAsset( "" ).getFilePath( );

        ArchivaItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.test2" )
            .build( );

        ContentItem item = myRepoContent.getItem( selector );
        assertTrue( item instanceof Namespace );
        try
        {
            myRepoContent.deleteItem( item );
            assertTrue( "ItemNotFoundException expected for non existing namespace", false );
        } catch ( ItemNotFoundException e) {
        }

        selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.test" )
            .withProjectId( "samplejar2" )
            .build( );
        item = myRepoContent.getItem( selector );
        assertTrue( item instanceof Project );
        try
        {
            myRepoContent.deleteItem( item );
            assertTrue( "ItemNotFoundException expected for non existing project", false );
        } catch ( ItemNotFoundException e) {
        }

        selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.test" )
            .withProjectId( "samplejar" )
            .withVersion("1.1")
            .build( );
        item = myRepoContent.getItem( selector );
        assertTrue( item instanceof Version );
        try
        {
            myRepoContent.deleteItem( item );
            assertTrue( "ItemNotFoundException expected for non existing version", false );
        } catch ( ItemNotFoundException e) {
        }

        selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.test" )
            .withProjectId( "samplejar" )
            .withVersion("1.0")
            .withArtifactId( "samplejar" )
            .withArtifactVersion( "1.0" )
            .withExtension( "jax" )
            .build( );
        item = myRepoContent.getItem( selector );
        assertTrue( item instanceof Artifact );
        try
        {
            myRepoContent.deleteItem( item );
            assertTrue( "ItemNotFoundException expected for non existing artifact", false );
        } catch ( ItemNotFoundException e) {
        }

    }


    @Test
    public void testAddArtifact() throws IOException, URISyntaxException
    {
        ManagedRepository repo = createManagedRepoWithContent( "delete-repository" );
        ManagedRepositoryContent myRepoContent = repo.getContent( );
        Path repoRoot = repo.getAsset( "" ).getFilePath( );

        Path tmpFile = Files.createTempFile( "archiva-mvn-repotest", "jar" );
        try( OutputStream outputStream = Files.newOutputStream( tmpFile ))
        {
            for ( int i = 0; i < 255; i++ )
            {
                outputStream.write( "test.test.test\n".getBytes( Charset.forName( "UTF-8" ) ) );
            }
        }

        Path file = repoRoot.resolve( "org/apache/maven/samplejar/2.0/samplejar-2.0.jar" );
        FileTime lmt = Files.getLastModifiedTime( file );
        ArchivaItemSelector selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withProjectId( "samplejar" )
            .withVersion( "2.0" )
            .withArtifactId( "samplejar" )
            .withArtifactVersion( "2.0" )
            .withExtension( "jar" )
            .build( );
        Artifact artifact = myRepoContent.getArtifact( selector );
        myRepoContent.addArtifact( tmpFile, artifact );
        FileTime lmtAfter = Files.getLastModifiedTime( file );
        assertNotEquals( lmtAfter, lmt );
        Reader ln = Files.newBufferedReader( file, Charset.forName( "UTF-8" ) );
        char[] content = new char[50];
        ln.read( content );
        assertTrue( new String( content ).startsWith( "test.test.test" ) );

        tmpFile = Files.createTempFile( "archiva-mvn-repotest", "jar" );
        try( OutputStream outputStream = Files.newOutputStream( tmpFile ))
        {
            for ( int i = 0; i < 255; i++ )
            {
                outputStream.write( "test.test.test\n".getBytes( Charset.forName( "UTF-8" ) ) );
            }
        }
        file = repoRoot.resolve( "org/apache/maven/samplejar/2.0/samplejar-2.0.test" );
        assertFalse( Files.exists( file ) );
        assertTrue( Files.exists( tmpFile ) );
        selector = ArchivaItemSelector.builder( )
            .withNamespace( "org.apache.maven" )
            .withProjectId( "samplejar" )
            .withVersion( "2.0" )
            .withArtifactId( "samplejar" )
            .withArtifactVersion( "2.0" )
            .withExtension( "test" )
            .build( );
        artifact = myRepoContent.getArtifact( selector );
        myRepoContent.addArtifact( tmpFile, artifact );
        ln = Files.newBufferedReader( file, Charset.forName( "UTF-8" ) );
        ln.read( content );
        assertTrue( new String( content ).startsWith( "test.test.test" ) );
    }

}
