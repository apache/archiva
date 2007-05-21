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

import org.apache.maven.archiva.database.AbstractArchivaDatabaseTestCase;
import org.apache.maven.archiva.database.ProjectModelDAO;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.jpox.ArchivaProjectModelKey;
import org.apache.maven.archiva.repository.project.ProjectModelReader;

import java.io.File;
import java.util.Date;
import java.util.List;

import javax.jdo.JDOHelper;
import javax.jdo.spi.JDOImplHelper;

/**
 * JdoProjectModelDAOTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class JdoProjectModelDAOTest
    extends AbstractArchivaDatabaseTestCase
{
    public void testProjectModelKey()
    {
        Object o = JDOImplHelper.getInstance().newObjectIdInstance( ArchivaProjectModel.class, "foo:bar:1.0" );
        assertNotNull( "Key should not be null.", o );
        assertTrue( "Key should be an instance of " + ArchivaProjectModelKey.class.getName(),
                    ( o instanceof ArchivaProjectModelKey ) );

        ArchivaProjectModelKey key = (ArchivaProjectModelKey) o;
        assertEquals( "foo", key.groupId );
        assertEquals( "bar", key.artifactId );
        assertEquals( "1.0", key.version );
    }

    public void testProjectModelCRUD()
        throws Exception
    {
        ProjectModelDAO projectDao = dao.getProjectModelDAO();

        // Create it
        ArchivaProjectModel model = projectDao.createProjectModel( "org.apache.maven.archiva", "archiva-test-module",
                                                                   "1.0" );
        assertNotNull( model );

        // Set some mandatory values
        model.setPackaging( "pom" );
        model.setWhenIndexed( new Date() );
        model.setOrigin( "test" );

        // Save it.
        ArchivaProjectModel savedModel = projectDao.saveProjectModel( model );
        assertNotNull( savedModel );
        String savedKeyId = JDOHelper.getObjectId( savedModel ).toString();
        assertEquals( "org.apache.maven.archiva:archiva-test-module:1.0", savedKeyId );

        // Test that something has been saved.
        List projects = projectDao.queryProjectModels( null );
        assertNotNull( projects );
        assertEquals( 1, projects.size() );

        // Test that retrieved object is what we expect.
        ArchivaProjectModel firstModel = (ArchivaProjectModel) projects.get( 0 );
        assertNotNull( firstModel );
        assertEquals( "org.apache.maven.archiva", firstModel.getGroupId() );
        assertEquals( "archiva-test-module", firstModel.getArtifactId() );
        assertEquals( "1.0", firstModel.getVersion() );

        // Change value and save.
        savedModel.setOrigin( "changed" );
        projectDao.saveProjectModel( savedModel );

        // Test that only 1 object is saved.
        assertEquals( 1, projectDao.queryProjectModels( null ).size() );

        // Get the specific artifact.
        ArchivaProjectModel actualModel = projectDao.getProjectModel( "org.apache.maven.archiva",
                                                                      "archiva-test-module", "1.0" );
        assertNotNull( actualModel );

        // Test expected values.
        assertEquals( "archiva-test-module", actualModel.getArtifactId() );
        assertEquals( "changed", actualModel.getOrigin() );

        // Test that only 1 object is saved.
        assertEquals( 1, projectDao.queryProjectModels( null ).size() );

        // Delete object.
        projectDao.deleteProjectModel( actualModel );
        assertEquals( 0, projectDao.queryProjectModels( null ).size() );
    }

    public void testSaveGetRealProjectModel()
        throws Exception
    {
        String groupId = "org.apache.maven.shared";
        String artifactId = "maven-shared-jar";
        String version = "1.0-SNAPSHOT";

        ProjectModelDAO projectDao = dao.getProjectModelDAO();

        ProjectModelReader modelReader = (ProjectModelReader) lookup( ProjectModelReader.class, "model400" );

        File pomFile = getTestFile( "src/test/resources/projects/maven-shared-jar-1.0-SNAPSHOT.pom" );

        assertTrue( "pom file should exist: " + pomFile.getAbsolutePath(), pomFile.exists() && pomFile.isFile() );

        ArchivaProjectModel model = modelReader.read( pomFile );
        assertNotNull( "Model should not be null.", model );

        // Fill in missing (mandatory) fields
        model.setGroupId( groupId );
        model.setOrigin( "testcase" );

        projectDao.saveProjectModel( model );

        ArchivaProjectModel savedModel = projectDao.getProjectModel( groupId, artifactId, version );
        assertNotNull( "Project model should not be null.", savedModel );

        // Test proper detachment of sub-objects.
        assertNotNull( "model.parent != null", savedModel.getParentProject() );
    }
}
