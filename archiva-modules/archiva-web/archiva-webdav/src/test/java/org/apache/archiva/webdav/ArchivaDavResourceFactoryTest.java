package org.apache.archiva.webdav;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.beans.RepositoryGroup;
import org.apache.archiva.admin.model.remote.RemoteRepositoryAdmin;
import org.apache.archiva.admin.repository.DefaultRepositoryCommonValidator;
import org.apache.archiva.admin.repository.group.DefaultRepositoryGroupAdmin;
import org.apache.archiva.admin.repository.managed.DefaultManagedRepositoryAdmin;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.archiva.proxy.DefaultRepositoryProxyConnectors;
import org.apache.archiva.proxy.model.ProxyFetchResult;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RepositoryContentFactory;
import org.apache.archiva.repository.content.maven2.ManagedDefaultRepositoryContent;
import org.apache.archiva.repository.content.maven2.RepositoryRequest;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.easymock.EasyMock.*;

/**
 * ArchivaDavResourceFactoryTest
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class ArchivaDavResourceFactoryTest
    extends TestCase
{
    private static final String RELEASES_REPO = "releases";

    private static final String INTERNAL_REPO = "internal";

    private static final String LOCAL_MIRROR_REPO = "local-mirror";

    private static final String LEGACY_REPO = "legacy-repo";

    private static final String LOCAL_REPO_GROUP = "local";

    private OverridingArchivaDavResourceFactory resourceFactory;

    private IMocksControl requestControl;

    private DavServletRequest request;

    private IMocksControl repoRequestControl;

    private RepositoryRequest repoRequest;

    private IMocksControl responseControl;

    private DavServletResponse response;

    private IMocksControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;

    private Configuration config;

    private IMocksControl repoContentFactoryControl;

    private RepositoryContentFactory repoFactory;

    @Inject
    ApplicationContext applicationContext;

    @Inject
    PlexusSisuBridge plexusSisuBridge;

    @Inject
    DefaultManagedRepositoryAdmin defaultManagedRepositoryAdmin;

    @Inject
    RemoteRepositoryAdmin remoteRepositoryAdmin;


    @Inject
    DefaultRepositoryGroupAdmin defaultRepositoryGroupAdmin;


    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        requestControl = createControl();
        request = requestControl.createMock( DavServletRequest.class );

        responseControl = createControl();
        response = responseControl.createMock( DavServletResponse.class );
        //responseControl.setDefaultMatcher( MockControl.ALWAYS_MATCHER );

        archivaConfigurationControl = createControl();
        archivaConfiguration = archivaConfigurationControl.createMock( ArchivaConfiguration.class );

        config = new Configuration();
        expect( archivaConfiguration.getConfiguration() ).andReturn( config ).times( 5, 20 );
        archivaConfiguration.save( config );

        expectLastCall().times( 1, 4 );
        archivaConfigurationControl.replay();

        defaultManagedRepositoryAdmin.setArchivaConfiguration( archivaConfiguration );
        ( (DefaultRepositoryCommonValidator) defaultManagedRepositoryAdmin.getRepositoryCommonValidator() ).setArchivaConfiguration(
            archivaConfiguration );
        if ( defaultManagedRepositoryAdmin.getManagedRepository( RELEASES_REPO ) == null )
        {
            defaultManagedRepositoryAdmin.addManagedRepository(
                createManagedRepository( RELEASES_REPO, Paths.get( "target/test-classes/" + RELEASES_REPO ).toString(),
                                         "default" ), false, null );
        }
        if ( defaultManagedRepositoryAdmin.getManagedRepository( INTERNAL_REPO ) == null )
        {
            defaultManagedRepositoryAdmin.addManagedRepository(
                createManagedRepository( INTERNAL_REPO, Paths.get( "target/test-classes/" + INTERNAL_REPO ).toString(),
                                         "default" ), false, null );
        }
        RepositoryGroup repoGroupConfig = new RepositoryGroup();
        repoGroupConfig.setId( LOCAL_REPO_GROUP );
        repoGroupConfig.addRepository( RELEASES_REPO );
        repoGroupConfig.addRepository( INTERNAL_REPO );

        defaultRepositoryGroupAdmin.setArchivaConfiguration( archivaConfiguration );
        if ( defaultManagedRepositoryAdmin.getManagedRepository( LOCAL_REPO_GROUP ) == null )
        {
            defaultRepositoryGroupAdmin.addRepositoryGroup( repoGroupConfig, null );
        }

        repoContentFactoryControl = createControl();
        repoFactory = repoContentFactoryControl.createMock( RepositoryContentFactory.class );

        repoRequestControl = createControl();
        repoRequest = repoRequestControl.createMock( RepositoryRequest.class );

        resourceFactory =
            new OverridingArchivaDavResourceFactory( applicationContext, plexusSisuBridge, archivaConfiguration );
        resourceFactory.setArchivaConfiguration( archivaConfiguration );
        resourceFactory.setRepositoryFactory( repoFactory );
        resourceFactory.setRepositoryRequest( repoRequest );
        resourceFactory.setConnectors( new OverridingRepositoryProxyConnectors() );
        resourceFactory.setRemoteRepositoryAdmin( remoteRepositoryAdmin );
        resourceFactory.setManagedRepositoryAdmin( defaultManagedRepositoryAdmin );
    }

    private ManagedRepository createManagedRepository( String id, String location, String layout )
    {
        ManagedRepository repoConfig = new ManagedRepository();
        repoConfig.setId( id );
        repoConfig.setName( id );
        repoConfig.setLocation( location );
        repoConfig.setLayout( layout );

        return repoConfig;
    }

    private ManagedRepositoryContent createManagedRepositoryContent( String repoId )
        throws RepositoryAdminException
    {
        ManagedRepositoryContent repoContent = new ManagedDefaultRepositoryContent();
        repoContent.setRepository( defaultManagedRepositoryAdmin.getManagedRepository( repoId ) );

        return repoContent;
    }

    @After
    @Override
    public void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    // MRM-1232 - Unable to get artifacts from repositories which requires Repository Manager role using repository group
    @Test
    public void testRepositoryGroupFirstRepositoryRequiresAuthentication()
        throws Exception
    {
        DavResourceLocator locator = new ArchivaDavResourceLocator( "", "/repository/" + LOCAL_REPO_GROUP
            + "/org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar", LOCAL_REPO_GROUP,
                                                                    new ArchivaDavLocatorFactory() );

        ManagedRepositoryContent internalRepo = createManagedRepositoryContent( INTERNAL_REPO );
        ManagedRepositoryContent releasesRepo = createManagedRepositoryContent( RELEASES_REPO );

        try
        {
            archivaConfigurationControl.reset();

            expect( archivaConfiguration.getConfiguration() ).andReturn( config ).times( 3 );

            expect( request.getMethod() ).andReturn( "GET" ).times( 3 );

            expect( request.getPathInfo() ).andReturn( "org/apache/archiva" ).times( 0, 2 );

            expect( repoFactory.getManagedRepositoryContent( RELEASES_REPO ) ).andReturn( releasesRepo );

            expect( request.getRemoteAddr() ).andReturn( "http://localhost:8080" ).times( 2 );

            expect( request.getDavSession() ).andReturn( new ArchivaDavSession() ).times( 2 );

            expect( request.getContextPath() ).andReturn( "" ).times( 2 );

            expect( repoRequest.isSupportFile( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ) ).andReturn( true );

            expect(
                repoRequest.isDefault( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ) ).andReturn(
                false );

            expect( repoRequest.toArtifactReference(
                "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ) ).andReturn( null );

            expect( repoRequest.toNativePath( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar",
                                              internalRepo ) ).andReturn(
                Paths.get( config.findManagedRepositoryById( INTERNAL_REPO ).getLocation(),
                          "target/test-classes/internal/org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ).toString());

            expect( repoFactory.getManagedRepositoryContent( INTERNAL_REPO ) ).andReturn( internalRepo );

            expect( repoRequest.isArchetypeCatalog(
                "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ) ).andReturn( false );
            archivaConfigurationControl.replay();
            requestControl.replay();
            repoContentFactoryControl.replay();
            repoRequestControl.replay();

            resourceFactory.createResource( locator, request, response );

            archivaConfigurationControl.verify();
            requestControl.verify();
            repoContentFactoryControl.verify();
            repoRequestControl.verify();

            fail( "A DavException with 401 error code should have been thrown." );
        }
        catch ( DavException e )
        {
            assertEquals( 401, e.getErrorCode() );
        }
    }

    @Test
    public void testRepositoryGroupLastRepositoryRequiresAuthentication()
        throws Exception
    {
        DavResourceLocator locator = new ArchivaDavResourceLocator( "", "/repository/" + LOCAL_REPO_GROUP
            + "/org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar", LOCAL_REPO_GROUP,
                                                                    new ArchivaDavLocatorFactory() );

        List<RepositoryGroupConfiguration> repoGroups = new ArrayList<>();
        RepositoryGroupConfiguration repoGroup = new RepositoryGroupConfiguration();
        repoGroup.setId( LOCAL_REPO_GROUP );
        repoGroup.addRepository( INTERNAL_REPO );
        repoGroup.addRepository( RELEASES_REPO );

        repoGroups.add( repoGroup );

        config.setRepositoryGroups( repoGroups );

        ManagedRepositoryContent internalRepo = createManagedRepositoryContent( INTERNAL_REPO );

        ManagedRepositoryContent releasesRepo = createManagedRepositoryContent( RELEASES_REPO );

        try
        {
            archivaConfigurationControl.reset();

            expect( archivaConfiguration.getConfiguration() ).andReturn( config ).times( 3 );

            expect( request.getMethod() ).andReturn( "GET" ).times( 3 );

            expect( request.getPathInfo() ).andReturn( "org/apache/archiva" ).times( 0, 2 );

            expect( repoFactory.getManagedRepositoryContent( INTERNAL_REPO ) ).andReturn( internalRepo );

            expect( repoFactory.getManagedRepositoryContent( RELEASES_REPO ) ).andReturn( releasesRepo );

            expect( request.getRemoteAddr() ).andReturn( "http://localhost:8080" ).times( 2 );

            expect( request.getDavSession() ).andReturn( new ArchivaDavSession() ).times( 2 );

            expect( request.getContextPath() ).andReturn( "" ).times( 2 );

            expect( repoRequest.isSupportFile( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ) ).andReturn( false );

            expect(
                repoRequest.isDefault( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ) ).andReturn(
                false );

            expect( repoRequest.toArtifactReference(
                "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ) ).andReturn( null );

            expect( repoRequest.toNativePath( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar",
                                              internalRepo ) ).andReturn(
                Paths.get( config.findManagedRepositoryById( INTERNAL_REPO ).getLocation(),
                          "target/test-classes/internal/org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ).toString());


            expect( repoRequest.isArchetypeCatalog(
                "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ) ).andReturn( false );
            archivaConfigurationControl.replay();
            requestControl.replay();
            repoContentFactoryControl.replay();
            repoRequestControl.replay();

            resourceFactory.createResource( locator, request, response );

            archivaConfigurationControl.verify();
            requestControl.verify();
            repoContentFactoryControl.verify();
            repoRequestControl.verify();

            fail( "A DavException with 401 error code should have been thrown." );
        }
        catch ( DavException e )
        {
            assertEquals( 401, e.getErrorCode() );
        }
    }

    @Test
    public void testRepositoryGroupArtifactDoesNotExistInAnyOfTheReposAuthenticationDisabled()
        throws Exception
    {
        DavResourceLocator locator = new ArchivaDavResourceLocator( "", "/repository/" + LOCAL_REPO_GROUP
            + "/org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar", LOCAL_REPO_GROUP,
                                                                    new ArchivaDavLocatorFactory() );

        defaultManagedRepositoryAdmin.addManagedRepository(
            createManagedRepository( LOCAL_MIRROR_REPO, Paths.get( "target/test-classes/local-mirror" ).toString(),
                                     "default" ), false, null );

        List<RepositoryGroupConfiguration> repoGroups = new ArrayList<>();
        RepositoryGroupConfiguration repoGroup = new RepositoryGroupConfiguration();
        repoGroup.setId( LOCAL_REPO_GROUP );
        repoGroup.addRepository( INTERNAL_REPO );
        repoGroup.addRepository( LOCAL_MIRROR_REPO );

        repoGroups.add( repoGroup );

        config.setRepositoryGroups( repoGroups );

        ManagedRepositoryContent internalRepo = createManagedRepositoryContent( INTERNAL_REPO );
        ManagedRepositoryContent localMirrorRepo = createManagedRepositoryContent( LOCAL_MIRROR_REPO );

        try
        {
            archivaConfigurationControl.reset();

            expect( archivaConfiguration.getConfiguration() ).andReturn( config ).times( 3 );

            expect( request.getMethod() ).andReturn( "GET" ).times( 5 );

            expect( request.getPathInfo() ).andReturn( "org/apache/archiva" ).times( 0, 2 );

            expect( repoFactory.getManagedRepositoryContent( INTERNAL_REPO ) ).andReturn( internalRepo );

            expect( repoFactory.getManagedRepositoryContent( LOCAL_MIRROR_REPO ) ).andReturn( localMirrorRepo );

            expect( request.getRemoteAddr() ).andReturn( "http://localhost:8080" ).times( 4 );

            expect( request.getDavSession() ).andReturn( new ArchivaDavSession() ).times( 4 );

            expect( request.getContextPath() ).andReturn( "" ).times( 2 );

            expect( repoRequest.isSupportFile( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ) ).andReturn( false ).times( 2 );

            expect(
                repoRequest.isDefault( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ) ).andReturn(
                false ).times( 2 );

            expect( repoRequest.toArtifactReference(
                "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ) ).andReturn( null ).times( 2 );

            expect( repoRequest.toNativePath( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar",
                                              internalRepo ) ).andReturn(
                Paths.get( config.findManagedRepositoryById( INTERNAL_REPO ).getLocation(),
                          "target/test-classes/internal/org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ).toString() );

            expect( repoRequest.toNativePath( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar",
                                              localMirrorRepo ) )
                .andReturn( Paths.get( config.findManagedRepositoryById( LOCAL_MIRROR_REPO ).getLocation(),
                                      "target/test-classes/internal/org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ).toString());

            expect( repoRequest.isArchetypeCatalog( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ) ).andReturn( false ).times( 2 );
            archivaConfigurationControl.replay();
            requestControl.replay();
            repoContentFactoryControl.replay();
            repoRequestControl.replay();

            resourceFactory.createResource( locator, request, response );

            archivaConfigurationControl.verify();
            requestControl.verify();
            repoContentFactoryControl.verify();
            repoRequestControl.verify();

            fail( "A DavException with 404 error code should have been thrown." );
        }
        catch ( DavException e )
        {
            assertEquals( 404, e.getErrorCode() );
        }
    }

    // MRM-1239
    @Test
    public void testRequestArtifactMetadataThreePartsRepoHasDefaultLayout()
        throws Exception
    {
        // should fetch metadata 
        DavResourceLocator locator =
            new ArchivaDavResourceLocator( "", "/repository/" + INTERNAL_REPO + "/eclipse/jdtcore/maven-metadata.xml",
                                           INTERNAL_REPO, new ArchivaDavLocatorFactory() );

        ManagedRepositoryContent internalRepo = createManagedRepositoryContent( INTERNAL_REPO );

        // use actual object (this performs the isMetadata, isDefault and isSupportFile check!)
        RepositoryRequest repoRequest = new RepositoryRequest( );
        resourceFactory.setRepositoryRequest( repoRequest );

        try
        {
            archivaConfigurationControl.reset();

            expect( archivaConfiguration.getConfiguration() ).andReturn( config ).times( 2 );

            expect( repoFactory.getManagedRepositoryContent( INTERNAL_REPO ) ).andReturn( internalRepo );

            expect( request.getMethod() ).andReturn( "GET" ).times( 4 );

            expect( request.getRemoteAddr() ).andReturn( "http://localhost:8080" ).times( 3 );

            expect( request.getContextPath() ).andReturn( "" ).times( 1 );

            expect( request.getDavSession() ).andReturn( new ArchivaDavSession() ).times( 2 );

            expect( request.getRequestURI() ).andReturn( "http://localhost:8080/archiva/repository/" + INTERNAL_REPO + "/eclipse/jdtcore/maven-metadata.xml" );
            response.setHeader( "Pragma", "no-cache" );

            expectLastCall();

            response.setHeader( "Cache-Control", "no-cache" );

            expectLastCall();

            response.setDateHeader( eq("Last-Modified"), anyLong() );
            expectLastCall();

            archivaConfigurationControl.replay();
            repoContentFactoryControl.replay();
            requestControl.replay();
            responseControl.replay();

            resourceFactory.createResource( locator, request, response );

            archivaConfigurationControl.verify();
            repoContentFactoryControl.verify();
            requestControl.verify();
            responseControl.verify();
        }
        catch ( DavException e )
        {
            fail( "A DavException should not have been thrown!" );
        }
    }

    @Test
    public void testRequestArtifactMetadataTwoPartsRepoHasDefaultLayout()
        throws Exception
    {
        // should not fetch metadata
        DavResourceLocator locator =
            new ArchivaDavResourceLocator( "", "/repository/" + INTERNAL_REPO + "/eclipse/maven-metadata.xml",
                                           INTERNAL_REPO, new ArchivaDavLocatorFactory() );

        ManagedRepositoryContent internalRepo = createManagedRepositoryContent( INTERNAL_REPO );

        // use actual object (this performs the isMetadata, isDefault and isSupportFile check!)
        RepositoryRequest repoRequest = new RepositoryRequest( );
        resourceFactory.setRepositoryRequest( repoRequest );

        try
        {
            archivaConfigurationControl.reset();

            expect( archivaConfiguration.getConfiguration() ).andReturn( config ).times( 2 );

            expect( repoFactory.getManagedRepositoryContent( INTERNAL_REPO ) ).andReturn( internalRepo );

            expect( request.getMethod() ).andReturn( "GET" ).times( 3 );

            expect( request.getRemoteAddr() ).andReturn( "http://localhost:8080" ).times( 3 );

            expect( request.getDavSession() ).andReturn( new ArchivaDavSession() ).times( 2 );

            expect( request.getContextPath() ).andReturn( "" ).times( 2 );

            archivaConfigurationControl.replay();
            repoContentFactoryControl.replay();
            requestControl.replay();

            resourceFactory.createResource( locator, request, response );

            archivaConfigurationControl.verify();
            repoContentFactoryControl.verify();
            requestControl.verify();

            fail( "A 404 error should have been thrown!" );
        }
        catch ( DavException e )
        {
            assertEquals( 404, e.getErrorCode() );
        }
    }

    @Test
    public void testRequestMetadataRepoIsLegacy()
        throws Exception
    {
        defaultManagedRepositoryAdmin.addManagedRepository(
            createManagedRepository( LEGACY_REPO, Paths.get( "target/test-classes/" + LEGACY_REPO ).toString(),
                                     "legacy" ), false, null );
        DavResourceLocator locator =
            new ArchivaDavResourceLocator( "", "/repository/" + LEGACY_REPO + "/eclipse/maven-metadata.xml",
                                           LEGACY_REPO, new ArchivaDavLocatorFactory() );

        ManagedRepositoryContent legacyRepo = createManagedRepositoryContent( LEGACY_REPO );

        // use actual object (this performs the isMetadata, isDefault and isSupportFile check!)
        RepositoryRequest repoRequest = new RepositoryRequest( );
        resourceFactory.setRepositoryRequest( repoRequest );

        try
        {
            archivaConfigurationControl.reset();

            expect( archivaConfiguration.getConfiguration() ).andReturn( config ).times( 2 );

            expect( repoFactory.getManagedRepositoryContent( LEGACY_REPO ) ).andReturn( legacyRepo );

            expect( request.getMethod() ).andReturn( "GET" ).times( 3 );

            expect( request.getRemoteAddr() ).andReturn( "http://localhost:8080" ).times( 3 );

            expect( request.getDavSession() ).andReturn( new ArchivaDavSession() ).times( 2 );

            expect( request.getContextPath() ).andReturn( "" ).times( 2 );

            archivaConfigurationControl.replay();
            repoContentFactoryControl.replay();
            requestControl.replay();

            resourceFactory.createResource( locator, request, response );

            archivaConfigurationControl.verify();
            repoContentFactoryControl.verify();
            requestControl.verify();

            fail( "A 404 error should have been thrown!" );
        }
        catch ( DavException e )
        {
            assertEquals( 404, e.getErrorCode() );
        }
    }

    class OverridingArchivaDavResourceFactory
        extends ArchivaDavResourceFactory
    {

        OverridingArchivaDavResourceFactory( ApplicationContext applicationContext, PlexusSisuBridge plexusSisuBridge,
                                             ArchivaConfiguration archivaConfiguration )
            throws PlexusSisuBridgeException
        {
            super( applicationContext, plexusSisuBridge, archivaConfiguration );
        }

        @Override
        protected boolean isAuthorized( DavServletRequest request, String repositoryId )
            throws DavException
        {
            if ( RELEASES_REPO.equals( repositoryId ) )
            {
                throw new UnauthorizedDavException( repositoryId,
                                                    "You are not authenticated and authorized to access any repository." );
            }
            else
            {
                return true;
            }
        }

        @Override
        protected String getActivePrincipal( DavServletRequest request )
        {
            return "guest";
        }
    }

    class OverridingRepositoryProxyConnectors
        extends DefaultRepositoryProxyConnectors
    {
        @Override
        public ProxyFetchResult fetchMetadataFromProxies( ManagedRepositoryContent repository, String logicalPath )
        {
            Path target = Paths.get(repository.getRepoRoot(), logicalPath );
            try
            {
                FileUtils.copyFile( Paths.get( "target/test-classes/maven-metadata.xml" ).toFile(), target.toFile() );
            }
            catch ( IOException e )
            {

            }

            return new ProxyFetchResult( target, true );
        }
    }
}
