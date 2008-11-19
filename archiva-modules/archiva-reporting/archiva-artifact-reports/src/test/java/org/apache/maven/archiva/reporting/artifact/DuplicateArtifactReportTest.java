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

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.ArchivaArtifactConsumer;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.RepositoryProblem;
import org.apache.maven.archiva.reporting.DynamicReportSource;

import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * DuplicateArtifactReportTest
 *
 * @version $Id$
 */
public class DuplicateArtifactReportTest
    extends AbstractArtifactReportsTestCase
{
    private static final String TESTABLE_REPO = "testable";

    private static final String HASH3 = "f3f653289f3217c65324830ab3415bc92feddefa";

    private static final String HASH2 = "a49810ad3eba8651677ab57cd40a0f76fdef9538";

    private static final String HASH1 = "232f01b24b1617c46a3d4b0ab3415bc9237dcdec";

    private ArtifactDAO artifactDao;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        artifactDao = dao.getArtifactDAO();

        ArchivaConfiguration config = (ArchivaConfiguration) lookup( ArchivaConfiguration.class.getName(), "default" );

        ManagedRepositoryConfiguration repoConfig = new ManagedRepositoryConfiguration();
        repoConfig.setId( TESTABLE_REPO );
        repoConfig.setLayout( "default" );
        File testRepoDir = new File( getBasedir(), "target/test-repository" );
        FileUtils.forceMkdir( testRepoDir );
        repoConfig.setLocation( testRepoDir.getAbsolutePath() );
        config.getConfiguration().addManagedRepository( repoConfig );
    }

    public ArchivaArtifact createArtifact( String artifactId, String version )
    {
        ArchivaArtifact artifact =
            artifactDao.createArtifact( "org.apache.maven.archiva.test", artifactId, version, "", "jar" );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setRepositoryId( TESTABLE_REPO );
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

        List allArtifacts = artifactDao.queryArtifacts( null );
        assertEquals( "Total Artifact Count", 7, allArtifacts.size() );

        DuplicateArtifactReport report =
            (DuplicateArtifactReport) lookup( DynamicReportSource.class.getName(), "duplicate-artifacts" );

        List results = report.getData();

        System.out.println( "Results.size: " + results.size() );
        int i = 0;
        Iterator it = results.iterator();
        while ( it.hasNext() )
        {
            RepositoryProblem problem = (RepositoryProblem) it.next();
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
        List artifacts = dao.getArtifactDAO().queryArtifacts( null );
        ArchivaArtifactConsumer consumer =
            (ArchivaArtifactConsumer) lookup( ArchivaArtifactConsumer.class.getName(), "duplicate-artifacts" );
        consumer.beginScan();
        try
        {
            Iterator it = artifacts.iterator();
            while ( it.hasNext() )
            {
                ArchivaArtifact artifact = (ArchivaArtifact) it.next();
                consumer.processArchivaArtifact( artifact );
            }
        }
        finally
        {
            consumer.completeScan();
        }
    }
}
