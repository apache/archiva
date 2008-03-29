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

package org.apache.maven.archiva.webdav.test;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.webdav.DavServerManager;
import org.apache.webdav.lib.WebdavResource;
import org.apache.webdav.lib.WebdavResources;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

import java.io.File;
import java.io.IOException;

/**
 * AbstractWebdavProviderTestCase 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id: AbstractWebdavProviderTestCase.java 5997 2007-03-04 19:41:15Z joakime $
 */
public abstract class AbstractWebdavProviderTestCase
    extends PlexusInSpringTestCase
{
    public static final int PORT = 4321;

    public static final String CONTEXT = "/repos";

    protected File serverRootDir = null;

    private DavServerManager manager;

    private String providerHint = "simple";

    public DavServerManager getManager()
    {
        return manager;
    }

    public String getProviderHint()
    {
        return providerHint;
    }

    public void setManager( DavServerManager manager )
    {
        this.manager = manager;
    }

    public void setProviderHint( String providerHint )
    {
        this.providerHint = providerHint;
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();
        try
        {
            manager = (DavServerManager) lookup( DavServerManager.ROLE, getProviderHint() );
            serverRootDir = getRootDir();
        }
        catch ( Exception e )
        {
            tearDown();
            throw e;
        }
    }

    protected void tearDown()
        throws Exception
    {
        serverRootDir = null;

        super.tearDown();
    }

    protected void dumpCollection( WebdavResource webdavResource, String path )
        throws Exception
    {
        webdavResource.setPath( path );
        WebdavResource resources[] = webdavResource.listWebdavResources();

        System.out.println( "Dump Collection [" + path + "]: " + resources.length + " hits." );

        dumpCollectionRecursive( "", webdavResource, path );
    }

    protected void dumpCollectionRecursive( String indent, WebdavResource webdavResource, String path )
        throws Exception
    {
        if ( indent.length() > 12 )
        {
            return;
        }

        WebdavResource resources[] = webdavResource.listWebdavResources();

        for ( int i = 0; i < resources.length; i++ )
        {
            System.out.println( indent + "WebDavResource[" + path + "|" + i + "]: "
                + ( resources[i].isCollection() ? "(collection) " : "" ) + resources[i].getName() );

            if ( resources[i].isCollection() )
            {
                dumpCollectionRecursive( indent + "  ", resources[i], path + "/" + resources[i].getName() );
            }
        }
    }

    // --------------------------------------------------------------------
    // Actual Test Cases.
    // --------------------------------------------------------------------

    public void assertNotExists( File basedir, String relativePath )
    {
        assertNotExists( new File( basedir, relativePath ) );
    }

    public void assertNotExists( File file )
    {
        if ( file.exists() )
        {
            fail( "Unexpected path <" + file.getAbsolutePath() + "> should not exist." );
        }
    }

    public void assertExists( File basedir, String relativePath )
    {
        assertExists( new File( basedir, relativePath ) );
    }

    public void assertExists( File file )
    {
        if ( !file.exists() )
        {
            fail( "Expected path <" + file.getAbsolutePath() + "> does not exist." );
        }
    }

    private void resetDirectory( File dir )
    {
        try
        {
            FileUtils.deleteDirectory( dir );
        }
        catch ( IOException e )
        {
            fail( "Unable to delete test directory [" + dir.getAbsolutePath() + "]." );
        }

        if ( dir.exists() )
        {
            fail( "Unable to execute test, test directory [" + dir.getAbsolutePath()
                + "] exists, and cannot be deleted by the test case." );
        }

        if ( !dir.mkdirs() )
        {
            fail( "Unable to execute test, test directory [" + dir.getAbsolutePath() + "] cannot be created." );
        }
    }

    private File getRootDir()
    {
        if ( this.serverRootDir == null )
        {
            String clazz = this.getClass().getName();
            clazz = clazz.substring( clazz.lastIndexOf( "." ) + 1 );
            serverRootDir = new File( "target/test-contents-" + clazz + "/" + getName() );

            resetDirectory( serverRootDir );
        }

        return serverRootDir;
    }

    protected File getTestDir( String subdir )
    {
        File testDir = new File( getRootDir(), subdir );
        resetDirectory( testDir );
        return testDir;
    }

    public boolean isHttpStatusOk( WebdavResource webdavResource )
    {
        int statusCode = webdavResource.getStatusCode();

        if ( statusCode == HttpStatus.SC_MULTI_STATUS )
        {
            // TODO: find out multi-status values.
        }

        return ( statusCode >= 200 ) && ( statusCode < 300 );
    }

    public void assertDavMkDir( WebdavResource webdavResource, String collectionName )
        throws Exception
    {
        String httpurl = webdavResource.getHttpURL().toString();

        if ( !webdavResource.mkcolMethod( collectionName ) )
        {
            fail( "Unable to create collection/dir <" + collectionName + "> against <" + httpurl + "> due to <"
                + webdavResource.getStatusMessage() + ">" );
        }

        assertDavDirExists( webdavResource, collectionName );
    }

    public void assertDavFileExists( WebdavResource webdavResource, String path, String filename )
        throws Exception
    {
        String httpurl = webdavResource.getHttpURL().toString();

        if ( !webdavResource.headMethod( path + "/" + filename ) )
        {
            fail( "Unable to verify that file/contents <" + path + "/" + filename + "> exists against <" + httpurl
                + "> due to <" + webdavResource.getStatusMessage() + ">" );
        }

        String oldPath = webdavResource.getPath();
        try
        {
            webdavResource.setPath( path );

            WebdavResources resources = webdavResource.getChildResources();

            WebdavResource testResource = resources.getResource( filename );

            if ( testResource == null )
            {
                fail( "The file/contents <" + path + "/" + filename + "> does not exist in <" + httpurl + ">" );
            }

            if ( testResource.isCollection() )
            {
                fail( "The file/contents <" + path + "/" + filename
                    + "> is incorrectly being reported as a collection." );
            }
        }
        finally
        {
            webdavResource.setPath( oldPath );
        }
    }

    public void assertDavFileNotExists( WebdavResource webdavResource, String path, String filename )
        throws Exception
    {
        String httpurl = webdavResource.getHttpURL().toString();

        if ( webdavResource.headMethod( path + "/" + filename ) )
        {
            fail( "Encountered unexpected file/contents <" + path + "/" + filename + "> at <" + httpurl + ">" );
        }

        String oldPath = webdavResource.getPath();
        try
        {
            webdavResource.setPath( path );

            WebdavResources resources = webdavResource.getChildResources();

            WebdavResource testResource = resources.getResource( filename );

            if ( testResource == null )
            {
                // Nothing found. we're done.
                return;
            }

            if ( !testResource.isCollection() )
            {
                fail( "Encountered unexpected file/contents <" + path + "/" + filename + "> at <" + httpurl + ">" );
            }
        }
        finally
        {
            webdavResource.setPath( oldPath );
        }
    }

    public void assertDavDirExists( WebdavResource webdavResource, String path )
        throws Exception
    {
        String httpurl = webdavResource.getHttpURL().toString();

        String oldPath = webdavResource.getPath();
        try
        {
            webdavResource.setPath( path );

            if ( !webdavResource.isCollection() )
            {
                if ( !isHttpStatusOk( webdavResource ) )
                {
                    fail( "Unable to verify that path <" + path + "> is really a collection against <" + httpurl
                        + "> due to <" + webdavResource.getStatusMessage() + ">" );
                }
            }
        }
        finally
        {
            webdavResource.setPath( oldPath );
        }
    }

    public void assertDavDirNotExists( WebdavResource webdavResource, String path )
        throws Exception
    {
        String httpurl = webdavResource.getHttpURL().toString();

        String oldPath = webdavResource.getPath();
        try
        {
            webdavResource.setPath( path );

            if ( webdavResource.isCollection() )
            {
                fail( "Encountered unexpected collection <" + path + "> at <" + httpurl + ">" );
            }
        }
        catch ( HttpException e )
        {
            if ( e.getReasonCode() == HttpStatus.SC_NOT_FOUND )
            {
                // Expected path.
                return;
            }

            fail( "Unable to set path due to HttpException: " + e.getReasonCode() + ":" + e.getReason() );
        }
        finally
        {
            webdavResource.setPath( oldPath );
        }
    }

    public void assertDavTouchFile( WebdavResource webdavResource, String path, String filename, String contents )
        throws Exception
    {
        String httpurl = webdavResource.getHttpURL().toString();

        webdavResource.setPath( path );

        if ( !webdavResource.putMethod( path + "/" + filename, contents ) )
        {
            fail( "Unable to create file/contents <" + path + "/" + filename + "> against <" + httpurl + "> due to <"
                + webdavResource.getStatusMessage() + ">" );
        }

        assertDavFileExists( webdavResource, path, filename );
    }

    protected void assertGet404( String url )
        throws IOException
    {
        HttpClient client = new HttpClient();
        GetMethod method = new GetMethod( url );
    
        try
        {
            client.executeMethod( method );
    
            if ( method.getStatusCode() == 404 )
            {
                // Expected path.
                return;
            }
    
            fail( "Request for resource " + url + " should have resulted in an HTTP 404 (Not Found) response, "
                + "instead got code " + method.getStatusCode() + " <" + method.getStatusText() + ">." );
        }
        catch ( HttpException e )
        {
            System.err.println( "HTTP Response: " + e.getReasonCode() + " " + e.getReason() );
            throw e;
        }
    }
}
