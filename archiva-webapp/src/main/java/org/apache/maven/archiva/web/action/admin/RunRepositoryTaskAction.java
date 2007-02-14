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

import org.apache.maven.archiva.scheduler.RepositoryTaskScheduler;
import org.apache.maven.archiva.scheduler.TaskExecutionException;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.security.rbac.Resource;
import org.codehaus.plexus.security.ui.web.interceptor.SecureAction;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionBundle;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionException;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

/**
 * Configures the application.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="runRepositoryTaskAction"
 */
public class RunRepositoryTaskAction
    extends PlexusActionSupport
    implements SecureAction
{
    /**
     * @plexus.requirement
     */
    private RepositoryTaskScheduler taskScheduler;

    public String runIndexer()
        throws TaskExecutionException
    {
        taskScheduler.runDataRefresh();

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
}
