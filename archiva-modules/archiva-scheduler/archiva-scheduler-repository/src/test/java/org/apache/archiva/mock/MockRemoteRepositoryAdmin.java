package org.apache.archiva.mock;
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

import org.apache.archiva.admin.model.AuditInformation;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.admin.model.remote.RemoteRepositoryAdmin;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.indexer.ArchivaIndexingContext;

import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 */
public class MockRemoteRepositoryAdmin
    implements RemoteRepositoryAdmin
{
    private ArchivaConfiguration archivaConfiguration;

    @Override
    public List<RemoteRepository> getRemoteRepositories()
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public RemoteRepository getRemoteRepository( String repositoryId )
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Boolean deleteRemoteRepository( String repositoryId, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Boolean addRemoteRepository( RemoteRepository remoteRepository, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Boolean updateRemoteRepository( RemoteRepository remoteRepository, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, RemoteRepository> getRemoteRepositoriesAsMap()
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ArchivaConfiguration getArchivaConfiguration()
    {
        return archivaConfiguration;
    }

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }

    @Override
    public ArchivaIndexingContext createIndexContext( RemoteRepository repository )
        throws RepositoryAdminException
    {
        return null;
    }
}
