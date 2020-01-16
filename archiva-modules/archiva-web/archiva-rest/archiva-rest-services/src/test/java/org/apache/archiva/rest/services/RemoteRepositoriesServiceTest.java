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
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Test;

import javax.ws.rs.ForbiddenException;
import java.util.List;
import java.util.Locale;

/**
 * @author Olivier Lamy
 */
public class RemoteRepositoriesServiceTest
    extends AbstractArchivaRestTest
{


    private void removeRemoteRepositories(String... repos) {
        try {
            RemoteRepositoriesService service = getRemoteRepositoriesService();
            WebClient.client( service ).header( "Authorization", authorizationHeader );
            for (String repo : repos ) {
                try {
                    service.deleteRemoteRepository(repo);
                } catch (Throwable ex) {
                    log.warn("Could not remove repo {}", repo);
                    // ignore
                }
            }
        } catch (Throwable ex) {
            // ignore
        }
    }

    @Test( expected = ForbiddenException.class )
    public void listRemoteRepositoriesKarmaFailed()
        throws Exception
    {
        RemoteRepositoriesService service = getRemoteRepositoriesService();
        try
        {
            assertFalse( service.getRemoteRepositories().isEmpty() );
        }
        catch ( ForbiddenException e )
        {
            assertEquals( 403, e.getResponse().getStatus() );
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
        assertEquals( getRemoteRepository().getDescription(),
                      service.getRemoteRepository( "id-new" ).getDescription() );

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
        repo.setDescription( "foo bar" );

        service.updateRemoteRepository( repo );

        assertEquals( repo.getName(), service.getRemoteRepository( "id-new" ).getName() );
        assertEquals( repo.getUrl(), service.getRemoteRepository( "id-new" ).getUrl() );
        assertEquals( repo.getLayout(), service.getRemoteRepository( "id-new" ).getLayout() );
        assertEquals( repo.getUserName(), service.getRemoteRepository( "id-new" ).getUserName() );
        assertEquals( repo.getPassword(), service.getRemoteRepository( "id-new" ).getPassword() );
        assertEquals( repo.getTimeout(), service.getRemoteRepository( "id-new" ).getTimeout() );
        assertEquals( repo.getDescription(), service.getRemoteRepository( "id-new" ).getDescription() );

        service.deleteRemoteRepository( "id-new" );

        assertNull( service.getRemoteRepository( "id-new" ) );

        assertEquals( initialSize, service.getRemoteRepositories().size() );

    }

    @Test
    public void checkRemoteConnectivity()
            throws Exception {
        try {
            RemoteRepositoriesService service = getRemoteRepositoriesService();

            WebClient.client(service).header("Authorization", authorizationHeader);

            int initialSize = service.getRemoteRepositories().size();

            service.addRemoteRepository(getRemoteRepository());

            assertTrue(service.checkRemoteConnectivity("id-new"));
        } finally {
            removeRemoteRepositories("id-new");
        }

    }

    /*
     * Check maven repository
     */
    @Test
    public void checkRemoteConnectivity2()
            throws Exception {
        try {
            RemoteRepositoriesService service = getRemoteRepositoriesService();

            WebClient.client(service).header("Authorization", authorizationHeader);

            int initialSize = service.getRemoteRepositories().size();

            service.addRemoteRepository(getRemoteMavenRepository());

            assertTrue(service.checkRemoteConnectivity("id-maven1"));
        } finally {
            removeRemoteRepositories("id-maven1");
        }

    }


    /*
     *  Check oracle repository that allows not browsing (MRM-1933)
     */
    @Test
    public void checkRemoteConnectivity3()
            throws Exception {
        try {
            RemoteRepositoriesService service = getRemoteRepositoriesService();

            WebClient.client(service).header("Authorization", authorizationHeader);
            WebClient.client(service).accept("application/json");

            int initialSize = service.getRemoteRepositories().size();

            service.addRemoteRepository(getRemoteOracleRepository());

            assertTrue(service.checkRemoteConnectivity("id-oracle"));
        } finally {
            removeRemoteRepositories("id-oracle");
        }

    }

    RemoteRepository getRemoteRepository()
    {
        return new RemoteRepository( Locale.getDefault( ), "id-new", "new one", "http://www.apache.org", "default", "foo", "foopassword", 120,
                                     "cool repo" );
    }


    RemoteRepository getRemoteMavenRepository()
    {
        return new RemoteRepository( Locale.getDefault( ),"id-maven1", "Maven1", "https://repo.maven.apache.org/maven2", "default", "foo", "foopassword", 120,
                "cool repo3" );
    }


    RemoteRepository getRemoteOracleRepository()
    {
        return new RemoteRepository( Locale.getDefault( ),"id-oracle", "Oracle", "http://download.oracle.com/maven", "default", "foo", "foopassword", 120,
                "cool repo4" );
    }

}
