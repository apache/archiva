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
import org.apache.archiva.rest.api.services.ManagedRepositoriesService;
import org.apache.archiva.rest.api.services.RepositoriesService;
import org.apache.cxf.jaxrs.client.ServerWebApplicationException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.maven.archiva.common.utils.FileUtil;
import org.junit.Test;

import java.io.File;

/**
 * @author Olivier Lamy
 */
public class RepositoriesServiceTest
    extends AbstractArchivaRestTest
{

    @Test( expected = ServerWebApplicationException.class )
    public void scanRepoKarmaFailed()
        throws Exception
    {
        RepositoriesService service = getRepositoriesService();
        try
        {
            service.scanRepository( "id", true );
        }
        catch ( ServerWebApplicationException e )
        {
            assertEquals( 403, e.getStatus() );
            throw e;
        }
    }

    @Test
    public void scanRepo()
        throws Exception
    {
        RepositoriesService service = getRepositoriesService();
        WebClient.client( service ).header( "Authorization", authorizationHeader );
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( 300000 );

        ManagedRepositoriesService managedRepositoriesService = getManagedRepositoriesService( authorizationHeader );

        WebClient.getConfig( managedRepositoriesService ).getHttpConduit().getClient().setReceiveTimeout( 300000 );

        String repoId = managedRepositoriesService.getManagedRepositories().get( 0 ).getId();

        assertTrue( service.scanRepository( repoId, true ) );

        log.info( "scanRepo call ok " );

        assertTrue( service.alreadyScanning( repoId ) );

    }


    protected ManagedRepository getTestManagedRepository()
    {
        String location = new File( FileUtil.getBasedir(), "target/test-repo" ).getAbsolutePath();
        return new ManagedRepository( "TEST", "test", location, "default", true, true, true, "2 * * * * ?", null, false,
                                      80, 80, true, false );
    }

}
