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
import org.apache.archiva.admin.mock.MockAuditListener;
import org.apache.archiva.audit.AuditEvent;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.redback.role.RoleManager;
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

    @Inject
    private MockAuditListener mockAuditListener;

    @Inject
    protected RoleManager roleManager;

    public static final String STAGE_REPO_ID_END = DefaultManagedRepositoryAdmin.STAGE_REPO_ID_END;

    @Test
    public void getAllManagedRepos()
        throws Exception
    {
        mockAuditListener.clearEvents();
        List<ManagedRepository> repos = managedRepositoryAdmin.getManagedRepositories();
        assertNotNull( repos );
        assertTrue( repos.size() > 0 );
        log.info( "repos " + repos );

        // check default internal
        ManagedRepository internal = findManagedRepoById( repos, "internal" );
        assertNotNull( internal );
        assertTrue( internal.isReleases() );
        assertFalse( internal.isSnapshots() );
        mockAuditListener.clearEvents();
    }

    @Test
    public void getById()
        throws Exception
    {
        mockAuditListener.clearEvents();
        ManagedRepository repo = managedRepositoryAdmin.getManagedRepository( "internal" );
        assertNotNull( repo );
        mockAuditListener.clearEvents();
    }

    @Test
    public void addDeleteManagedRepo()
        throws Exception
    {
        mockAuditListener.clearEvents();

        String repoId = "test-new-one";

        String repoLocation = APPSERVER_BASE_PATH + File.separator + repoId;

        File repoDir = clearRepoLocation( repoLocation );

        List<ManagedRepository> repos = managedRepositoryAdmin.getManagedRepositories();
        assertNotNull( repos );
        int initialSize = repos.size();
        assertTrue( initialSize > 0 );

        ManagedRepository repo = new ManagedRepository();
        repo.setId( repoId );
        repo.setName( "test repo" );
        repo.setLocation( repoLocation );
        managedRepositoryAdmin.addManagedRepository( repo, false, getFakeAuditInformation() );
        repos = managedRepositoryAdmin.getManagedRepositories();
        assertNotNull( repos );
        assertEquals( initialSize + 1, repos.size() );

        assertNotNull( managedRepositoryAdmin.getManagedRepository( repoId ) );

        assertTemplateRoleExists( repoId );

        managedRepositoryAdmin.deleteManagedRepository( repoId, getFakeAuditInformation(), false );

        // deleteContents false
        assertTrue( repoDir.exists() );

        repos = managedRepositoryAdmin.getManagedRepositories();
        assertNotNull( repos );
        assertEquals( initialSize, repos.size() );

        assertTemplateRoleNotExists( repoId );

        assertEquals( 2, mockAuditListener.getAuditEvents().size() );

        assertAuditListenerCallAddAndDelete();

        mockAuditListener.clearEvents();
    }

    @Test
    public void updateDeleteManagedRepo()
        throws Exception
    {
        String repoId = "test-new-one";

        String repoLocation = APPSERVER_BASE_PATH + File.separator + "new-path";

        File repoDir = clearRepoLocation( repoLocation );

        mockAuditListener.clearEvents();
        List<ManagedRepository> repos = managedRepositoryAdmin.getManagedRepositories();
        assertNotNull( repos );
        int initialSize = repos.size();
        assertTrue( initialSize > 0 );

        ManagedRepository repo = new ManagedRepository();
        repo.setId( repoId );
        repo.setName( "test repo" );
        repo.setLocation( repoLocation );
        managedRepositoryAdmin.addManagedRepository( repo, false, getFakeAuditInformation() );

        assertTemplateRoleExists( repoId );

        repos = managedRepositoryAdmin.getManagedRepositories();
        assertNotNull( repos );
        assertEquals( initialSize + 1, repos.size() );

        String newName = "test repo update";

        repo.setName( newName );

        repo.setLocation( repoLocation );

        managedRepositoryAdmin.updateManagedRepository( repo, false, getFakeAuditInformation() );

        repo = managedRepositoryAdmin.getManagedRepository( repoId );
        assertNotNull( repo );
        assertEquals( newName, repo.getName() );
        assertEquals( new File( repoLocation ).getCanonicalPath(), new File( repo.getLocation() ).getCanonicalPath() );
        assertTrue( new File( repoLocation ).exists() );

        assertTemplateRoleExists( repoId );

        managedRepositoryAdmin.deleteManagedRepository( repo.getId(), getFakeAuditInformation(), false );

        // check deleteContents false
        assertTrue( repoDir.exists() );

        assertTemplateRoleNotExists( repoId );

        assertEquals( "not 3 audit events " + mockAuditListener.getAuditEvents(), 3,
                      mockAuditListener.getAuditEvents().size() );

        assertEquals( AuditEvent.ADD_MANAGED_REPO, mockAuditListener.getAuditEvents().get( 0 ).getAction() );
        assertEquals( "root", mockAuditListener.getAuditEvents().get( 0 ).getUserId() );
        assertEquals( "archiva-localhost", mockAuditListener.getAuditEvents().get( 0 ).getRemoteIP() );

        assertEquals( AuditEvent.MODIFY_MANAGED_REPO, mockAuditListener.getAuditEvents().get( 1 ).getAction() );

        assertEquals( AuditEvent.DELETE_MANAGED_REPO, mockAuditListener.getAuditEvents().get( 2 ).getAction() );

        mockAuditListener.clearEvents();
    }


    @Test
    public void addDeleteManagedRepoWithStaged()
        throws Exception
    {

        String repoId = "test-new-one";
        String repoLocation = APPSERVER_BASE_PATH + File.separator + repoId;

        File repoDir = clearRepoLocation( repoLocation );

        mockAuditListener.clearEvents();
        List<ManagedRepository> repos = managedRepositoryAdmin.getManagedRepositories();
        assertNotNull( repos );
        int initialSize = repos.size();
        assertTrue( initialSize > 0 );

        ManagedRepository repo = new ManagedRepository();
        repo.setId( repoId );
        repo.setName( "test repo" );
        repo.setLocation( repoLocation );
        managedRepositoryAdmin.addManagedRepository( repo, true, getFakeAuditInformation() );
        repos = managedRepositoryAdmin.getManagedRepositories();
        assertNotNull( repos );
        assertEquals( initialSize + 2, repos.size() );

        assertNotNull( managedRepositoryAdmin.getManagedRepository( repoId ) );

        assertTemplateRoleExists( repoId );

        assertTrue( repoDir.exists() );

        assertNotNull( managedRepositoryAdmin.getManagedRepository( repoId + STAGE_REPO_ID_END ) );

        assertTemplateRoleExists( repoId + STAGE_REPO_ID_END );

        assertTrue( new File( repoLocation + STAGE_REPO_ID_END ).exists() );

        managedRepositoryAdmin.deleteManagedRepository( repoId, getFakeAuditInformation(), true );

        assertFalse( repoDir.exists() );

        assertFalse( new File( repoLocation + STAGE_REPO_ID_END ).exists() );

        repos = managedRepositoryAdmin.getManagedRepositories();
        assertNotNull( repos );
        assertEquals( initialSize, repos.size() );

        assertTemplateRoleNotExists( repoId );

        assertTemplateRoleNotExists( repoId + STAGE_REPO_ID_END );

        mockAuditListener.clearEvents();
    }

    //----------------------------------
    // utility methods
    //----------------------------------

    private void assertTemplateRoleExists( String repoId )
        throws Exception
    {
        assertTrue( roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId ) );
        assertTrue( roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId ) );
    }


    private void assertTemplateRoleNotExists( String repoId )
        throws Exception
    {
        assertFalse( roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId ) );
        assertFalse( roleManager.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId ) );
    }

    private void assertAuditListenerCallAddAndDelete()
    {
        assertEquals( 2, mockAuditListener.getAuditEvents().size() );

        assertEquals( AuditEvent.ADD_MANAGED_REPO, mockAuditListener.getAuditEvents().get( 0 ).getAction() );
        assertEquals( "root", mockAuditListener.getAuditEvents().get( 0 ).getUserId() );
        assertEquals( "archiva-localhost", mockAuditListener.getAuditEvents().get( 0 ).getRemoteIP() );

        assertEquals( AuditEvent.DELETE_MANAGED_REPO, mockAuditListener.getAuditEvents().get( 1 ).getAction() );
        assertEquals( "root", mockAuditListener.getAuditEvents().get( 0 ).getUserId() );
    }

    private File clearRepoLocation( String path )
        throws Exception
    {
        File repoDir = new File( path );
        if ( repoDir.exists() )
        {
            FileUtils.deleteDirectory( repoDir );
        }
        assertFalse( repoDir.exists() );
        return repoDir;
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
        SimpleUser user = new SimpleUser()
        {
            @Override
            public Object getPrincipal()
            {
                return "root";
            }

        };

        user.setUsername( "root" );
        user.setFullName( "The top user" );
        return user;
    }

}
