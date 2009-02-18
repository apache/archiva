package org.apache.archiva.web.servlet;

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

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import java.io.File;

/**
 * RepositoryServletTest 
 *
 * @version $Id$
 */
public class RepositoryServletTest
    extends AbstractRepositoryServletTestCase
{
    private static final String REQUEST_PATH = "http://machine.com/repository/internal/";

    public void testGetRepositoryInvalidPathPassthroughPresent()
        throws Exception
    {
        String path = REQUEST_PATH + ".index/filecontent/segments.gen";

        populateRepo( repoRootInternal, ".index/filecontent/segments.gen", "index file" );
        
        WebRequest request = new GetMethodWebRequest( path );
        WebResponse response = sc.getResponse( request );
        assertResponseOK( response );
        assertEquals( "index file", response.getText() );        
    }

    public void testGetRepositoryInvalidPathPassthroughMissing()
        throws Exception
    {
        String path = REQUEST_PATH + ".index/filecontent/foo.bar";

        WebRequest request = new GetMethodWebRequest( path );
        WebResponse response = sc.getResponse( request );
        assertResponseNotFound( response );
        assertEquals( "Could not find /internal/.index/filecontent/foo.bar", response.getResponseMessage() );
    }
}
