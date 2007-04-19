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
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.ArchivaRepositoryMetadata;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.proxy.ProxyConnector;
import org.apache.maven.archiva.proxy.ProxyException;
import org.apache.maven.archiva.proxy.RepositoryProxyConnectors;
import org.apache.maven.archiva.repository.ArchivaConfigurationAdaptor;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayoutFactory;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.codehaus.plexus.webdav.AbstractDavServerComponent;
import org.codehaus.plexus.webdav.DavServerComponent;
import org.codehaus.plexus.webdav.DavServerException;
import org.codehaus.plexus.webdav.servlet.DavServerRequest;
import org.codehaus.plexus.webdav.util.WebdavMethodUtil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

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
     * @plexus.requirement role-hint="default"
     */
    private RepositoryProxyConnectors connectors;

    /**
     * @plexus.requirement
     */
    private BidirectionalRepositoryLayoutFactory layoutFactory;

    private BidirectionalRepositoryLayout layout;

    private RepositoryConfiguration repositoryConfiguration;

    private ArchivaRepository managedRepository;

    private List/*<ArtifactRepository>*/proxiedRepositories;

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

        repositoryConfiguration = config.findRepositoryById( getPrefix() );

        managedRepository = ArchivaConfigurationAdaptor.toArchivaRepository( repositoryConfiguration );

        try
        {
            layout = layoutFactory.getLayout( managedRepository.getLayoutType() );
        }
        catch ( LayoutException e )
        {
            throw new DavServerException( "Unable to initialize dav server: " + e.getMessage(), e );
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
        String resource = request.getLogicalResource();
        
        if( resource.endsWith( ".sha1" ) ||
            resource.endsWith( ".md5") )
        {
            // Checksums are fetched with artifact / metadata.
            return;
        }
        
        try
        {
            ProjectReference project;
            VersionedReference versioned;
            ArtifactReference artifact;
            
            artifact = layout.toArtifactReference( resource );
            if( artifact != null )
            {
                connectors.fetchFromProxies( managedRepository, artifact );
                return;
            }
            
            versioned = layout.toVersionedReference( resource );
            if( versioned != null )
            {
                connectors.fetchFromProxies( managedRepository, versioned );
                return;
            }
            
            project = layout.toProjectReference( resource );
            if( project != null )
            {
                connectors.fetchFromProxies( managedRepository, project );
                return;
            }
        }
        catch ( ResourceDoesNotExistException e )
        {
            // return an HTTP 404 instead of HTTP 500 error.
            return;
        }
        catch ( ProxyException e )
        {
            throw new ServletException( "Unable to fetch resource.", e );
        }
    }

    public RepositoryConfiguration getRepositoryConfiguration()
    {
        return repositoryConfiguration;
    }
}
