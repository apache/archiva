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
import org.apache.lucene.document.NumberTools;
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
import org.apache.maven.repository.indexing.record.MinimalIndexRecordFields;
import org.apache.maven.repository.indexing.record.RepositoryIndexRecord;
import org.apache.maven.repository.indexing.record.RepositoryIndexRecordFactory;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Test the Lucene implementation of the artifact index.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class LuceneMinimalArtifactIndexTest
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

        recordFactory = (RepositoryIndexRecordFactory) lookup( RepositoryIndexRecordFactory.ROLE, "minimal" );

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

        index = factory.createMinimalIndex( indexLocation, repository );
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
            assertEquals( "Check document", repository.pathOf( artifact ),
                          document.get( MinimalIndexRecordFields.FILENAME ) );
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
            assertRecord( document, artifact, "3a0adc365f849366cd8b633cad155cb7", "A\nb.B\nb.c.C" );
            assertEquals( "Check index size", 1, reader.numDocs() );
        }
        finally
        {
            reader.close();
        }
    }

    public void testAddRecordInIndex()
        throws IOException, RepositoryIndexException
    {
        createEmptyIndex();

        Artifact artifact = createArtifact( "test-jar" );

        RepositoryIndexRecord record = recordFactory.createRecord( artifact );
        index.indexRecords( Collections.singletonList( record ) );

        // Do it again
        record = recordFactory.createRecord( artifact );
        index.indexRecords( Collections.singletonList( record ) );

        IndexReader reader = IndexReader.open( indexLocation );
        try
        {
            Document document = reader.document( 0 );
            assertRecord( document, artifact, "3a0adc365f849366cd8b633cad155cb7", "A\nb.B\nb.c.C" );
            assertEquals( "Check index size", 1, reader.numDocs() );
        }
        finally
        {
            reader.close();
        }
    }

    public void testDeleteRecordInIndex()
        throws IOException, RepositoryIndexException
    {
        createEmptyIndex();

        Artifact artifact = createArtifact( "test-jar" );

        RepositoryIndexRecord record = recordFactory.createRecord( artifact );
        index.indexRecords( Collections.singletonList( record ) );

        index.deleteRecords( Collections.singletonList( record ) );

        IndexReader reader = IndexReader.open( indexLocation );
        try
        {
            assertEquals( "No documents", 0, reader.numDocs() );
        }
        finally
        {
            reader.close();
        }
    }

    public void testDeleteRecordNotInIndex()
        throws IOException, RepositoryIndexException
    {
        createEmptyIndex();

        Artifact artifact = createArtifact( "test-jar" );

        RepositoryIndexRecord record = recordFactory.createRecord( artifact );

        index.deleteRecords( Collections.singletonList( record ) );

        IndexReader reader = IndexReader.open( indexLocation );
        try
        {
            assertEquals( "No documents", 0, reader.numDocs() );
        }
        finally
        {
            reader.close();
        }
    }

    public void testDeleteRecordNoIndex()
        throws IOException, RepositoryIndexException
    {
        Artifact artifact = createArtifact( "test-jar" );

        RepositoryIndexRecord record = recordFactory.createRecord( artifact );
        index.deleteRecords( Collections.singleton( record ) );

        assertFalse( index.exists() );
    }

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
            assertEquals( "No documents", 0, reader.numDocs() );
        }
        finally
        {
            reader.close();
        }
    }

    public void testAddPlugin()
        throws IOException, RepositoryIndexException, XmlPullParserException
    {
        createEmptyIndex();

        Artifact artifact = createArtifact( "test-plugin" );

        RepositoryIndexRecord record = recordFactory.createRecord( artifact );

        index.indexRecords( Collections.singletonList( record ) );

        IndexReader reader = IndexReader.open( indexLocation );
        try
        {
            Document document = reader.document( 0 );
            assertRecord( document, artifact, "06f6fe25e46c4d4fb5be4f56a9bab0ee",
                          "org.apache.maven.repository.record.MyMojo" );
            assertEquals( "Check index size", 1, reader.numDocs() );
        }
        finally
        {
            reader.close();
        }
    }

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

    private void assertRecord( Document document, Artifact artifact, String expectedChecksum, String expectedClasses )
    {
        assertEquals( "Check document filename", repository.pathOf( artifact ),
                      document.get( MinimalIndexRecordFields.FILENAME ) );
        assertEquals( "Check document timestamp", getLastModified( artifact.getFile() ),
                      document.get( MinimalIndexRecordFields.LAST_MODIFIED ) );
        assertEquals( "Check document checksum", expectedChecksum, document.get( MinimalIndexRecordFields.MD5 ) );
        assertEquals( "Check document size", artifact.getFile().length(),
                      NumberTools.stringToLong( document.get( MinimalIndexRecordFields.FILE_SIZE ) ) );
        assertEquals( "Check document classes", expectedClasses, document.get( MinimalIndexRecordFields.CLASSES ) );
    }

    private String getLastModified( File file )
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyyMMddHHmmss", Locale.US );
        dateFormat.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        return dateFormat.format( new Date( file.lastModified() ) );
    }
}
