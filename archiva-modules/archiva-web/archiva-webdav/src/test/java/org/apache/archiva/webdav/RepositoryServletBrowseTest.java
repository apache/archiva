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


import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RepositoryServletBrowseTest
 */
public class RepositoryServletBrowseTest
    extends AbstractRepositoryServletTestCase
{
    @Override
    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

        Files.createDirectories( repoRootInternal.resolve( "org/apache/archiva" ));
        Files.createDirectories( repoRootInternal.resolve( "org/codehaus/mojo/" ));
        Files.createDirectories( repoRootInternal.resolve("net/sourceforge" ));
        Files.createDirectories( repoRootInternal.resolve("commons-lang" ));

        startRepository();
    }

    @Test
    public void testBrowse()
        throws Exception
    {
        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertEquals( "Response", HttpServletResponse.SC_OK, response.getStatusCode() );

        // dumpResponse( response );

        List<String> expectedLinks = Arrays.asList( ".indexer/", "commons-lang/", "net/", "org/" );

        Document document = Jsoup.parse( response.getContentAsString() );
        Elements elements = document.getElementsByTag( "a" );

        assertLinks( expectedLinks, elements );
    }

    @Test
    public void testBrowseSubdirectory()
        throws Exception
    {
        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/org" );
        WebResponse response = getServletUnitClient().getResponse( request );
        assertEquals( "Response", HttpServletResponse.SC_OK, response.getStatusCode() );

        List<String> expectedLinks = Arrays.asList( "../", "apache/", "codehaus/" );

        Document document = Jsoup.parse( response.getContentAsString() );
        Elements elements = document.getElementsByTag( "a" );

        assertLinks( expectedLinks, elements );
    }

    @Test
    public void testGetDirectoryWhichHasMatchingFile() //MRM-893
        throws Exception
    {
        Files.createDirectories( repoRootInternal.resolve("org/apache/archiva/artifactId/1.0" ));
        Files.createFile( repoRootInternal.resolve("org/apache/archiva/artifactId/1.0/artifactId-1.0.jar" ));

        WebRequest request =
            new GetMethodWebRequest( "http://machine.com/repository/internal/org/apache/archiva/artifactId" );
        WebResponse response = getServletUnitClient().getResponse( request, true );
        assertEquals( "1st Response", HttpServletResponse.SC_OK, response.getStatusCode() );

        request = new GetMethodWebRequest( "http://machine.com/repository/internal/org/apache/archiva/artifactId/" );
        response = getServletUnitClient().getResponse( request );
        assertEquals( "2nd Response", HttpServletResponse.SC_OK, response.getStatusCode() );

        request = new GetMethodWebRequest(
            "http://machine.com/repository/internal/org/apache/archiva/artifactId/1.0/artifactId-1.0.jar" );
        response = getServletUnitClient().getResponse( request );
        assertEquals( "3rd Response", HttpServletResponse.SC_OK, response.getStatusCode() );

        request = new GetMethodWebRequest(
            "http://machine.com/repository/internal/org/apache/archiva/artifactId/1.0/artifactId-1.0.jar/" );
        response = getServletUnitClient().getResponse( request );
        assertEquals( "4th Response", HttpServletResponse.SC_NOT_FOUND, response.getStatusCode() );
    }

    private void assertLinks( List<String> expectedLinks, Elements actualLinks )
    {
        assertThat( actualLinks ).hasSize( expectedLinks.size() );

        for ( int i = 0; i < actualLinks.size(); i++ )
        {
            Element element = actualLinks.get( i );
            assertEquals( "Link[" + i + "]", expectedLinks.get( i ), element.attr( "href" ) );
        }
    }

}
