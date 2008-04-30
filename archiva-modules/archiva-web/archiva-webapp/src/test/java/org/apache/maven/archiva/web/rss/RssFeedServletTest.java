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
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;

/**
 * 
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
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
            (RssFeedServlet) client.newInvocation( "http://localhost/rss/rss_feeds?repoId=test-repo" ).getServlet();
        assertNotNull( servlet );
    }

    public void testRequestNewArtifactsInRepo()
        throws Exception
    {
        RssFeedServlet servlet =
            (RssFeedServlet) client.newInvocation( "http://localhost/rss/rss_feeds?repoId=test-repo" ).getServlet();
        assertNotNull( servlet );

        WebResponse response = client.getResponse( "http://localhost/rss/rss_feeds?repoId=test-repo" );
        assertEquals( RssFeedServlet.MIME_TYPE, response.getHeaderField( "CONTENT-TYPE" ) );

        assertNotNull( "Should have recieved a response", response );
        assertEquals( "Should have been an OK response code.", HttpServletResponse.SC_OK, response.getResponseCode() );
    }

    public void testRequestNewVersionsOfArtifact()
        throws Exception
    {
        RssFeedServlet servlet =
            (RssFeedServlet) client.newInvocation(
                                                   "http://localhost/rss/rss_feeds?groupId=org.apache.archiva&artifactId=artifact-two" ).getServlet();
        assertNotNull( servlet );

        WebResponse response = client.getResponse( "http://localhost/rss/rss_feeds?groupId=org.apache.archiva&artifactId=artifact-two" );        
        assertEquals( RssFeedServlet.MIME_TYPE, response.getHeaderField( "CONTENT-TYPE" ) );

        assertNotNull( "Should have recieved a response", response );
        assertEquals( "Should have been an OK response code.", HttpServletResponse.SC_OK, response.getResponseCode() );        
    }
    
    public void testInvalidRequest()
        throws Exception
    {
        RssFeedServlet servlet =
            (RssFeedServlet) client.newInvocation(
                                                   "http://localhost/rss/rss_feeds?invalid_param=xxx" ).getServlet();
        assertNotNull( servlet );

        try
        {
            WebResponse response = client.getResponse( "http://localhost/rss/rss_feeds?invalid_param=xxx" );
        }
        catch ( HttpException he )
        {
            assertEquals( "Should have been a bad request response code.", HttpServletResponse.SC_BAD_REQUEST, he.getResponseCode() );
        }                
    }
    
    public void testUnAuthorizedRequest()
        throws Exception
    {
        RssFeedServlet servlet =
            (RssFeedServlet) client.newInvocation(
                                                   "http://localhost/rss/rss_feeds" ).getServlet();
        assertNotNull( servlet );
    
        //WebResponse response = client.getResponse( "http://localhost/rss/rss_feeds" );
        //assertNotNull( "Should have recieved a response", response );
        //assertEquals( "Should have been a bad request response code.", HttpServletResponse.SC_BAD_REQUEST, response.getResponseCode() );
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
