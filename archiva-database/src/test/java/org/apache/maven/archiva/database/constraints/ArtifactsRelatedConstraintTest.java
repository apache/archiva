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
 * ArtifactsRelatedConstraintTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ArtifactsRelatedConstraintTest
    extends AbstractArchivaDatabaseTestCase
{
    private static final String TEST_GROUPID = "org.apache.maven.archiva.test";
    private ArtifactDAO artifactDao;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArchivaDAO dao = (ArchivaDAO) lookup( ArchivaDAO.ROLE, "jdo" );
        artifactDao = dao.getArtifactDAO();
    }

    public ArchivaArtifact createArtifact( String artifactId, String version, String classifier, String type )
    {
        ArchivaArtifact artifact = artifactDao.createArtifact( TEST_GROUPID, artifactId, version,
                                                               classifier, type );
        Calendar cal = Calendar.getInstance();
        artifact.getModel().setLastModified( cal.getTime() );
        artifact.getModel().setRepositoryId( "testable_repo" );
        return artifact;
    }

    public void testConstraint()
        throws Exception
    {
        // Setup artifacts in fresh DB.
        artifactDao.saveArtifact( createArtifact( "test-one", "1.0", "", "jar" ) );
        artifactDao.saveArtifact( createArtifact( "test-one", "1.0", "", "pom" ) );
        artifactDao.saveArtifact( createArtifact( "test-one", "1.0", "javadoc", "jar" ) );
        artifactDao.saveArtifact( createArtifact( "test-one", "1.0", "sources", "jar" ) );

        artifactDao.saveArtifact( createArtifact( "test-one", "1.1", "", "jar" ) );
        artifactDao.saveArtifact( createArtifact( "test-one", "1.2", "", "jar" ) );
        
        artifactDao.saveArtifact( createArtifact( "test-two", "1.0", "", "jar" ) );
        artifactDao.saveArtifact( createArtifact( "test-two", "2.0", "", "jar" ) );
        artifactDao.saveArtifact( createArtifact( "test-two", "2.1", "", "jar" ) );
        artifactDao.saveArtifact( createArtifact( "test-two", "3.0", "", "jar" ) );

        assertConstraint( 4, new ArtifactsRelatedConstraint( TEST_GROUPID, "test-one", "1.0" ) );
        assertConstraint( 1, new ArtifactsRelatedConstraint( TEST_GROUPID, "test-one", "1.1" ) );
    }

    private void assertConstraint( int expectedHits, Constraint constraint )
        throws Exception
    {
        List results = artifactDao.queryArtifacts( constraint );
        assertNotNull( "Related Artifacts: Not Null", results );
        assertEquals( "Related Artifacts: Results.size", expectedHits, results.size() );
    }

}
