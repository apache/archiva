package org.apache.maven.archiva.database.constraints;

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

import org.apache.maven.archiva.database.AbstractArchivaDatabaseTestCase;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.model.ArchivaArtifact;

import java.util.Calendar;
import java.util.List;

/**
 * RecentArtifactsByAgeConstraintTest 
 *
 * @version $Id$
 */
public class RecentArtifactsByAgeConstraintTest
    extends AbstractArchivaDatabaseTestCase
{
    private ArtifactDAO artifactDao;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArchivaDAO dao = (ArchivaDAO) lookup( ArchivaDAO.ROLE, "jdo" );
        artifactDao = dao.getArtifactDAO();
    }

    public ArchivaArtifact createArtifact( String artifactId, String version, int daysOld )
    {
        ArchivaArtifact artifact = artifactDao.createArtifact( "org.apache.maven.archiva.test", artifactId, version,
                                                               "", "jar", "testable_repo" );
        Calendar cal = Calendar.getInstance();
        cal.add( Calendar.DAY_OF_MONTH, ( -1 ) * daysOld );
        artifact.getModel().setLastModified( cal.getTime() );
        artifact.getModel().setRepositoryId( "testable_repo" );
        return artifact;
    }

    public void testConstraint()
        throws Exception
    {
        ArchivaArtifact artifact;

        // Setup artifacts in fresh DB.
        artifact = createArtifact( "test-one", "1.0", 200 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-one", "1.1", 100 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-one", "1.2", 50 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-two", "1.0", 200 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-two", "2.0", 150 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-two", "2.1", 100 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-two", "3.0", 5 );
        artifactDao.saveArtifact( artifact );

        assertConstraint( 0, new RecentArtifactsByAgeConstraint( 2 ) );
        assertConstraint( 1, new RecentArtifactsByAgeConstraint( 7 ) );
        assertConstraint( 2, new RecentArtifactsByAgeConstraint( 90 ) );
        assertConstraint( 4, new RecentArtifactsByAgeConstraint( 100 ) );
        assertConstraint( 5, new RecentArtifactsByAgeConstraint( 150 ) );
        assertConstraint( 7, new RecentArtifactsByAgeConstraint( 9000 ) );
    }

    private void assertConstraint( int expectedHits, Constraint constraint )
        throws Exception
    {
        List results = artifactDao.queryArtifacts( constraint );
        assertNotNull( "Recent Artifacts By Age: Not Null", results );
        assertEquals( "Recent Artifacts By Age: Results.size", expectedHits, results.size() );
    }
}
