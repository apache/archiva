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

import org.apache.archiva.rest.api.model.ManagedRepository;
import org.apache.archiva.rest.api.services.ManagedRepositoriesService;
import org.junit.Test;

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
        assertNotNull( service.getManagedRepository( repo.getId() ) );

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
        repo = service.getManagedRepository( repo.getId() );
        assertNotNull( repo );
        assertEquals( "test", repo.getName() );
        // toto is foo in French :-)
        repo.setName( "toto" );

        service.updateManagedRepository( repo );

        repo = service.getManagedRepository( repo.getId() );
        assertNotNull( repo );
        assertEquals( "toto", repo.getName() );

        service.deleteManagedRepository( repo.getId(), true );
        assertNull( service.getManagedRepository( repo.getId() ) );

    }


}
