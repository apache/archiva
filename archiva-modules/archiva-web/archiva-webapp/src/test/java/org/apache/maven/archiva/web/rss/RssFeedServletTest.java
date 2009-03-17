package org.apache.maven.archiva.web.rss;

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

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.Encoder;
import org.apache.commons.codec.binary.Base64;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

import sun.misc.BASE64Encoder;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;

/**
 * 
 * @version
 */
public class RssFeedServletTest
    extends PlexusInSpringTestCase
{
    private ServletRunner sr;

    private ServletUnitClient client;

    public void setUp()
        throws Exception
    {
        sr = new ServletRunner( getTestFile( "src/test/webapp/WEB-INF/feedServletTest-web.xml" ) );
        client = sr.newClient();
    }

    public void testRetrieveServlet()
        throws Exception
    {
        RssFeedServlet servlet =
            (RssFeedServlet) client.newInvocation( "http://localhost/feeds/test-repo" ).getServlet();
        assertNotNull( servlet );
    }

    public void testRequestNewArtifactsInRepo()
        throws Exception
    {
        RssFeedServlet servlet =
            (RssFeedServlet) client.newInvocation( "http://localhost/feeds/test-repo" ).getServlet();
        assertNotNull( servlet );

        WebRequest request = new GetMethodWebRequest( "http://localhost/feeds/test-repo" );
        
        BASE64Encoder encoder = new BASE64Encoder();
        String userPass = "user1:password1";
        String encodedUserPass = encoder.encode( userPass.getBytes() );        
        request.setHeaderField( "Authorization", "BASIC " + encodedUserPass );
        
        WebResponse response = client.getResponse( request );
        assertEquals( RssFeedServlet.MIME_TYPE, response.getHeaderField( "CONTENT-TYPE" ) );
        assertNotNull( "Should have recieved a response", response );
        assertEquals( "Should have been an OK response code.", HttpServletResponse.SC_OK, response.getResponseCode() );
    }
    
    public void testRequestNewVersionsOfArtifact()
        throws Exception
    {
        RssFeedServlet servlet =
            (RssFeedServlet) client.newInvocation(
                                                   "http://localhost/feeds/org/apache/archiva/artifact-two" ).getServlet();
        assertNotNull( servlet );

        WebRequest request = new GetMethodWebRequest( "http://localhost/feeds/org/apache/archiva/artifact-two" );
        
        BASE64Encoder encoder = new BASE64Encoder();
        String userPass = "user1:password1";
        String encodedUserPass = encoder.encode( userPass.getBytes() );        
        request.setHeaderField( "Authorization", "BASIC " + encodedUserPass );        
        
        WebResponse response = client.getResponse( request );        
        assertEquals( RssFeedServlet.MIME_TYPE, response.getHeaderField( "CONTENT-TYPE" ) );
        assertNotNull( "Should have recieved a response", response );
        assertEquals( "Should have been an OK response code.", HttpServletResponse.SC_OK, response.getResponseCode() );        
    }
    
    public void testInvalidRequest()
        throws Exception
    {
        RssFeedServlet servlet =
            (RssFeedServlet) client.newInvocation(
                                                   "http://localhost/feeds?invalid_param=xxx" ).getServlet();
        assertNotNull( servlet );

        try
        {
            client.getResponse( "http://localhost/feeds?invalid_param=xxx" );
            fail( "Expected exception" );
        }
        catch ( HttpException he )
        {
            assertEquals( "Should have been a bad request response code.", HttpServletResponse.SC_BAD_REQUEST, he.getResponseCode() );
        }                
    }
       
    public void testInvalidAuthenticationRequest()
        throws Exception
    {
        RssFeedServlet servlet =
            (RssFeedServlet) client.newInvocation(
                                                   "http://localhost/feeds/unauthorized-repo" ).getServlet();
        assertNotNull( servlet );
    
        
        WebRequest request = new GetMethodWebRequest( "http://localhost/feeds/unauthorized-repo" );
        
        Encoder encoder = new Base64();
        String userPass = "unauthUser:unauthPass";
        String encodedUserPass = new String( ( byte[] ) encoder.encode( userPass.getBytes() ) );        
        request.setHeaderField( "Authorization", "BASIC " + encodedUserPass );        
        
        try
        {
            client.getResponse( request );
            fail( "Expected exception" );
        }
        catch ( HttpException he )
        {            
            assertEquals( "Should have been a unauthorized response.", HttpServletResponse.SC_UNAUTHORIZED, he.getResponseCode() );
        }
    }
    
    public void testUnauthorizedRequest()
        throws Exception
    {
        RssFeedServlet servlet =
            (RssFeedServlet) client.newInvocation(
                                                   "http://localhost/feeds/unauthorized-repo" ).getServlet();
        assertNotNull( servlet );
    
        
        WebRequest request = new GetMethodWebRequest( "http://localhost/feeds/unauthorized-repo" );
        
        BASE64Encoder encoder = new BASE64Encoder();
        String userPass = "user1:password1";
        String encodedUserPass = encoder.encode( userPass.getBytes() );        
        request.setHeaderField( "Authorization", "BASIC " + encodedUserPass );        
        
        try
        {
            client.getResponse( request );
            fail( "Expected exception" );
        }
        catch ( HttpException he )
        {            
            assertEquals( "Should have been a unauthorized response.", HttpServletResponse.SC_UNAUTHORIZED, he.getResponseCode() );
        }
    } 
    
    @Override
    protected String getPlexusConfigLocation()
    {
        return "org/apache/maven/archiva/web/rss/RssFeedServletTest.xml";
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        if ( client != null )
        {
            client.clearContents();
        }

        if ( sr != null )
        {
            sr.shutDown();
        }

        super.tearDown();
    }

}
