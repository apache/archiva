package org.apache.maven.repository.manager.web.action;

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

import com.opensymphony.xwork.Action;
import org.apache.maven.repository.manager.web.execution.DiscovererExecution;
import org.apache.maven.repository.manager.web.job.DiscovererScheduler;

/**
 * This is the Action class of index.jsp, which is the initial page of the web application.
 * It invokes the DiscovererScheduler to set the DiscoverJob in the scheduler.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="org.apache.maven.repository.manager.web.action.BaseAction"
 */
public class BaseAction
    implements Action
{
    /**
     * @plexus.requirement
     */
    private DiscovererExecution execution;

    /**
     * @plexus.requirement
     */
    private DiscovererScheduler discovererScheduler;

    /**
     * Method that executes the action
     *
     * @return a String that specifies if the action executed was a success or a failure
     */
    public String execute()
    {
        try
        {
            execution.executeDiscovererIfIndexDoesNotExist();
            discovererScheduler.setSchedule();
        }
        catch ( Exception e )
        {
            // TODO: better exception handling!
            e.printStackTrace();
            return ERROR;
        }

        return SUCCESS;
    }

}
