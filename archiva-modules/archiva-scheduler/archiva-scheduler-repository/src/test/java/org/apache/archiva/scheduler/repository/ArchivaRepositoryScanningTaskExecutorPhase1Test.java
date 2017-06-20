package org.apache.archiva.scheduler.repository;

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

import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.scheduler.repository.model.RepositoryTask;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import java.util.Collection;

/**
 * ArchivaRepositoryScanningTaskExecutorPhase1Test
 *
 *
 */
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public class ArchivaRepositoryScanningTaskExecutorPhase1Test
    extends AbstractArchivaRepositoryScanningTaskExecutorTest
{
    // Split of ArchivaRepositoryScanningTaskExecutorTest should be executed first 
    // to avoid testConsumer in unknown state if member of Phase2 all ready executed
    @Test
    public void testExecutor()
        throws Exception
    {
        RepositoryTask repoTask = new RepositoryTask();

        repoTask.setRepositoryId( TEST_REPO_ID );

        taskExecutor.executeTask( repoTask );

        Collection<ArtifactReference> unprocessedResultList = testConsumer.getConsumed();

        assertNotNull( unprocessedResultList );
        assertEquals( "Incorrect number of unprocessed artifacts detected.", 8, unprocessedResultList.size() );

    }

}
