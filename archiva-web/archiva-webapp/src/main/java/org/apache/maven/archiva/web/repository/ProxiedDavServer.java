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

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.proxy.ProxyException;
import org.apache.maven.archiva.proxy.RepositoryProxyConnectors;
import org.apache.maven.archiva.repository.ArchivaConfigurationAdaptor;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayoutFactory;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.archiva.repository.metadata.MetadataTools;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.codehaus.plexus.webdav.AbstractDavServerComponent;
import org.codehaus.plexus.webdav.DavServerComponent;
import org.codehaus.plexus.webdav.DavServerException;
import org.codehaus.plexus.webdav.servlet.DavServerRequest;
import org.codehaus.plexus.webdav.util.WebdavMethodUtil;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * ProxiedDavServer
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.codehaus.plexus.webdav.DavServerComponent"
 * role-hint="proxied" instantiation-strategy="per-lookup"
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
    private MetadataTools metadataTools;

    /**
     * @plexus.requirement
     */
    private BidirectionalRepositoryLayoutFactory layoutFactory;

    private BidirectionalRepositoryLayout layout;

    private ManagedRepositoryConfiguration repositoryConfiguration;

    private ArchivaRepository managedRepository;

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

        Configuration config = archivaConfiguration.getConfiguration();

        repositoryConfiguration = config.findManagedRepositoryById( getPrefix() );

        managedRepository =
            ArchivaConfigurationAdaptor.toArchivaRepository( repositoryConfiguration );

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
            fetchContentFromProxies( request );
        }
        else
        {
            // Create parent directories that don't exist when writing a file
            // This actually makes this implementation not compliant to the
            // WebDAV RFC - but we have enough knowledge
            // about how the collection is being used to do this reasonably and
            // some versions of Maven's WebDAV don't
            // correctly create the collections themselves.
            File rootDirectory = getRootDirectory();
            if ( rootDirectory != null )
            {
                new File( rootDirectory, request.getLogicalResource() ).getParentFile().mkdirs();
            }
        }

        davServer.process( request, response );
    }

    private void fetchContentFromProxies( DavServerRequest request )
        throws ServletException
    {
        String resource = request.getLogicalResource();

        if ( resource.endsWith( ".sha1" ) || resource.endsWith( ".md5" ) )
        {
            // Checksums are fetched with artifact / metadata.
            return;
        }

        // Is it a Metadata resource?
        if ( resource.endsWith( "/" + MetadataTools.MAVEN_METADATA ) )
        {
            ProjectReference project;
            VersionedReference versioned;

            try
            {

                versioned = metadataTools.toVersionedReference( resource );
                if ( versioned != null )
                {
                    connectors.fetchFromProxies( managedRepository, versioned );
                    request.getRequest().setPathInfo( metadataTools.toPath( versioned ) );
                    return;
                }
            }
            catch ( RepositoryMetadataException e )
            {
                /* eat it */
            }
            catch ( ProxyException e )
            {
                throw new ServletException( "Unable to fetch versioned metadata resource.", e );
            }

            try
            {
                project = metadataTools.toProjectReference( resource );
                if ( project != null )
                {
                    connectors.fetchFromProxies( managedRepository, project );
                    request.getRequest().setPathInfo( metadataTools.toPath( project ) );
                }
            }
            catch ( RepositoryMetadataException e )
            {
                /* eat it */
            }
            catch ( ProxyException e )
            {
                throw new ServletException( "Unable to fetch project metadata resource.", e );
            }
        }

        // Not any of the above? Then it's gotta be an artifact reference.
        ArtifactReference artifact;
        BidirectionalRepositoryLayout resourceLayout;

        try
        {
            resourceLayout = layoutFactory.getLayoutForPath( resource );
        }
        catch ( LayoutException e )
        {
            /* invalid request - eat it */
            return;
        }

        try
        {
            artifact = resourceLayout.toArtifactReference( resource );
            if ( artifact != null )
            {
                applyServerSideRelocation( artifact );

                connectors.fetchFromProxies( managedRepository, artifact );
                request.getRequest().setPathInfo( layout.toPath( artifact ) );
                return;
            }
        }
        catch ( LayoutException e )
        {
            /* eat it */
        }
        catch ( ProxyException e )
        {
            throw new ServletException( "Unable to fetch artifact resource.", e );
        }
    }

    /**
     * A relocation capable client will request the POM prior to the artifact,
     * and will then read meta-data and do client side relocation. A simplier
     * client (like maven 1) will only request the artifact and not use the
     * metadatas.
     * <p>
     * For such clients, archiva does server-side relocation by reading itself
     * the &lt;relocation&gt; element in metadatas and serving the expected
     * artifact.
     */
    protected void applyServerSideRelocation( ArtifactReference artifact )
        throws ProxyException
    {
        if ( "pom".equals( artifact.getType() ) )
        {
            return;
        }

        // Build the artifact POM reference
        ArtifactReference pomReference = new ArtifactReference();
        pomReference.setGroupId( artifact.getGroupId() );
        pomReference.setArtifactId( artifact.getArtifactId() );
        pomReference.setVersion( artifact.getVersion() );
        pomReference.setType( "pom" );

        // Get the artifact POM from proxied repositories if needed
        connectors.fetchFromProxies( managedRepository, pomReference );

        // Open and read the POM from the managed repo
        File pom = new File( getRootDirectory(), layout.toPath( pomReference ) );
        try
        {
            Model model = new MavenXpp3Reader().read( new FileReader( pom ) );
            DistributionManagement dist = model.getDistributionManagement();
            if ( dist != null )
            {
                Relocation relocation = dist.getRelocation();
                if ( relocation != null )
                {
                    // artifact is relocated : update the repositoryPath
                    if ( relocation.getGroupId() != null )
                    {
                        artifact.setGroupId( relocation.getGroupId() );
                    }
                    if ( relocation.getArtifactId() != null )
                    {
                        artifact.setArtifactId( relocation.getArtifactId() );
                    }
                    if ( relocation.getVersion() != null )
                    {
                        artifact.setVersion( relocation.getVersion() );
                    }
                }
            }
        }
        catch ( FileNotFoundException e )
        {
            // Artifact has no POM in repo : ignore
        }
        catch ( Exception e )
        {
            // invalid POM : ignore
        }
    }

    public ManagedRepositoryConfiguration getRepositoryConfiguration()
    {
        return repositoryConfiguration;
    }
}
