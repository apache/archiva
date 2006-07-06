package org.apache.maven.repository.proxy.web.action;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import com.opensymphony.xwork.Action;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.repository.proxy.ProxyException;
import org.apache.maven.repository.proxy.ProxyManager;
import org.apache.maven.repository.proxy.configuration.MavenProxyPropertyLoader;
import org.apache.maven.repository.proxy.configuration.ProxyConfiguration;
import org.apache.maven.repository.proxy.configuration.ValidationException;
import org.apache.maven.wagon.ResourceDoesNotExistException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

/**
 * This is the Action class responsible for processing artifact request,
 * relies on the RestfulActionMapper to map the artifact request to this action.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="org.apache.maven.repository.manager.web.action.RepositoryProxyAction"
 */
public class RepositoryProxyAction
    implements Action
{
    /**
     * logger instance
     */
    protected static final Log log = LogFactory.getLog( RepositoryProxyAction.class );

    public static final String NOTFOUND = "notFound";

    public static final String PROXYERROR = "proxyError";

    /**
     * file requested by the client,
     * TODO: validate the requestd file using na interceptor
     */
    private String requestedFile;

    /**
     * main proxy logic
     *
     * @plexus.requirement role="org.apache.maven.repository.proxy.ProxyManager"
     */
    private ProxyManager repositoryProxyManager;

    /**
     * configuration for the ProxyManager
     *
     * @plexus.requirement
     */
    private ProxyConfiguration proxyConfig;

    /**
     * the inputstream for the artifact file
     */
    private FileInputStream artifactStream;

    /**
     * the cached artifact file
     */
    private File cachedFile;

    /**
     * proxy configuration file
     * TODO: recode the configuration part when Configuration is finalized
     * TODO: this is only temporary
     */
    private String configFile;

    // setters and getters

    public void setProxyManager( ProxyManager manager )
    {
        repositoryProxyManager = manager;
    }

    public void setRequestedFile( String reqFile )
    {
        requestedFile = reqFile;
    }

    public String getRequestedFile()
    {
        return requestedFile;
    }

    public FileInputStream getArtifactStream()
    {
        return artifactStream;
    }

    public File getCachedFile()
    {
        return cachedFile;
    }

    public void setConfigFile( String fileName )
    {
        configFile = fileName;
    }

    /**
     * entry-point
     */
    public String execute()
        throws MalformedURLException, IOException, ValidationException
    {
        try
        {
            MavenProxyPropertyLoader loader = new MavenProxyPropertyLoader();
            proxyConfig = loader.load( new FileInputStream( configFile ) );
            repositoryProxyManager.setConfiguration( proxyConfig );
            cachedFile = repositoryProxyManager.get( requestedFile );
            artifactStream = new FileInputStream( cachedFile );
        }
        catch ( ResourceDoesNotExistException ex )
        {
            log.info( "[not found] " + ex.getMessage() );
            return NOTFOUND;
        }
        catch ( ProxyException ex )
        {
            log.info( "[proxy error] " + ex.getMessage() );
            return PROXYERROR;
        }
        catch ( FileNotFoundException ex )
        {
            log.info( "[not found] " + ex.getMessage() );
            return NOTFOUND;
        }

        return SUCCESS;
    }
}
