package org.apache.maven.repository.indexing.lucene;

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

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.repository.indexing.RepositoryArtifactIndex;
import org.apache.maven.repository.indexing.RepositoryArtifactIndexFactory;
import org.apache.maven.repository.indexing.RepositoryIndexException;
import org.apache.maven.repository.indexing.record.RepositoryIndexRecord;
import org.apache.maven.repository.indexing.record.RepositoryIndexRecordFactory;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Test the Lucene implementation of the artifact index.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class LuceneRepositoryArtifactIndexTest
    extends PlexusTestCase
{
    private RepositoryArtifactIndex index;

    private ArtifactRepository repository;

    private ArtifactFactory artifactFactory;

    private File indexLocation;

    private RepositoryIndexRecordFactory recordFactory;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        recordFactory = (RepositoryIndexRecordFactory) lookup( RepositoryIndexRecordFactory.ROLE, "standard" );

        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );

        ArtifactRepositoryFactory repositoryFactory =
            (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );

        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );

        File file = getTestFile( "src/test/managed-repository" );
        repository =
            repositoryFactory.createArtifactRepository( "test", file.toURI().toURL().toString(), layout, null, null );

        RepositoryArtifactIndexFactory factory =
            (RepositoryArtifactIndexFactory) lookup( RepositoryArtifactIndexFactory.ROLE, "lucene" );

        indexLocation = getTestFile( "target/test-index" );

        FileUtils.deleteDirectory( indexLocation );

        // TODO: test minimal one in same way
        index = factory.createStandardIndex( indexLocation, repository );
    }

    public void testIndexExists()
        throws IOException, RepositoryIndexException
    {
        assertFalse( "check index doesn't exist", index.exists() );

        // create empty directory
        indexLocation.mkdirs();
        assertFalse( "check index doesn't exist even if directory does", index.exists() );

        // create index, with no records
        createEmptyIndex();
        assertTrue( "check index is considered to exist", index.exists() );

        // Test non-directory
        FileUtils.deleteDirectory( indexLocation );
        indexLocation.createNewFile();
        try
        {
            index.exists();
            fail( "Index operation should fail as the location is not valid" );
        }
        catch ( RepositoryIndexException e )
        {
            // great
        }
        finally
        {
            indexLocation.delete();
        }
    }

    public void testAddRecordNoIndex()
        throws IOException, RepositoryIndexException
    {
        Artifact artifact = createArtifact( "test-jar" );

        RepositoryIndexRecord record = recordFactory.createRecord( artifact );
        index.indexRecords( Collections.singletonList( record ) );

        IndexReader reader = IndexReader.open( indexLocation );
        try
        {
            Document document = reader.document( 0 );
            assertEquals( "Check document", "test-jar", document.getField( "artifactId" ).stringValue() );
            assertEquals( "Check index size", 1, reader.numDocs() );
        }
        finally
        {
            reader.close();
        }
    }

    public void testAddRecordExistingEmptyIndex()
        throws IOException, RepositoryIndexException
    {
        createEmptyIndex();

        Artifact artifact = createArtifact( "test-jar" );

        RepositoryIndexRecord record = recordFactory.createRecord( artifact );
        index.indexRecords( Collections.singletonList( record ) );

        IndexReader reader = IndexReader.open( indexLocation );
        try
        {
            Document document = reader.document( 0 );
            assertEquals( "Check document", "test-jar", document.getField( "artifactId" ).stringValue() );
            assertEquals( "Check index size", 1, reader.numDocs() );
        }
        finally
        {
            reader.close();
        }
    }

/*
    public void testUpdateRecordWithPomMetadata()
        throws IOException, RepositoryIndexException
    {
        createEmptyIndex();

        Artifact artifact = createArtifact( "test-plugin" );

        RepositoryIndexRecord record = recordFactory.createRecord( artifact );
        index.indexRecords( Collections.singletonList( record ) );

        // TODO: index again, with the POM metadata! Make sure a value in the first one is not present, and that is tested for

        IndexReader reader = IndexReader.open( indexLocation );
        try
        {
            Document document = reader.document( 0 );
            assertEquals( "Check document", "test-plugin", document.getField( "artifactId" ).stringValue() );
            assertEquals( "Check document", "jar", document.getField( "type" ).stringValue() );
            assertEquals( "Check document", "maven-plugin", document.getField( "packaging" ).stringValue() );
            assertEquals( "Check index size", 1, reader.numDocs() );
        }
        finally
        {
            reader.close();
        }
    }
*/

    public void testAddPomRecord()
        throws IOException, RepositoryIndexException
    {
        createEmptyIndex();

        Artifact artifact = createArtifact( "test-pom", "1.0", "pom" );

        RepositoryIndexRecord record = recordFactory.createRecord( artifact );
        index.indexRecords( Collections.singletonList( record ) );

        IndexReader reader = IndexReader.open( indexLocation );
        try
        {
            Document document = reader.document( 0 );
            assertEquals( "Check document", "test-pom", document.getField( "artifactId" ).stringValue() );
            assertEquals( "Check document", "pom", document.getField( "type" ).stringValue() );
//            assertEquals( "Check document", "pom", document.getField( "packaging" ).stringValue() ); // TODO!
            assertEquals( "Check index size", 1, reader.numDocs() );
        }
        finally
        {
            reader.close();
        }
    }

/*
    public void testUpdateRecordWithRepoMetadata()
        throws IOException, RepositoryIndexException
    {
        createEmptyIndex();

        Artifact artifact = createArtifact( "test-plugin" );

        RepositoryIndexRecord record = recordFactory.createRecord( artifact );
        index.indexRecords( Collections.singletonList( record ) );

        // TODO: index again, with the repo metadata!

        IndexReader reader = IndexReader.open( indexLocation );
        try
        {
            Document document = reader.document( 0 );
            assertEquals( "Check document", "test-plugin", document.getField( "artifactId" ).stringValue() );
            assertEquals( "Check document", "maven-plugin", document.getField( "packaging" ).stringValue() );
            assertEquals( "Check document", "plugin", document.getField( "pluginPrefix" ).stringValue() );
            assertEquals( "Check index size", 1, reader.numDocs() );
        }
        finally
        {
            reader.close();
        }
    }

    public void testUpdateRecordWithArtifactData()
        throws IOException, RepositoryIndexException
    {
        createEmptyIndex();

        // TODO: index with the repo/POM metadata!

        Artifact artifact = createArtifact( "test-plugin" );

        RepositoryIndexRecord record = recordFactory.createRecord( artifact );
        index.indexRecords( Collections.singletonList( record ) );

        IndexReader reader = IndexReader.open( indexLocation );
        try
        {
            Document document = reader.document( 0 );
            assertEquals( "Check document", "test-plugin", document.getField( "artifactId" ).stringValue() );
            assertEquals( "Check document", "maven-plugin", document.getField( "packaging" ).stringValue() );
            assertEquals( "Check document", "plugin", document.getField( "pluginPrefix" ).stringValue() );
            assertEquals( "Check index size", 1, reader.numDocs() );
        }
        finally
        {
            reader.close();
        }
    }
*/

    private Artifact createArtifact( String artifactId )
    {
        return createArtifact( artifactId, "1.0", "jar" );
    }

    private Artifact createArtifact( String artifactId, String version, String type )
    {
        Artifact artifact =
            artifactFactory.createBuildArtifact( "org.apache.maven.repository.record", artifactId, version, type );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        artifact.setRepository( repository );
        return artifact;
    }

    private void createEmptyIndex()
        throws IOException
    {
        createIndex( Collections.EMPTY_LIST );
    }

    private void createIndex( List docments )
        throws IOException
    {
        IndexWriter writer = new IndexWriter( indexLocation, new StandardAnalyzer(), true );
        for ( Iterator i = docments.iterator(); i.hasNext(); )
        {
            Document document = (Document) i.next();
            writer.addDocument( document );
        }
        writer.optimize();
        writer.close();
    }
}
