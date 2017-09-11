package org.apache.archiva.remotedownload;
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
import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.*;
import org.junit.runner.RunWith;

import javax.ws.rs.RedirectionException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Olivier Lamy
 */
@RunWith( ArchivaBlockJUnit4ClassRunner.class )
public class DownloadArtifactFromQueryTest
    extends AbstractDownloadTest
{

    @BeforeClass
    public static void setAppServerBase()
        throws IOException
    {
        previousAppServerBase = System.getProperty( "appserver.base" );
        System.setProperty( "appserver.base",
                            Paths.get( System.getProperty( "java.io.tmpdir" ) ).toAbsolutePath().resolve("target")
                                .resolve(DownloadArtifactFromQueryTest.class.getName() ).toString());
    }

    @AfterClass
    public static void resetAppServerBase()
    {
        System.setProperty( "appserver.base", previousAppServerBase );
    }

    @Override
    protected String getSpringConfigLocation()
    {
        return "classpath*:META-INF/spring-context.xml classpath*:spring-context-test-common.xml classpath*:spring-context-merge-index-download.xml";
    }

    @After
    public void cleanup()
        throws Exception
    {
        super.tearDown();
        Path tmpIndexDir = Paths.get( System.getProperty( "java.io.tmpdir" ), "tmpIndex" );
        if ( Files.exists( tmpIndexDir ) )
        {
            FileUtils.deleteDirectory( tmpIndexDir.toFile() );
        }
    }


    protected String createAndScanRepo()
        throws Exception
    {

        Path tmpIndexDir = Paths.get( System.getProperty( "java.io.tmpdir" ), "tmpIndex" );
        if ( Files.exists( tmpIndexDir ) )
        {
            FileUtils.deleteDirectory( tmpIndexDir.toFile() );
        }
        String id = Long.toString( System.currentTimeMillis() );
        ManagedRepository managedRepository = new ManagedRepository();
        managedRepository.setId( id );
        managedRepository.setName( "name of " + id );
        managedRepository.setLocation( System.getProperty( "basedir" ) + "/src/test/repositories/test-repo" );
        managedRepository.setIndexDirectory( System.getProperty( "java.io.tmpdir" ) + "/tmpIndex/" + id );

        ManagedRepositoriesService managedRepositoriesService = getManagedRepositoriesService();

        if ( managedRepositoriesService.getManagedRepository( id ) != null )
        {
            managedRepositoriesService.deleteManagedRepository( id, false );
        }

        getManagedRepositoriesService().addManagedRepository( managedRepository );

        RepositoriesService repositoriesService = getRepositoriesService();

        repositoriesService.scanRepositoryNow( id, true );

        // wait a bit to ensure index is finished
        int timeout = 20000;
        while ( timeout > 0 && repositoriesService.alreadyScanning( id ) )
        {
            Thread.sleep( 500 );
            timeout -= 500;
        }

        return id;

    }

    @Test( expected = RedirectionException.class )
    public void downloadFixedVersion()
        throws Exception
    {

        String id = createAndScanRepo();

        try
        {
            Response response =
                getSearchService().redirectToArtifactFile( null, "org.apache.archiva", "archiva-test", "1.0", null,
                                                           null );

        }
        catch ( RedirectionException e )
        {
            Assertions.assertThat( e.getLocation().compareTo( new URI( "http://localhost:" + port + "/repository/" + id
                                                                           + "/org/apache/archiva/archiva-test/1.0/archiva-test-1.0.jar" ) ) ).isEqualTo(
                0 );
            throw e;
        }
        finally
        {
            getManagedRepositoriesService().deleteManagedRepository( id, false );
        }

    }


    @Test( expected = RedirectionException.class )
    public void downloadLatestVersion()
        throws Exception
    {
        String id = createAndScanRepo();

        try
        {
            Response response =
                getSearchService().redirectToArtifactFile( null, "org.apache.archiva", "archiva-test", "LATEST", null,
                                                           null );

        }
        catch ( RedirectionException e )
        {
            Assertions.assertThat( e.getLocation().compareTo( new URI( "http://localhost:" + port + "/repository/" + id
                                                                           + "/org/apache/archiva/archiva-test/2.0/archiva-test-2.0.jar" ) ) ).isEqualTo(
                0 );
            throw e;
        }
        finally
        {
            getManagedRepositoriesService().deleteManagedRepository( id, false );
        }

    }

    @Test
    public void download_no_content()
        throws Exception
    {
        String id = createAndScanRepo();

        try
        {
            Response response =
                getSearchService().redirectToArtifactFile( null, "org.apache.archiva.beer", "archiva-wine", "LATEST",
                                                           null, null );

            Assert.assertEquals( Response.Status.NO_CONTENT.getStatusCode(), response.getStatus() );


        }
        finally
        {
            getManagedRepositoriesService().deleteManagedRepository( id, false );
        }

    }
}
