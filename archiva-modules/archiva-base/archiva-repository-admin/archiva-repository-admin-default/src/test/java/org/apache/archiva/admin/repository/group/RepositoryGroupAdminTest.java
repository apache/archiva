package org.apache.archiva.admin.repository.group;
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

import org.apache.archiva.admin.model.group.RepositoryGroup;
import org.apache.archiva.admin.model.group.RepositoryGroupAdmin;
import org.apache.archiva.admin.model.managed.ManagedRepository;
import org.apache.archiva.admin.repository.AbstractRepositoryAdminTest;
import org.apache.archiva.audit.AuditEvent;
import org.junit.Test;

import javax.inject.Inject;
import java.io.File;
import java.util.Arrays;

/**
 * @author Olivier Lamy
 */
public class RepositoryGroupAdminTest
    extends AbstractRepositoryAdminTest
{
    @Inject
    RepositoryGroupAdmin repositoryGroupAdmin;

    @Test
    public void addAndDeleteGroup()
        throws Exception
    {
        try
        {
            ManagedRepository managedRepositoryOne =
                getTestManagedRepository( "test-new-one", APPSERVER_BASE_PATH + File.separator + "test-new-one" );

            ManagedRepository managedRepositoryTwo =
                getTestManagedRepository( "test-new-two", APPSERVER_BASE_PATH + File.separator + "test-new-two" );

            managedRepositoryAdmin.addManagedRepository( managedRepositoryOne, false, getFakeAuditInformation() );

            managedRepositoryAdmin.addManagedRepository( managedRepositoryTwo, false, getFakeAuditInformation() );

            RepositoryGroup repositoryGroup =
                new RepositoryGroup( "repo-group-one", Arrays.asList( "test-new-one", "test-new-two" ) );

            mockAuditListener.clearEvents();

            repositoryGroupAdmin.addRepositoryGroup( repositoryGroup, getFakeAuditInformation() );

            assertEquals( 1, repositoryGroupAdmin.getRepositoriesGroups().size() );
            assertEquals( "repo-group-one", repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getId() );
            assertEquals( 2, repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getRepositories().size() );
            assertEquals( Arrays.asList( "test-new-one", "test-new-two" ),
                          repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getRepositories() );

            repositoryGroupAdmin.deleteRepositoryGroup( "repo-group-one", getFakeAuditInformation() );

            assertEquals( 0, repositoryGroupAdmin.getRepositoriesGroups().size() );

            assertEquals( 2, mockAuditListener.getAuditEvents().size() );

            assertEquals( AuditEvent.ADD_REPO_GROUP, mockAuditListener.getAuditEvents().get( 0 ).getAction() );
            assertEquals( AuditEvent.DELETE_REPO_GROUP, mockAuditListener.getAuditEvents().get( 1 ).getAction() );
        }
        finally
        {
            mockAuditListener.clearEvents();
            managedRepositoryAdmin.deleteManagedRepository( "test-new-one", getFakeAuditInformation(), true );
            managedRepositoryAdmin.deleteManagedRepository( "test-new-two", getFakeAuditInformation(), true );
        }
    }

    @Test
    public void addAndUpdateAndDeleteGroup()
        throws Exception
    {
        try
        {
            ManagedRepository managedRepositoryOne =
                getTestManagedRepository( "test-new-one", APPSERVER_BASE_PATH + File.separator + "test-new-one" );

            ManagedRepository managedRepositoryTwo =
                getTestManagedRepository( "test-new-two", APPSERVER_BASE_PATH + File.separator + "test-new-two" );

            managedRepositoryAdmin.addManagedRepository( managedRepositoryOne, false, getFakeAuditInformation() );

            managedRepositoryAdmin.addManagedRepository( managedRepositoryTwo, false, getFakeAuditInformation() );

            RepositoryGroup repositoryGroup = new RepositoryGroup( "repo-group-one", Arrays.asList( "test-new-one" ) );

            mockAuditListener.clearEvents();

            repositoryGroupAdmin.addRepositoryGroup( repositoryGroup, getFakeAuditInformation() );

            assertEquals( 1, repositoryGroupAdmin.getRepositoriesGroups().size() );
            assertEquals( "repo-group-one", repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getId() );
            assertEquals( 1, repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getRepositories().size() );
            assertEquals( Arrays.asList( "test-new-one" ),
                          repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getRepositories() );

            repositoryGroup = repositoryGroupAdmin.getRepositoryGroup( "repo-group-one" );
            assertNotNull( repositoryGroup );

            repositoryGroup.addRepository( managedRepositoryTwo.getId() );

            repositoryGroupAdmin.updateRepositoryGroup( repositoryGroup, getFakeAuditInformation() );

            assertEquals( 1, repositoryGroupAdmin.getRepositoriesGroups().size() );
            assertEquals( "repo-group-one", repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getId() );
            assertEquals( 2, repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getRepositories().size() );
            assertEquals( Arrays.asList( "test-new-one", "test-new-two" ),
                          repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getRepositories() );

            repositoryGroupAdmin.deleteRepositoryGroup( "repo-group-one", getFakeAuditInformation() );

            assertEquals( 0, repositoryGroupAdmin.getRepositoriesGroups().size() );

            assertEquals( 3, mockAuditListener.getAuditEvents().size() );

            assertEquals( AuditEvent.ADD_REPO_GROUP, mockAuditListener.getAuditEvents().get( 0 ).getAction() );
            assertEquals( AuditEvent.MODIFY_REPO_GROUP, mockAuditListener.getAuditEvents().get( 1 ).getAction() );
            assertEquals( AuditEvent.DELETE_REPO_GROUP, mockAuditListener.getAuditEvents().get( 2 ).getAction() );
        }
        finally
        {
            mockAuditListener.clearEvents();
            managedRepositoryAdmin.deleteManagedRepository( "test-new-one", getFakeAuditInformation(), true );
            managedRepositoryAdmin.deleteManagedRepository( "test-new-two", getFakeAuditInformation(), true );
        }
    }


    @Test
    public void addAndDeleteGroupWithRemowingManagedRepo()
        throws Exception
    {
        try
        {
            ManagedRepository managedRepositoryOne =
                getTestManagedRepository( "test-new-one", APPSERVER_BASE_PATH + File.separator + "test-new-one" );

            ManagedRepository managedRepositoryTwo =
                getTestManagedRepository( "test-new-two", APPSERVER_BASE_PATH + File.separator + "test-new-two" );

            managedRepositoryAdmin.addManagedRepository( managedRepositoryOne, false, getFakeAuditInformation() );

            managedRepositoryAdmin.addManagedRepository( managedRepositoryTwo, false, getFakeAuditInformation() );

            RepositoryGroup repositoryGroup =
                new RepositoryGroup( "repo-group-one", Arrays.asList( "test-new-one", "test-new-two" ) );

            mockAuditListener.clearEvents();

            repositoryGroupAdmin.addRepositoryGroup( repositoryGroup, getFakeAuditInformation() );

            assertEquals( 1, repositoryGroupAdmin.getRepositoriesGroups().size() );
            assertEquals( "repo-group-one", repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getId() );
            assertEquals( 2, repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getRepositories().size() );
            assertEquals( Arrays.asList( "test-new-one", "test-new-two" ),
                          repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getRepositories() );

            // deleting a managed repo to validate repogroup correctly updated !
            managedRepositoryAdmin.deleteManagedRepository( "test-new-one", getFakeAuditInformation(), true );

            assertEquals( 1, repositoryGroupAdmin.getRepositoriesGroups().size() );
            assertEquals( "repo-group-one", repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getId() );
            assertEquals( 1, repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getRepositories().size() );
            assertEquals( Arrays.asList( "test-new-two" ),
                          repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getRepositories() );

            repositoryGroupAdmin.deleteRepositoryGroup( "repo-group-one", getFakeAuditInformation() );

            assertEquals( 0, repositoryGroupAdmin.getRepositoriesGroups().size() );

            assertEquals( 3, mockAuditListener.getAuditEvents().size() );

            assertEquals( AuditEvent.ADD_REPO_GROUP, mockAuditListener.getAuditEvents().get( 0 ).getAction() );
            assertEquals( AuditEvent.DELETE_MANAGED_REPO, mockAuditListener.getAuditEvents().get( 1 ).getAction() );
            assertEquals( AuditEvent.DELETE_REPO_GROUP, mockAuditListener.getAuditEvents().get( 2 ).getAction() );
        }
        finally
        {
            mockAuditListener.clearEvents();

            managedRepositoryAdmin.deleteManagedRepository( "test-new-two", getFakeAuditInformation(), true );
        }
    }
}
