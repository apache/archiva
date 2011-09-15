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

import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.rest.api.services.RemoteRepositoriesService;
import org.apache.cxf.jaxrs.client.ServerWebApplicationException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Test;

import java.util.List;

/**
 * @author Olivier Lamy
 */
public class RemoteRepositoriesServiceTest
    extends AbstractArchivaRestTest
{


    @Test( expected = ServerWebApplicationException.class )
    public void listRemoteRepositoriesKarmaFailed()
        throws Exception
    {
        RemoteRepositoriesService service = getRemoteRepositoriesService();
        try
        {
            assertFalse( service.getRemoteRepositories().isEmpty() );
        }
        catch ( ServerWebApplicationException e )
        {
            assertEquals( 403, e.getStatus() );
            throw e;
        }
    }

    @Test
    public void listRemoteRepositoriesKarma()
        throws Exception
    {
        RemoteRepositoriesService service = getRemoteRepositoriesService();

        WebClient.client( service ).header( "Authorization", authorizationHeader );
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 300000 );
        List<RemoteRepository> repos = service.getRemoteRepositories();
        assertFalse( repos.isEmpty() );
        log.info( "repos {}", repos );

    }

    @Test
    public void addAndDeleteRemoteRepository()
        throws Exception
    {
        RemoteRepositoriesService service = getRemoteRepositoriesService();

        WebClient.client( service ).header( "Authorization", authorizationHeader );

        int initialSize = service.getRemoteRepositories().size();

        service.addRemoteRepository( getRemoteRepository() );

        assertNotNull( service.getRemoteRepository( "id-new" ) );

        assertEquals( getRemoteRepository().getName(), service.getRemoteRepository( "id-new" ).getName() );
        assertEquals( getRemoteRepository().getUrl(), service.getRemoteRepository( "id-new" ).getUrl() );
        assertEquals( getRemoteRepository().getLayout(), service.getRemoteRepository( "id-new" ).getLayout() );
        assertEquals( getRemoteRepository().getUserName(), service.getRemoteRepository( "id-new" ).getUserName() );
        assertEquals( getRemoteRepository().getPassword(), service.getRemoteRepository( "id-new" ).getPassword() );
        assertEquals( getRemoteRepository().getTimeout(), service.getRemoteRepository( "id-new" ).getTimeout() );

        assertEquals( initialSize + 1, service.getRemoteRepositories().size() );

        service.deleteRemoteRepository( "id-new" );

        assertNull( service.getRemoteRepository( "id-new" ) );

        assertEquals( initialSize, service.getRemoteRepositories().size() );

    }

    @Test
    public void addAndUpdateAndDeleteRemoteRepository()
        throws Exception
    {
        RemoteRepositoriesService service = getRemoteRepositoriesService();

        WebClient.client( service ).header( "Authorization", authorizationHeader );

        int initialSize = service.getRemoteRepositories().size();

        service.addRemoteRepository( getRemoteRepository() );

        assertNotNull( service.getRemoteRepository( "id-new" ) );

        assertEquals( getRemoteRepository().getName(), service.getRemoteRepository( "id-new" ).getName() );
        assertEquals( getRemoteRepository().getUrl(), service.getRemoteRepository( "id-new" ).getUrl() );
        assertEquals( getRemoteRepository().getLayout(), service.getRemoteRepository( "id-new" ).getLayout() );
        assertEquals( getRemoteRepository().getUserName(), service.getRemoteRepository( "id-new" ).getUserName() );
        assertEquals( getRemoteRepository().getPassword(), service.getRemoteRepository( "id-new" ).getPassword() );
        assertEquals( getRemoteRepository().getTimeout(), service.getRemoteRepository( "id-new" ).getTimeout() );

        assertEquals( initialSize + 1, service.getRemoteRepositories().size() );

        RemoteRepository repo = getRemoteRepository();
        repo.setName( "name changed" );
        repo.setPassword( "new password" );
        repo.setUserName( "new username" );
        repo.setUrl( "http://foo.new.org" );

        service.updateRemoteRepository( repo );

        assertEquals( repo.getName(), service.getRemoteRepository( "id-new" ).getName() );
        assertEquals( repo.getUrl(), service.getRemoteRepository( "id-new" ).getUrl() );
        assertEquals( repo.getLayout(), service.getRemoteRepository( "id-new" ).getLayout() );
        assertEquals( repo.getUserName(), service.getRemoteRepository( "id-new" ).getUserName() );
        assertEquals( repo.getPassword(), service.getRemoteRepository( "id-new" ).getPassword() );
        assertEquals( repo.getTimeout(), service.getRemoteRepository( "id-new" ).getTimeout() );

        service.deleteRemoteRepository( "id-new" );

        assertNull( service.getRemoteRepository( "id-new" ) );

        assertEquals( initialSize, service.getRemoteRepositories().size() );

    }

    RemoteRepository getRemoteRepository()
    {
        return new RemoteRepository( "id-new", "new one", "http://foo.com", "default", "foo", "foopassword", 120 );
    }


}
