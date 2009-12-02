package org.apache.maven.archiva.reporting.artifact;

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
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.RepositoryProblem;
import org.apache.maven.archiva.reporting.DynamicReportSource;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;

/**
 * DuplicateArtifactReportTest
 *
 * @version $Id$
 */
public class DuplicateArtifactReportTest
    extends AbstractArtifactReportsTestCase
{
    private static final String TESTABLE_REPO = "testable";

    private static final String HASH3 = "94ca33031e37aa3f3b67e5b921c729f08a6bba75";

    private static final String HASH2 = "43f7aa390f1a0265fc2de7010133951c0718a67e";

    private static final String HASH1 = "8107759ababcbfa34bcb02bc4309caf6354982ab";

    private ArtifactDAO artifactDao;

    private ManagedRepositoryConfiguration repoConfig;

    private ManagedRepositoryContent content;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        artifactDao = dao.getArtifactDAO();

        ArchivaConfiguration config = (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "default" );

        repoConfig = new ManagedRepositoryConfiguration();
        repoConfig.setId( TESTABLE_REPO );
        repoConfig.setLayout( "default" );
        File testRepoDir = new File( getBasedir(), "target/test-repository" );
        FileUtils.forceMkdir( testRepoDir );
        repoConfig.setLocation( testRepoDir.getAbsolutePath() );
        config.getConfiguration().addManagedRepository( repoConfig );

        RepositoryContentFactory factory = (RepositoryContentFactory) lookup( RepositoryContentFactory.class );
        content = factory.getManagedRepositoryContent( TESTABLE_REPO );

        createArtifactFile( testRepoDir, "test-one", "1.0", "value1" );
        createArtifactFile( testRepoDir, "test-one", "1.1", "value1" );
        createArtifactFile( testRepoDir, "test-one", "1.2", "value1" );
        createArtifactFile( testRepoDir, "test-two", "1.0", "value1" );
        createArtifactFile( testRepoDir, "test-two", "2.0", "value3" );
        createArtifactFile( testRepoDir, "test-two", "2.1", "value2" );
        createArtifactFile( testRepoDir, "test-two", "3.0", "value2" );
    }

    private void createArtifactFile( File testRepoDir, String artifactId, String version, String value )
        throws IOException
    {
        File file = new File( testRepoDir,
                              "org/apache/maven/archiva/test/" + artifactId + "/" + version + "/" + artifactId + "-" +
                                  version + ".jar" );
        file.getParentFile().mkdirs();
        FileUtils.writeStringToFile( file, value );
    }

    public ArchivaArtifact createArtifact( String artifactId, String version )
    {
        ArchivaArtifact artifact =
            artifactDao.createArtifact( "org.apache.maven.archiva.test", artifactId, version, "", "jar",
                                        TESTABLE_REPO );
        artifact.getModel().setLastModified( new Date() );
        return artifact;
    }

    public void testSimpleReport()
        throws Exception
    {
        ArchivaArtifact artifact;

        // Setup artifacts in fresh DB.
        artifact = createArtifact( "test-one", "1.0" );
        artifact.getModel().setChecksumSHA1( HASH1 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-one", "1.1" );
        artifact.getModel().setChecksumSHA1( HASH1 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-one", "1.2" );
        artifact.getModel().setChecksumSHA1( HASH1 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-two", "1.0" );
        artifact.getModel().setChecksumSHA1( HASH1 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-two", "2.0" );
        artifact.getModel().setChecksumSHA1( HASH3 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-two", "2.1" );
        artifact.getModel().setChecksumSHA1( HASH2 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-two", "3.0" );
        artifact.getModel().setChecksumSHA1( HASH2 );
        artifactDao.saveArtifact( artifact );

        // Setup entries for bad/duplicate in problem DB.
        pretendToRunDuplicateArtifactsConsumer();

        List<ArchivaArtifact> allArtifacts = artifactDao.queryArtifacts( null );
        assertEquals( "Total Artifact Count", 7, allArtifacts.size() );

        DuplicateArtifactReport report =
            (DuplicateArtifactReport) lookup( DynamicReportSource.class.getName(), "duplicate-artifacts" );

        List<RepositoryProblem> results = report.getData();

        System.out.println( "Results.size: " + results.size() );
        int i = 0;
        for ( RepositoryProblem problem : results )
        {
            System.out.println( "[" + ( i++ ) + "] " + problem.getMessage() );
        }

        int hash1Count = 4;
        int hash2Count = 2;
        int hash3Count = 1;

        int totals = ( ( hash1Count * hash1Count ) - hash1Count ) + ( ( hash2Count * hash2Count ) - hash2Count ) +
            ( ( hash3Count * hash3Count ) - hash3Count );
        assertEquals( "Total report hits.", totals, results.size() );
    }

    private void pretendToRunDuplicateArtifactsConsumer()
        throws Exception
    {
        List<ArchivaArtifact> artifacts = dao.getArtifactDAO().queryArtifacts( null );
        KnownRepositoryContentConsumer consumer =
            (KnownRepositoryContentConsumer) lookup( KnownRepositoryContentConsumer.class.getName(),
                                                     "duplicate-artifacts" );
        consumer.beginScan( repoConfig, new Date() );
        try
        {
            for ( ArchivaArtifact artifact : artifacts )
            {
                consumer.processFile( content.toPath( artifact ) );
            }
        }
        finally
        {
            consumer.completeScan();
        }
    }
}
