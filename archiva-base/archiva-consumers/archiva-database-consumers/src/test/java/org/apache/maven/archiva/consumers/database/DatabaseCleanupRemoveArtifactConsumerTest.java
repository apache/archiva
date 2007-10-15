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

import org.easymock.MockControl;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.database.ArtifactDAO;

/**
 * Test for DatabaseCleanupRemoveArtifactConsumerTest
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class DatabaseCleanupRemoveArtifactConsumerTest
    extends AbstractDatabaseCleanupTest
{
    private MockControl artifactDAOControl;

    private ArtifactDAO artifactDAOMock;

    private DatabaseCleanupRemoveArtifactConsumer dbCleanupRemoveArtifactConsumer;

    public void setUp()
        throws Exception
    {
        super.setUp();

        dbCleanupRemoveArtifactConsumer = new DatabaseCleanupRemoveArtifactConsumer();

        artifactDAOControl = MockControl.createControl( ArtifactDAO.class );

        artifactDAOMock = (ArtifactDAO) artifactDAOControl.getMock();

        dbCleanupRemoveArtifactConsumer.setArtifactDAO( artifactDAOMock );
        
        dbCleanupRemoveArtifactConsumer.setBidirectionalRepositoryLayoutFactory( layoutFactory );
        
        dbCleanupRemoveArtifactConsumer.setRepositoryFactory( repositoryFactory );
    }

    public void testIfArtifactWasNotDeleted()
        throws Exception
    {
        ArchivaArtifact artifact = createArtifact( TEST_GROUP_ID, "do-not-cleanup-artifact-test", TEST_VERSION, "jar" );

        artifactDAOControl.replay();

        dbCleanupRemoveArtifactConsumer.processArchivaArtifact( artifact );

        artifactDAOControl.verify();
    }

    public void testIfArtifactWasDeleted()
        throws Exception
    {
        ArchivaArtifact artifact = createArtifact( TEST_GROUP_ID, TEST_ARTIFACT_ID, TEST_VERSION, "jar" );

        artifactDAOMock.deleteArtifact( artifact );

        artifactDAOControl.replay();

        dbCleanupRemoveArtifactConsumer.processArchivaArtifact( artifact );

        artifactDAOControl.verify();
    }

}
