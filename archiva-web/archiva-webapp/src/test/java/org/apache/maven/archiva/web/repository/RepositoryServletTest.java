package org.apache.maven.archiva.web.repository;

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

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.PutMethodWebRequest;
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;

import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.webdav.util.MimeTypes;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * RepositoryServletTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryServletTest
    extends PlexusTestCase
{
    private ServletUnitClient sc;

    private static final String REQUEST_PATH = "http://localhost/repository/internal/path/to/artifact.jar";

    private File repositoryLocation;

    private ArchivaConfiguration configuration;

    private static final String REPOSITORY_ID = "internal";

    private static final String NEW_REPOSITORY_ID = "new-id";

    private static final String NEW_REPOSITORY_NAME = "New Repository";

    protected void setUp()
        throws Exception
    {
        super.setUp();

        String appserverBase = getTestFile( "target/appserver-base" ).getAbsolutePath();
        System.setProperty( "appserver.base", appserverBase );

        File testConf = getTestFile( "src/test/resources/repository-archiva.xml" );
        File testConfDest = new File( appserverBase, "conf/archiva.xml" );
        FileUtils.copyFile( testConf, testConfDest );

        configuration = (ArchivaConfiguration) lookup( ArchivaConfiguration.ROLE );
        repositoryLocation = new File( appserverBase, "data/repositories/internal" );
        Configuration config = configuration.getConfiguration();

        config.addManagedRepository( createManagedRepository( "internal", "Internal Test Repo", repositoryLocation ) );
        saveConfiguration();

        ServletRunner sr = new ServletRunner();
        sr.registerServlet( "/repository/*", UnauthenticatedRepositoryServlet.class.getName() );
        sc = sr.newClient();
        HttpSession session = sc.getSession( true );
        ServletContext servletContext = session.getServletContext();
        servletContext.setAttribute( PlexusConstants.PLEXUS_KEY, getContainer() );
    }

    public void testPutWithMissingParentCollection()
        throws Exception
    {
        FileUtils.deleteDirectory( repositoryLocation );

        WebRequest request = new PutMethodWebRequest( REQUEST_PATH, getClass().getResourceAsStream( "/artifact.jar" ),
                                                      "application/octet-stream" );
        WebResponse response = sc.getResponse( request );
        assertNotNull( "Should have received response", response );
        assertEquals( "file contents", "artifact.jar\n", FileUtils
            .readFileToString( new File( repositoryLocation, "path/to/artifact.jar" ), null ) );
    }

    public void testGetRepository()
        throws Exception
    {
        RepositoryServlet servlet = (RepositoryServlet) sc.newInvocation( REQUEST_PATH ).getServlet();
        assertNotNull( servlet );

        assertRepositoryValid( servlet, REPOSITORY_ID );
    }

    public void testGetRepositoryAfterDelete()
        throws Exception
    {
        RepositoryServlet servlet = (RepositoryServlet) sc.newInvocation( REQUEST_PATH ).getServlet();
        assertNotNull( servlet );

        Configuration c = configuration.getConfiguration();
        c.removeManagedRepository( c.findManagedRepositoryById( REPOSITORY_ID ) );
        saveConfiguration();

        ManagedRepositoryConfiguration repository = servlet.getRepository( REPOSITORY_ID );
        assertNull( repository );
    }

    public void testGetRepositoryAfterAdd()
        throws Exception
    {
        RepositoryServlet servlet = (RepositoryServlet) sc.newInvocation( REQUEST_PATH ).getServlet();
        assertNotNull( servlet );

        Configuration c = configuration.getConfiguration();
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( NEW_REPOSITORY_ID );
        repo.setName( NEW_REPOSITORY_NAME );
        File repoRoot = new File( getBasedir(), "target/test-repository-root" );
        if ( !repoRoot.exists() )
        {
            repoRoot.mkdirs();
        }
        repo.setLocation( repoRoot.getAbsolutePath() );
        c.addManagedRepository( repo );
        saveConfiguration();

        ManagedRepositoryConfiguration repository = servlet.getRepository( NEW_REPOSITORY_ID );
        assertNotNull( repository );
        assertEquals( NEW_REPOSITORY_NAME, repository.getName() );

        // check other is still intact
        assertRepositoryValid( servlet, REPOSITORY_ID );
    }

    public void testBrowse()
        throws Exception
    {
        RepositoryServlet servlet = (RepositoryServlet) sc.newInvocation( REQUEST_PATH ).getServlet();
        assertNotNull( servlet );
        assertRepositoryValid( servlet, REPOSITORY_ID );

        new File( repositoryLocation, "org/apache/archiva" ).mkdirs();
        new File( repositoryLocation, "net/sourceforge" ).mkdirs();
        new File( repositoryLocation, "commons-lang" ).mkdirs();

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" );
        WebResponse response = sc.getResponse( request );
        assertEquals( "Response", HttpServletResponse.SC_OK, response.getResponseCode() );

        // dumpResponse( response );

        WebLink links[] = response.getLinks();
        String expectedLinks[] = new String[] { "./commons-lang/", "./net/", "./org/", "./path/" };

        assertEquals( "Links.length", expectedLinks.length, links.length );
        for ( int i = 0; i < links.length; i++ )
        {
            assertEquals( "Link[" + i + "]", expectedLinks[i], links[i].getURLString() );
        }
    }

    public void testGetNoProxyChecksumDefaultLayout()
        throws Exception
    {
        RepositoryServlet servlet = (RepositoryServlet) sc.newInvocation( REQUEST_PATH ).getServlet();
        assertNotNull( servlet );
        assertRepositoryValid( servlet, REPOSITORY_ID );

        String commonsLangSha1 = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar.sha1";

        File checksumFile = new File( repositoryLocation, commonsLangSha1 );
        checksumFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( checksumFile, "dummy-checksum", null );
        
        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangSha1 );
        WebResponse response = sc.getResponse( request );
        assertEquals( "Response OK", HttpServletResponse.SC_OK, response.getResponseCode() );

        assertEquals( "Expected file contents", "dummy-checksum", response.getText() );
    }
    
    public void testGetNoProxyChecksumLegacyLayout()
        throws Exception
    {
        RepositoryServlet servlet = (RepositoryServlet) sc.newInvocation( REQUEST_PATH ).getServlet();
        assertNotNull( servlet );
        assertRepositoryValid( servlet, REPOSITORY_ID );

        String commonsLangSha1 = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar.sha1";

        File checksumFile = new File( repositoryLocation, commonsLangSha1 );
        checksumFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( checksumFile, "dummy-checksum", null );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + 
                                                      "commons-lang/jars/commons-lang-2.1.jar.sha1" );
        WebResponse response = sc.getResponse( request );
        assertEquals( "Response OK", HttpServletResponse.SC_OK, response.getResponseCode() );

        assertEquals( "Expected file contents", "dummy-checksum", response.getText() );
    }
    
    public void testGetNoProxyVersionedMetadataDefaultLayout()
        throws Exception
    {
        RepositoryServlet servlet = (RepositoryServlet) sc.newInvocation( REQUEST_PATH ).getServlet();
        assertNotNull( servlet );
        assertRepositoryValid( servlet, REPOSITORY_ID );

        String commonsLangMetadata = "commons-lang/commons-lang/2.1/maven-metadata.xml";
        String expectedMetadataContents = "dummy-versioned-metadata";

        File metadataFile = new File( repositoryLocation, commonsLangMetadata );
        metadataFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( metadataFile, expectedMetadataContents, null );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangMetadata );
        WebResponse response = sc.getResponse( request );
        assertEquals( "Response OK", HttpServletResponse.SC_OK, response.getResponseCode() );

        assertEquals( "Expected file contents", expectedMetadataContents, response.getText() );
    }
    
    public void testGetNoProxyProjectMetadataDefaultLayout()
        throws Exception
    {
        RepositoryServlet servlet = (RepositoryServlet) sc.newInvocation( REQUEST_PATH ).getServlet();
        assertNotNull( servlet );
        assertRepositoryValid( servlet, REPOSITORY_ID );

        String commonsLangMetadata = "commons-lang/commons-lang/maven-metadata.xml";
        String expectedMetadataContents = "dummy-project-metadata";

        File metadataFile = new File( repositoryLocation, commonsLangMetadata );
        metadataFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( metadataFile, expectedMetadataContents, null );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangMetadata );
        WebResponse response = sc.getResponse( request );
        assertEquals( "Response OK", HttpServletResponse.SC_OK, response.getResponseCode() );

        assertEquals( "Expected file contents", expectedMetadataContents, response.getText() );
    }
    
    public void testGetNoProxyArtifactDefaultLayout()
        throws Exception
    {
        RepositoryServlet servlet = (RepositoryServlet) sc.newInvocation( REQUEST_PATH ).getServlet();
        assertNotNull( servlet );
        assertRepositoryValid( servlet, REPOSITORY_ID );

        String commonsLangJar = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar";
        String expectedArtifactContents = "dummy-commons-lang-artifact";

        File artifactFile = new File( repositoryLocation, commonsLangJar );
        artifactFile.getParentFile().mkdirs();

        FileUtils.writeStringToFile( artifactFile, expectedArtifactContents, null );

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" + commonsLangJar );
        WebResponse response = sc.getResponse( request );
        assertEquals( "Response OK", HttpServletResponse.SC_OK, response.getResponseCode() );

        assertEquals( "Expected file contents", expectedArtifactContents, response.getText() );
    }
    
    public void testMimeTypesAvailable()
        throws Exception
    {
        MimeTypes mimeTypes = (MimeTypes) lookup( MimeTypes.class );
        assertNotNull( mimeTypes );
        
        // Test for some added types.
        assertEquals( "sha1", "text/plain", mimeTypes.getMimeType( "foo.sha1" ) );
        assertEquals( "md5", "text/plain", mimeTypes.getMimeType( "foo.md5" ) );
        assertEquals( "pgp", "application/pgp-encrypted", mimeTypes.getMimeType( "foo.pgp" ) );
    }

    private void dumpResponse( WebResponse response )
    {
        System.out.println( "---(response)---" );
        System.out.println( "" + response.getResponseCode() + " " + response.getResponseMessage() );

        String headerNames[] = response.getHeaderFieldNames();
        for ( String headerName : headerNames )
        {
            System.out.println( "[header] " + headerName + ": " + response.getHeaderField( headerName ) );
        }

        System.out.println( "---(text)---" );
        try
        {
            System.out.println( response.getText() );
        }
        catch ( IOException e )
        {
            System.err.print( "[Exception] : " );
            e.printStackTrace( System.err );
        }
    }

    private void assertRepositoryValid( RepositoryServlet servlet, String repoId )
    {
        ManagedRepositoryConfiguration repository = servlet.getRepository( repoId );
        assertNotNull( "Archiva Managed Repository id:<" + repoId + "> should exist.", repository );
        File repoRoot = new File( repository.getLocation() );
        assertTrue( "Archiva Managed Repository id:<" + repoId + "> should have a valid location on disk.", repoRoot
            .exists()
            && repoRoot.isDirectory() );
    }

    private void saveConfiguration()
        throws Exception
    {
        configuration.save( configuration.getConfiguration() );
        // TODO it would be better to use a mock configuration and "save" to more accurately reflect the calls made
        // RepositoryServlet servlet
        // servlet.configurationEvent( new ConfigurationEvent( ConfigurationEvent.SAVED ) );
    }

    private ManagedRepositoryConfiguration createManagedRepository( String id, String name, File location )
    {
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setLocation( location.getAbsolutePath() );
        return repo;
    }

    private RemoteRepositoryConfiguration createRemoteRepository( String id, String name, String url )
    {
        RemoteRepositoryConfiguration repo = new RemoteRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setUrl( url );
        return repo;
    }
}
