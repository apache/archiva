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

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArchivaDAO dao = (ArchivaDAO) lookup( ArchivaDAO.ROLE, "jdo" );
        artifactDao = dao.getArtifactDAO();
    }

    public ArchivaArtifact createArtifact( String groupId, String artifactId, String version )
    {
        ArchivaArtifact artifact = artifactDao.createArtifact( groupId, artifactId, version, "", "jar" );
        artifact.getModel().setLastModified( new Date() ); // mandatory field.
        artifact.getModel().setRepositoryId( "testable_repo" );
        return artifact;
    }

    public void testConstraint()
        throws Exception
    {
        ArchivaArtifact artifact;

        // Setup artifacts in fresh DB.
        artifact = createArtifact( "commons-lang", "commons-lang", "2.0" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "commons-lang", "commons-lang", "2.1" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.apache.maven.test", "test-one", "1.2" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.apache.maven.test.foo", "test-two", "1.0" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.apache.maven.shared", "test-two", "2.0" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.apache.maven.shared", "test-two", "2.1" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.codehaus.modello", "test-two", "3.0" );
        artifactDao.saveArtifact( artifact );

        assertConstraint( new String[] {
            "commons-lang",
            "org.apache.maven.test",
            "org.apache.maven.test.foo",
            "org.apache.maven.shared",
            "org.codehaus.modello" }, new UniqueGroupIdConstraint() );
        assertConstraint( new String[] { "commons-lang" }, new UniqueGroupIdConstraint( "commons-lang" ) );
        assertConstraint( new String[] {
            "org.apache.maven.test",
            "org.apache.maven.test.foo",
            "org.apache.maven.shared" }, new UniqueGroupIdConstraint( "org.apache.maven" ) );
        assertConstraint( new String[] {
            "org.apache.maven.test",
            "org.apache.maven.test.foo",
            "org.apache.maven.shared" }, new UniqueGroupIdConstraint( "org.apache" ) );
        assertConstraint( new String[] {
            "org.apache.maven.test",
            "org.apache.maven.test.foo",
            "org.apache.maven.shared",
            "org.codehaus.modello" }, new UniqueGroupIdConstraint( "org" ) );
    }

    private void assertConstraint( String[] expectedGroupIds, SimpleConstraint constraint )
        throws Exception
    {
        String prefix = "Unique Group IDs: ";
        
        List results = dao.query( constraint );
        assertNotNull( prefix + "Not Null", results );
        assertEquals( prefix + "Results.size", expectedGroupIds.length, results.size() );

        List groupIdList = Arrays.asList( expectedGroupIds );

        Iterator it = results.iterator();
        while ( it.hasNext() )
        {
            String actualGroupId = (String) it.next();
            assertTrue( prefix + "groupId result should not be blank.", StringUtils.isNotBlank( actualGroupId ) );
            assertTrue( prefix + " groupId result <" + actualGroupId + "> exists in expected GroupIds.",
                        groupIdList.contains( actualGroupId ) );
        }
    }

}
