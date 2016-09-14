package org.apache.archiva.security;

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

import org.apache.archiva.admin.model.beans.ManagedRepository;

import java.util.Collections;
import java.util.List;

/**
 * UserRepositories stub used for testing.
 *
 *
 */
public class UserRepositoriesStub
    implements UserRepositories
{
    private List<String> repoIds = Collections.singletonList( "test-repo" );

    @Override
    public void createMissingRepositoryRoles( String repoId )
        throws ArchivaSecurityException
    {
    }

    @Override
    public List<String> getObservableRepositoryIds( String principal )
        throws ArchivaSecurityException
    {
        return repoIds;
    }

    public void setObservableRepositoryIds( List<String> repoIds )
    {
        this.repoIds = repoIds;
    }

    @Override
    public boolean isAuthorizedToUploadArtifacts( String principal, String repoId )
        throws ArchivaSecurityException
    {
        return true;
    }

    @Override
    public boolean isAuthorizedToDeleteArtifacts( String principal, String repoId )
    {
        return true;
    }

    @Override
    public List<String> getManagableRepositoryIds( String principal )
        throws ArchivaSecurityException
    {
        return null;
    }

    public List<String> getRepoIds()
    {
        return repoIds;
    }

    public void setRepoIds( List<String> repoIds )
    {
        this.repoIds = repoIds;
    }

    @Override
    public List<ManagedRepository> getAccessibleRepositories( String principal )
        throws ArchivaSecurityException, AccessDeniedException, PrincipalNotFoundException
    {
        return Collections.emptyList();
    }

    @Override
    public List<ManagedRepository> getManagableRepositories(String principal) throws ArchivaSecurityException, AccessDeniedException, PrincipalNotFoundException {
        return Collections.emptyList();
    }
}
