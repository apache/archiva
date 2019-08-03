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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.RedirectionException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * @author Olivier Lamy
 */
@RunWith( ArchivaBlockJUnit4ClassRunner.class )
public class DownloadArtifactFromQueryTest
    extends AbstractDownloadTest
{

    private static Path appServerBase;

    private Path indexDir;

    @BeforeClass
    public static void setAppServerBase()
        throws IOException
    {
        previousAppServerBase = System.getProperty( "appserver.base" );
        appServerBase = Files.createTempDirectory( "archiva-common-web_appsrv1_" ).toAbsolutePath();
        System.setProperty( "appserver.base", appServerBase.toString( ) );
    }

    @AfterClass
    public static void resetAppServerBase()
    {
        if (Files.exists(appServerBase)) {
            FileUtils.deleteQuietly( appServerBase.toFile() );
        }
        System.setProperty( "appserver.base", previousAppServerBase );
    }

    @Override
    protected String getSpringConfigLocation()
    {
        System.out.println( "Appserver base: " + System.getProperty( "appserver.base" ) );
        return "classpath*:META-INF/spring-context.xml classpath*:spring-context-test-common.xml classpath*:spring-context-merge-index-download.xml";
    }

    @Before
    public void init() throws IOException
    {
        indexDir = Files.createTempDirectory( "archiva-web-common-index" );
    }

    @After
    public void cleanup()
        throws Exception
    {
        super.tearDown();
        if ( Files.exists( indexDir ) )
        {
            FileUtils.deleteDirectory( indexDir.toFile() );
        }
    }


    protected String createAndScanRepo()
        throws Exception
    {

        String id = Long.toString( System.currentTimeMillis() );
        Path srcRep = getProjectDirectory( ).resolve( "src/test/repositories/test-repo" );
        Path testRep = getBasedir( ).resolve( "target" ).resolve( "test-repo-" + id ).toAbsolutePath();
        FileUtils.copyDirectory( srcRep.toFile( ), testRep.toFile( ) );
        createdPaths.add( testRep );


        ManagedRepository managedRepository = new ManagedRepository( Locale.getDefault());
        managedRepository.setId( id );
        managedRepository.setName( "name of " + id );
        managedRepository.setLocation( testRep.toString() );
        managedRepository.setIndexDirectory( indexDir.resolve( "index-"+id ).toString());
        managedRepository.setPackedIndexDirectory( indexDir.resolve( "indexpacked-"+id ).toString());

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
