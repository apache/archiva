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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="org.apache.maven.repository.manager.web.action.IndexConfigurationAction"
 */
public class IndexConfigurationAction
    implements Action, ParameterAware
{
    private Map parameters;

    /**
     * @plexus.requirement
     */
    private ConfigurationManager configManager;

    public Map getParameters()
    {
        return parameters;
    }

    public void setParameters( Map parameters )
    {
        this.parameters = parameters;
    }

    /**
     * Method that is executed when the action is invoked.
     *
     * @return a String that specifies where to go to next
     * @throws IOException
     */
    public String execute()
        throws IOException
    {
        String[] indexPath = (String[]) parameters.get( ConfigurationManager.INDEXPATH );
        Map map = new HashMap();

        if ( indexPath != null && indexPath.length == 1 && indexPath[0] != null )
        {
            String[] discoverSnapshots = (String[]) parameters.get( ConfigurationManager.DISCOVER_SNAPSHOTS );
            String[] minimalIndexPath = (String[]) parameters.get( ConfigurationManager.MIN_INDEXPATH );
            if ( minimalIndexPath != null && minimalIndexPath.length == 1 && minimalIndexPath[0] != null )
            {
                map.put( ConfigurationManager.MIN_INDEXPATH, minimalIndexPath[0] );
            }

            map.put( ConfigurationManager.INDEXPATH, indexPath[0] );
            map.put( ConfigurationManager.DISCOVER_SNAPSHOTS, discoverSnapshots[0] );

            String[] blacklistPatterns = (String[]) parameters.get( ConfigurationManager.DISCOVERY_BLACKLIST_PATTERNS );
            if ( blacklistPatterns != null && blacklistPatterns.length == 1 && blacklistPatterns[0] != null )
            {
                map.put( ConfigurationManager.DISCOVERY_BLACKLIST_PATTERNS, blacklistPatterns[0] );
            }

            configManager.updateConfiguration( map );

            return SUCCESS;
        }
        else
        {
            return ERROR;
        }
    }
}
