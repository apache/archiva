package org.apache.maven.archiva.web.action.admin;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.scheduled.ArchivaTaskScheduler;
import org.apache.maven.archiva.scheduled.DefaultArchivaTaskScheduler;
import org.apache.maven.archiva.scheduled.tasks.ArchivaTask;
import org.apache.maven.archiva.scheduled.tasks.RepositoryTask;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.xwork.interceptor.SecureAction;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionBundle;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionException;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

/**
 * Configures the application.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="indexRepositoryAction"
 */
public class IndexRepositoryAction
    extends PlexusActionSupport
    implements SecureAction
{
    /**
     * @plexus.requirement
     */
    private ArchivaTaskScheduler taskScheduler;

    private String repoid;

    public String run()
    {
        if ( StringUtils.isBlank( repoid ) )
        {
            addActionError( "Cannot run indexer on blank repository id." );
            return SUCCESS;
        }

        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repoid );
        task.setName( DefaultArchivaTaskScheduler.REPOSITORY_JOB + ":" + repoid );
        task.setQueuePolicy( ArchivaTask.QUEUE_POLICY_WAIT );

        boolean scheduleTask = false;

        if ( taskScheduler.getTaskQueue().hasFilesystemTaskInQueue() )
        {
            if ( taskScheduler.getTaskQueue().hasRepositoryTaskInQueue( repoid ) )
            {
                addActionError( "Repository [" + repoid + "] task was already queued." );
            }
            else
            {
                scheduleTask = true;
            }
        }
        else
        {
            scheduleTask = true;
        }

        if ( scheduleTask )
        {
            try
            {
                taskScheduler.getTaskQueue().put( task );
                addActionMessage( "Your request to have repository [" + repoid + "] be indexed has been queued." );
            }
            catch ( TaskQueueException e )
            {
                addActionError( "Unable to queue your request to have repository [" + repoid + "] be indexed: "
                    + e.getMessage() );
            }
        }

        // Return to the repositories screen.
        return SUCCESS;
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_RUN_INDEXER, Resource.GLOBAL );

        return bundle;
    }

    public String getRepoid()
    {
        return repoid;
    }

    public void setRepoid( String repoid )
    {
        this.repoid = repoid;
    }
}
