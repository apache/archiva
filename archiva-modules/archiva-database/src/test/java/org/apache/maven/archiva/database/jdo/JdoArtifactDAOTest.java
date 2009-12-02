package org.apache.maven.archiva.database.jdo;

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

import java.util.Date;
import java.util.List;
import javax.jdo.JDOHelper;
import javax.jdo.spi.JDOImplHelper;

import org.apache.maven.archiva.database.AbstractArchivaDatabaseTestCase;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaArtifactModel;
import org.apache.maven.archiva.model.jpox.ArchivaArtifactModelKey;

/**
 * JdoArtifactDAOTest 
 *
 * @version $Id$
 */
public class JdoArtifactDAOTest
    extends AbstractArchivaDatabaseTestCase
{
    public void testArtifactKey()
    {
        Object o = JDOImplHelper.getInstance().newObjectIdInstance( ArchivaArtifactModel.class, "foo:bar:1.0::jar:testrepo" );
        assertNotNull( "Key should not be null.", o );
        assertTrue( "Key should be an instance of " + ArchivaArtifactModelKey.class.getName(),
                    ( o instanceof ArchivaArtifactModelKey ) );

        ArchivaArtifactModelKey key = (ArchivaArtifactModelKey) o;
        assertEquals( "foo", key.groupId );
        assertEquals( "bar", key.artifactId );
        assertEquals( "1.0", key.version );
        assertEquals( "", key.classifier );
        assertEquals( "jar", key.type );
        assertEquals("testrepo", key.repositoryId);
    }

    public void testArtifactCRUD()
        throws Exception
    {
        ArtifactDAO artiDao = dao.getArtifactDAO();

        // Create it
        ArchivaArtifact artifact = artiDao.createArtifact( "org.apache.maven.archiva", "archiva-test-module", "1.0",
                                                           "", "jar", "testrepo" );
        assertNotNull( artifact );

        // Set some mandatory values
        artifact.getModel().setLastModified( new Date() );

        // Save it.
        ArchivaArtifact savedArtifact = artiDao.saveArtifact( artifact );
        assertNotNull( savedArtifact );
        String savedKeyId = JDOHelper.getObjectId( savedArtifact.getModel() ).toString();
        assertEquals( "org.apache.maven.archiva:archiva-test-module:1.0::jar:testrepo", savedKeyId );

        // Test that something has been saved.
        List<ArchivaArtifact> artifacts = artiDao.queryArtifacts( null );
        assertNotNull( artifacts );
        assertEquals( 1, artifacts.size() );

        // Test that retrieved object is what we expect.
        ArchivaArtifact firstArtifact = (ArchivaArtifact) artifacts.get( 0 );
        assertNotNull( firstArtifact );
        assertEquals( "org.apache.maven.archiva", firstArtifact.getGroupId() );
        assertEquals( "archiva-test-module", firstArtifact.getArtifactId() );
        assertEquals( "1.0", firstArtifact.getVersion() );
        assertEquals( "", firstArtifact.getClassifier() );
        assertEquals( "jar", firstArtifact.getType() );

        // Change value and save.
        savedArtifact.getModel().setLastModified( new Date() );
        artiDao.saveArtifact( savedArtifact );

        // Test that only 1 object is saved.
        assertEquals( 1, artiDao.queryArtifacts( null ).size() );

        // Get the specific artifact.
        ArchivaArtifact actualArtifact = artiDao.getArtifact( "org.apache.maven.archiva", "archiva-test-module", "1.0",
                                                              null, "jar", "testrepo" );
        assertNotNull( actualArtifact );

        // Test expected values.
        assertEquals( "archiva-test-module", actualArtifact.getArtifactId() );

        // Test that only 1 object is saved.
        assertEquals( 1, artiDao.queryArtifacts( null ).size() );

        // Delete object.
        artiDao.deleteArtifact( actualArtifact );
        assertEquals( 0, artiDao.queryArtifacts( null ).size() );
    }
}
