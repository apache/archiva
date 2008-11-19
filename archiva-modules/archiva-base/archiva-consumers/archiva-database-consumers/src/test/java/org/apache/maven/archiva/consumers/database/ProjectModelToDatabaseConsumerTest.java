package org.apache.maven.archiva.consumers.database;

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

import java.io.File;
import java.util.Iterator;

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.DatabaseUnprocessedArtifactConsumer;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.ProjectModelDAO;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaArtifactModel;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.model.Keys;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

/**
 * Test for ProjectModelToDatabaseConsumerTest
 * 
 */
public class ProjectModelToDatabaseConsumerTest
    extends PlexusInSpringTestCase
{
    private ProjectModelToDatabaseConsumer consumer;

    private ProjectModelDAO modelDao;

    public void setUp()
        throws Exception
    {
        super.setUp();

        ArchivaConfiguration archivaConfig = (ArchivaConfiguration) lookup( ArchivaConfiguration.class );

        Configuration configuration = archivaConfig.getConfiguration();
        ManagedRepositoryConfiguration repo = configuration.findManagedRepositoryById( "internal" );
        repo.setLocation( new File( getBasedir(), "src/test/resources/test-repo" ).toString() );

        consumer =
            (ProjectModelToDatabaseConsumer) lookup( DatabaseUnprocessedArtifactConsumer.class, "update-db-project" );
        modelDao = (ProjectModelDAO) lookup( ProjectModelDAO.class, "jdo" );
    }

    public void testProcess()
        throws Exception
    {
        ArchivaProjectModel model = processAndGetModel( "test-project", "test-project-endpoint-pom", "2.4.4" );
        assertNotNull( model.getParentProject() );
        assertEquals( "test-project:test-project:2.4.4", Keys.toKey( model.getParentProject() ) );

        assertFalse( model.getDependencyManagement().isEmpty() );

        model = processAndGetModel( "test-project", "test-project-endpoint-ejb", "2.4.4" );
        assertNotNull( model.getParentProject() );
        assertEquals( "test-project:test-project-endpoint-pom:2.4.4", Keys.toKey( model.getParentProject() ) );
        assertTrue( hasDependency( model, "test-project:test-project-api:2.4.4" ) );
        assertTrue( hasDependency( model, "commons-id:commons-id:0.1-dev" ) );

        model = processAndGetModel( "test-project", "test-project", "2.4.4" );
        assertFalse( model.getDependencyManagement().isEmpty() );
    }

    private boolean hasDependency( ArchivaProjectModel model, String key )
    {
        for ( Iterator i = model.getDependencies().iterator(); i.hasNext(); )
        {
            Dependency dependency = (Dependency) i.next();
            if ( key.equals( Keys.toKey( dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion() ) ) )
            {
                return true;
            }
        }
        return false;
    }

    private ArchivaProjectModel processAndGetModel( String group, String artifactId, String version )
        throws ConsumerException, ObjectNotFoundException, ArchivaDatabaseException
    {
        ArchivaArtifact artifact = createArtifact( group, artifactId, version, "pom" );
        consumer.processArchivaArtifact( artifact );

        ArchivaProjectModel model = modelDao.getProjectModel( group, artifactId, version );
        assertEquals( group, model.getGroupId() );
        assertEquals( artifactId, model.getArtifactId() );
        assertEquals( version, model.getVersion() );
        return model;
    }

    protected ArchivaArtifact createArtifact( String group, String artifactId, String version, String type )
    {
        ArchivaArtifactModel model = new ArchivaArtifactModel();
        model.setGroupId( group );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        model.setType( type );
        model.setRepositoryId( "internal" );

        return new ArchivaArtifact( model );
    }

}
