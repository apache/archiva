package org.apache.maven.archiva.web.servlet.repository;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import it.could.webdav.DAVTransaction;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * RepositoryRequest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryRequest
    extends HttpServletRequestWrapper
{
    private String repoUrlName;

    public RepositoryRequest( HttpServletRequest request, String repoUrlName )
    {
        super( request );
        this.repoUrlName = "";
        
        if(repoUrlName != null) {
            this.repoUrlName = repoUrlName;
        }
    }

    /**
     * Adjust the path info value to remove reference to repoUrlName.
     * This is done to satisfy the needs of {@link DAVTransaction}
     */
    public String getPathInfo()
    {
        String pathInfo = super.getPathInfo();

        if ( pathInfo == null )
        {
            return "";
        }

        if ( ( pathInfo.length() > 1 ) && ( pathInfo.charAt( 0 ) == '/' ) )
        {
            pathInfo = pathInfo.substring( 1 );
        }

        if ( pathInfo.startsWith( repoUrlName ) )
        {
            pathInfo = pathInfo.substring( repoUrlName.length() );
        }

        return pathInfo;
    }

    public String getServletPath()
    {
        return super.getServletPath() + "/" + this.repoUrlName;
    }
    
}
