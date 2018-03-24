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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.beans.RepositoryGroup;
import org.apache.archiva.admin.model.group.RepositoryGroupAdmin;
import org.apache.archiva.admin.repository.AbstractRepositoryAdminTest;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryRegistry;
import org.junit.Test;

import javax.inject.Inject;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * @author Olivier Lamy
 */
public class RepositoryGroupAdminTest
    extends AbstractRepositoryAdminTest
{
    @Inject
    RepositoryGroupAdmin repositoryGroupAdmin;

    @Inject
    RepositoryRegistry repositoryRegistry;

    @Test
    public void addAndDeleteGroup()
        throws Exception
    {
        try
        {
            Repository repo = repositoryRegistry.getRepository("test-new-one");
            if (repo!=null) {
                repositoryRegistry.removeRepository(repo);
            }
            repo = repositoryRegistry.getRepository("test-new-two");
            if (repo!=null) {
                repositoryRegistry.removeRepository(repo);
            }
            ManagedRepository managedRepositoryOne =
                getTestManagedRepository( "test-new-one", Paths.get(APPSERVER_BASE_PATH,"test-new-one" ).toString());

            ManagedRepository managedRepositoryTwo =
                getTestManagedRepository( "test-new-two", Paths.get(APPSERVER_BASE_PATH, "test-new-two" ).toString());

            managedRepositoryAdmin.addManagedRepository( managedRepositoryOne, false, getFakeAuditInformation() );

            managedRepositoryAdmin.addManagedRepository( managedRepositoryTwo, false, getFakeAuditInformation() );


            RepositoryGroup repositoryGroup =
                new RepositoryGroup( "repo-group-one", Arrays.asList( "test-new-one", "test-new-two" ) );
            // repositoryGroupAdmin.deleteRepositoryGroup("repo-group-one", null);

            mockAuditListener.clearEvents();

            repositoryGroupAdmin.addRepositoryGroup( repositoryGroup, getFakeAuditInformation() );

            assertEquals( 1, repositoryGroupAdmin.getRepositoriesGroups().size() );
            assertEquals( "repo-group-one", repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getId() );
            assertEquals( 2, repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getRepositories().size() );
            assertEquals( Arrays.asList( "test-new-one", "test-new-two" ),
                          repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getRepositories() );

            // verify if default values were saved
            assertEquals(30, repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getMergedIndexTtl() );
            assertEquals(".indexer", repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getMergedIndexPath() );

            repositoryGroupAdmin.deleteRepositoryGroup( "repo-group-one", getFakeAuditInformation() );

            assertEquals( 0, repositoryGroupAdmin.getRepositoriesGroups().size() );

            assertEquals( 2, mockAuditListener.getAuditEvents().size() );

            assertEquals( AuditEvent.ADD_REPO_GROUP, mockAuditListener.getAuditEvents().get( 0 ).getAction() );
            assertEquals( AuditEvent.DELETE_REPO_GROUP, mockAuditListener.getAuditEvents().get( 1 ).getAction() );
        }
        finally
        {
            mockAuditListener.clearEvents();
            repositoryRegistry.removeRepository(repositoryRegistry.getManagedRepository("test-new-one"));
            repositoryRegistry.removeRepository(repositoryRegistry.getManagedRepository("test-new-two"));
        }
    }

    @Test
    public void addAndUpdateAndDeleteGroup()
        throws Exception
    {
        try
        {
            ManagedRepository managedRepositoryOne =
                getTestManagedRepository( "test-new-one", Paths.get(APPSERVER_BASE_PATH,"test-new-one" ).toString());

            ManagedRepository managedRepositoryTwo =
                getTestManagedRepository( "test-new-two", Paths.get(APPSERVER_BASE_PATH, "test-new-two" ).toString());

            managedRepositoryAdmin.addManagedRepository( managedRepositoryOne, false, getFakeAuditInformation() );

            managedRepositoryAdmin.addManagedRepository( managedRepositoryTwo, false, getFakeAuditInformation() );

            RepositoryGroup repositoryGroup = new RepositoryGroup( "repo-group-one", Arrays.asList( "test-new-one" ) )
                    .mergedIndexTtl( 20 ).mergedIndexPath( "/.nonDefaultPath" );

            mockAuditListener.clearEvents();

            repositoryGroupAdmin.addRepositoryGroup( repositoryGroup, getFakeAuditInformation() );

            assertEquals( 1, repositoryGroupAdmin.getRepositoriesGroups().size() );
            assertEquals( "repo-group-one", repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getId() );
            assertEquals( 1, repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getRepositories().size() );
            assertEquals( Arrays.asList( "test-new-one" ),
                          repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getRepositories() );
            assertEquals( 20, repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getMergedIndexTtl() );
            assertEquals( "/.nonDefaultPath", repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getMergedIndexPath() );

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
    public void addAndDeleteGroupWithRemovedManagedRepo()
        throws Exception
    {
        try
        {
            ManagedRepository managedRepositoryOne =
                getTestManagedRepository( "test-new-one", Paths.get(APPSERVER_BASE_PATH , "test-new-one" ).toString());

            ManagedRepository managedRepositoryTwo =
                getTestManagedRepository( "test-new-two", Paths.get(APPSERVER_BASE_PATH ,"test-new-two" ).toString());

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
            repositoryRegistry.removeRepository(repositoryRegistry.getRepository("test-new-two"));
        }
    }

    @Test( expected = RepositoryAdminException.class )
    public void testAddGroupWithInvalidMergedIndexTtl() throws Exception {
        try {
            ManagedRepository managedRepositoryOne =
                    getTestManagedRepository( "test-new-one", Paths.get(APPSERVER_BASE_PATH , "test-new-one" ).toString());

            ManagedRepository managedRepositoryTwo =
                    getTestManagedRepository( "test-new-two", Paths.get(APPSERVER_BASE_PATH , "test-new-two" ).toString());

            managedRepositoryAdmin.addManagedRepository( managedRepositoryOne, false, getFakeAuditInformation() );

            managedRepositoryAdmin.addManagedRepository( managedRepositoryTwo, false, getFakeAuditInformation() );

            RepositoryGroup repositoryGroup =
                    new RepositoryGroup( "repo-group-one", Arrays.asList( "test-new-one", "test-new-two" ) )
                    .mergedIndexTtl( -1 );

            mockAuditListener.clearEvents();

            repositoryGroupAdmin.addRepositoryGroup( repositoryGroup, getFakeAuditInformation() );
        }
        finally
        {
            mockAuditListener.clearEvents();
            repositoryRegistry.removeRepository(repositoryRegistry.getRepository("test-new-one"));
            repositoryRegistry.removeRepository(repositoryRegistry.getRepository("test-new-two"));
        }
    }

    @Test( expected = RepositoryAdminException.class )
    public void testAddAndUpdateGroupWithInvalidMergedIndexTtl() throws Exception {
        try {
            ManagedRepository managedRepositoryOne =
                    getTestManagedRepository( "test-new-one", Paths.get(APPSERVER_BASE_PATH , "test-new-one" ).toString());

            ManagedRepository managedRepositoryTwo =
                    getTestManagedRepository( "test-new-two", Paths.get(APPSERVER_BASE_PATH , "test-new-two" ).toString());

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

            // verify if default values were saved
            assertEquals(30, repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getMergedIndexTtl() );
            assertEquals(".indexer", repositoryGroupAdmin.getRepositoriesGroups().get( 0 ).getMergedIndexPath() );

            repositoryGroup = repositoryGroupAdmin.getRepositoryGroup( "repo-group-one" );
            assertNotNull( repositoryGroup );

            repositoryGroup.mergedIndexTtl( -1 );

            repositoryGroupAdmin.updateRepositoryGroup( repositoryGroup, getFakeAuditInformation() );
        }
        finally
        {
            mockAuditListener.clearEvents();
            managedRepositoryAdmin.deleteManagedRepository( "test-new-one", getFakeAuditInformation(), true );
            managedRepositoryAdmin.deleteManagedRepository( "test-new-two", getFakeAuditInformation(), true );
        }
    }
}
