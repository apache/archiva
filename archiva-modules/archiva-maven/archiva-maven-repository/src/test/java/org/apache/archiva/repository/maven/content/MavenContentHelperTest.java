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

import org.apache.archiva.common.filelock.DefaultFileLockManager;
import org.apache.archiva.metadata.maven.MavenMetadataReader;
import org.apache.archiva.repository.content.ItemSelector;
import org.apache.archiva.repository.content.base.ArchivaItemSelector;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
class MavenContentHelperTest
{

    private static FilesystemStorage storage;
    private static Path tempDir;

    @BeforeAll
    static void setUp() throws IOException
    {
        tempDir = Files.createTempDirectory( "archivamaventest" );
        storage = new FilesystemStorage( tempDir, new DefaultFileLockManager() );
    }

    @AfterAll
    static void tearDown() {
        try
        {
            Files.deleteIfExists( tempDir );
        }
        catch ( IOException e )
        {
            System.err.println( "Could not delete " + tempDir );
        }
    }

    @Test
    void getNamespaceFromNamespacePath( )
    {
        StorageAsset asset = storage.getAsset( "org/apache/archiva" );
        String ns = MavenContentHelper.getNamespaceFromNamespacePath( asset );
        assertNotNull( ns );
        assertEquals( "org.apache.archiva", ns );

        asset = storage.getAsset( "" );
        ns = MavenContentHelper.getNamespaceFromNamespacePath( asset );
        assertNotNull( ns );
        assertEquals( "", ns );
    }

    @Test
    void getArtifactVersion( ) throws IOException, URISyntaxException
    {
        MavenContentHelper mavenContentHelper = new MavenContentHelper( );
        MavenMetadataReader reader = new MavenMetadataReader( );
        mavenContentHelper.setMetadataReader( reader );
        Path testRepoPath = Paths.get( Thread.currentThread( ).getContextClassLoader( ).getResource( "repositories/metadata-repository" ).toURI() );
        FilesystemStorage storage = new FilesystemStorage( testRepoPath, new DefaultFileLockManager( ) );
        assertArtifactVersion( mavenContentHelper, "1.0-alpha-11-SNAPSHOT", storage.getAsset( "org/apache/archiva/metadata/tests/snap_shots_1/1.0-alpha-11-SNAPSHOT" )
        , "1.0-alpha-11-SNAPSHOT", "1.0-alpha-11-SNAPSHOT");

        assertArtifactVersion( mavenContentHelper, "1.0-alpha-11-20070316.175232-11", storage.getAsset( "org/apache/archiva/metadata/tests/snap_shots_a/1.0-alpha-11-SNAPSHOT" )
            , "", "1.0-alpha-11-SNAPSHOT");

        assertArtifactVersion( mavenContentHelper, "2.2-20070316.153953-10", storage.getAsset( "org/apache/archiva/metadata/tests/snap_shots_b/2.2-SNAPSHOT" )
            , "", "2.2-SNAPSHOT");

    }

    private void assertArtifactVersion(MavenContentHelper mavenContentHelper, String expectedVersion, StorageAsset dir, String selectorArtifactVersion, String selectorVersion) {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withVersion( selectorVersion )
            .withArtifactVersion( selectorArtifactVersion )
            .build( );
        assertEquals( expectedVersion, mavenContentHelper.getArtifactVersion( dir, selector ) );
    }

    @Test
    void getLatestArtifactSnapshotVersion( ) throws URISyntaxException, IOException
    {
        MavenContentHelper mavenContentHelper = new MavenContentHelper( );
        MavenMetadataReader reader = new MavenMetadataReader( );
        mavenContentHelper.setMetadataReader( reader );
        Path testRepoPath = Paths.get( Thread.currentThread( ).getContextClassLoader( ).getResource( "repositories/default-repository" ).toURI() );
        FilesystemStorage storage = new FilesystemStorage( testRepoPath, new DefaultFileLockManager( ) );
        // Directory without metadata file
        assertEquals( "2.1-20090808.085535-2", mavenContentHelper.getLatestArtifactSnapshotVersion( storage.getAsset( "org/apache/archiva/sample-parent/2.1-SNAPSHOT" ), "2.1-SNAPSHOT" ) );
        // Directory with metadata file
        assertEquals( "1.3-20070802.113139-29", mavenContentHelper.getLatestArtifactSnapshotVersion( storage.getAsset( "org/apache/axis2/axis2/1.3-SNAPSHOT" ), "1.3-SNAPSHOT" ) );
    }

    @Test
    void getArtifactFileName( )
    {
        assertFileName( "test-1.0.jar", "test", "", "1.0", "jar" );
        assertFileName( "test-1.1-client.jar", "test", "client", "1.1", "jar" );
        assertFileName( "te445st-2.1-sources.jar", "te445st", "sources", "2.1", "jar" );
        assertFileName( "abcde-8888.994894.48484-10.jar", "abcde", "", "8888.994894.48484-10", "jar" );
        assertFileName( "testarchive-5.0.war", "testarchive", "", "5.0", "war" );
    }

    private void assertFileName(String expectedFilename, String artifactId, String classifier, String version, String extension) {
        assertEquals( expectedFilename, MavenContentHelper.getArtifactFileName( artifactId, version, classifier, extension ) );
    }

    @Test
    void getClassifier( )
    {
        assertClassifier( "sources", "","java-source" );
        assertClassifier( "tests", "", "test-jar" );
        assertClassifier( "client", "","ejb-client" );
        assertClassifier( "javadoc", "","javadoc" );
        assertClassifier( "", "","test" );
        assertClassifier( "test1", "test1","java-source" );
        assertClassifier( "test2", "test2", "test-jar" );
        assertClassifier( "test3", "test3","ejb-client" );
        assertClassifier( "test4", "test4","javadoc" );
        assertClassifier( "test5", "test5","test" );
    }
    private void assertClassifier(String expectedClassifier, String classifier, String type) {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withClassifier( classifier )
            .withType( type ).build();
        assertEquals( expectedClassifier, MavenContentHelper.getClassifier( selector ) );
    }

    @Test
    void getClassifierFromType( )
    {
        assertClassifier( "sources", "java-source" );
        assertClassifier( "tests", "test-jar" );
        assertClassifier( "client", "ejb-client" );
        assertClassifier( "javadoc", "javadoc" );
        assertClassifier( "", "test" );
    }

    private void assertClassifier(String expectedClassifier, String type) {
        assertEquals( expectedClassifier, MavenContentHelper.getClassifierFromType( type ) );
    }

    @Test
    void getTypeFromClassifierAndExtension( )
    {
        assertType( "javadoc", "javadoc", "jar" );
        assertType( "war", "", "war" );
        assertType( "ear", "", "ear" );
        assertType( "rar", "", "rar" );
        assertType( "java-source", "sources", "jar" );
        assertType( "ejb-client", "client", "jar" );
        assertType( "pom", "", "pom" );
        assertType( "test-jar", "tests", "jar" );

    }

    private void assertType(String expectedType, String classifier, String extension) {
        assertEquals( expectedType, MavenContentHelper.getTypeFromClassifierAndExtension( classifier, extension ) );
    }



    @Test
    void getArtifactExtension( )
    {
        assertExtension( "test", "", "test" );
        assertExtension( "jar", "javadoc", "" );
        assertExtension( "war", "war", "" );
        assertExtension( "ear", "ear", "" );
        assertExtension( "rar", "rar", "" );
        assertExtension( "jar", "", "" );
    }

    private void assertExtension( String expectedExtension, String type, String extension )
    {
        ItemSelector selector = ArchivaItemSelector.builder( )
            .withType( type ).withExtension( extension ).build();
        assertEquals( expectedExtension, MavenContentHelper.getArtifactExtension( selector ) );
    }
}