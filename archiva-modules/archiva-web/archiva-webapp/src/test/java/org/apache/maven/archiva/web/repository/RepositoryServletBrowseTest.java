package org.apache.maven.archiva.web.repository;

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
import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import java.io.File;

import javax.servlet.http.HttpServletResponse;

/**
 * RepositoryServletBrowseTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryServletBrowseTest
    extends AbstractRepositoryServletTestCase
{
    public void testBrowse()
        throws Exception
    {
        new File( repoRootInternal, "org/apache/archiva" ).mkdirs();
        new File( repoRootInternal, "net/sourceforge" ).mkdirs();
        new File( repoRootInternal, "commons-lang" ).mkdirs();

        WebRequest request = new GetMethodWebRequest( "http://machine.com/repository/internal/" );
        WebResponse response = sc.getResponse( request );
        assertEquals( "Response", HttpServletResponse.SC_OK, response.getResponseCode() );

        // dumpResponse( response );

        WebLink links[] = response.getLinks();
        String expectedLinks[] = new String[] { "./commons-lang/", "./net/", "./org/" };

        assertEquals( "Links.length", expectedLinks.length, links.length );
        for ( int i = 0; i < links.length; i++ )
        {
            assertEquals( "Link[" + i + "]", expectedLinks[i], links[i].getURLString() );
        }
    }
}
