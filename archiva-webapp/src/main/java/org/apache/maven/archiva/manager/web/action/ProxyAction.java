package org.apache.maven.archiva.manager.web.action;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.opensymphony.xwork.ActionSupport;
import org.apache.maven.archiva.proxy.ProxyException;
import org.apache.maven.archiva.proxy.ProxyManager;
import org.apache.maven.wagon.ResourceDoesNotExistException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Proxy functionality.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="proxyAction"
 */
public class ProxyAction
    extends ActionSupport
{
    /**
     * @plexus.requirement
     */
    private ProxyManager proxyManager;

    private String path;

    private String filename;

    private String contentType;

    private static final String NOT_FOUND = "notFound";

    private InputStream artifactStream;

    public String execute()
        throws ProxyException
    {
        try
        {
            File file = proxyManager.get( path );

            artifactStream = new FileInputStream( file );

            // TODO: could be better
            contentType = "application/octet-stream";

            filename = file.getName();
        }
        catch ( ResourceDoesNotExistException e )
        {
            // TODO: set message?
            return NOT_FOUND;
        }
        catch ( FileNotFoundException e )
        {
            // TODO: set message?
            return NOT_FOUND;
        }

        return SUCCESS;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath( String path )
    {
        this.path = path;
    }

    public String getFilename()
    {
        return filename;
    }

    public String getContentType()
    {
        return contentType;
    }

    public InputStream getArtifactStream()
    {
        return artifactStream;
    }
}
