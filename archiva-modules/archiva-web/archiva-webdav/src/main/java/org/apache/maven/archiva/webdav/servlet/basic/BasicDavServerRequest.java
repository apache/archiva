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

package org.apache.maven.archiva.webdav.servlet.basic;

import org.apache.maven.archiva.webdav.servlet.DavServerRequest;
import org.apache.maven.archiva.webdav.util.WrappedRepositoryRequest;

/**
 * BasicDavServerRequest - for requests that have a prefix based off of the servlet path id.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id: BasicDavServerRequest.java 7073 2007-11-22 04:04:50Z brett $
 */
public class BasicDavServerRequest
    implements DavServerRequest
{
    private WrappedRepositoryRequest request;

    private String prefix;

    private String logicalResource;

    public BasicDavServerRequest( WrappedRepositoryRequest request )
    {
        this.request = request;
        this.prefix = request.getServletPath();
        this.logicalResource = request.getPathInfo();
    }

    public void setLogicalResource( String logicalResource )
    {
        this.logicalResource = logicalResource;
        this.request.setPathInfo( logicalResource );
    }

    public String getLogicalResource()
    {
        return this.logicalResource;
    }

    public String getPrefix()
    {
        return this.prefix;
    }

    public WrappedRepositoryRequest getRequest()
    {
        return request;
    }
}
