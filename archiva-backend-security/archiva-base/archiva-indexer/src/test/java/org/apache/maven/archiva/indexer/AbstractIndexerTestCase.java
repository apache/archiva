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

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.indexer.lucene.LuceneIndexHandlers;
import org.apache.maven.archiva.indexer.lucene.LuceneRepositoryContentRecord;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * AbstractIndexerTestCase
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractIndexerTestCase
    extends PlexusTestCase
{
    protected RepositoryContentIndex index;

    protected LuceneIndexHandlers indexHandlers;

    private static final String TEST_DEFAULT_REPOSITORY_NAME = "Test Default Repository";

    private static final String TEST_DEFAULT_REPO_ID = "testDefaultRepo";

    public abstract String getIndexName();

    protected void assertRecord( LuceneRepositoryContentRecord expectedRecord, Document luceneDocument )
        throws ParseException
    {
        LuceneRepositoryContentRecord actualRecord = indexHandlers.getConverter().convert( luceneDocument );
        assertRecord( expectedRecord, actualRecord );
    }

    protected void assertRecord( LuceneRepositoryContentRecord expectedRecord,
                                 LuceneRepositoryContentRecord actualRecord )
    {
        assertEquals( expectedRecord, actualRecord );
    }

    public abstract RepositoryContentIndex createIndex( RepositoryContentIndexFactory indexFactory,
                                                        ManagedRepositoryConfiguration repository );

    public abstract LuceneIndexHandlers getIndexHandler();

    protected void setUp()
        throws Exception
    {
        super.setUp();

        RepositoryContentIndexFactory indexFactory =
            (RepositoryContentIndexFactory) lookup( RepositoryContentIndexFactory.class
                .getName(), "lucene" );

        ManagedRepositoryConfiguration repository = createTestIndex( getIndexName() );

        index = createIndex( indexFactory, repository );

        indexHandlers = getIndexHandler();
    }

    private ManagedRepositoryConfiguration createTestIndex( String indexName )
        throws Exception
    {
        File repoDir = new File( getBasedir(), "src/test/managed-repository" );
        File testIndexesDir = new File( getBasedir(), "target/test-indexes" );

        if ( !testIndexesDir.exists() )
        {
            testIndexesDir.mkdirs();
        }

        assertTrue( "Default Test Repository should exist.", repoDir.exists() && repoDir.isDirectory() );

        ManagedRepositoryConfiguration repository = createRepository( TEST_DEFAULT_REPO_ID,
                                                                      TEST_DEFAULT_REPOSITORY_NAME, repoDir );

        File indexLocation = new File( testIndexesDir, "/index-" + indexName + "-" + getName() + "/" );

        MockConfiguration config = (MockConfiguration) lookup( ArchivaConfiguration.class.getName(), "mock" );

        ManagedRepositoryConfiguration repoConfig = new ManagedRepositoryConfiguration();
        repoConfig.setId( TEST_DEFAULT_REPO_ID );
        repoConfig.setName( TEST_DEFAULT_REPOSITORY_NAME );
        repoConfig.setLocation( repoDir.getAbsolutePath() );
        repoConfig.setIndexDir( indexLocation.getAbsolutePath() );

        if ( indexLocation.exists() )
        {
            FileUtils.deleteDirectory( indexLocation );
        }

        config.getConfiguration().addManagedRepository( repoConfig );
        return repository;
    }

    protected Map getArchivaArtifactDumpMap()
    {
        Map dumps = new HashMap();

        // archiva-common-1.0.jar.txt
        dumps.put( "archiva-common",
                   createArchivaArtifact( "org.apache.maven.archiva", "archiva-common", "1.0", "", "jar" ) );

        // continuum-webapp-1.0.3-SNAPSHOT.war.txt
        dumps.put( "continuum-webapp", createArchivaArtifact( "org.apache.maven.continuum", "continuum-webapp",
                                                              "1.0.3-SNAPSHOT", "", "war" ) );

        // daytrader-ear-1.1.ear.txt
        dumps.put( "daytrader-ear", createArchivaArtifact( "org.apache.geronimo", "daytrader-ear", "1.1", "", "ear" ) );

        // maven-archetype-simple-1.0-alpha-4.jar.txt
        dumps.put( "maven-archetype-simple", createArchivaArtifact( "org.apache.maven", "maven-archetype-simple",
                                                                    "1.0-alpha-4", "", "maven-archetype" ) );

        // maven-help-plugin-2.0.2-20070119.121239-2.jar.txt
        dumps.put( "maven-help-plugin", createArchivaArtifact( "org.apache.maven.plugins", "maven-help-plugin",
                                                               "2.0.2-20070119.121239-2", "", "maven-plugin" ) );

        // redback-authorization-open-1.0-alpha-1-SNAPSHOT.jar.txt
        dumps.put( "redback-authorization-open", createArchivaArtifact( "org.codehaus.plexus.redback",
                                                                        "redback-authorization-open",
                                                                        "1.0-alpha-1-SNAPSHOT", "", "jar" ) );

        // testng-5.1-jdk15.jar.txt
        dumps.put( "testng", createArchivaArtifact( "org.testng", "testng", "5.1", "jdk15", "jar" ) );

        // wagon-provider-api-1.0-beta-3-20070209.213958-2.jar.txt
        dumps.put( "wagon-provider-api", createArchivaArtifact( "org.apache.maven.wagon", "wagon-provider-api",
                                                                "1.0-beta-3-20070209.213958-2", "", "jar" ) );

        return dumps;
    }

    protected File getDumpFile( ArchivaArtifact artifact )
    {
        File dumpDir = new File( getBasedir(), "src/test/artifact-dumps" );
        StringBuffer filename = new StringBuffer();

        filename.append( artifact.getArtifactId() ).append( "-" ).append( artifact.getVersion() );

        if ( artifact.hasClassifier() )
        {
            filename.append( "-" ).append( artifact.getClassifier() );
        }

        filename.append( "." );

        // TODO: use the ArtifactExtensionMapping object
        if ( "maven-plugin".equals( artifact.getType() ) || "maven-archetype".equals( artifact.getType() ) )
        {
            filename.append( "jar" );
        }
        else
        {
            filename.append( artifact.getType() );
        }
        filename.append( ".txt" );

        File dumpFile = new File( dumpDir, filename.toString() );

        if ( !dumpFile.exists() )
        {
            fail( "Dump file " + dumpFile.getAbsolutePath() + " does not exist (should it?)." );
        }

        return dumpFile;
    }

    private ArchivaArtifact createArchivaArtifact( String groupId, String artifactId, String version, String classifier,
                                                   String type )
    {
        ArchivaArtifact artifact = new ArchivaArtifact( groupId, artifactId, version, classifier, type );
        return artifact;
    }

    protected void createEmptyIndex()
        throws IOException
    {
        createIndex( Collections.EMPTY_LIST );
    }

    protected void createIndex( List documents )
        throws IOException
    {
        IndexWriter writer = new IndexWriter( index.getIndexDirectory(), indexHandlers.getAnalyzer(), true );
        for ( Iterator i = documents.iterator(); i.hasNext(); )
        {
            Document document = (Document) i.next();
            writer.addDocument( document );
        }
        writer.optimize();
        writer.close();
    }
    
    protected ManagedRepositoryConfiguration createRepository( String id, String name, File location )
    {
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setLocation( location.getAbsolutePath() );
        return repo;
    }
}
