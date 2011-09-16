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

import com.meterware.httpunit.PutMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import java.io.File;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.webdav.httpunit.MkColMethodWebRequest;
import org.junit.Test;


/**
 * Deploy / Put Test cases for RepositoryServlet.  
 *
 * @version $Id$
 */
public class RepositoryServletDeployTest
    extends AbstractRepositoryServletTestCase
{
    private static final String ARTIFACT_DEFAULT_LAYOUT = "/path/to/artifact/1.0.0/artifact-1.0.0.jar";

    @Test
    public void testPutWithMissingParentCollection()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );

        String putUrl = "http://machine.com/repository/internal" + ARTIFACT_DEFAULT_LAYOUT;
        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );
        // verify that the file exists in resources-dir
        assertNotNull( "artifact.jar inputstream", is );

        WebRequest request = new PutMethodWebRequest( putUrl, is, "application/octet-stream" );

        WebResponse response = sc.getResponse( request );
        assertResponseCreated( response );
        assertFileContents( "artifact.jar\n", repoRootInternal, ARTIFACT_DEFAULT_LAYOUT );
    }    

    /**
     * MRM-747
     * test whether trying to overwrite existing relase-artifact is blocked by returning HTTP-code 409 
     * 
     * @throws Exception
     */
    @Test
    public void testReleaseArtifactsRedeploymentValidPath()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );

        String putUrl = "http://machine.com/repository/internal" + ARTIFACT_DEFAULT_LAYOUT;
        String metadataUrl = "http://machine.com/repository/internal/path/to/artifact/maven-metadata.xml";
        String checksumUrl = "http://machine.com/repository/internal" + ARTIFACT_DEFAULT_LAYOUT + ".sha1";
        
        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );
        // verify that the file exists in resources-dir
        assertNotNull( "artifact.jar inputstream", is );

        // send request #1 and verify it's successful
        WebRequest request = new PutMethodWebRequest( putUrl, is, "application/octet-stream" );
        WebResponse response = sc.getResponse( request );
        assertResponseCreated( response );
        
        is = getClass().getResourceAsStream( "/artifact.jar.sha1" );
        request = new PutMethodWebRequest( checksumUrl, is, "application/octet-stream" );
        response = sc.getResponse( request );
        assertResponseCreated( response );
        
        is = getClass().getResourceAsStream( "/maven-metadata.xml" );
        request = new PutMethodWebRequest( metadataUrl, is, "application/octet-stream" );
        response = sc.getResponse( request );
        assertResponseCreated( response );
        
        // send request #2 and verify it's blocked
        is = getClass().getResourceAsStream( "/artifact.jar" );
        request = new PutMethodWebRequest( putUrl, is, "application/octet-stream" );
        response = sc.getResponse( request );
        assertResponseConflictError( response );        
    }

    @Test
    public void testReleaseArtifactsRedeploymentIsAllowed()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );
        
        ManagedRepositoryConfiguration managedRepo = archivaConfiguration.getConfiguration().findManagedRepositoryById( REPOID_INTERNAL );
        managedRepo.setBlockRedeployments( false );
        
        saveConfiguration( archivaConfiguration );
    
        String putUrl = "http://machine.com/repository/internal" + ARTIFACT_DEFAULT_LAYOUT;
        String metadataUrl = "http://machine.com/repository/internal/path/to/artifact/maven-metadata.xml";
        String checksumUrl = "http://machine.com/repository/internal" + ARTIFACT_DEFAULT_LAYOUT + ".sha1";
        
        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );
        // verify that the file exists in resources-dir
        assertNotNull( "artifact.jar inputstream", is );
    
        // send request #1 and verify it's successful
        WebRequest request = new PutMethodWebRequest( putUrl, is, "application/octet-stream" );
        WebResponse response = sc.getResponse( request );
        assertResponseCreated( response );
        
        is = getClass().getResourceAsStream( "/artifact.jar.sha1" );
        request = new PutMethodWebRequest( checksumUrl, is, "application/octet-stream" );
        response = sc.getResponse( request );
        assertResponseCreated( response );
        
        is = getClass().getResourceAsStream( "/maven-metadata.xml" );
        request = new PutMethodWebRequest( metadataUrl, is, "application/octet-stream" );
        response = sc.getResponse( request );
        assertResponseCreated( response );
        
        // send request #2 and verify if it's still successful
        is = getClass().getResourceAsStream( "/artifact.jar" );
        request = new PutMethodWebRequest( putUrl, is, "application/octet-stream" );
        response = sc.getResponse( request );
        assertResponseNoContent( response );        
    }

    @Test
    public void testReleaseArtifactsRedeploymentInvalidPath()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );

        String putUrl = "http://machine.com/repository/internal/artifact.jar";
        String metadataUrl = "http://machine.com/repository/internal/maven-metadata.xml";
        String checksumUrl = "http://machine.com/repository/internal/artifact.jar.sha1";
        
        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );
        // verify that the file exists in resources-dir
        assertNotNull( "artifact.jar inputstream", is );

        // send request #1 and verify it's successful
        WebRequest request = new PutMethodWebRequest( putUrl, is, "application/octet-stream" );
        WebResponse response = sc.getResponse( request );
        assertResponseCreated( response );
        
        is = getClass().getResourceAsStream( "/artifact.jar.sha1" );
        request = new PutMethodWebRequest( checksumUrl, is, "application/octet-stream" );
        response = sc.getResponse( request );
        assertResponseCreated( response );
        
        is = getClass().getResourceAsStream( "/maven-metadata.xml" );
        request = new PutMethodWebRequest( metadataUrl, is, "application/octet-stream" );
        response = sc.getResponse( request );
        assertResponseCreated( response );
        
        // send request #2 and verify it's re-deployed
        is = getClass().getResourceAsStream( "/artifact.jar" );
        request = new PutMethodWebRequest( putUrl, is, "application/octet-stream" );
        response = sc.getResponse( request );
        assertResponseNoContent( response );
    } 

    @Test
    public void testReleaseArtifactsRedeploymentArtifactIsSnapshot()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );

        String putUrl = "http://machine.com/repository/internal/path/to/artifact/1.0-SNAPSHOT/artifact-1.0-SNAPSHOT.jar";
        String metadataUrl = "http://machine.com/repository/internal/path/to/artifact/maven-metadata.xml";
        String checksumUrl = "http://machine.com/repository/internal/path/to/artifact/1.0-SNAPSHOT/artifact-1.0-SNAPSHOT.jar.sha1";
        
        InputStream is = getClass().getResourceAsStream( "/artifact.jar" );
        // verify that the file exists in resources-dir
        assertNotNull( "artifact.jar inputstream", is );

        // send request #1 and verify it's successful
        WebRequest request = new PutMethodWebRequest( putUrl, is, "application/octet-stream" );
        WebResponse response = sc.getResponse( request );
        assertResponseCreated( response );
        
        is = getClass().getResourceAsStream( "/artifact.jar.sha1" );
        request = new PutMethodWebRequest( checksumUrl, is, "application/octet-stream" );
        response = sc.getResponse( request );
        assertResponseCreated( response );
        
        is = getClass().getResourceAsStream( "/maven-metadata.xml" );
        request = new PutMethodWebRequest( metadataUrl, is, "application/octet-stream" );
        response = sc.getResponse( request );
        assertResponseCreated( response );
        
        // send request #2 and verify it's re-deployed
        is = getClass().getResourceAsStream( "/artifact.jar" );
        request = new PutMethodWebRequest( putUrl, is, "application/octet-stream" );
        response = sc.getResponse( request );
        assertResponseNoContent( response );
    } 

    @Test
    public void testMkColWithMissingParentCollectionFails()
        throws Exception
    {
        setupCleanRepo( repoRootInternal );

        String putUrl = "http://machine.com/repository/internal/path/to/";

        WebRequest request = new MkColMethodWebRequest( putUrl );

        WebResponse response = sc.getResponse( request );
        
        assertEquals(HttpServletResponse.SC_CONFLICT, response.getResponseCode());
        
        File mkColLocalPath = new File(repoRootInternal, "path/to/");
        assertFalse(mkColLocalPath.exists());
    }
    
    protected void assertResponseNoContent( WebResponse response )
    {
        assertNotNull( "Should have recieved a response", response );
        assertEquals( "Should have been a 204/NO CONTENT response code.", HttpServletResponse.SC_NO_CONTENT, response
            .getResponseCode() );
    }
    
    protected void assertResponseCreated( WebResponse response )
    {
        assertNotNull( "Should have recieved a response", response );
        assertEquals( "Should have been a 201/CREATED response code.", HttpServletResponse.SC_CREATED, response
            .getResponseCode() );
    }
}
