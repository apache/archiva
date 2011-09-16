package org.apache.archiva.web.action.admin;

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

import org.apache.archiva.scheduler.repository.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.RepositoryTask;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.archiva.web.action.AbstractActionSupport;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.codehaus.redback.integration.interceptor.SecureAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Configures the application.
 */
@Controller( "schedulerAction" )
@Scope( "prototype" )
public class SchedulerAction
    extends AbstractActionSupport
    implements SecureAction
{

    @Inject
    @Named( value = "archivaTaskScheduler#repository" )
    private RepositoryArchivaTaskScheduler repositoryTaskScheduler;

    private String repoid;

    private boolean scanAll;

    public String scanRepository()
    {
        if ( StringUtils.isBlank( repoid ) )
        {
            addActionError( "Cannot run indexer on blank repository id." );
            return SUCCESS;
        }

        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repoid );
        task.setScanAll( scanAll );

        if ( repositoryTaskScheduler.isProcessingRepositoryTask( repoid ) )
        {
            addActionError( "Repository [" + repoid + "] task was already queued." );
        }
        else
        {
            try
            {
                addActionMessage( "Your request to have repository [" + repoid + "] be indexed has been queued." );
                repositoryTaskScheduler.queueTask( task );
            }
            catch ( TaskQueueException e )
            {
                addActionError(
                    "Unable to queue your request to have repository [" + repoid + "] be indexed: " + e.getMessage() );
            }
        }

        // Return to the repositories screen.
        return SUCCESS;
    }

    @Override
    public void addActionMessage( String aMessage )
    {
        super.addActionMessage( aMessage );
        log.info( "[ActionMessage] " + aMessage );
    }

    @Override
    public void addActionError( String anErrorMessage )
    {
        super.addActionError( anErrorMessage );
        log.warn( "[ActionError] " + anErrorMessage );
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

    public boolean getScanAll()
    {
        return scanAll;
    }

    public void setScanAll( boolean scanAll )
    {
        this.scanAll = scanAll;
    }
}
