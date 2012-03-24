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

import org.apache.archiva.rest.api.services.BrowseService;
import org.fest.assertions.MapAssert;
import org.junit.Test;

import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Olivier Lamy
 */
public class BrowseServiceTest
    extends AbstractArchivaRestTest
{
    @Test
    public void metadatagetthenadd()
        throws Exception
    {

        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, "src/test/repo-with-osgi" );

        BrowseService browseService = getBrowseService( authorizationHeader );

        Map<String, String> metadatas = browseService.getMetadatas( "commons-cli", "commons-cli", "1.0", testRepoId );

        assertThat( metadatas ).isNotNull().isEmpty();

        browseService.addMetadata( "commons-cli", "commons-cli", "1.0", "wine", "bordeaux", testRepoId );

        metadatas = browseService.getMetadatas( "commons-cli", "commons-cli", "1.0", testRepoId );

        assertThat( metadatas ).isNotNull().isNotEmpty().includes( MapAssert.entry( "wine", "bordeaux" ) );

        deleteTestRepo( testRepoId );

    }


    @Test
    public void metadatagetthenaddthendelete()
        throws Exception
    {

        String testRepoId = "test-repo";
        // force guest user creation if not exists
        if ( getUserService( authorizationHeader ).getGuestUser() == null )
        {
            assertNotNull( getUserService( authorizationHeader ).createGuestUser() );
        }

        createAndIndexRepo( testRepoId, "src/test/repo-with-osgi" );

        BrowseService browseService = getBrowseService( authorizationHeader );

        Map<String, String> metadatas = browseService.getMetadatas( "commons-cli", "commons-cli", "1.0", testRepoId );

        assertThat( metadatas ).isNotNull().isEmpty();

        browseService.addMetadata( "commons-cli", "commons-cli", "1.0", "wine", "bordeaux", testRepoId );

        metadatas = browseService.getMetadatas( "commons-cli", "commons-cli", "1.0", testRepoId );

        assertThat( metadatas ).isNotNull().isNotEmpty().includes( MapAssert.entry( "wine", "bordeaux" ) );

        browseService.deleteMetadata( "commons-cli", "commons-cli", "1.0", "wine", testRepoId );

        metadatas = browseService.getMetadatas( "commons-cli", "commons-cli", "1.0", testRepoId );

        assertThat( metadatas ).isNotNull().isEmpty();

        deleteTestRepo( testRepoId );

    }
}
