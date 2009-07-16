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

import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;
import net.sf.ehcache.CacheManager;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.maven.archiva.webdav.RepositoryServlet;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

/**
 * AbstractRepositoryServletTestCase 
 *
 * @version $Id$
 */
public abstract class AbstractRepositoryServletTestCase
    extends PlexusInSpringTestCase
{
    protected static final String REPOID_INTERNAL = "internal";

    protected ServletUnitClient sc;

    protected File repoRootInternal;
    
    private ServletRunner sr;

    protected ArchivaConfiguration archivaConfiguration;

    protected void saveConfiguration()
        throws Exception
    {
        saveConfiguration( archivaConfiguration );
    }

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
        Assert.assertEquals( "Should have been an OK response code", HttpServletResponse.SC_OK,
                             response.getResponseCode() );
    }

    protected void assertResponseOK( WebResponse response, String path )
    {
        assertNotNull( "Should have recieved a response", response );
        Assert.assertEquals( "Should have been an OK response code for path: " + path, HttpServletResponse.SC_OK,
                             response.getResponseCode() );
    }
    
    protected void assertResponseNotFound( WebResponse response )
    {
        assertNotNull( "Should have recieved a response", response );
        Assert.assertEquals( "Should have been an 404/Not Found response code.", HttpServletResponse.SC_NOT_FOUND, response
            .getResponseCode() );
    }

    protected void assertResponseInternalServerError( WebResponse response )
    {
        assertNotNull( "Should have recieved a response", response );
        Assert.assertEquals( "Should have been an 500/Internal Server Error response code.", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response
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

    protected void saveConfiguration( ArchivaConfiguration archivaConfiguration )
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
        saveConfiguration( archivaConfiguration );

        CacheManager.getInstance().removeCache( "url-failures-cache" );

        HttpUnitOptions.setExceptionsThrownOnErrorStatus( false );                

        sr = new ServletRunner( getTestFile( "src/test/resources/WEB-INF/web.xml" ) );
        sr.registerServlet( "/repository/*", UnauthenticatedRepositoryServlet.class.getName() );
        sc = sr.newClient();
    }

    @Override
    protected String getPlexusConfigLocation()
    {
        return "org/apache/maven/archiva/webdav/RepositoryServletTest.xml";
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        if ( sc != null )
        {
            sc.clearContents();
        }

        if ( sr != null )
        {
            sr.shutDown();
        }
        
        if (repoRootInternal.exists())
        {
            FileUtils.deleteDirectory(repoRootInternal);
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
