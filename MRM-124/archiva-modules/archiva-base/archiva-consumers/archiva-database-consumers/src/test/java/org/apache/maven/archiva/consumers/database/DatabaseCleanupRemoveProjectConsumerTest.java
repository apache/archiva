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

import org.codehaus.plexus.cache.Cache;
import org.easymock.MockControl;
import org.apache.maven.archiva.database.ProjectModelDAO;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaProjectModel;

/**
 * Test for DatabaseCleanupRemoveProjectConsumer
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class DatabaseCleanupRemoveProjectConsumerTest
	extends AbstractDatabaseCleanupTest
{
    private MockControl projectModelDAOControl;

    private ProjectModelDAO projectModelDAOMock;

    private DatabaseCleanupRemoveProjectConsumer dbCleanupRemoveProjectConsumer;
    
    private Cache effectiveProjectCache;

    public void setUp()
        throws Exception
    {
        super.setUp();

        dbCleanupRemoveProjectConsumer = new DatabaseCleanupRemoveProjectConsumer();

        projectModelDAOControl = MockControl.createControl( ProjectModelDAO.class );

        projectModelDAOMock = (ProjectModelDAO) projectModelDAOControl.getMock();        
        
        effectiveProjectCache = (Cache) lookup( Cache.class, "effective-project-cache" );

        dbCleanupRemoveProjectConsumer.setProjectModelDAO( projectModelDAOMock );
        
        dbCleanupRemoveProjectConsumer.setRepositoryFactory( repositoryFactory );
        
        dbCleanupRemoveProjectConsumer.setEffectiveProjectCache( effectiveProjectCache );
    }

    public void testIfArtifactWasNotDeleted()
        throws Exception
    {
        ArchivaArtifact artifact = createArtifact( TEST_GROUP_ID, "do-not-cleanup-artifact-test", TEST_VERSION, "pom" );

        projectModelDAOControl.replay();

        dbCleanupRemoveProjectConsumer.processArchivaArtifact( artifact );

        projectModelDAOControl.verify();
    }

    public void testIfArtifactWasDeleted()
        throws Exception
    {
        ArchivaArtifact artifact = createArtifact( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION, "pom" );

        ArchivaProjectModel projectModel = createProjectModel( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION );

        //this should return a value
        projectModelDAOControl.expectAndReturn(
            projectModelDAOMock.getProjectModel( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION ),
            (ArchivaProjectModel) projectModel );

        projectModelDAOMock.deleteProjectModel( projectModel );

        projectModelDAOControl.replay();

        dbCleanupRemoveProjectConsumer.processArchivaArtifact( artifact );

        projectModelDAOControl.verify();
    }
    
    public void testIfArtifactWasNotAPom()
    	throws Exception
	{
    	ArchivaArtifact artifact = createArtifact( TEST_GROUP_ID, "do-not-cleanup-artifact-test", TEST_VERSION, "jar" );

        projectModelDAOControl.replay();

        dbCleanupRemoveProjectConsumer.processArchivaArtifact( artifact );

        projectModelDAOControl.verify();
	}

    public void tearDown()
        throws Exception
    {
        super.tearDown();
    }

}
