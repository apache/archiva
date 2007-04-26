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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.model.RepositoryURL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * AdminModel 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class AdminModel
{
    private String baseUrl;
    
    private List managedRepositories = new ArrayList();

    private List remoteRepositories = new ArrayList();

    public AdminModel()
    {
        /* do nothing */
    }

    public AdminModel( Configuration configuration )
    {
        Iterator it = configuration.getRepositories().iterator();
        while ( it.hasNext() )
        {
            RepositoryConfiguration repoconfig = (RepositoryConfiguration) it.next();
            RepositoryURL repourl = new RepositoryURL( repoconfig.getUrl() );
            if ( "file".equals( repourl.getProtocol() ) )
            {
                managedRepositories.add( repoconfig );
            }
            else
            {
                remoteRepositories.add( repoconfig );
            }
        }
    }

    public List getManagedRepositories()
    {
        return managedRepositories;
    }

    public void setManagedRepositories( List managedRepositories )
    {
        this.managedRepositories = managedRepositories;
    }

    public List getRemoteRepositories()
    {
        return remoteRepositories;
    }

    public void setRemoteRepositories( List remoteRepositories )
    {
        this.remoteRepositories = remoteRepositories;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl( String baseUrl )
    {
        this.baseUrl = baseUrl;
    }
}
