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

import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.Iterator;
import java.util.List;

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
    private ArchivaDAO dao;

    /**
     * @param actionInvocation
     * @return
     * @throws Exception
     */
    public String intercept( ActionInvocation actionInvocation )
        throws Exception
    {
        List repos = dao.getRepositoryDAO().getRepositories();

        if ( !hasManagedRepository( repos ) )
        {
            getLogger().info( "No repositories exist - forwarding to repository configuration page" );
            return "config-repository-needed";
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

    public boolean hasManagedRepository( List repos )
    {
        if ( repos == null )
        {
            return false;
        }

        if ( repos.isEmpty() )
        {
            return false;
        }

        Iterator it = repos.iterator();
        while ( it.hasNext() )
        {
            ArchivaRepository repo = (ArchivaRepository) it.next();
            if ( repo.isManaged() )
            {
                return true;
            }
        }

        return false;
    }
}
