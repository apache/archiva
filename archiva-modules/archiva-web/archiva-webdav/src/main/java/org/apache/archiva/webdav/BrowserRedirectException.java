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

import org.apache.archiva.metadata.repository.storage.RelocationException;
import org.apache.jackrabbit.webdav.DavException;

import javax.servlet.http.HttpServletResponse;

/**
 */
public class BrowserRedirectException
    extends DavException
{
    final String location;

    public BrowserRedirectException( String location )
    {
        super( HttpServletResponse.SC_MOVED_PERMANENTLY );
        this.location = location;
    }

    /**
     *
     * @param location
     * @param relocationType see {@link RelocationException.RelocationType}
     * @since 2.0.0
     */
    public BrowserRedirectException( String location, RelocationException.RelocationType relocationType )
    {
        super( relocationType == RelocationException.RelocationType.TEMPORARY
                   ? HttpServletResponse.SC_MOVED_TEMPORARILY
                   : HttpServletResponse.SC_MOVED_PERMANENTLY );

        this.location = location;
    }

    public String getLocation()
    {
        return location;
    }
}
