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
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.SimpleConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * UniqueArtifactIdConstraintTest 
 *
 * @version $Id$
 */
public class UniqueArtifactIdConstraintTest
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
        setUpArtifacts();

        assertConstraint( new String[] {}, new UniqueArtifactIdConstraint( "org.apache" ) );
        assertConstraint( new String[] { "commons-lang" }, new UniqueArtifactIdConstraint( "commons-lang" ) );
        assertConstraint( new String[] { "test-one" }, new UniqueArtifactIdConstraint( "org.apache.maven.test" ) );
        assertConstraint( new String[] { "test-two", "test-bar" },
                          new UniqueArtifactIdConstraint( "org.apache.maven.shared" ) );
        assertConstraint( new String[] { "modellong" }, new UniqueArtifactIdConstraint( "org.codehaus.modello" ) );
    }
    
    public void testConstraintDisregardGroupId()
        throws Exception
    {
        setUpArtifacts();
        
        assertConstraintWithMultipleResultTypes( new String[] { "commons-lang", "test-one", "test-two", "test-two", "test-bar", "modellong" },
                          new UniqueArtifactIdConstraint( "testable_repo", true ) );
    }

    private void setUpArtifacts()
        throws ArchivaDatabaseException
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

        artifact = createArtifact( "org.apache.maven.shared", "test-bar", "2.1" );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.codehaus.modello", "modellong", "3.0" );
        artifactDao.saveArtifact( artifact );
    }
    
    private void assertConstraintWithMultipleResultTypes( String[] artifactIds, SimpleConstraint constraint )
        throws Exception
    {
        String prefix = "Unique Artifact IDs: ";
    
        List<Object[]> results = dao.query( constraint );
        assertNotNull( prefix + "Not Null", results );
        assertEquals( prefix + "Results.size", artifactIds.length, results.size() );
    
        List<String> expectedArtifactIds = Arrays.asList( artifactIds );
    
        Iterator<Object[]> it = results.iterator();
        while ( it.hasNext() )
        {
            Object[] actualArtifactIds = (Object[]) it.next();            
            String actualArtifactId = ( String ) actualArtifactIds[1];
            assertTrue( prefix + "artifactId result should not be blank.", StringUtils.isNotBlank( actualArtifactId ) );
            assertTrue( prefix + " artifactId result <" + actualArtifactId + "> exists in expected artifactIds.",
                        expectedArtifactIds.contains( actualArtifactId ) );            
        }
    }
    
    private void assertConstraint( String[] artifactIds, SimpleConstraint constraint )
    {
        String prefix = "Unique Artifact IDs: ";

        List<String> results = dao.query( constraint );
        assertNotNull( prefix + "Not Null", results );
        assertEquals( prefix + "Results.size", artifactIds.length, results.size() );

        List<String> expectedArtifactIds = Arrays.asList( artifactIds );

        Iterator<String> it = results.iterator();
        while ( it.hasNext() )
        {
            String actualArtifactId = (String) it.next();
            assertTrue( prefix + "artifactId result should not be blank.", StringUtils.isNotBlank( actualArtifactId ) );
            assertTrue( prefix + " artifactId result <" + actualArtifactId + "> exists in expected artifactIds.",
                        expectedArtifactIds.contains( actualArtifactId ) );
        }
    }
}
