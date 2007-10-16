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
import org.codehaus.plexus.webdav.AbstractDavServerComponent;
import org.codehaus.plexus.webdav.DavServerComponent;
import org.codehaus.plexus.webdav.DavServerException;
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
        if ( WebdavMethodUtil.isReadMethod( request.getRequest().getMethod() ) )
        {
            fetchContentFromProxies( request );
        }
        else
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
        }

        // [MRM-503] - Metadata file need Pragma:no-cache response header.
        if ( request.getLogicalResource().endsWith( "/maven-metadata.xml" ) )
        {
            response.addHeader( "Pragma", "no-cache" );
            response.addHeader( "Cache-Control", "no-cache" );
        }

        // TODO: [MRM-524] determine http caching options for other types of files (artifacts, sha1, md5, snapshots)

        if( resourceExists( request ) )
        {
            davServer.process( request, response );
        }
        else
        {
            respondResourceMissing( request, response );
        }
    }

    private void respondResourceMissing( DavServerRequest request, HttpServletResponse response )
    {
        response.setStatus( HttpServletResponse.SC_NOT_FOUND );

        try
        {
            StringBuffer missingUrl = new StringBuffer();
            missingUrl.append( request.getRequest().getScheme() ).append( "://" );
            missingUrl.append( request.getRequest().getServerName() ).append( ":" );
            missingUrl.append( request.getRequest().getServerPort() );
            missingUrl.append( request.getRequest().getServletPath() );
            // missingUrl.append( request.getRequest().getPathInfo() );

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

            out.println( "</body></html>" );

            out.flush();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
    }

    private boolean resourceExists( DavServerRequest request )
    {
        String resource = request.getLogicalResource();
        File resourceFile = new File( managedRepository.getRepoRoot(), resource );
        return resourceFile.exists();
    }

    private void fetchContentFromProxies( DavServerRequest request )
        throws ServletException
    {
        String resource = request.getLogicalResource();
        
        // Cleanup bad requests from maven 1.
        // Often seen is a double slash.
        // example: http://hostname:8080/archiva/repository/internal//pmd/jars/pmd-3.0.jar
        if ( resource.startsWith( "/" ) )
        {
            resource = resource.substring( 1 );
        }

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

    public ManagedRepositoryContent getRepository()
    {
        return managedRepository;
    }
}
