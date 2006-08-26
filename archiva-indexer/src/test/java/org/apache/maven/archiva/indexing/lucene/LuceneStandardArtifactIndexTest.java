package org.apache.maven.archiva.indexing.lucene;

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
import org.apache.maven.archiva.indexing.RepositoryArtifactIndex;
import org.apache.maven.archiva.indexing.RepositoryArtifactIndexFactory;
import org.apache.maven.archiva.indexing.RepositoryIndexException;
import org.apache.maven.archiva.indexing.record.RepositoryIndexRecord;
import org.apache.maven.archiva.indexing.record.RepositoryIndexRecordFactory;
import org.apache.maven.archiva.indexing.record.StandardIndexRecordFields;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
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
public class LuceneStandardArtifactIndexTest
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

        index = factory.createStandardIndex( indexLocation );
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
            assertJarRecord( artifact, document );
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
            assertJarRecord( artifact, document );
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
            assertJarRecord( artifact, document );
            assertEquals( "Check index size", 1, reader.numDocs() );
        }
        finally
        {
            reader.close();
        }
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
            Document document = reader.document( 0 );
            assertPomRecord( artifact, document );
            assertEquals( "Check index size", 1, reader.numDocs() );
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
            assertPluginRecord( artifact, document );
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

    private Artifact createArtifact( String artifactId )
    {
        return createArtifact( artifactId, "1.0", "jar" );
    }

    private Artifact createArtifact( String artifactId, String version, String type )
    {
        Artifact artifact =
            artifactFactory.createBuildArtifact( "org.apache.maven.archiva.record", artifactId, version, type );
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

    private void assertRecord( Artifact artifact, Document document, String expectedArtifactId, String expectedType,
                               String expectedMd5, String expectedSha1 )
    {
        assertEquals( "Check document filename", repository.pathOf( artifact ),
                      document.get( StandardIndexRecordFields.FILENAME ) );
        assertEquals( "Check document groupId", "org.apache.maven.archiva.record",
                      document.get( StandardIndexRecordFields.GROUPID ) );
        assertEquals( "Check document artifactId", expectedArtifactId,
                      document.get( StandardIndexRecordFields.ARTIFACTID ) );
        assertEquals( "Check document version", "1.0", document.get( StandardIndexRecordFields.VERSION ) );
        assertEquals( "Check document type", expectedType, document.get( StandardIndexRecordFields.TYPE ) );
        assertEquals( "Check document repository", "test", document.get( StandardIndexRecordFields.REPOSITORY ) );
        assertEquals( "Check document timestamp", getLastModified( artifact.getFile() ),
                      document.get( StandardIndexRecordFields.LAST_MODIFIED ) );
        assertEquals( "Check document md5", expectedMd5, document.get( StandardIndexRecordFields.MD5 ) );
        assertEquals( "Check document sha1", expectedSha1, document.get( StandardIndexRecordFields.SHA1 ) );
        assertEquals( "Check document file size", artifact.getFile().length(),
                      NumberTools.stringToLong( document.get( StandardIndexRecordFields.FILE_SIZE ) ) );
        assertNull( "Check document classifier", document.get( StandardIndexRecordFields.CLASSIFIER ) );
    }

    private void assertPomRecord( Artifact artifact, Document document )
    {
        assertRecord( artifact, document, "test-pom", "pom", "103d11ac601a42ccf2a2ae54d308c362",
                      "4c4d237c5366df877c3a636d5b6241822d090355" );
        assertNull( "Check document classes", document.get( StandardIndexRecordFields.CLASSES ) );
        assertNull( "Check document files", document.get( StandardIndexRecordFields.FILES ) );
        assertNull( "Check document pluginPrefix", document.get( StandardIndexRecordFields.PLUGIN_PREFIX ) );
        assertEquals( "Check document year", "2005", document.get( StandardIndexRecordFields.INCEPTION_YEAR ) );
        assertEquals( "Check document project name", "Maven Repository Manager Test POM",
                      document.get( StandardIndexRecordFields.PROJECT_NAME ) );
        assertEquals( "Check document project description", "Description",
                      document.get( StandardIndexRecordFields.PROJECT_DESCRIPTION ) );
        assertEquals( "Check document packaging", "pom", document.get( StandardIndexRecordFields.PACKAGING ) );
    }

    private void assertJarRecord( Artifact artifact, Document document )
    {
        assertRecord( artifact, document, "test-jar", "jar", "3a0adc365f849366cd8b633cad155cb7",
                      "c66f18bf192cb613fc2febb4da541a34133eedc2" );
        assertEquals( "Check document classes", "A\nb.B\nb.c.C", document.get( StandardIndexRecordFields.CLASSES ) );
        assertEquals( "Check document files", "META-INF/MANIFEST.MF\nA.class\nb/B.class\nb/c/C.class",
                      document.get( StandardIndexRecordFields.FILES ) );
        assertNull( "Check document inceptionYear", document.get( StandardIndexRecordFields.INCEPTION_YEAR ) );
        assertNull( "Check document projectName", document.get( StandardIndexRecordFields.PROJECT_NAME ) );
        assertNull( "Check document projectDesc", document.get( StandardIndexRecordFields.PROJECT_DESCRIPTION ) );
        assertNull( "Check document pluginPrefix", document.get( StandardIndexRecordFields.PLUGIN_PREFIX ) );
        assertNull( "Check document packaging", document.get( StandardIndexRecordFields.PACKAGING ) );
    }

    private void assertPluginRecord( Artifact artifact, Document document )
    {
        assertRecord( artifact, document, "test-plugin", "maven-plugin", "3530896791670ebb45e17708e5d52c40",
                      "2cd2619d59a684e82e97471d2c2e004144c8f24e" );
        assertEquals( "Check document classes", "org.apache.maven.archiva.record.MyMojo",
                      document.get( StandardIndexRecordFields.CLASSES ) );
        assertEquals( "Check document files", "META-INF/MANIFEST.MF\n" +
            "META-INF/maven/org.apache.maven.archiva.record/test-plugin/pom.properties\n" +
            "META-INF/maven/org.apache.maven.archiva.record/test-plugin/pom.xml\n" + "META-INF/maven/plugin.xml\n" +
            "org/apache/maven/archiva/record/MyMojo.class", document.get( StandardIndexRecordFields.FILES ) );
        assertEquals( "Check document pluginPrefix", "test", document.get( StandardIndexRecordFields.PLUGIN_PREFIX ) );
        assertEquals( "Check document packaging", "maven-plugin", document.get( StandardIndexRecordFields.PACKAGING ) );
        assertNull( "Check document inceptionYear", document.get( StandardIndexRecordFields.INCEPTION_YEAR ) );
        assertEquals( "Check document project name", "Maven Mojo Archetype",
                      document.get( StandardIndexRecordFields.PROJECT_NAME ) );
        assertNull( "Check document projectDesc", document.get( StandardIndexRecordFields.PROJECT_DESCRIPTION ) );
    }

    private String getLastModified( File file )
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyyMMddHHmmss", Locale.US );
        dateFormat.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        return dateFormat.format( new Date( file.lastModified() ) );
    }
}
