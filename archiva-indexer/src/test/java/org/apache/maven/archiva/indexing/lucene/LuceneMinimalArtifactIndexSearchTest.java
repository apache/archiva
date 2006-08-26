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

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.maven.archiva.indexing.RepositoryArtifactIndex;
import org.apache.maven.archiva.indexing.RepositoryArtifactIndexFactory;
import org.apache.maven.archiva.indexing.RepositoryIndexSearchException;
import org.apache.maven.archiva.indexing.record.MinimalIndexRecordFields;
import org.apache.maven.archiva.indexing.record.RepositoryIndexRecordFactory;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.versioning.VersionRange;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test the Lucene implementation of the artifact index search.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @todo would be nice to abstract some of the query away, but for now passing in a Lucene query directly is good enough
 */
public class LuceneMinimalArtifactIndexSearchTest
    extends PlexusTestCase
{
    private RepositoryArtifactIndex index;

    private ArtifactRepository repository;

    private ArtifactFactory artifactFactory;

    private File indexLocation;

    private RepositoryIndexRecordFactory recordFactory;

    private Map records = new HashMap();

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

        index = factory.createMinimalIndex( indexLocation );

        records.put( "test-jar", recordFactory.createRecord( createArtifact( "test-jar" ) ) );
        records.put( "test-jar-jdk14",
                     recordFactory.createRecord( createArtifact( "test-jar", "1.0", "jar", "jdk14" ) ) );
        records.put( "test-jar-and-pom",
                     recordFactory.createRecord( createArtifact( "test-jar-and-pom", "1.0-alpha-1", "jar" ) ) );
        records.put( "test-jar-and-pom-jdk14", recordFactory.createRecord(
            createArtifact( "test-jar-and-pom", "1.0-alpha-1", "jar", "jdk14" ) ) );
        records.put( "test-child-pom",
                     recordFactory.createRecord( createArtifact( "test-child-pom", "1.0-20060728.121314-1", "jar" ) ) );
        records.put( "test-archetype", recordFactory.createRecord( createArtifact( "test-archetype" ) ) );
        records.put( "test-plugin", recordFactory.createRecord( createArtifact( "test-plugin" ) ) );
        records.put( "test-pom", recordFactory.createRecord( createArtifact( "test-pom", "1.0", "pom" ) ) );
        records.put( "parent-pom", recordFactory.createRecord( createArtifact( "parent-pom", "1", "pom" ) ) );
        records.put( "test-dll", recordFactory.createRecord( createArtifact( "test-dll", "1.0.1.34", "dll" ) ) );

        index.indexRecords( records.values() );
    }

    public void testExactMatchMd5()
        throws RepositoryIndexSearchException
    {
        Query query = new TermQuery( new Term( MinimalIndexRecordFields.MD5, "3a0adc365f849366cd8b633cad155cb7" ) );
        List results = index.search( new LuceneQuery( query ) );

        assertTrue( "Check result", results.contains( records.get( "test-jar" ) ) );
        assertTrue( "Check result", results.contains( records.get( "test-jar-jdk14" ) ) );
        assertTrue( "Check result", results.contains( records.get( "test-jar-and-pom" ) ) );
        assertTrue( "Check result", results.contains( records.get( "test-jar-and-pom-jdk14" ) ) );
        assertTrue( "Check result", results.contains( records.get( "test-child-pom" ) ) );
        assertEquals( "Check results size", 5, results.size() );

        // test non-match fails
        query = new TermQuery( new Term( MinimalIndexRecordFields.MD5, "foo" ) );
        results = index.search( new LuceneQuery( query ) );

        assertTrue( "Check results size", results.isEmpty() );
    }

    public void testMatchFilename()
        throws RepositoryIndexSearchException
    {
        Query query = new TermQuery( new Term( MinimalIndexRecordFields.FILENAME, "maven" ) );
        List results = index.search( new LuceneQuery( query ) );

        assertFalse( "Check result", results.contains( records.get( "test-pom" ) ) );
        assertFalse( "Check result", results.contains( records.get( "parent-pom" ) ) );
        assertFalse( "Check result", results.contains( records.get( "test-dll" ) ) );
        assertEquals( "Check results size", 7, results.size() );

/* TODO: if this is a result we want, we need to change the analyzer. Currently, it is tokenizing it as plugin-1.0 and plugin/1.0 in the path
        query = new TermQuery( new Term( MinimalIndexRecordFields.FILENAME, "plugin" ) );
        results = index.search( new LuceneQuery( query ) );

        assertTrue( "Check result", results.contains( records.get( "test-plugin" ) ) );
        assertEquals( "Check results size", 1, results.size() );
*/
        query = new TermQuery( new Term( MinimalIndexRecordFields.FILENAME, "test" ) );
        results = index.search( new LuceneQuery( query ) );

        assertFalse( "Check result", results.contains( records.get( "parent-pom" ) ) );
        assertFalse( "Check result", results.contains( records.get( "test-pom" ) ) );
        assertFalse( "Check result", results.contains( records.get( "test-dll" ) ) );
        assertEquals( "Check results size", 7, results.size() );

        // test non-match fails
        query = new TermQuery( new Term( MinimalIndexRecordFields.FILENAME, "foo" ) );
        results = index.search( new LuceneQuery( query ) );

        assertTrue( "Check results size", results.isEmpty() );
    }

    public void testMatchClass()
        throws RepositoryIndexSearchException
    {
        // TODO: should be preserving case!
        Query query = new TermQuery( new Term( MinimalIndexRecordFields.CLASSES, "b.c.c" ) );
        List results = index.search( new LuceneQuery( query ) );

        assertTrue( "Check result", results.contains( records.get( "test-child-pom" ) ) );
        assertTrue( "Check result", results.contains( records.get( "test-jar" ) ) );
        assertTrue( "Check result", results.contains( records.get( "test-jar-jdk14" ) ) );
        assertTrue( "Check result", results.contains( records.get( "test-jar-and-pom" ) ) );
        assertTrue( "Check result", results.contains( records.get( "test-jar-and-pom-jdk14" ) ) );
        assertEquals( "Check results size", 5, results.size() );

/* TODO!: need to change the analyzer if we want partial classes (split on '.')
        query = new TermQuery( new Term( MinimalIndexRecordFields.CLASSES, "C" ) );
        results = index.search( new LuceneQuery( query ) );

        assertTrue( "Check result", results.contains( records.get( "test-jar" ) ) );
        assertTrue( "Check result", results.contains( records.get( "test-jar-jdk14" ) ) );
        assertTrue( "Check result", results.contains( records.get( "test-jar-and-pom" ) ) );
        assertTrue( "Check result", results.contains( records.get( "test-jar-and-pom-jdk14" ) ) );
        assertEquals( "Check results size", 4, results.size() );

        query = new TermQuery( new Term( MinimalIndexRecordFields.CLASSES, "MyMojo" ) );
        results = index.search( new LuceneQuery( query ) );

        assertTrue( "Check result", results.contains( records.get( "test-plugin" ) ) );
        assertEquals( "Check results size", 1, results.size() );
*/

        // test non-match fails
        query = new TermQuery( new Term( MinimalIndexRecordFields.CLASSES, "foo" ) );
        results = index.search( new LuceneQuery( query ) );

        assertTrue( "Check results size", results.isEmpty() );
    }

    private Artifact createArtifact( String artifactId )
    {
        return createArtifact( artifactId, "1.0", "jar", null );
    }

    private Artifact createArtifact( String artifactId, String version, String type )
    {
        return createArtifact( artifactId, version, type, null );
    }

    private Artifact createArtifact( String artifactId, String version, String type, String classifier )
    {
        Artifact artifact = artifactFactory.createDependencyArtifact( "org.apache.maven.archiva.record", artifactId,
                                                                      VersionRange.createFromVersion( version ), type,
                                                                      classifier, Artifact.SCOPE_RUNTIME );
        artifact.isSnapshot();
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        artifact.setRepository( repository );
        return artifact;
    }
}
