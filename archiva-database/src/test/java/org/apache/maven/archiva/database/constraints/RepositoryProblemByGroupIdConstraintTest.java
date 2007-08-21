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
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.RepositoryProblemDAO;
import org.apache.maven.archiva.model.RepositoryProblem;

import java.util.List;

/**
 * RepositoryProblemByGroupIdConstraintTest
 */
public class RepositoryProblemByGroupIdConstraintTest
    extends AbstractArchivaDatabaseTestCase
{
    private static final String GROUP_ID_1 = "org.apache.maven.archiva.test.1";

    private static final String GROUP_ID_2 = "org.apache.maven.archiva.test.2";

    private static final String GROUP_ID_3 = "org.apache.maven.archiva.test.3";

    private static final String GROUP_ID_PARTIAL = "org.apache.maven.archiva";

    private RepositoryProblemDAO repoProblemDao;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArchivaDAO dao = (ArchivaDAO) lookup( ArchivaDAO.ROLE, "jdo" );
        repoProblemDao = dao.getRepositoryProblemDAO();
    }

    public RepositoryProblem createRepoProblem( String groupId )
    {
        RepositoryProblem repoProblem = new RepositoryProblem();

        repoProblem.setGroupId( groupId );
        repoProblem.setArtifactId( "artifactId" );
        repoProblem.setMessage( "message" );
        repoProblem.setOrigin( "origin" );
        repoProblem.setPath( "path" );
        repoProblem.setRepositoryId( "repositoryId" );
        repoProblem.setType( "type" );
        repoProblem.setVersion( "version" );

        return repoProblem;
    }

    public void testConstraint()
        throws Exception
    {
        repoProblemDao.saveRepositoryProblem( createRepoProblem( GROUP_ID_1 ) );

        repoProblemDao.saveRepositoryProblem( createRepoProblem( GROUP_ID_2 ) );
        repoProblemDao.saveRepositoryProblem( createRepoProblem( GROUP_ID_2 ) );

        repoProblemDao.saveRepositoryProblem( createRepoProblem( GROUP_ID_3 ) );
        repoProblemDao.saveRepositoryProblem( createRepoProblem( GROUP_ID_3 ) );
        repoProblemDao.saveRepositoryProblem( createRepoProblem( GROUP_ID_3 ) );

        assertConstraint( 1, new RepositoryProblemByGroupIdConstraint( GROUP_ID_1 ) );
        assertConstraint( 2, new RepositoryProblemByGroupIdConstraint( GROUP_ID_2 ) );
        assertConstraint( 3, new RepositoryProblemByGroupIdConstraint( GROUP_ID_3 ) );
        assertConstraint( 6, new RepositoryProblemByGroupIdConstraint( GROUP_ID_PARTIAL ) );
    }

    private void assertConstraint( int expectedHits, Constraint constraint )
        throws Exception
    {
        List results = repoProblemDao.queryRepositoryProblems( constraint );
        assertNotNull( "Repository Problems by Group Id: Not Null", results );
        assertEquals( "Repository Problems by Group Id: Results.size", expectedHits, results.size() );
    }
}
