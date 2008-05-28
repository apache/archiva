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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * PolicingServletRequest is for policing the incoming request for naughty bits, such as a double slashes,
 * or paths that include "/../" type syntax, or query string.  Stripping out all things that are 
 * not appropriate. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class PolicingServletRequest
    extends HttpServletRequestWrapper
    implements HttpServletRequest
{
    private String fixedPathInfo;

    public PolicingServletRequest( HttpServletRequest originalRequest )
    {
        super( originalRequest );

        fixedPathInfo = originalRequest.getPathInfo();

        if ( StringUtils.isNotBlank( fixedPathInfo ) )
        {
            /* Perform a simple security normalization of the requested pathinfo.
             * This is to cleanup requests that use "/../" or "///" type hacks.
             */
            fixedPathInfo = FilenameUtils.normalize( fixedPathInfo );
            if ( SystemUtils.IS_OS_WINDOWS )
            {
                // Adjust paths back to unix & url format expectations (when on windows)
                fixedPathInfo = FilenameUtils.separatorsToUnix( fixedPathInfo );
            }
        }
    }

    @Override
    public String getPathInfo()
    {
        return fixedPathInfo;
    }

    @Override
    public String getQueryString()
    {
        // No query string allowed.
        return null;
    }
}
