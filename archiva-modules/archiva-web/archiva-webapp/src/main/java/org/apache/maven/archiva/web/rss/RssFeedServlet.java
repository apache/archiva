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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.archiva.rss.RssFeedGenerator;
import org.apache.archiva.rss.processor.RssFeedProcessor;
import org.codehaus.plexus.spring.PlexusToSpringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * Servlet for handling rss feed requests.
 * 
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version
 */
public class RssFeedServlet
    extends HttpServlet
{
    public static final String MIME_TYPE = "application/xml; charset=UTF-8";

    private static final String COULD_NOT_GENERATE_FEED_ERROR = "Could not generate feed";

    private Logger log = LoggerFactory.getLogger( RssFeedGenerator.class );

    private RssFeedProcessor processor;

    private WebApplicationContext wac;

    public void init( javax.servlet.ServletConfig servletConfig )
        throws ServletException
    {
        super.init( servletConfig );
        wac = WebApplicationContextUtils.getRequiredWebApplicationContext( servletConfig.getServletContext() );
    }

    public void doGet( HttpServletRequest req, HttpServletResponse res )
        throws ServletException, IOException
    {
        log.info( "Request URL: " + req.getRequestURL() );
        try
        {
            Map<String, String> map = new HashMap<String, String>();
            SyndFeed feed = null;
            
            if ( req.getParameter( "repoId" ) != null )
            {
                if ( isAuthorized() )
                {
                 // new artifacts in repo feed request
                    processor =
                        (RssFeedProcessor) wac.getBean( PlexusToSpringUtils.buildSpringId(
                                                                                           RssFeedProcessor.class.getName(),
                                                                                           "new-artifacts" ) );
                    map.put( RssFeedProcessor.KEY_REPO_ID, req.getParameter( "repoId" ) );
                }
                else
                {
                    res.sendError( HttpServletResponse.SC_UNAUTHORIZED, "Request is not authorized." );
                    return;
                }
            }
            else if ( ( req.getParameter( "groupId" ) != null ) && ( req.getParameter( "artifactId" ) != null ) )
            {
                if ( isAuthorized() )
                {
                 // new versions of artifact feed request
                    processor =
                        (RssFeedProcessor) wac.getBean( PlexusToSpringUtils.buildSpringId(
                                                                                           RssFeedProcessor.class.getName(),
                                                                                           "new-versions" ) );
                    map.put( RssFeedProcessor.KEY_GROUP_ID, req.getParameter( "groupId" ) );
                    map.put( RssFeedProcessor.KEY_ARTIFACT_ID, req.getParameter( "artifactId" ) );
                }
                else
                {
                    res.sendError( HttpServletResponse.SC_UNAUTHORIZED, "Request is not authorized." );
                    return;
                }
            }
            else
            {
                res.sendError( HttpServletResponse.SC_BAD_REQUEST, "Required fields not found in request." );
                return;
            }

            feed = processor.process( map );
            res.setContentType( MIME_TYPE );

            SyndFeedOutput output = new SyndFeedOutput();
            output.output( feed, res.getWriter() );
        }
        catch ( FeedException ex )
        {
            String msg = COULD_NOT_GENERATE_FEED_ERROR;
            log.error( msg, ex );
            res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg );
        }
    }

    private boolean isAuthorized()
    {
        return true;
    }
}
