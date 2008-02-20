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

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * AbstractRepositoryServletTestCase 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractRepositoryServletTestCase
    extends PlexusTestCase
{
    protected static final String REPOID_INTERNAL = "internal";

    protected ServletUnitClient sc;

    protected ArchivaConfiguration archivaConfiguration;

    protected File repoRootInternal;
    
    private ServletRunner sr;

    protected void assertFileContents( String expectedContents, File repoRoot, String path )
        throws IOException
    {
        File actualFile = new File( repoRoot, path );
        assertTrue( "File <" + actualFile.getAbsolutePath() + "> should exist.", actualFile.exists() );
        assertTrue( "File <" + actualFile.getAbsolutePath() + "> should be a file (not a dir/link/device/etc).",
                    actualFile.isFile() );
    
        String actualContents = FileUtils.readFileToString( actualFile, null );
        assertEquals( "File Contents of <" + actualFile.getAbsolutePath() + ">", expectedContents, actualContents );
    }

    protected void assertRepositoryValid( RepositoryServlet servlet, String repoId )
    {
        ManagedRepositoryConfiguration repository = servlet.getRepository( repoId );
        assertNotNull( "Archiva Managed Repository id:<" + repoId + "> should exist.", repository );
        File repoRoot = new File( repository.getLocation() );
        assertTrue( "Archiva Managed Repository id:<" + repoId + "> should have a valid location on disk.", repoRoot
            .exists()
            && repoRoot.isDirectory() );
    }

    protected void assertResponseOK( WebResponse response )
    {
        assertNotNull( "Should have recieved a response", response );
        assertEquals( "Should have been an OK response code.", HttpServletResponse.SC_OK, response.getResponseCode() );
    }
    
    protected void assertResponseNotFound( WebResponse response )
    {
        assertNotNull( "Should have recieved a response", response );
        assertEquals( "Should have been an 404/Not Found response code.", HttpServletResponse.SC_NOT_FOUND, response
            .getResponseCode() );
    }

    protected ManagedRepositoryConfiguration createManagedRepository( String id, String name, File location )
    {
        ManagedRepositoryConfiguration repo = new ManagedRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setLocation( location.getAbsolutePath() );
        return repo;
    }

    protected RemoteRepositoryConfiguration createRemoteRepository( String id, String name, String url )
    {
        RemoteRepositoryConfiguration repo = new RemoteRepositoryConfiguration();
        repo.setId( id );
        repo.setName( name );
        repo.setUrl( url );
        return repo;
    }

    protected void dumpResponse( WebResponse response )
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

    protected void saveConfiguration()
        throws Exception
    {
        archivaConfiguration.save( archivaConfiguration.getConfiguration() );
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();
        
        String appserverBase = getTestFile( "target/appserver-base" ).getAbsolutePath();
        System.setProperty( "appserver.base", appserverBase );

        File testConf = getTestFile( "src/test/resources/repository-archiva.xml" );
        File testConfDest = new File( appserverBase, "conf/archiva.xml" );
        FileUtils.copyFile( testConf, testConfDest );

        archivaConfiguration = (ArchivaConfiguration) lookup( ArchivaConfiguration.class );
        repoRootInternal = new File( appserverBase, "data/repositories/internal" );
        Configuration config = archivaConfiguration.getConfiguration();

        config.addManagedRepository( createManagedRepository( REPOID_INTERNAL, "Internal Test Repo", repoRootInternal ) );
        saveConfiguration();

        sr = new ServletRunner();
        sr.registerServlet( "/repository/*", UnauthenticatedRepositoryServlet.class.getName() );
        sc = sr.newClient();
        HttpSession session = sc.getSession( true );
        ServletContext servletContext = session.getServletContext();
        servletContext.setAttribute( PlexusConstants.PLEXUS_KEY, getContainer() );
    }
    
    @Override
    protected String getConfigurationName( String subname )
        throws Exception
    {
        return "org/apache/maven/archiva/web/repository/RepositoryServletTest.xml";
    }
    
    @Override
    protected void tearDown()
        throws Exception
    {
        release( archivaConfiguration );
        
        if ( sc != null )
        {
            sc.clearContents();
        }

        if ( sr != null )
        {
            sr.shutDown();
        }
        
        super.tearDown();
    }

    protected void setupCleanRepo( File repoRootDir )
        throws IOException
    {
        FileUtils.deleteDirectory( repoRootDir );
        if ( !repoRootDir.exists() )
        {
            repoRootDir.mkdirs();
        }
    }

    protected void assertManagedFileNotExists( File repoRootInternal, String resourcePath )
    {
        File repoFile = new File( repoRootInternal, resourcePath );
        assertFalse( "Managed Repository File <" + repoFile.getAbsolutePath() + "> should not exist.", repoFile
            .exists() );
    }

    protected void setupCleanInternalRepo()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );
    }

    protected File populateRepo( File repoRootManaged, String path, String contents )
        throws Exception
    {
        File destFile = new File( repoRootManaged, path );
        destFile.getParentFile().mkdirs();
        FileUtils.writeStringToFile( destFile, contents, null );
        return destFile;
    }
}
