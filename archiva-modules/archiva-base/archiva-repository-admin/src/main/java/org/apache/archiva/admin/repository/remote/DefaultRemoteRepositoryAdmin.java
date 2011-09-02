package org.apache.archiva.admin.repository.remote;
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

import org.apache.archiva.admin.repository.RepositoryAdminException;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@Service( "remoteRepositoryAdmin#default" )
public class DefaultRemoteRepositoryAdmin
    implements RemoteRepositoryAdmin
{
    @Inject
    private ArchivaConfiguration archivaConfiguration;

    public List<RemoteRepository> getRemoteRepositories()
        throws RepositoryAdminException
    {
        List<RemoteRepository> remoteRepositories = new ArrayList<RemoteRepository>();
        for ( RemoteRepositoryConfiguration repositoryConfiguration : archivaConfiguration.getConfiguration().getRemoteRepositories() )
        {
            remoteRepositories.add(
                new RemoteRepository( repositoryConfiguration.getId(), repositoryConfiguration.getName(),
                                      repositoryConfiguration.getUrl(), repositoryConfiguration.getLayout(),
                                      repositoryConfiguration.getUsername(), repositoryConfiguration.getPassword(),
                                      repositoryConfiguration.getTimeout() ) );
        }
        return remoteRepositories;
    }

    public RemoteRepository getRemoteRepository( String repositoryId )
        throws RepositoryAdminException
    {
        for ( RemoteRepository remoteRepository : getRemoteRepositories() )
        {
            if ( StringUtils.equals( repositoryId, remoteRepository.getId() ) )
            {
                return remoteRepository;
            }
        }
        return null;
    }

    public Boolean deleteRemoteRepository( String repositoryId )
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean addRemoteRepository( RemoteRepository remoteRepository )
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean updateRemoteRepository( RemoteRepository remoteRepository )
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
