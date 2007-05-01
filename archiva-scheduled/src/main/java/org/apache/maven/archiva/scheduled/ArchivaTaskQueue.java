package org.apache.maven.archiva.scheduled;

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
import org.apache.maven.archiva.scheduled.tasks.DatabaseTask;
import org.apache.maven.archiva.scheduled.tasks.RepositoryTask;
import org.codehaus.plexus.taskqueue.DefaultTaskQueue;
import org.codehaus.plexus.taskqueue.Task;
import org.codehaus.plexus.taskqueue.TaskQueueException;

import java.util.Iterator;
import java.util.List;

/**
 * ArchivaTaskQueue 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.codehaus.plexus.taskqueue.TaskQueue" 
 *                   role-hint="archiva-task-queue"
 *                   lifecycle-handler="plexus-configurable"
 */
public class ArchivaTaskQueue
    extends DefaultTaskQueue
{

    public ArchivaTaskQueue()
    {
        super();
        /* do nothing special */
    }

    public boolean hasDatabaseTaskInQueue()
    {
        try
        {
            List queue = getQueueSnapshot();
            Iterator it = queue.iterator();
            while ( it.hasNext() )
            {
                Task task = (Task) it.next();
                if ( task instanceof DatabaseTask )
                {
                    return true;
                }
            }
            return false;
        }
        catch ( TaskQueueException e )
        {
            return false;
        }
    }

    public boolean hasFilesystemTaskInQueue()
    {
        try
        {
            List queue = getQueueSnapshot();
            Iterator it = queue.iterator();
            while ( it.hasNext() )
            {
                Task task = (Task) it.next();
                if ( task instanceof RepositoryTask )
                {
                    return true;
                }
            }
            return false;
        }
        catch ( TaskQueueException e )
        {
            return false;
        }
    }

    public boolean hasRepositoryTaskInQueue( String repoid )
    {
        try
        {
            List queue = getQueueSnapshot();
            Iterator it = queue.iterator();
            while ( it.hasNext() )
            {
                Task task = (Task) it.next();
                if ( task instanceof RepositoryTask )
                {
                    RepositoryTask rtask = (RepositoryTask) task;
                    if ( StringUtils.equals( repoid, rtask.getRepositoryId() ) )
                    {
                        return true;
                    }
                }
            }
            return false;
        }
        catch ( TaskQueueException e )
        {
            return false;
        }
    }
}
