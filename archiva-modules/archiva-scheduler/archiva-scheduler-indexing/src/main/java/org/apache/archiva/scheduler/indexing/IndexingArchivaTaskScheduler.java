package org.apache.archiva.scheduler.indexing;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.codehaus.plexus.taskqueue.TaskQueue;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Default implementation of a scheduling component for archiva.
 *
 * @todo TODO - consider just folding in, not really scheduled
 */
@Service("archivaTaskScheduler#indexing")
public class IndexingArchivaTaskScheduler
    implements ArchivaTaskScheduler<ArtifactIndexingTask>
{
    private Logger log = LoggerFactory.getLogger( IndexingArchivaTaskScheduler.class );

    /**
     * plexus.requirement role-hint="indexing"
     */
    @Inject
    @Named(value = "taskQueue#indexing")
    private TaskQueue indexingQueue;

    public void queueTask( ArtifactIndexingTask task )
        throws TaskQueueException
    {
        indexingQueue.put( task );
    }

}
