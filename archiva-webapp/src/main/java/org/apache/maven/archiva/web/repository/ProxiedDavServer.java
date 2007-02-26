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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ConfiguredRepositoryFactory;
import org.apache.maven.archiva.configuration.ProxiedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.Proxy;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.proxy.ProxyException;
import org.apache.maven.archiva.proxy.ProxyRequestHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.codehaus.plexus.webdav.AbstractDavServerComponent;
import org.codehaus.plexus.webdav.DavServerComponent;
import org.codehaus.plexus.webdav.DavServerException;
import org.codehaus.plexus.webdav.servlet.DavServerRequest;
import org.codehaus.plexus.webdav.util.WebdavMethodUtil;

import sun.security.action.GetLongAction;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * ProxiedDavServer
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.codehaus.plexus.webdav.DavServerComponent"
 * role-hint="proxied"
 * instantiation-strategy="per-lookup"
 */
public class ProxiedDavServer
    extends AbstractDavServerComponent
{
    /**
     * @plexus.requirement role-hint="simple"
     */
    private DavServerComponent davServer;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.proxy.ProxyRequestHandler"
     * @todo seems to be a bug in qdox that the role above is required
     */
    private ProxyRequestHandler proxyRequestHandler;

    /**
     * @plexus.requirement
     */
    private ConfiguredRepositoryFactory repositoryFactory;

    private RepositoryConfiguration repositoryConfiguration;

    private ArtifactRepository managedRepository;

    private List/*<ArtifactRepository>*/proxiedRepositories;

    private ProxyInfo wagonProxy;

    public String getPrefix()
    {
        return davServer.getPrefix();
    }

    public File getRootDirectory()
    {
        return davServer.getRootDirectory();
    }

    public void setPrefix( String prefix )
    {
        davServer.setPrefix( prefix );
    }

    public void setRootDirectory( File rootDirectory )
    {
        davServer.setRootDirectory( rootDirectory );
    }

    public void init( ServletConfig servletConfig )
        throws DavServerException
    {
        davServer.init( servletConfig );

        proxiedRepositories = new ArrayList();

        Configuration config = archivaConfiguration.getConfiguration();

        wagonProxy = createWagonProxy( config.getProxy() );

        repositoryConfiguration = config.getRepositoryByUrlName( getPrefix() );

        managedRepository = repositoryFactory.createRepository( repositoryConfiguration );

        for ( Iterator i = config.getProxiedRepositories().iterator(); i.hasNext(); )
        {
            ProxiedRepositoryConfiguration proxiedRepoConfig = (ProxiedRepositoryConfiguration) i.next();

            if ( proxiedRepoConfig.getManagedRepository().equals( repositoryConfiguration.getId() ) )
            {
                proxiedRepositories.add( repositoryFactory.createProxiedRepository( proxiedRepoConfig ) );
            }
        }
    }

    public void process( DavServerRequest request, HttpServletResponse response )
        throws DavServerException, ServletException, IOException
    {
        if ( WebdavMethodUtil.isReadMethod( request.getRequest().getMethod() ) )
        {
            if ( !hasResource( request.getLogicalResource() ) )
            {
                fetchContentFromProxies( request );
            }
        }

        davServer.process( request, response );
    }

    private void fetchContentFromProxies( DavServerRequest request )
        throws ServletException
    {
        try
        {
            proxyRequestHandler.get( request.getLogicalResource(), this.proxiedRepositories, this.managedRepository,
                                     this.wagonProxy );
        }
        catch ( ResourceDoesNotExistException e )
        {
            // TODO: getLogger().info( "Unable to fetch resource, it does not exist.", e );
            // return an HTTP 404 instead of HTTP 500 error.
            return;
        }
        catch ( ProxyException e )
        {
            throw new ServletException( "Unable to fetch resource.", e );
        }
    }

    private ProxyInfo createWagonProxy( Proxy proxy )
    {
        ProxyInfo proxyInfo = null;
        if ( proxy != null && StringUtils.isNotEmpty( proxy.getHost() ) )
        {
            proxyInfo = new ProxyInfo();
            proxyInfo.setHost( proxy.getHost() );
            proxyInfo.setPort( proxy.getPort() );
            proxyInfo.setUserName( proxy.getUsername() );
            proxyInfo.setPassword( proxy.getPassword() );
            proxyInfo.setNonProxyHosts( proxy.getNonProxyHosts() );
            proxyInfo.setType( proxy.getProtocol() );
        }
        return proxyInfo;
    }

    public RepositoryConfiguration getRepositoryConfiguration()
    {
        return repositoryConfiguration;
    }
}
