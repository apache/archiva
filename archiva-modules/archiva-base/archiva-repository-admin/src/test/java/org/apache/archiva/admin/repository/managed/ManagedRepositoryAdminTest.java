package org.apache.archiva.admin.repository.managed;
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

import org.apache.archiva.admin.AuditInformation;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.plexus.redback.users.memory.SimpleUser;
import org.junit.Test;

import javax.inject.Inject;
import java.io.File;
import java.util.List;

/**
 * @author Olivier Lamy
 */
public class ManagedRepositoryAdminTest
    extends AbstractRepositoryAdminTest
{

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Test
    public void getAllManagedRepos()
        throws Exception
    {
        List<ManagedRepository> repos = managedRepositoryAdmin.getManagedRepositories();
        assertNotNull( repos );
        assertTrue( repos.size() > 0 );
        log.info( "repos " + repos );

        // check default internal
        ManagedRepository internal = findManagedRepoById( repos, "internal" );
        assertNotNull( internal );
        assertTrue( internal.isReleases() );
        assertFalse( internal.isSnapshots() );
    }

    @Test
    public void getById()
        throws Exception
    {
        ManagedRepository repo = managedRepositoryAdmin.getManagedRepository( "internal" );
        assertNotNull( repo );
    }

    @Test
    public void addDeleteManagedRepo()
        throws Exception
    {
        List<ManagedRepository> repos = managedRepositoryAdmin.getManagedRepositories();
        assertNotNull( repos );
        int initialSize = repos.size();
        assertTrue( initialSize > 0 );

        ManagedRepository repo = new ManagedRepository();
        repo.setId( "test-new-one" );
        repo.setName( "test repo" );
        repo.setLocation( APPSERVER_BASE_PATH + repo.getId() );
        managedRepositoryAdmin.addManagedRepository( repo, false, getFakeAuditInformation() );
        repos = managedRepositoryAdmin.getManagedRepositories();
        assertNotNull( repos );
        assertEquals( initialSize + 1, repos.size() );

        assertNotNull( managedRepositoryAdmin.getManagedRepository( "test-new-one" ) );

        managedRepositoryAdmin.deleteManagedRepository( "test-new-one", getFakeAuditInformation() );

        repos = managedRepositoryAdmin.getManagedRepositories();
        assertNotNull( repos );
        assertEquals( initialSize, repos.size() );
    }

    @Test
    public void updateDeleteManagedRepo()
        throws Exception
    {
        List<ManagedRepository> repos = managedRepositoryAdmin.getManagedRepositories();
        assertNotNull( repos );
        int initialSize = repos.size();
        assertTrue( initialSize > 0 );

        ManagedRepository repo = new ManagedRepository();
        repo.setId( "test-new-one" );
        repo.setName( "test repo" );
        repo.setLocation( APPSERVER_BASE_PATH + repo.getId() );
        managedRepositoryAdmin.addManagedRepository( repo, false, getFakeAuditInformation() );
        repos = managedRepositoryAdmin.getManagedRepositories();
        assertNotNull( repos );
        assertEquals( initialSize + 1, repos.size() );

        String newName = "test repo update";

        repo.setName( newName );

        repo.setLocation( APPSERVER_BASE_PATH + "new-path" );

        managedRepositoryAdmin.updateManagedRepository( repo, false, getFakeAuditInformation() );

        repo = managedRepositoryAdmin.getManagedRepository( "test-new-one" );
        assertNotNull( repo );
        assertEquals( newName, repo.getName() );
        assertEquals( APPSERVER_BASE_PATH + "new-path", repo.getLocation() );
        assertTrue( new File( APPSERVER_BASE_PATH + "new-path" ).exists() );
    }


    private ManagedRepository findManagedRepoById( List<ManagedRepository> repos, String id )
    {
        for ( ManagedRepository repo : repos )
        {
            if ( StringUtils.equals( id, repo.getId() ) )
            {
                return repo;
            }
        }
        return null;
    }

    AuditInformation getFakeAuditInformation()
    {
        AuditInformation auditInformation = new AuditInformation( getFakeUser(), "archiva-localhost" );
        return auditInformation;
    }

    User getFakeUser()
    {
        SimpleUser user = new SimpleUser();
        user.setUsername( "root" );
        user.setFullName( "The top user" );
        return user;
    }

}
