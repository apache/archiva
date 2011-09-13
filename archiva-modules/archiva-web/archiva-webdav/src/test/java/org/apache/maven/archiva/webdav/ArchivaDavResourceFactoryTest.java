package org.apache.maven.archiva.webdav;

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
import org.apache.archiva.admin.model.group.RepositoryGroup;
import org.apache.archiva.admin.model.managed.ManagedRepository;
import org.apache.archiva.admin.repository.group.DefaultRepositoryGroupAdmin;
import org.apache.archiva.admin.repository.managed.DefaultManagedRepositoryAdmin;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.maven.archiva.proxy.DefaultRepositoryProxyConnectors;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.content.LegacyPathParser;
import org.apache.maven.archiva.repository.content.ManagedDefaultRepositoryContent;
import org.apache.maven.archiva.repository.content.RepositoryRequest;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ArchivaDavResourceFactoryTest
 */
@RunWith( SpringJUnit4ClassRunner.class )
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

    private MockControl requestControl;

    private DavServletRequest request;

    private MockControl repoRequestControl;

    private RepositoryRequest repoRequest;

    private MockControl responseControl;

    private DavServletResponse response;

    private MockControl archivaConfigurationControl;

    private ArchivaConfiguration archivaConfiguration;

    private Configuration config;

    private MockControl repoContentFactoryControl;

    private RepositoryContentFactory repoFactory;

    @Inject
    ApplicationContext applicationContext;

    @Inject
    PlexusSisuBridge plexusSisuBridge;

    @Inject
    DefaultManagedRepositoryAdmin defaultManagedRepositoryAdmin;

    @Inject
    DefaultRepositoryGroupAdmin defaultRepositoryGroupAdmin;


    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        requestControl = MockControl.createControl( DavServletRequest.class );
        request = (DavServletRequest) requestControl.getMock();

        responseControl = MockControl.createControl( DavServletResponse.class );
        response = (DavServletResponse) responseControl.getMock();
        responseControl.setDefaultMatcher( MockControl.ALWAYS_MATCHER );

        archivaConfigurationControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfiguration = (ArchivaConfiguration) archivaConfigurationControl.getMock();

        config = new Configuration();
        archivaConfiguration.getConfiguration();
        archivaConfigurationControl.setReturnValue( config, 5, 20 );
        archivaConfiguration.save( config );
        archivaConfigurationControl.setVoidCallable( 1, 4 );
        archivaConfigurationControl.replay();

        defaultManagedRepositoryAdmin.setArchivaConfiguration( archivaConfiguration );
        defaultManagedRepositoryAdmin.getRepositoryCommonValidator().setArchivaConfiguration( archivaConfiguration );
        if ( defaultManagedRepositoryAdmin.getManagedRepository( RELEASES_REPO ) == null )
        {
            defaultManagedRepositoryAdmin.addManagedRepository(
                createManagedRepository( RELEASES_REPO, new File( "target/test-classes/" + RELEASES_REPO ).getPath(),
                                         "default" ), false, null );
        }
        if ( defaultManagedRepositoryAdmin.getManagedRepository( INTERNAL_REPO ) == null )
        {
            defaultManagedRepositoryAdmin.addManagedRepository(
                createManagedRepository( INTERNAL_REPO, new File( "target/test-classes/" + INTERNAL_REPO ).getPath(),
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

        repoContentFactoryControl = MockClassControl.createControl( RepositoryContentFactory.class );
        repoFactory = (RepositoryContentFactory) repoContentFactoryControl.getMock();

        repoRequestControl = MockClassControl.createControl( RepositoryRequest.class );
        repoRequest = (RepositoryRequest) repoRequestControl.getMock();

        resourceFactory =
            new OverridingArchivaDavResourceFactory( applicationContext, plexusSisuBridge, archivaConfiguration );
        resourceFactory.setArchivaConfiguration( archivaConfiguration );
        resourceFactory.setRepositoryFactory( repoFactory );
        resourceFactory.setRepositoryRequest( repoRequest );
        resourceFactory.setConnectors( new OverridingRepositoryProxyConnectors() );
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
            archivaConfigurationControl.expectAndReturn( archivaConfiguration.getConfiguration(), config );
            requestControl.expectAndReturn( request.getMethod(), "GET", 2 );
            repoContentFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( RELEASES_REPO ),
                                                       releasesRepo );
            requestControl.expectAndReturn( request.getRemoteAddr(), "http://localhost:8080", 2 );
            requestControl.expectAndReturn( request.getDavSession(), new ArchivaDavSession(), 2 );
            repoRequestControl.expectAndReturn(
                repoRequest.isSupportFile( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ),
                false );
            repoRequestControl.expectAndReturn(
                repoRequest.isDefault( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ), false );
            repoRequestControl.expectAndReturn(
                repoRequest.toArtifactReference( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ),
                null );
            repoRequestControl.expectAndReturn(
                repoRequest.toNativePath( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar",
                                          internalRepo ),
                new File( config.findManagedRepositoryById( INTERNAL_REPO ).getLocation(),
                          "target/test-classes/internal/org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ).getPath() );
            repoContentFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( INTERNAL_REPO ),
                                                       internalRepo );

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

        List<RepositoryGroupConfiguration> repoGroups = new ArrayList<RepositoryGroupConfiguration>();
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
            archivaConfigurationControl.expectAndReturn( archivaConfiguration.getConfiguration(), config );
            requestControl.expectAndReturn( request.getMethod(), "GET", 2 );
            repoContentFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( INTERNAL_REPO ),
                                                       internalRepo );
            repoContentFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( RELEASES_REPO ),
                                                       releasesRepo );
            requestControl.expectAndReturn( request.getRemoteAddr(), "http://localhost:8080", 2 );
            requestControl.expectAndReturn( request.getDavSession(), new ArchivaDavSession(), 2 );
            repoRequestControl.expectAndReturn(
                repoRequest.isSupportFile( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ),
                false );
            repoRequestControl.expectAndReturn(
                repoRequest.isDefault( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ), false );
            repoRequestControl.expectAndReturn(
                repoRequest.toArtifactReference( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ),
                null );
            repoRequestControl.expectAndReturn(
                repoRequest.toNativePath( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar",
                                          internalRepo ),
                new File( config.findManagedRepositoryById( INTERNAL_REPO ).getLocation(),
                          "target/test-classes/internal/org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ).getPath() );

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
            createManagedRepository( LOCAL_MIRROR_REPO, new File( "target/test-classes/local-mirror" ).getPath(),
                                     "default" ), false, null );

        List<RepositoryGroupConfiguration> repoGroups = new ArrayList<RepositoryGroupConfiguration>();
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
            archivaConfigurationControl.expectAndReturn( archivaConfiguration.getConfiguration(), config );
            requestControl.expectAndReturn( request.getMethod(), "GET", 4 );
            repoContentFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( INTERNAL_REPO ),
                                                       internalRepo );
            repoContentFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( LOCAL_MIRROR_REPO ),
                                                       localMirrorRepo );
            requestControl.expectAndReturn( request.getRemoteAddr(), "http://localhost:8080", 4 );
            requestControl.expectAndReturn( request.getDavSession(), new ArchivaDavSession(), 4 );
            repoRequestControl.expectAndReturn(
                repoRequest.isSupportFile( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ), false,
                2 );
            repoRequestControl.expectAndReturn(
                repoRequest.isDefault( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ), false, 2 );
            repoRequestControl.expectAndReturn(
                repoRequest.toArtifactReference( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ),
                null, 2 );
            repoRequestControl.expectAndReturn(
                repoRequest.toNativePath( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar",
                                          internalRepo ),
                new File( config.findManagedRepositoryById( INTERNAL_REPO ).getLocation(),
                          "target/test-classes/internal/org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ).getPath() );

            repoRequestControl.expectAndReturn(
                repoRequest.toNativePath( "org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar",
                                          localMirrorRepo ),
                new File( config.findManagedRepositoryById( LOCAL_MIRROR_REPO ).getLocation(),
                          "target/test-classes/internal/org/apache/archiva/archiva/1.2-SNAPSHOT/archiva-1.2-SNAPSHOT.jar" ).getPath() );

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
        RepositoryRequest repoRequest = new RepositoryRequest( new LegacyPathParser( this.archivaConfiguration ) );
        resourceFactory.setRepositoryRequest( repoRequest );

        try
        {
            archivaConfigurationControl.reset();
            archivaConfigurationControl.expectAndReturn( archivaConfiguration.getConfiguration(), config );
            repoContentFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( INTERNAL_REPO ),
                                                       internalRepo );
            requestControl.expectAndReturn( request.getMethod(), "GET", 3 );
            requestControl.expectAndReturn( request.getRemoteAddr(), "http://localhost:8080", 3 );
            requestControl.expectAndReturn( request.getDavSession(), new ArchivaDavSession(), 2 );
            requestControl.expectAndReturn( request.getRequestURI(),
                                            "http://localhost:8080/archiva/repository/" + INTERNAL_REPO
                                                + "/eclipse/jdtcore/maven-metadata.xml" );
            response.addHeader( "Pragma", "no-cache" );
            responseControl.setVoidCallable();

            response.addHeader( "Cache-Control", "no-cache" );
            responseControl.setVoidCallable();

            long date = 2039842134;
            response.addDateHeader( "last-modified", date );
            responseControl.setVoidCallable();

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
        RepositoryRequest repoRequest = new RepositoryRequest( new LegacyPathParser( this.archivaConfiguration ) );
        resourceFactory.setRepositoryRequest( repoRequest );

        try
        {
            archivaConfigurationControl.reset();
            archivaConfigurationControl.expectAndReturn( archivaConfiguration.getConfiguration(), config );
            repoContentFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( INTERNAL_REPO ),
                                                       internalRepo );
            requestControl.expectAndReturn( request.getMethod(), "GET", 2 );
            requestControl.expectAndReturn( request.getRemoteAddr(), "http://localhost:8080", 2 );
            requestControl.expectAndReturn( request.getDavSession(), new ArchivaDavSession(), 2 );

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
            createManagedRepository( LEGACY_REPO, new File( "target/test-classes/" + LEGACY_REPO ).getPath(),
                                     "legacy" ), false, null );
        DavResourceLocator locator =
            new ArchivaDavResourceLocator( "", "/repository/" + LEGACY_REPO + "/eclipse/maven-metadata.xml",
                                           LEGACY_REPO, new ArchivaDavLocatorFactory() );

        ManagedRepositoryContent legacyRepo = createManagedRepositoryContent( LEGACY_REPO );

        // use actual object (this performs the isMetadata, isDefault and isSupportFile check!)
        RepositoryRequest repoRequest = new RepositoryRequest( new LegacyPathParser( this.archivaConfiguration ) );
        resourceFactory.setRepositoryRequest( repoRequest );

        try
        {
            archivaConfigurationControl.reset();
            archivaConfigurationControl.expectAndReturn( archivaConfiguration.getConfiguration(), config );
            repoContentFactoryControl.expectAndReturn( repoFactory.getManagedRepositoryContent( LEGACY_REPO ),
                                                       legacyRepo );
            requestControl.expectAndReturn( request.getMethod(), "GET", 2 );
            requestControl.expectAndReturn( request.getRemoteAddr(), "http://localhost:8080", 2 );
            requestControl.expectAndReturn( request.getDavSession(), new ArchivaDavSession(), 2 );

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

        protected String getActivePrincipal( DavServletRequest request )
        {
            return "guest";
        }
    }

    class OverridingRepositoryProxyConnectors
        extends DefaultRepositoryProxyConnectors
    {
        public File fetchMetatadaFromProxies( ManagedRepositoryContent repository, String logicalPath )
        {
            File target = new File( repository.getRepoRoot(), logicalPath );
            try
            {
                FileUtils.copyFile( new File( "target/test-classes/maven-metadata.xml" ), target );
            }
            catch ( IOException e )
            {

            }

            return target;
        }
    }
}
