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

    private void assertRecord( Artifact artifact, Document document, String expectedArtifactId, String expectedType,
                               String expectedMd5, String expectedSha1 )
    {
        assertEquals( "Check document filename", repository.pathOf( artifact ), document.get( "filename" ) );
        assertEquals( "Check document groupId", "org.apache.maven.repository.record", document.get( "groupId" ) );
        assertEquals( "Check document artifactId", expectedArtifactId, document.get( "artifactId" ) );
        assertEquals( "Check document version", "1.0", document.get( "version" ) );
        assertEquals( "Check document type", expectedType, document.get( "type" ) );
        assertEquals( "Check document repository", "test", document.get( "repo" ) );
        assertEquals( "Check document timestamp", getLastModified( artifact.getFile() ),
                      document.get( "lastModified" ) );
        assertEquals( "Check document md5", expectedMd5, document.get( "md5" ) );
        assertEquals( "Check document sha1", expectedSha1, document.get( "sha1" ) );
        assertEquals( "Check document file size", artifact.getFile().length(),
                      NumberTools.stringToLong( document.get( "fileSize" ) ) );
        assertNull( "Check document classifier", document.get( "classifier" ) );
    }

    private void assertPomRecord( Artifact artifact, Document document )
    {
        assertRecord( artifact, document, "test-pom", "pom", "32dbef7ff11eb933bd8b7e7bcab85406",
                      "c3b374e394607e1e705e71c227f62641e8621ebe" );
        assertNull( "Check document classes", document.get( "classes" ) );
        assertNull( "Check document files", document.get( "files" ) );
        assertNull( "Check document pluginPrefix", document.get( "pluginPrefix" ) );
        assertEquals( "Check document year", "2005", document.get( "inceptionYear" ) );
        assertEquals( "Check document project name", "Maven Repository Manager Test POM",
                      document.get( "projectName" ) );
        assertEquals( "Check document project description", "Description", document.get( "projectDesc" ) );
        assertEquals( "Check document packaging", "pom", document.get( "packaging" ) );
    }

    private void assertJarRecord( Artifact artifact, Document document )
    {
        assertRecord( artifact, document, "test-jar", "jar", "3a0adc365f849366cd8b633cad155cb7",
                      "c66f18bf192cb613fc2febb4da541a34133eedc2" );
        assertEquals( "Check document classes", "A\nb.B\nb.c.C", document.get( "classes" ) );
        assertEquals( "Check document files", "META-INF/MANIFEST.MF\nA.class\nb/B.class\nb/c/C.class",
                      document.get( "files" ) );
        assertNull( "Check document inceptionYear", document.get( "inceptionYear" ) );
        assertNull( "Check document projectName", document.get( "projectName" ) );
        assertNull( "Check document projectDesc", document.get( "projectDesc" ) );
        assertNull( "Check document pluginPrefix", document.get( "pluginPrefix" ) );
        assertNull( "Check document packaging", document.get( "packaging" ) );
    }

    private void assertPluginRecord( Artifact artifact, Document document )
    {
        assertRecord( artifact, document, "test-plugin", "maven-plugin", "06f6fe25e46c4d4fb5be4f56a9bab0ee",
                      "382c1ebfb5d0c7d6061c2f8569fb53f8fc00fec2" );
        assertEquals( "Check document classes", "org.apache.maven.repository.record.MyMojo",
                      document.get( "classes" ) );
        assertEquals( "Check document files", "META-INF/MANIFEST.MF\n" + "META-INF/maven/plugin.xml\n" +
            "org/apache/maven/repository/record/MyMojo.class\n" +
            "META-INF/maven/org.apache.maven.repository.record/test-plugin/pom.xml\n" +
            "META-INF/maven/org.apache.maven.repository.record/test-plugin/pom.properties", document.get( "files" ) );
        assertEquals( "Check document pluginPrefix", "test", document.get( "pluginPrefix" ) );
        assertEquals( "Check document packaging", "maven-plugin", document.get( "packaging" ) );
        assertNull( "Check document inceptionYear", document.get( "inceptionYear" ) );
        assertEquals( "Check document project name", "Maven Mojo Archetype", document.get( "projectName" ) );
        assertNull( "Check document projectDesc", document.get( "projectDesc" ) );
    }

    private String getLastModified( File file )
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyyMMddHHmmss", Locale.US );
        dateFormat.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
        return dateFormat.format( new Date( file.lastModified() ) );
    }
}
