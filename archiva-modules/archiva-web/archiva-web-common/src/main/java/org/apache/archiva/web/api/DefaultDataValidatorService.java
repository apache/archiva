package org.apache.archiva.web.api;
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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.group.RepositoryGroupAdmin;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.admin.model.networkproxy.NetworkProxyAdmin;
import org.apache.archiva.admin.model.remote.RemoteRepositoryAdmin;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@Service( "dataValidatorService#rest" )
public class DefaultDataValidatorService
    implements DataValidatorService
{

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    private RemoteRepositoryAdmin remoteRepositoryAdmin;

    @Inject
    private NetworkProxyAdmin networkProxyAdmin;

    @Inject
    private RepositoryGroupAdmin repositoryGroupAdmin;


    @Override
    public Boolean managedRepositoryIdNotExists( String id )
        throws ArchivaRestServiceException
    {
        try
        {
            return !idExist( id );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public Boolean remoteRepositoryIdNotExists( String id )
        throws ArchivaRestServiceException
    {
        try
        {
            return !idExist( id );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public Boolean networkProxyIdNotExists( String id )
        throws ArchivaRestServiceException
    {
        try
        {
            return networkProxyAdmin.getNetworkProxy( id ) == null;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    /**
     * check if managedRepo, remoteRepo ou group exists with this id
     *
     * @param id
     * @return true if something exists with this id.
     */
    private Boolean idExist( String id )
        throws RepositoryAdminException
    {
        return ( managedRepositoryAdmin.getManagedRepository( id ) != null ) || (
            remoteRepositoryAdmin.getRemoteRepository( id ) != null ) || ( repositoryGroupAdmin.getRepositoryGroup( id )
            != null );
    }
}
