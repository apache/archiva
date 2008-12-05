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

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.database.AbstractArchivaDatabaseTestCase;
import org.apache.maven.archiva.database.ProjectModelDAO;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.repository.project.ProjectModelReader;
import org.apache.maven.archiva.repository.project.readers.ProjectModel400Reader;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.jdo.JDOHelper;

/**
 * JdoProjectModelDAOTest 
 *
 * @version $Id$
 */
public class JdoProjectModelDAOTest
    extends AbstractArchivaDatabaseTestCase
{
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

        ProjectModelReader modelReader = new ProjectModel400Reader();

        File pomFile = getTestFile( "src/test/resources/projects/maven-shared-jar-1.0-SNAPSHOT.pom" );

        assertTrue( "pom file should exist: " + pomFile.getAbsolutePath(), pomFile.exists() && pomFile.isFile() );

        ArchivaProjectModel model = modelReader.read( pomFile );
        assertNotNull( "Model should not be null.", model );

        /* NOTE: We are intentionally using a basic project model in this unit test.
         *       The expansion of expressions, resolving of dependencies, and merging
         *       of parent poms is *NOT* performed to keep this unit test simple.
         */

        // Fill in mandatory/missing fields
        model.setGroupId( groupId );
        model.setOrigin( "testcase" );

        projectDao.saveProjectModel( model );

        ArchivaProjectModel savedModel = projectDao.getProjectModel( groupId, artifactId, version );
        assertNotNull( "Project model should not be null.", savedModel );

        // Test proper detachment of sub-objects.
        List exprs = new ArrayList();
        exprs.add( "parentProject.groupId" );
        exprs.add( "organization.name" );
        exprs.add( "issueManagement.system" );
        exprs.add( "ciManagement.system" );
        exprs.add( "scm.url" );
        exprs.add( "individuals[0].name" );
        exprs.add( "dependencies[0].groupId" );
        exprs.add( "dependencyManagement[0].artifactId" );
        exprs.add( "repositories[0].id" );
        exprs.add( "plugins[0].artifactId" );
        exprs.add( "reports[0].artifactId" );
        exprs.add( "buildExtensions[0].artifactId" );
        exprs.add( "licenses[0].url" );
        exprs.add( "mailingLists[0].name" );

        Iterator it = exprs.iterator();
        while ( it.hasNext() )
        {
            String expr = (String) it.next();
            try
            {
                Object obj = PropertyUtils.getProperty( model, expr );
                assertNotNull( "Expr \"" + expr + "\" != null", obj );
                assertTrue( "Expr \"" + expr + "\" should be a String.", ( obj instanceof String ) );
                String value = (String) obj;
                assertTrue( "Expr \"" + expr + "\" value should not be blank.", StringUtils.isNotBlank( value ) );
            }
            catch ( IndexOutOfBoundsException e )
            {
                fail( "Expr \"" + expr + "\" unable to get indexed property: " + e.getClass().getName() + ": "
                    + e.getMessage() );
            }
        }
    }
}
