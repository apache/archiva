package org.apache.archiva.web.rss;

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
import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;
import junit.framework.TestCase;
import org.apache.commons.codec.Encoder;
import org.apache.commons.codec.binary.Base64;


import java.io.File;
import javax.servlet.http.HttpServletResponse;

public class RssFeedServletTest
    extends TestCase
{
    private ServletRunner sr;

    private ServletUnitClient client;

    public void setUp()
        throws Exception
    {
        sr = new ServletRunner( new File( "src/test/webapp/WEB-INF/feedServletTest-web.xml" ) );
        client = sr.newClient();
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

    public void testRetrieveServlet()
        throws Exception
    {
        RssFeedServlet servlet = (RssFeedServlet) client.newInvocation(
            "http://localhost/feeds/test-repo" ).getServlet();
        assertNotNull( servlet );
    }

    public void testRequestNewArtifactsInRepo()
        throws Exception
    {
        RssFeedServlet servlet = (RssFeedServlet) client.newInvocation(
            "http://localhost/feeds/test-repo" ).getServlet();
        assertNotNull( servlet );

        WebRequest request = new GetMethodWebRequest( "http://localhost/feeds/test-repo" );

        Base64 encoder = new Base64(0, new byte[0]);
        String userPass = "user1:password1";
        String encodedUserPass = encoder.encodeToString( userPass.getBytes() );
        request.setHeaderField( "Authorization", "BASIC " + encodedUserPass );

        WebResponse response = client.getResponse( request );
        assertEquals( RssFeedServlet.MIME_TYPE, response.getHeaderField( "CONTENT-TYPE" ) );
        assertNotNull( "Should have recieved a response", response );
        assertEquals( "Should have been an OK response code.", HttpServletResponse.SC_OK, response.getResponseCode() );
    }

    public void testRequestNewVersionsOfArtifact()
        throws Exception
    {
        RssFeedServlet servlet = (RssFeedServlet) client.newInvocation(
            "http://localhost/feeds/org/apache/archiva/artifact-two" ).getServlet();
        assertNotNull( servlet );

        WebRequest request = new GetMethodWebRequest( "http://localhost/feeds/org/apache/archiva/artifact-two" );

        Base64 encoder = new Base64(0, new byte[0]);
        String userPass = "user1:password1";
        String encodedUserPass = encoder.encodeToString( userPass.getBytes() );
        request.setHeaderField( "Authorization", "BASIC " + encodedUserPass );

        WebResponse response = client.getResponse( request );
        assertEquals( RssFeedServlet.MIME_TYPE, response.getHeaderField( "CONTENT-TYPE" ) );
        assertNotNull( "Should have recieved a response", response );
        assertEquals( "Should have been an OK response code.", HttpServletResponse.SC_OK, response.getResponseCode() );
    }

    public void XXX_testInvalidRequest()
        throws Exception
    {
        RssFeedServlet servlet = (RssFeedServlet) client.newInvocation(
            "http://localhost/feeds?invalid_param=xxx" ).getServlet();
        assertNotNull( servlet );

        try
        {
            WebResponse resp = client.getResponse( "http://localhost/feeds?invalid_param=xxx" );
            assertEquals( HttpServletResponse.SC_BAD_REQUEST, resp.getResponseCode() );
        }
        catch ( HttpException he )
        {
            assertEquals( "Should have been a bad request response code.", HttpServletResponse.SC_BAD_REQUEST,
                          he.getResponseCode() );
        }
    }

    public void XXX_testInvalidAuthenticationRequest()
        throws Exception
    {
        RssFeedServlet servlet = (RssFeedServlet) client.newInvocation(
            "http://localhost/feeds/unauthorized-repo" ).getServlet();
        assertNotNull( servlet );

        WebRequest request = new GetMethodWebRequest( "http://localhost/feeds/unauthorized-repo" );

        Encoder encoder = new Base64();
        String userPass = "unauthUser:unauthPass";
        String encodedUserPass = new String( (byte[]) encoder.encode( userPass.getBytes() ) );
        request.setHeaderField( "Authorization", "BASIC " + encodedUserPass );

        try
        {
            WebResponse resp = client.getResponse( request );
            assertEquals( HttpServletResponse.SC_UNAUTHORIZED, resp.getResponseCode() );
        }
        catch ( HttpException he )
        {
            assertEquals( "Should have been a unauthorized response.", HttpServletResponse.SC_UNAUTHORIZED,
                          he.getResponseCode() );
        }
    }

    public void XXX_testUnauthorizedRequest()
        throws Exception
    {
        RssFeedServlet servlet = (RssFeedServlet) client.newInvocation(
            "http://localhost/feeds/unauthorized-repo" ).getServlet();
        assertNotNull( servlet );

        WebRequest request = new GetMethodWebRequest( "http://localhost/feeds/unauthorized-repo" );

        Base64 encoder = new Base64(0, new byte[0]);
        String userPass = "user1:password1";
        String encodedUserPass = encoder.encodeToString( userPass.getBytes() );
        request.setHeaderField( "Authorization", "BASIC " + encodedUserPass );

        try
        {
            WebResponse resp = client.getResponse( request );
            assertEquals( HttpServletResponse.SC_UNAUTHORIZED, resp.getResponseCode() );
        }
        catch ( HttpException he )
        {
            assertEquals( "Should have been a unauthorized response.", HttpServletResponse.SC_UNAUTHORIZED,
                          he.getResponseCode() );
        }
    }



}
