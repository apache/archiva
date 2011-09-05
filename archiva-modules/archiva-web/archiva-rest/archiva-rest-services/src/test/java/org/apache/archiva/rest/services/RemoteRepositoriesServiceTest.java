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

import org.apache.archiva.rest.api.model.RemoteRepository;
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


}
