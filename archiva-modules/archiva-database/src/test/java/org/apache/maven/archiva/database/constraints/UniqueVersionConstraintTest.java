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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.database.AbstractArchivaDatabaseTestCase;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.SimpleConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * UniqueVersionConstraintTest 
 *
 * @version $Id$
 */
public class UniqueVersionConstraintTest
    extends AbstractArchivaDatabaseTestCase
{
    private ArtifactDAO artifactDao;

    public void testConstraintGroupIdArtifactIdCommonsLang()
        throws Exception
    {
        setupArtifacts();

        assertConstraint( new String[] { "2.0", "2.1" }, new UniqueVersionConstraint( "commons-lang", "commons-lang" ) );
    }

    public void testConstraintGroupIdArtifactIdInvalid()
        throws Exception
    {
        setupArtifacts();

        assertConstraint( new String[] {}, new UniqueVersionConstraint( "org.apache", "invalid" ) );
        assertConstraint( new String[] {}, new UniqueVersionConstraint( "org.apache.test", "invalid" ) );
        assertConstraint( new String[] {}, new UniqueVersionConstraint( "invalid", "test-two" ) );
    }

    public void testConstraintGroupIdArtifactIdMavenSharedTestTwo()
        throws Exception
    {
        setupArtifacts();

        assertConstraint( new String[] { "2.0", "2.1-SNAPSHOT", "2.1.1", "2.1-alpha-1" },
                          new UniqueVersionConstraint( "org.apache.maven.shared", "test-two" ) );
    }

    public void testConstraintGroupIdArtifactIdMavenSharedTestTwoCentralOnly()
        throws Exception
    {
        setupArtifacts();

        List<String> observableRepositories = new ArrayList<String>();
        observableRepositories.add( "central" );

        assertConstraint( new String[] { "2.0", "2.1.1", "2.1-alpha-1" },
                          new UniqueVersionConstraint( observableRepositories, "org.apache.maven.shared", "test-two" ) );
    }

    public void testConstraintGroupIdArtifactIdMavenSharedTestTwoSnapshotsOnly()
        throws Exception
    {
        setupArtifacts();

        List<String> observableRepositories = new ArrayList<String>();
        observableRepositories.add( "snapshots" );

        assertConstraint( new String[] { "2.1-SNAPSHOT" }, 
                          new UniqueVersionConstraint( observableRepositories, "org.apache.maven.shared", "test-two" ) );
    }

    public void testConstraintGroupIdArtifactIdMavenTestOne()
        throws Exception
    {
        setupArtifacts();

        assertConstraint( new String[] { "1.2" }, new UniqueVersionConstraint( "org.apache.maven.test", "test-one" ) );
    }

    public void testConstraintGroupIdArtifactIdModelloLong()
        throws Exception
    {
        setupArtifacts();

        assertConstraint( new String[] { "3.0" }, new UniqueVersionConstraint( "org.codehaus.modello", "modellong" ) );
    }

    private void assertConstraint( String[] versions, SimpleConstraint constraint )
    {
        String prefix = "Unique Versions: ";

        List<String> results = dao.query( constraint );
        assertNotNull( prefix + "Not Null", results );
        assertEquals( prefix + "Results.size", versions.length, results.size() );

        List<String> expectedVersions = Arrays.asList( versions );

        for ( String actualVersion : results )
        {
            assertTrue( prefix + "version result should not be blank.", StringUtils.isNotBlank( actualVersion ) );
            assertTrue( prefix + "version result <" + actualVersion + "> exists in expected versions.",
                        expectedVersions.contains( actualVersion ) );
        }
    }

    private ArchivaArtifact createArtifact( String repoId, String groupId, String artifactId, String version )
    {
        ArchivaArtifact artifact = artifactDao.createArtifact( groupId, artifactId, version, "", "jar", "testrepo" );
        artifact.getModel().setLastModified( new Date() ); // mandatory field.
        artifact.getModel().setRepositoryId( repoId );
        return artifact;
    }

    private void setupArtifacts()
        throws Exception
    {
        ArchivaArtifact artifact;

        // Setup artifacts in fresh DB.
        artifact = createArtifact( "central", "commons-lang", "commons-lang", "2.0" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "central", "commons-lang", "commons-lang", "2.1" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "central", "org.apache.maven.test", "test-one", "1.2" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "central", "org.apache.maven.test.foo", "test-two", "1.0" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "central", "org.apache.maven.shared", "test-two", "2.0" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "central", "org.apache.maven.shared", "test-two", "2.1.1" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "central", "org.apache.maven.shared", "test-two", "2.1-alpha-1" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "central", "org.apache.maven.shared", "test-bar", "2.1" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "central", "org.codehaus.modello", "modellong", "3.0" );
        artifactDao.saveArtifact( artifact );

        // Snapshots repository artifacts
        artifact = createArtifact( "snapshots", "org.apache.maven.shared", "test-two", "2.1-SNAPSHOT" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "snapshots", "org.codehaus.modello", "test-three", "1.0-SNAPSHOT" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "snapshots", "org.codehaus.mojo", "testable-maven-plugin", "2.1-SNAPSHOT" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "snapshots", "org.apache.archiva", "testable", "1.1-alpha-1-20070822.033400-43" );
        artifactDao.saveArtifact( artifact );
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArchivaDAO dao = (ArchivaDAO) lookup( ArchivaDAO.ROLE, "jdo" );
        artifactDao = dao.getArtifactDAO();
    }
}
