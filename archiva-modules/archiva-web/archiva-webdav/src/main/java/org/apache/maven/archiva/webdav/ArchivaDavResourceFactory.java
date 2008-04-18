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

import com.opensymphony.xwork.ActionContext;
import org.apache.jackrabbit.webdav.*;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.archiva.repository.content.RepositoryRequest;
import org.apache.maven.archiva.repository.audit.AuditListener;
import org.apache.maven.archiva.repository.audit.Auditable;
import org.apache.maven.archiva.repository.audit.AuditEvent;
import org.apache.maven.archiva.repository.metadata.MetadataTools;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.maven.archiva.webdav.util.WebdavMethodUtil;
import org.apache.maven.archiva.webdav.util.MimeTypes;
import org.apache.maven.archiva.webdav.util.RepositoryPathUtil;
import org.apache.maven.archiva.proxy.RepositoryProxyConnectors;
import org.apache.maven.archiva.common.utils.PathUtil;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.policies.ProxyDownloadException;
import org.apache.maven.archiva.security.ArchivaXworkUser;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

/**
 * @author <a href="mailto:james@atlassian.com">James William Dumay</a>
 * @plexus.component role="org.apache.maven.archiva.webdav.ArchivaDavResourceFactory"
 */
public class ArchivaDavResourceFactory implements DavResourceFactory, Auditable
{
    private Logger log = LoggerFactory.getLogger(ArchivaDavResourceFactory.class);

    /**
     * @plexus.requirement role="org.apache.maven.archiva.repository.audit.AuditListener"
     */
    private List<AuditListener> auditListeners = new ArrayList<AuditListener>();

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

    /**
     * @plexus.requirement
     */
    private MimeTypes mimeTypes;

    public DavResource createResource(final DavResourceLocator locator, final DavServletRequest request, final DavServletResponse response) throws DavException
    {
        final ManagedRepositoryContent managedRepository = getManagedRepository(locator.getWorkspaceName());
        final LogicalResource logicalResource = new LogicalResource(RepositoryPathUtil.getLogicalResource(locator.getResourcePath()));

        DavResource resource = null;

        if (managedRepository != null)
        {
            final boolean isGet = WebdavMethodUtil.isReadMethod( request.getMethod() );
            final boolean isPut = WebdavMethodUtil.isWriteMethod( request.getMethod() );

            if (isGet)
            {
                resource = doGet(managedRepository, request, locator, logicalResource);
            }

            if (isPut)
            {
                resource = doPut(managedRepository, request, locator, logicalResource);
            }
        }
        else
        {
            throw new DavException(HttpServletResponse.SC_NOT_FOUND, "Repository does not exist");
        }

        if (resource != null)
        {
            setHeaders(locator, response);
            return resource;
        }

        throw new DavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not get resource for method " + request.getMethod());
    }

    public DavResource createResource(final DavResourceLocator locator, final DavSession davSession) throws DavException
    {
        final ManagedRepositoryContent managedRepository = getManagedRepository(locator.getWorkspaceName());
        final String logicalResource = RepositoryPathUtil.getLogicalResource(locator.getResourcePath());
        final File resourceFile = new File ( managedRepository.getRepoRoot(), logicalResource);
        
        return new ArchivaDavResource(resourceFile.getAbsolutePath(), logicalResource, mimeTypes, locator, this, null);
    }

    private DavResource doGet(ManagedRepositoryContent managedRepository, DavServletRequest request, DavResourceLocator locator, LogicalResource logicalResource) throws DavException
    {
        File resourceFile = new File ( managedRepository.getRepoRoot(), logicalResource.getPath());
        ArchivaDavResource resource = new ArchivaDavResource(resourceFile.getAbsolutePath(), logicalResource.getPath(), mimeTypes, locator, this, null);

        if ( !resource.isCollection() )
        {
            // At this point the incoming request can either be in default or
            // legacy layout format.
            try
            {
                boolean fromProxy = fetchContentFromProxies(managedRepository, request, logicalResource );

                // Perform an adjustment of the resource to the managed
                // repository expected path.
                String localResourcePath = repositoryRequest.toNativePath( logicalResource.getPath(), managedRepository );
                resourceFile = new File( managedRepository.getRepoRoot(), localResourcePath );

                boolean previouslyExisted = resourceFile.exists();

                // Attempt to fetch the resource from any defined proxy.
                if ( fromProxy )
                {
                    processAuditEvents(request, locator.getWorkspaceName(), logicalResource.getPath(), previouslyExisted, resourceFile, " (proxied)");
                }
                resource = new ArchivaDavResource(resourceFile.getAbsolutePath(), logicalResource.getPath(), mimeTypes, locator, this, null);

            }
            catch ( LayoutException e )
            {
                throw new DavException(HttpServletResponse.SC_NOT_FOUND, e);
            }
        }
        return resource;
    }

    private DavResource doPut(ManagedRepositoryContent managedRepository, DavServletRequest request, DavResourceLocator locator, LogicalResource logicalResource) throws DavException
    {
        /*
         * Create parent directories that don't exist when writing a file
         * This actually makes this implementation not compliant to the
         * WebDAV RFC - but we have enough knowledge about how the
         * collection is being used to do this reasonably and some versions
         * of Maven's WebDAV don't correctly create the collections
         * themselves.
         */

        File rootDirectory = new File(managedRepository.getRepoRoot());
        File destDir = new File( rootDirectory, logicalResource.getPath() ).getParentFile();
        if ( !destDir.exists() )
        {
            destDir.mkdirs();
            String relPath =
                PathUtil.getRelative( rootDirectory.getAbsolutePath(), destDir );
            triggerAuditEvent(request, logicalResource.getPath(), relPath, AuditEvent.CREATE_DIR );
        }

        File resourceFile = new File( managedRepository.getRepoRoot(), logicalResource.getPath() );

        boolean previouslyExisted = resourceFile.exists();

        processAuditEvents(request, locator.getWorkspaceName(), logicalResource.getPath(), previouslyExisted, resourceFile, null );

        return new ArchivaDavResource(resourceFile.getAbsolutePath(), logicalResource.getPath(), mimeTypes, locator, this, null);
    }

    private boolean fetchContentFromProxies( ManagedRepositoryContent managedRepository, DavServletRequest request, LogicalResource resource )
        throws DavException
    {
        if ( repositoryRequest.isSupportFile( resource.getPath() ) )
        {
            // Checksums are fetched with artifact / metadata.

            // Need to adjust the path for the checksum resource.
            return false;
        }

        // Is it a Metadata resource?
        if ( repositoryRequest.isDefault( resource.getPath() ) && repositoryRequest.isMetadata( resource.getPath() ) )
        {
            return fetchMetadataFromProxies(managedRepository, request, resource );
        }

        // Not any of the above? Then it's gotta be an artifact reference.
        try
        {
            // Get the artifact reference in a layout neutral way.
            ArtifactReference artifact = repositoryRequest.toArtifactReference( resource.getPath() );

            if ( artifact != null )
            {
                applyServerSideRelocation(managedRepository, artifact );

                File proxiedFile = connectors.fetchFromProxies( managedRepository, artifact );

                resource.setPath( managedRepository.toPath( artifact ) );

                return ( proxiedFile != null );
            }
        }
        catch ( LayoutException e )
        {
            /* eat it */
        }
        catch ( ProxyDownloadException e )
        {
            log.error(e.getMessage(), e);
            throw new DavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to fetch artifact resource.");
        }
        return false;
    }

    private boolean fetchMetadataFromProxies(ManagedRepositoryContent managedRepository, DavServletRequest request, LogicalResource resource )
        throws DavException
    {
        ProjectReference project;
        VersionedReference versioned;

        try
        {

            versioned = metadataTools.toVersionedReference( resource.getPath() );
            if ( versioned != null )
            {
                connectors.fetchFromProxies( managedRepository, versioned );
                return true;
            }
        }
        catch ( RepositoryMetadataException e )
        {
            log.error(e.getMessage(), e);
        }

        try
        {
            project = metadataTools.toProjectReference( resource.getPath() );
            if ( project != null )
            {
                connectors.fetchFromProxies( managedRepository, project );
                return true;
            }
        }
        catch ( RepositoryMetadataException e )
        {
            log.error(e.getMessage(), e);
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
    protected void applyServerSideRelocation( ManagedRepositoryContent managedRepository, ArtifactReference artifact )
        throws ProxyDownloadException
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

        if ( !pom.exists() )
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

    private void processAuditEvents( DavServletRequest request, String repositoryId, String resource,
                                     boolean previouslyExisted, File resourceFile, String suffix )
    {
        if ( suffix == null )
        {
            suffix = "";
        }

        // Process Create Audit Events.
        if ( !previouslyExisted && resourceFile.exists() )
        {
            if ( resourceFile.isFile() )
            {
                triggerAuditEvent( request, repositoryId, resource, AuditEvent.CREATE_FILE + suffix );
            }
            else if ( resourceFile.isDirectory() )
            {
                triggerAuditEvent( request, repositoryId, resource, AuditEvent.CREATE_DIR + suffix );
            }
        }
        // Process Remove Audit Events.
        else if ( previouslyExisted && !resourceFile.exists() )
        {
            if ( resourceFile.isFile() )
            {
                triggerAuditEvent( request, repositoryId, resource, AuditEvent.REMOVE_FILE + suffix );
            }
            else if ( resourceFile.isDirectory() )
            {
                triggerAuditEvent( request, repositoryId, resource, AuditEvent.REMOVE_DIR + suffix );
            }
        }
        // Process modify events.
        else
        {
            if ( resourceFile.isFile() )
            {
                triggerAuditEvent( request, repositoryId, resource, AuditEvent.MODIFY_FILE + suffix );
            }
        }
    }

    private void triggerAuditEvent( String user, String remoteIP, String repositoryId, String resource, String action )
    {
        AuditEvent event = new AuditEvent( repositoryId, user, resource, action );
        event.setRemoteIP( remoteIP );

        for ( AuditListener listener : auditListeners )
        {
            listener.auditEvent( event );
        }
    }

    private void triggerAuditEvent( DavServletRequest request, String repositoryId, String resource, String action )
    {
        triggerAuditEvent( ArchivaXworkUser.getActivePrincipal( ActionContext.getContext().getSession() ), getRemoteIP( request ), repositoryId, resource, action );
    }

    private String getRemoteIP( DavServletRequest request )
    {
        return request.getRemoteAddr();
    }

    public void addAuditListener( AuditListener listener )
    {
        this.auditListeners.add( listener );
    }

    public void clearAuditListeners()
    {
        this.auditListeners.clear();
    }

    public void removeAuditListener( AuditListener listener )
    {
        this.auditListeners.remove( listener );
    }

    private void setHeaders(DavResourceLocator locator, DavServletResponse response)
    {
        // [MRM-503] - Metadata file need Pragma:no-cache response
        // header.
        if ( locator.getResourcePath().endsWith( "/maven-metadata.xml" ) )
        {
            response.addHeader( "Pragma", "no-cache" );
            response.addHeader( "Cache-Control", "no-cache" );
        }

        // TODO: [MRM-524] determine http caching options for other types of files (artifacts, sha1, md5, snapshots)
    }

    private ManagedRepositoryContent getManagedRepository(String respositoryId) throws DavException
    {
        if (respositoryId != null)
        {
            try
            {
                return repositoryFactory.getManagedRepositoryContent(respositoryId);
            }
            catch (RepositoryNotFoundException e)
            {
                throw new DavException(HttpServletResponse.SC_NOT_FOUND, e);
            }
            catch (RepositoryException e)
            {
                throw new DavException(HttpServletResponse.SC_NOT_FOUND, e);
            }
        }
        return null;
    }

    class LogicalResource
    {
        private String path;

        public LogicalResource(String path)
        {
            this.path = path;
        }

        public String getPath()
        {
            return path;
        }

        public void setPath(String path)
        {
            this.path = path;
        }
    }
}
