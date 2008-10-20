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
import org.apache.maven.archiva.database.RepositoryProblemDAO;
import org.apache.maven.archiva.database.SimpleConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaArtifactModel;
import org.apache.maven.archiva.model.RepositoryProblem;

import java.util.Date;
import java.util.List;

/**
 * UniqueFieldConstraintTest
 */
public class UniqueFieldConstraintTest
    extends AbstractArchivaDatabaseTestCase
{
    private static final String GROUP_ID_1 = "org.apache.maven.archiva.test.1";

    private static final String GROUP_ID_2 = "org.apache.maven.archiva.test.2";

    private static final String GROUP_ID_3 = "org.apache.maven.archiva.test.3";

    private ArchivaDAO archivaDao;

    private ArtifactDAO artifactDao;

    private RepositoryProblemDAO repoProblemDao;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        archivaDao = (ArchivaDAO) lookup( ArchivaDAO.ROLE, "jdo" );
        artifactDao = archivaDao.getArtifactDAO();
        repoProblemDao = archivaDao.getRepositoryProblemDAO();
    }

    public ArchivaArtifact createArtifact( String groupId )
    {
        ArchivaArtifact artifact = artifactDao.createArtifact( groupId, "artifactId", "version", "classifier", "jar" );

        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setRepositoryId( "repoId" );

        return artifact;
    }

    public RepositoryProblem createRepoProblem( String groupId )
    {
        RepositoryProblem repoProblem = new RepositoryProblem();

        repoProblem.setGroupId( groupId );
        repoProblem.setArtifactId( "artifactId" );
        repoProblem.setMessage( "message" );
        repoProblem.setOrigin( "origin" );
        repoProblem.setPath( "path" );
        repoProblem.setRepositoryId( "repoId" );
        repoProblem.setType( "type" );
        repoProblem.setVersion( "version" );

        return repoProblem;
    }

    public void testArtifact()
        throws Exception
    {
        artifactDao.saveArtifact( createArtifact( GROUP_ID_1 ) );
        artifactDao.saveArtifact( createArtifact( GROUP_ID_2 ) );
        artifactDao.saveArtifact( createArtifact( GROUP_ID_3 ) );

        assertConstraint( 1, new UniqueFieldConstraint( ArchivaArtifactModel.class.getName(), "artifactId" ) );
        assertConstraint( 3, new UniqueFieldConstraint( ArchivaArtifactModel.class.getName(), "groupId" ) );
    }

    public void testRepoProblem()
        throws Exception
    {
        repoProblemDao.saveRepositoryProblem( createRepoProblem( GROUP_ID_1 ) );
        repoProblemDao.saveRepositoryProblem( createRepoProblem( GROUP_ID_2 ) );
        repoProblemDao.saveRepositoryProblem( createRepoProblem( GROUP_ID_3 ) );

        assertConstraint( 1, new UniqueFieldConstraint( RepositoryProblem.class.getName(), "artifactId" ) );
        assertConstraint( 3, new UniqueFieldConstraint( RepositoryProblem.class.getName(), "groupId" ) );
    }

    private void assertConstraint( int expectedHits, SimpleConstraint constraint )
        throws Exception
    {
        List results = archivaDao.query( constraint );
        assertNotNull( "Repository Problems: Not Null", results );
        assertEquals( "Repository Problems: Results.size", expectedHits, results.size() );
    }
}
