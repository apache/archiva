package org.apache.maven.archiva.indexer;

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
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.indexer.bytecode.BytecodeRecord;
import org.apache.maven.archiva.indexer.filecontent.FileContentRecord;
import org.apache.maven.archiva.indexer.hashcodes.HashcodesRecord;
import org.apache.maven.archiva.indexer.search.BytecodeIndexPopulator;
import org.apache.maven.archiva.indexer.search.FileContentIndexPopulator;
import org.apache.maven.archiva.indexer.search.HashcodesIndexPopulator;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.events.RepositoryListener;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.codehaus.plexus.spring.PlexusToSpringUtils;

public class RepositoryContentIndexEventListenerTest
    extends PlexusInSpringTestCase
{
    private static final String TEST_DEFAULT_REPOSITORY_NAME = "Test Default Repository";

    private static final String TEST_DEFAULT_REPO_ID = "test-repo";

    private RepositoryListener listener;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        listener = (RepositoryListener) lookup( RepositoryListener.class.getName(), "indexer" );
    }

    public void testWiring()
    {
        List<RepositoryListener> listeners =
            PlexusToSpringUtils.lookupList( PlexusToSpringUtils.buildSpringId( RepositoryListener.class ),
                                            getApplicationContext() );

        assertEquals( 1, listeners.size() );
        assertEquals( listener, listeners.get( 0 ) );
    }

    public ArchivaArtifact createArtifact( String artifactId, String version )
    {
        ArchivaArtifact artifact =
            new ArchivaArtifact( "org.apache.maven.archiva.test", artifactId, version, "", "jar" );
        artifact.getModel().setRepositoryId( "testable_repo" );
        return artifact;
    }

    public void testDeleteArtifact()
        throws Exception
    {
        RepositoryContentIndexFactory indexFactory =
            (RepositoryContentIndexFactory) lookup( RepositoryContentIndexFactory.class.getName(), "lucene" );

        File repoDir = new File( getBasedir(), "src/test/managed-repository" );

        assertTrue( "Default Test Repository should exist.", repoDir.exists() && repoDir.isDirectory() );

        ManagedRepositoryConfiguration repository =
            createRepository( TEST_DEFAULT_REPO_ID, TEST_DEFAULT_REPOSITORY_NAME, repoDir );

        File indexLocation = new File( "target/index-events-" + getName() + "/" );

        MockConfiguration config = (MockConfiguration) lookup( ArchivaConfiguration.class.getName(), "mock" );

        ManagedRepositoryConfiguration repoConfig = new ManagedRepositoryConfiguration();
        repoConfig.setId( TEST_DEFAULT_REPO_ID );
        repoConfig.setName( TEST_DEFAULT_REPOSITORY_NAME );
        repoConfig.setLocation( repoDir.getAbsolutePath() );
        repoConfig.setIndexDir( indexLocation.getAbsolutePath() );
        repoConfig.setScanned( true );

        if ( indexLocation.exists() )
        {
            FileUtils.deleteDirectory( indexLocation );
        }

        config.getConfiguration().addManagedRepository( repoConfig );

        // Create the (empty) indexes.
        RepositoryContentIndex indexHashcode = indexFactory.createHashcodeIndex( repository );
        RepositoryContentIndex indexBytecode = indexFactory.createBytecodeIndex( repository );
        RepositoryContentIndex indexContents = indexFactory.createFileContentIndex( repository );

        // Now populate them.
        Map<String, HashcodesRecord> hashcodesMap = new HashcodesIndexPopulator().populate( new File( getBasedir() ) );
        indexHashcode.indexRecords( hashcodesMap.values() );
        assertEquals( "Hashcode Key Count", hashcodesMap.size(), indexHashcode.getAllRecordKeys().size() );
        assertRecordCount( indexHashcode, hashcodesMap.size() );

        Map<String, BytecodeRecord> bytecodeMap = new BytecodeIndexPopulator().populate( new File( getBasedir() ) );
        indexBytecode.indexRecords( bytecodeMap.values() );
        assertEquals( "Bytecode Key Count", bytecodeMap.size(), indexBytecode.getAllRecordKeys().size() );
        assertRecordCount( indexBytecode, bytecodeMap.size() );

        Map<String, FileContentRecord> contentMap = new FileContentIndexPopulator().populate( new File( getBasedir() ) );
        indexContents.indexRecords( contentMap.values() );
        assertEquals( "File Content Key Count", contentMap.size(), indexContents.getAllRecordKeys().size() );
        assertRecordCount( indexContents, contentMap.size() );

        ManagedRepositoryContent repositoryContent =
            (ManagedRepositoryContent) lookup( ManagedRepositoryContent.class.getName(), "default" );
        repositoryContent.setRepository( repository );

        ArchivaArtifact artifact =
            new ArchivaArtifact( "org.apache.maven.archiva", "archiva-common", "1.0", "", "jar" );
        listener.deleteArtifact( repositoryContent, artifact );

        artifact =
            new ArchivaArtifact( "org.apache.maven.archiva.record", "test-pom", "1.0", "", "pom" );
        listener.deleteArtifact( repositoryContent, artifact );

        assertRecordCount( indexHashcode, hashcodesMap.size() - 1 );
        assertRecordCount( indexBytecode, bytecodeMap.size() - 1 );
        assertRecordCount( indexContents, contentMap.size() - 1 );
    }

    protected ManagedRepositoryConfiguration createRepository( String id, String name, File location )
    {
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setLocation( location.getAbsolutePath() );
        return repo;
    }

    private void assertRecordCount( RepositoryContentIndex index, int expectedCount )
        throws Exception
    {
        Query query = new MatchAllDocsQuery();
        Searcher searcher = (Searcher) index.getSearchable();
        Hits hits = searcher.search( query );
        assertEquals( "Expected Record Count for " + index.getId(), expectedCount, hits.length() );
    }
}
