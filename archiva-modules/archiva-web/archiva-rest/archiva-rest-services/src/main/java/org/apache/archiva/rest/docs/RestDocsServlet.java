package org.apache.archiva.rest.docs;
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

import org.apache.commons.lang.StringUtils;
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
import java.util.Iterator;

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

        String startPath = StringUtils.substringBefore( path, "/" );

        // replace all links !!
        Document document = Jsoup.parse( is, "UTF-8", "" );

        Element body = document.body().child( 0 );

        Elements links = body.select( "a[href]" );

        for ( Iterator<Element> elementIterator = links.iterator(); elementIterator.hasNext(); )
        {
            Element link = elementIterator.next();
            //link.attr( "onclick", "loadRestDocs('" + startPath + "\',\'"+ "rest-docs/" + startPath + "/" + link.attr( "href" ) + "\');" );
            link.attr( "href", "#" + startPath + "/" + link.attr( "href" ) );

        }

        Elements codes = body.select( "code" );

        for ( Iterator<Element> elementIterator = codes.iterator(); elementIterator.hasNext(); )
        {
            Element code = elementIterator.next();
            code.attr( "class", code.attr( "class" ) + " nice-code" );
        }

        //res.appendChild( body.child( 1 ) );

        Document res = new Document( "" );
        res.appendChild( body.select( "div[id=main]" ).first() );

        resp.getOutputStream().write( res.outerHtml().getBytes() );

        //IOUtils.copy( is, resp.getOutputStream() );
        //super.doGet( req, resp );
    }
}
