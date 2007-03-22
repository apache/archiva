package org.apache.maven.archiva.web.interceptor;

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

import com.opensymphony.xwork.ActionInvocation;
import com.opensymphony.xwork.interceptor.Interceptor;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * An interceptor that makes the application configuration available
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="com.opensymphony.xwork.interceptor.Interceptor"
 * role-hint="configurationInterceptor"
 */
public class ConfigurationInterceptor
    extends AbstractLogEnabled
    implements Interceptor
{
    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @param actionInvocation
     * @return
     * @throws Exception
     */
    public String intercept( ActionInvocation actionInvocation )
        throws Exception
    {
        Configuration configuration = archivaConfiguration.getConfiguration();

        if ( !configuration.isValid() )
        {
            if ( configuration.getRepositories().isEmpty() )
            {
                getLogger().info( "No repositories were configured - forwarding to repository configuration page" );
                return "config-repository-needed";
            }
            else
            {
                getLogger().info( "Configuration is incomplete - forwarding to configuration page" );
                return "config-needed";
            }
        }
        else
        {
            return actionInvocation.invoke();
        }
    }

    public void destroy()
    {
        // This space left intentionally blank
    }

    public void init()
    {
        // This space left intentionally blank
    }
}
