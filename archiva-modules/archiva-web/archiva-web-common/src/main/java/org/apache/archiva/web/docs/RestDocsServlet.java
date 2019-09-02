package org.apache.archiva.web.docs;
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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Olivier Lamy
 * @since 1.4-M4
 */
public class RestDocsServlet
    extends HttpServlet
{
    private Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    protected void doGet( HttpServletRequest req, HttpServletResponse resp )
        throws ServletException, IOException
    {

        logger.debug( "docs request to path: {}", req.getPathInfo() );

        String path = StringUtils.removeStart( req.getPathInfo(), "/" );
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( path );

        if ( StringUtils.endsWith( path, ".xsd" ) )
        {
            resp.getWriter().write(StringEscapeUtils.escapeXml11( IOUtils.toString( is, "UTF-8" ) ));
            //IOUtils.copy( is, resp.getOutputStream() );
            return;
        }

        String startPath = StringUtils.substringBefore( path, "/" );

        // replace all links !!
        Document document = Jsoup.parse( is, "UTF-8", "" );

        Element body = document.body().child( 0 );

        Elements links = body.select( "a[href]" );

        for ( Element link : links ) {
            link.attr( "href", "#" + startPath + "/" + link.attr( "href" ) );
        }
        
        Elements datalinks = body.select( "[data-href]" );

        for ( Element link : datalinks ) {
            link.attr( "data-href", "#" + startPath + "/" + link.attr( "data-href" ) );
        }

        Elements codes = body.select( "code" );

        for ( Element code : codes ) {
            code.attr( "class", code.attr( "class" ) + " nice-code" );
        }

        //default generated enunciate use h1/h2/h3 which is quite big so transform to h3/h4/h5

        Elements headers = body.select( "h1" );

        for ( Element header : headers ) {
            header.tagName( "h3" );
        }

        headers = body.select( "h2" );

        for ( Element header : headers ) {
            header.tagName( "h4" );
        }

        headers = body.select( "h3" );

        for ( Element header : headers ) {
            header.tagName( "h5" );
        }

        Document res = new Document( "" );
        res.appendChild( body.select( "div[id=main]" ).first() );
        
        Elements scripts = body.select( "script" );
        for ( Element script : scripts )
        {
             res.appendChild( script );
        } 
        resp.getOutputStream().write( res.outerHtml().getBytes() );

    }
}
