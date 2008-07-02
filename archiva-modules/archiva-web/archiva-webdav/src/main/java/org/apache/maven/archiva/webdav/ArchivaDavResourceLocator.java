package org.apache.maven.archiva.webdav;

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

import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavLocatorFactory;
import org.apache.jackrabbit.util.Text;

/**
 * @author <a href="mailto:james@atlassian.com">James William Dumay</a>
 */
public class ArchivaDavResourceLocator
    implements DavResourceLocator, RepositoryLocator
{
    private final String prefix;

    private final String resourcePath;

    private final String href;

    private final String repositoryId;

    private final DavLocatorFactory davLocatorFactory;

    public ArchivaDavResourceLocator( String prefix, String resourcePath, String repositoryId,
                                      DavLocatorFactory davLocatorFactory )
    {
        this.prefix = prefix;
        this.repositoryId = repositoryId;
        this.davLocatorFactory = davLocatorFactory;
        
        String path = resourcePath;
        
        if (!resourcePath.startsWith("/"))
        {
            path = "/" + resourcePath;
        }

        String escapedPath = Text.escapePath( resourcePath );
        String hrefPrefix = prefix;

        // Ensure no extra slashes when href is joined
        if ( hrefPrefix.endsWith( "/" ) && escapedPath.startsWith( "/" ) )
        {
            hrefPrefix = hrefPrefix.substring( 0, hrefPrefix.length() - 1 );
        }

        href = hrefPrefix + escapedPath;
        
        //Remove trailing slashes otherwise Text.getRelativeParent fails
        if (resourcePath.endsWith("/") && resourcePath.length() > 1)
        {
            path = resourcePath.substring( 0, resourcePath.length() - 1 );
        }
        
        this.resourcePath = path;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public String getPrefix()
    {
        return prefix;
    }

    public String getResourcePath()
    {
        return resourcePath;
    }

    public String getWorkspacePath()
    {
        return "";
    }

    public String getWorkspaceName()
    {
        return "";
    }

    public boolean isSameWorkspace( DavResourceLocator locator )
    {
        return isSameWorkspace( locator.getWorkspaceName() );
    }

    public boolean isSameWorkspace( String workspaceName )
    {
        return getWorkspaceName().equals( workspaceName );
    }

    public String getHref( boolean isCollection )
    {
        // avoid doubled trailing '/' for the root item
        String suffix = ( isCollection && !isRootLocation() && !href.endsWith("/") ) ? "/" : "";
        return href + suffix;
    }

    public boolean isRootLocation()
    {
        return "/".equals( resourcePath );
    }

    public DavLocatorFactory getFactory()
    {
        return davLocatorFactory;
    }

    public String getRepositoryPath()
    {
        return getResourcePath();
    }

    /**
     * Computes the hash code from the href, which is built using the final fields prefix and resourcePath.
     * 
     * @return the hash code
     */
    public int hashCode()
    {
        return href.hashCode();
    }

    /**
     * Equality of path is achieved if the specified object is a <code>DavResourceLocator</code> object with the same
     * hash code.
     * 
     * @param obj the object to compare to
     * @return <code>true</code> if the 2 objects are equal; <code>false</code> otherwise
     */
    public boolean equals( Object obj )
    {
        if ( obj instanceof DavResourceLocator )
        {
            DavResourceLocator other = (DavResourceLocator) obj;
            return hashCode() == other.hashCode();
        }
        return false;
    }
}
