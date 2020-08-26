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
import org.apache.archiva.redback.rest.api.services.RoleManagementService;
import org.apache.archiva.rest.api.services.ManagedRepositoriesService;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.apache.commons.io.FileUtils;
import org.apache.maven.wagon.providers.http.HttpWagon;
import org.apache.maven.wagon.repository.Repository;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Olivier Lamy
 */
@RunWith( ArchivaBlockJUnit4ClassRunner.class )
public class DownloadSnapshotTest
    extends AbstractDownloadTest
{
    protected Logger log = LoggerFactory.getLogger( getClass() );

    private static Path appServerBase;
    private Path indexDir;

    @BeforeClass
    public static void setAppServerBase()
        throws IOException
    {
        previousAppServerBase = System.getProperty( "appserver.base" );
        appServerBase = Files.createTempDirectory( "archiva-common-web_appsrv5_" ).toAbsolutePath();
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
        System.out.println( "AppserverBase: " + System.getProperty( "appserver.base" ) );
        return "classpath*:META-INF/spring-context.xml classpath*:spring-context-test-common.xml classpath*:spring-context-artifacts-download.xml";
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


    @Test
    public void downloadSNAPSHOT()
        throws Exception
    {

        String id = Long.toString( System.currentTimeMillis() );
        Path srcRep = getProjectDirectory( ).resolve( "src/test/repositories/snapshot-repo" );
        Path testRep = getBasedir( ).resolve( "target" ).resolve( "snapshot-repo-" + id ).toAbsolutePath();
        FileUtils.copyDirectory( srcRep.toFile( ), testRep.toFile( ) );
        createdPaths.add( testRep );

        ManagedRepository managedRepository = new ManagedRepository( Locale.getDefault());
        managedRepository.setId( id );
        managedRepository.setName( "name of " + id );
        managedRepository.setLocation( testRep.toString() );
        managedRepository.setIndexDirectory( indexDir.resolve( "index-" + id ).toString() );
        managedRepository.setPackedIndexDirectory( indexDir.resolve( "indexpacked-" + id ).toString() );

        ManagedRepositoriesService managedRepositoriesService = getManagedRepositoriesService();

        if ( managedRepositoriesService.getManagedRepository( id ) != null )
        {
            managedRepositoriesService.deleteManagedRepository( id, false );
        }

        getManagedRepositoriesService().addManagedRepository( managedRepository );

        RoleManagementService roleManagementService = getRoleManagementService( authorizationHeader );

        if ( !roleManagementService.templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER,
                                                         id ) )
        {
            roleManagementService.createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, id );
        }

        getUserService( authorizationHeader ).createGuestUser();
        roleManagementService.assignRole( ArchivaRoleConstants.TEMPLATE_GUEST, "guest" );

        roleManagementService.assignTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, id,
                                                   "guest" );

        getUserService( authorizationHeader ).removeFromCache( "guest" );

        Path file = Paths.get( "target/archiva-model-1.4-M4-SNAPSHOT.jar" );
        Files.deleteIfExists(file);

        HttpWagon httpWagon = new HttpWagon();
        httpWagon.connect( new Repository( "foo", "http://localhost:" + port ) );

        httpWagon.get( "/repository/"+ id +"/org/apache/archiva/archiva-model/1.4-M4-SNAPSHOT/archiva-model-1.4-M4-SNAPSHOT.jar", file.toFile() );

        ZipFile zipFile = new ZipFile( file.toFile() );
        List<String> entries = getZipEntriesNames( zipFile );
        ZipEntry zipEntry = zipFile.getEntry( "org/apache/archiva/model/ArchivaArtifact.class" );
        assertNotNull( "cannot find zipEntry org/apache/archiva/model/ArchivaArtifact.class, entries: " + entries + ", content is: "
                           + FileUtils.readFileToString( file.toFile(), Charset.forName( "UTF-8") ), zipEntry );
        zipFile.close();
        file.toFile().deleteOnExit();



    }

}
