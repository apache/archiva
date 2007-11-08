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

import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.proxy.ProxyException;
import org.apache.maven.archiva.proxy.RepositoryProxyConnectors;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.apache.maven.archiva.repository.content.RepositoryRequest;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.archiva.repository.metadata.MetadataTools;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.plexus.webdav.AbstractDavServerComponent;
import org.codehaus.plexus.webdav.DavServerComponent;
import org.codehaus.plexus.webdav.DavServerException;
import org.codehaus.plexus.webdav.DavServerListener;
import org.codehaus.plexus.webdav.servlet.DavServerRequest;
import org.codehaus.plexus.webdav.util.WebdavMethodUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

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
    private RepositoryContentFactory repositoryFactory;

    /**
     * @plexus.requirement
     */
    private RepositoryRequest repositoryRequest;

    /**
     * @plexus.requirement role-hint="default"
     */
    private RepositoryProxyConnectors connectors;

    /**
     * @plexus.requirement
     */
    private MetadataTools metadataTools;

    private ManagedRepositoryContent managedRepository;

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

        try
        {
            managedRepository = repositoryFactory.getManagedRepositoryContent( getPrefix() );
        }
        catch ( RepositoryNotFoundException e )
        {
            throw new DavServerException( e.getMessage(), e );
        }
        catch ( RepositoryException e )
        {
            throw new DavServerException( e.getMessage(), e );
        }
    }

    public void process( DavServerRequest request, HttpServletResponse response )
        throws DavServerException, ServletException, IOException
    {
        boolean isGet = WebdavMethodUtil.isReadMethod( request.getRequest().getMethod() );
        boolean isPut = WebdavMethodUtil.isWriteMethod( request.getRequest().getMethod() );
        
        if ( isGet )
        {
            // Default behaviour is to treat the resource natively.
            String resource = request.getLogicalResource();
            File resourceFile = new File( managedRepository.getRepoRoot(), resource );

            // If this a directory resource, then we are likely browsing.
            if ( resourceFile.exists() && resourceFile.isDirectory() )
            {
                String requestURL = request.getRequest().getRequestURL().toString();
                
                // [MRM-440] - If webdav URL lacks a trailing /, navigating to all links in the listing return 404.
                if( !requestURL.endsWith( "/" ) )
                {
                    String redirectToLocation = requestURL + "/";
                    response.sendRedirect( redirectToLocation );
                    return;
                }
                
                // Process the request.
                davServer.process( request, response );
                
                // All done.
                return;
            }

            // At this point the incoming request can either be in default or legacy layout format.
            try
            {
                // Perform an adjustment of the resource to the managed repository expected path.
                resource = repositoryRequest.toNativePath( request.getLogicalResource(), managedRepository );
                resourceFile = new File( managedRepository.getRepoRoot(), resource );

                // Adjust the pathInfo resource to be in the format that the dav server impl expects.
                request.getRequest().setPathInfo( resource );

                // Attempt to fetch the resource from any defined proxy.
                fetchContentFromProxies( request, resource );
            }
            catch ( LayoutException e )
            {
                // Invalid resource, pass it on.
                respondResourceMissing( request, response, e );

                // All done.
                return;
            }

            if ( resourceFile.exists() )
            {
                // [MRM-503] - Metadata file need Pragma:no-cache response header.
                if ( request.getLogicalResource().endsWith( "/maven-metadata.xml" ) )
                {
                    response.addHeader( "Pragma", "no-cache" );
                    response.addHeader( "Cache-Control", "no-cache" );
                }

                // TODO: [MRM-524] determine http caching options for other types of files (artifacts, sha1, md5, snapshots)

                davServer.process( request, response );
            }
            else
            {
                respondResourceMissing( request, response, null );
            }
        }

        if ( isPut )
        {
            /* Create parent directories that don't exist when writing a file
             * This actually makes this implementation not compliant to the
             * WebDAV RFC - but we have enough knowledge
             * about how the collection is being used to do this reasonably and
             * some versions of Maven's WebDAV don't
             * correctly create the collections themselves.
             */

            File rootDirectory = getRootDirectory();
            if ( rootDirectory != null )
            {
                new File( rootDirectory, request.getLogicalResource() ).getParentFile().mkdirs();
            }
            
            // Allow the dav server to process the put request.
            davServer.process( request, response );
            
            // All done.
            return;
        }
    }

    private void respondResourceMissing( DavServerRequest request, HttpServletResponse response, Throwable t )
    {
        response.setStatus( HttpServletResponse.SC_NOT_FOUND );

        try
        {
            StringBuffer missingUrl = new StringBuffer();
            missingUrl.append( request.getRequest().getScheme() ).append( "://" );
            missingUrl.append( request.getRequest().getServerName() ).append( ":" );
            missingUrl.append( request.getRequest().getServerPort() );
            missingUrl.append( request.getRequest().getServletPath() );

            String message = "Error 404 Not Found";

            PrintWriter out = new PrintWriter( response.getOutputStream() );

            response.setContentType( "text/html; charset=\"UTF-8\"" );

            out.println( "<html>" );
            out.println( "<head><title>" + message + "</title></head>" );
            out.println( "<body>" );

            out.print( "<p><h1>" );
            out.print( message );
            out.println( "</h1></p>" );

            out.print( "<p>The following resource does not exist: <a href=\"" );
            out.print( missingUrl.toString() );
            out.println( "\">" );
            out.print( missingUrl.toString() );
            out.println( "</a></p>" );
            
            if ( t != null )
            {
                out.println( "<pre>" );
                t.printStackTrace( out );
                out.println( "</pre>" );
            }

            out.println( "</body></html>" );

            out.flush();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
    }

    private void fetchContentFromProxies( DavServerRequest request, String resource )
        throws ServletException
    {
        if ( repositoryRequest.isSupportFile( resource ) )
        {
            // Checksums are fetched with artifact / metadata.
            
            // Need to adjust the path for the checksum resource.
            return;
        }

        // Is it a Metadata resource?
        if ( repositoryRequest.isDefault( resource ) && repositoryRequest.isMetadata( resource ) )
        {
            if ( fetchMetadataFromProxies( request, resource ) )
            {
                return;
            }
        }

        // Not any of the above? Then it's gotta be an artifact reference.
        try
        {
            // Get the artifact reference in a layout neutral way.
            ArtifactReference artifact = repositoryRequest.toArtifactReference( resource );
            
            if ( artifact != null )
            {
                applyServerSideRelocation( artifact );

                connectors.fetchFromProxies( managedRepository, artifact );
                
                // Set the path to the resource using managed repository specific layout format.
                request.getRequest().setPathInfo( managedRepository.toPath( artifact ) );
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

    private boolean fetchMetadataFromProxies( DavServerRequest request, String resource )
        throws ServletException
    {
        ProjectReference project;
        VersionedReference versioned;

        try
        {

            versioned = metadataTools.toVersionedReference( resource );
            if ( versioned != null )
            {
                connectors.fetchFromProxies( managedRepository, versioned );
                return true;
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
                return true;
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
        
        return false;
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
        File pom = managedRepository.toFile( pomReference );
        
        if( !pom.exists() )
        {
            return;
        }
        
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
        catch ( IOException e )
        {
            // Unable to read POM : ignore.
        }
        catch ( XmlPullParserException e )
        {
            // Invalid POM : ignore
        }
    }
    
    @Override
    public void addListener( DavServerListener listener )
    {
        super.addListener( listener );
        davServer.addListener( listener );
    }
    
    @Override
    public boolean isUseIndexHtml()
    {
        return davServer.isUseIndexHtml();
    }
    
    @Override
    public boolean hasResource( String resource )
    {
        return davServer.hasResource( resource );
    }
    
    @Override
    public void removeListener( DavServerListener listener )
    {
        davServer.removeListener( listener );
    }
    
    @Override
    public void setUseIndexHtml( boolean useIndexHtml )
    {
        super.setUseIndexHtml( useIndexHtml );
        davServer.setUseIndexHtml( useIndexHtml );
    }
    
    public ManagedRepositoryContent getRepository()
    {
        return managedRepository;
    }
}
