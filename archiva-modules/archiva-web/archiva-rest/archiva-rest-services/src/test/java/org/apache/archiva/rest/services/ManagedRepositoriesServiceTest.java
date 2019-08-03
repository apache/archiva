package org.apache.archiva.rest.services;

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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.rest.api.model.ArchivaRepositoryStatistics;
import org.apache.archiva.rest.api.services.ManagedRepositoriesService;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * @author Olivier Lamy
 */
public class ManagedRepositoriesServiceTest
    extends AbstractArchivaRestTest
{


    @Test
    public void addManagedRepo()
        throws Exception
    {
        ManagedRepositoriesService service = getManagedRepositoriesService( authorizationHeader );

        ManagedRepository repo = getTestManagedRepository();
        if ( service.getManagedRepository( repo.getId() ) != null )
        {
            service.deleteManagedRepository( repo.getId(), true );
            assertNull( service.getManagedRepository( repo.getId() ) );
        }
        service.addManagedRepository( repo );
        repo = service.getManagedRepository( repo.getId() );
        assertNotNull( repo );

        assertEquals( getTestManagedRepository().getDescription(), repo.getDescription() );

        RepositoriesService repositoriesService = getRepositoriesService( authorizationHeader );

        int timeout = 20000;
        while ( timeout > 0 && repositoriesService.alreadyScanning( repo.getId() ) )
        {
            Thread.sleep( 500 );
            timeout -= 500;
        }

        service.deleteManagedRepository( repo.getId(), true );
        assertNull( service.getManagedRepository( repo.getId() ) );
    }

    @Test
    public void updateManagedRepo()
        throws Exception
    {
        ManagedRepositoriesService service = getManagedRepositoriesService( authorizationHeader );

        ManagedRepository repo = getTestManagedRepository();
        if ( service.getManagedRepository( repo.getId() ) != null )
        {
            service.deleteManagedRepository( repo.getId(), true );
            assertNull( service.getManagedRepository( repo.getId() ) );
        }
        service.addManagedRepository( repo );

        RepositoriesService repositoriesService = getRepositoriesService( authorizationHeader );

        int timeout = 20000;
        while ( timeout > 0 && repositoriesService.alreadyScanning( repo.getId() ) )
        {
            Thread.sleep( 500 );
            timeout -= 500;
        }

        repo = service.getManagedRepository( repo.getId() );
        assertNotNull( repo );
        assertEquals( "test", repo.getName() );
        // toto is foo in French :-)
        repo.setName( "toto" );

        service.updateManagedRepository( repo );

        repo = service.getManagedRepository( repo.getId() );
        assertNotNull( repo );
        assertEquals( "toto", repo.getName() );

        timeout = 20000;
        while ( timeout > 0 && repositoriesService.alreadyScanning( repo.getId() ) )
        {
            Thread.sleep( 500 );
            timeout -= 500;
        }

        service.deleteManagedRepository( repo.getId(), true );
        assertNull( service.getManagedRepository( repo.getId() ) );

    }

    //@Test
    public void fileLocationExists()
        throws Exception
    {
        ManagedRepositoriesService service = getManagedRepositoriesService( authorizationHeader );
        Path target = getProjectDirectory().resolve( "target" );

        assertTrue( service.fileLocationExists( target.toAbsolutePath().toString() ) );

        // normally should not exists :-)
        assertFalse( service.fileLocationExists( "/fooofofof/foddfdofd/dedede/kdeo" ) );

    }

    @Test
    public void getManagedRepositoryStatistics()
        throws Exception
    {

        getArchivaAdministrationService().addFileTypePattern("ignored", ".index-*/**");
        getArchivaAdministrationService().addFileTypePattern("ignored", ".indexer-*/**");
        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        RepositoriesService repositoriesService = getRepositoriesService( authorizationHeader );

        createAndIndexRepo( testRepoId,
                            getProjectDirectory().resolve("src/test/repo-with-osgi" ) );

        repositoriesService.scanRepositoryDirectoriesNow( testRepoId );

        int timeout = 20000;
        while ( timeout > 0 && repositoriesService.alreadyScanning( testRepoId ) )
        {
            Thread.sleep( 500 );
            timeout -= 500;
        }

        ManagedRepositoriesService service = getManagedRepositoriesService( authorizationHeader );

        ArchivaRepositoryStatistics archivaRepositoryStatistics =
            service.getManagedRepositoryStatistics( testRepoId, "en" );

        assertNotNull( archivaRepositoryStatistics );

        log.info( "archivaRepositoryStatistics: {}", archivaRepositoryStatistics.toString() );

        assertEquals( 92, archivaRepositoryStatistics.getNewFileCount() );
        assertEquals( 92, archivaRepositoryStatistics.getTotalFileCount() );

        deleteTestRepo( testRepoId );
    }


}
