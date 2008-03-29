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
import java.util.Iterator;
import java.util.List;

/**
 * UniqueGroupIdConstraintTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class UniqueGroupIdConstraintTest
    extends AbstractArchivaDatabaseTestCase
{
    private ArtifactDAO artifactDao;

    public void testConstraintGroupIdParamCommonsLang()
        throws Exception
    {
        setupArtifacts();

        assertConstraint( new String[] { "commons-lang" }, new UniqueGroupIdConstraint( "commons-lang" ) );
    }

    public void testConstraintGroupIdParamNoRepos()
        throws Exception
    {
        try
        {
            List<String> selectedRepos = new ArrayList<String>();
            new UniqueGroupIdConstraint( selectedRepos, "org" );
            fail( "Should have thrown an IllegalArgumentException due to lack of specified repos." );
        }
        catch ( IllegalArgumentException e )
        {
            // expected path.
        }
    }

    public void testConstraintGroupIdParamNullRepos()
        throws Exception
    {
        try
        {
            new UniqueGroupIdConstraint( (List<String>) null, "org" );
            fail( "Should have thrown an NullPointerException due to lack of specified repos." );
        }
        catch ( NullPointerException e )
        {
            // expected path.
        }
    }

    public void testConstraintGroupIdParamOrg()
        throws Exception
    {
        setupArtifacts();

        assertConstraint( new String[] {
            "org.apache.maven.test",
            "org.apache.maven.test.foo",
            "org.apache.maven.shared",
            "org.apache.archiva",
            "org.codehaus.modello",
            "org.codehaus.mojo" }, new UniqueGroupIdConstraint( "org" ) );
    }

    public void testConstraintGroupIdParamOrgApache()
        throws Exception
    {
        setupArtifacts();

        assertConstraint( new String[] {
            "org.apache.maven.test",
            "org.apache.maven.test.foo",
            "org.apache.maven.shared",
            "org.apache.archiva" }, new UniqueGroupIdConstraint( "org.apache" ) );
    }

    public void testConstraintGroupIdParamOrgApacheMaven()
        throws Exception
    {
        setupArtifacts();

        assertConstraint( new String[] {
            "org.apache.maven.test",
            "org.apache.maven.test.foo",
            "org.apache.maven.shared" }, new UniqueGroupIdConstraint( "org.apache.maven" ) );
    }

    public void testConstraintGroupIdParamOrgApacheSnapshotsOnly()
        throws Exception
    {
        setupArtifacts();

        List<String> observableRepositories = new ArrayList<String>();
        observableRepositories.add( "snapshots" );

        assertConstraint( new String[] { "org.apache.archiva" }, new UniqueGroupIdConstraint( observableRepositories,
                                                                                              "org.apache" ) );
    }

    public void testConstraintGroupIdParamOrgSnapshotsOnly()
        throws Exception
    {
        setupArtifacts();

        List<String> observableRepositories = new ArrayList<String>();
        observableRepositories.add( "snapshots" );

        assertConstraint( new String[] { "org.apache.archiva", "org.codehaus.modello", "org.codehaus.mojo" },
                          new UniqueGroupIdConstraint( observableRepositories, "org" ) );
    }

    public void testConstraintNoGroupIdParam()
        throws Exception
    {
        setupArtifacts();

        assertConstraint( new String[] {
            "commons-lang",
            "org.apache.maven.test",
            "org.apache.maven.test.foo",
            "org.apache.maven.shared",
            "org.codehaus.modello",
            "org.codehaus.mojo",
            "org.apache.archiva" }, new UniqueGroupIdConstraint() );
    }

    public void testConstraintNoGroupIdParamCentralAndSnapshots()
        throws Exception
    {
        setupArtifacts();

        List<String> observableRepositories = new ArrayList<String>();
        observableRepositories.add( "central" );
        observableRepositories.add( "snapshots" );

        assertConstraint( new String[] {
            "commons-lang",
            "org.apache.maven.test",
            "org.apache.maven.test.foo",
            "org.apache.maven.shared",
            "org.codehaus.modello",
            "org.codehaus.mojo",
            "org.apache.archiva" }, new UniqueGroupIdConstraint( observableRepositories ) );
    }

    public void testConstraintNoGroupIdParamCentralOnly()
        throws Exception
    {
        setupArtifacts();

        List<String> observableRepositories = new ArrayList<String>();
        observableRepositories.add( "central" );

        assertConstraint( new String[] {
            "commons-lang",
            "org.apache.maven.test",
            "org.apache.maven.test.foo",
            "org.apache.maven.shared",
            "org.codehaus.modello" }, new UniqueGroupIdConstraint( observableRepositories ) );
    }

    public void testConstraintNoGroupIdParamNoRepos()
        throws Exception
    {
        try
        {
            List<String> selectedRepos = new ArrayList<String>();
            new UniqueGroupIdConstraint( selectedRepos );
            fail( "Should have thrown an IllegalArgumentException due to lack of specified repos." );
        }
        catch ( IllegalArgumentException e )
        {
            // expected path.
        }
    }

    public void testConstraintNoGroupIdParamNullRepos()
        throws Exception
    {
        try
        {
            new UniqueGroupIdConstraint( (List<String>) null );
            fail( "Should have thrown an NullPointerException due to lack of specified repos." );
        }
        catch ( NullPointerException e )
        {
            // expected path.
        }
    }

    public void testConstraintNoGroupIdParamSnapshotsOnly()
        throws Exception
    {
        setupArtifacts();

        List<String> observableRepositories = new ArrayList<String>();
        observableRepositories.add( "snapshots" );

        assertConstraint( new String[] { "org.codehaus.modello", "org.codehaus.mojo", "org.apache.archiva" },
                          new UniqueGroupIdConstraint( observableRepositories ) );
    }

    private void assertConstraint( String[] expectedGroupIds, SimpleConstraint constraint )
        throws Exception
    {
        String prefix = "Unique Group IDs: ";

        List<String> results = dao.query( constraint );
        assertNotNull( prefix + "Not Null", results );
        assertEquals( prefix + "Results.size", expectedGroupIds.length, results.size() );

        List<String> groupIdList = Arrays.asList( expectedGroupIds );

        Iterator<String> it = results.iterator();
        while ( it.hasNext() )
        {
            String actualGroupId = (String) it.next();
            assertTrue( prefix + "groupId result should not be blank.", StringUtils.isNotBlank( actualGroupId ) );
            assertTrue( prefix + " groupId result <" + actualGroupId + "> exists in expected GroupIds.", groupIdList
                .contains( actualGroupId ) );
        }
    }

    private ArchivaArtifact createArtifact( String repoId, String groupId, String artifactId, String version )
    {
        ArchivaArtifact artifact = artifactDao.createArtifact( groupId, artifactId, version, "", "jar" );
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

        artifact = createArtifact( "central", "org.apache.maven.shared", "test-two", "2.1" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "central", "org.codehaus.modello", "test-two", "3.0" );
        artifactDao.saveArtifact( artifact );

        // Snapshots repository artifacts
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
