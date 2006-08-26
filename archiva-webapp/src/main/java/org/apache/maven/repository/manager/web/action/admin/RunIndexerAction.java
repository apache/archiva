package org.apache.maven.repository.manager.web.action.admin;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.opensymphony.xwork.ActionSupport;
import org.apache.maven.repository.scheduler.RepositoryTaskScheduler;
import org.apache.maven.repository.scheduler.TaskExecutionException;

/**
 * Configures the application.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="runIndexerAction"
 */
public class RunIndexerAction
    extends ActionSupport
{
    /**
     * @plexus.requirement
     */
    private RepositoryTaskScheduler taskScheduler;

    public String execute()
        throws TaskExecutionException
    {
        taskScheduler.runIndexer();

        return SUCCESS;
    }
}
