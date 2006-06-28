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

import com.opensymphony.webwork.interceptor.ParameterAware;
import com.opensymphony.xwork.Action;
import org.apache.maven.repository.manager.web.utils.ConfigurationManager;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:aramirez@apache.org">Allan Ramirez</a>
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="org.apache.maven.repository.manager.web.action.SchedulerConfigurationAction"
 */
public class SchedulerConfigurationAction
    implements Action, ParameterAware
{
    /**
     * @plexus.requirement
     */
    private ConfigurationManager plexusConfig;

    private Map parameters;

    public Map getParameters()
    {
        return parameters;
    }

    public void setParameters( Map parameters )
    {
        this.parameters = parameters;
    }

    /**
     * Execute this method if the action was invoked
     *
     * @return String success or error
     */
    public String execute()
    {
        try
        {
            Map map = new HashMap();

            String[] cronExpression = (String[]) parameters.get( ConfigurationManager.DISCOVERY_CRON_EXPRESSION );

            if ( cronExpression[0] != null )
            {
                map.put( ConfigurationManager.DISCOVERY_CRON_EXPRESSION, cronExpression[0] );

                plexusConfig.updateConfiguration( map );

                return SUCCESS;
            }
            else
            {
                return ERROR;
            }
        }
        catch ( Exception e )
        {
            // TODO: fix error handling!
            e.printStackTrace();
            return ERROR;
        }
    }
}
