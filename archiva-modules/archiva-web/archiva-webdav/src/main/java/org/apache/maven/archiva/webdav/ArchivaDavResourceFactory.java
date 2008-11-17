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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.SimpleLockManager;
import org.apache.maven.archiva.common.utils.PathUtil;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.maven.archiva.model.ArchivaRepositoryMetadata;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.policies.ProxyDownloadException;
import org.apache.maven.archiva.proxy.RepositoryProxyConnectors;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.RepositoryNotFoundException;
import org.apache.maven.archiva.repository.audit.AuditEvent;
import org.apache.maven.archiva.repository.audit.AuditListener;
import org.apache.maven.archiva.repository.audit.Auditable;
import org.apache.maven.archiva.repository.content.RepositoryRequest;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.archiva.repository.metadata.MetadataTools;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataMerge;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataReader;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataWriter;
import org.apache.maven.archiva.repository.scanner.RepositoryContentConsumers;
import org.apache.maven.archiva.security.ArchivaXworkUser;
import org.apache.maven.archiva.security.ServletAuthenticator;
import org.apache.maven.archiva.webdav.util.MimeTypes;
import org.apache.maven.archiva.webdav.util.RepositoryPathUtil;
import org.apache.maven.archiva.webdav.util.WebdavMethodUtil;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Relocation;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.digest.ChecksumFile;
import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.digest.DigesterException;
import org.codehaus.plexus.redback.authentication.AuthenticationException;
import org.codehaus.plexus.redback.authentication.AuthenticationResult;
import org.codehaus.plexus.redback.authorization.AuthorizationException;
import org.codehaus.plexus.redback.authorization.UnauthorizedException;
import org.codehaus.plexus.redback.policy.AccountLockedException;
import org.codehaus.plexus.redback.policy.MustChangePasswordException;
import org.codehaus.plexus.redback.system.SecuritySession;
import org.codehaus.plexus.redback.system.SecuritySystemConstants;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.codehaus.redback.integration.filter.authentication.HttpAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opensymphony.xwork2.ActionContext;

/**
 * @author <a href="mailto:james@atlassian.com">James William Dumay</a>
 * @plexus.component role="org.apache.maven.archiva.webdav.ArchivaDavResourceFactory"
 */
public class ArchivaDavResourceFactory
    implements DavResourceFactory, Auditable
{   
    private static final String PROXIED_SUFFIX = " (proxied)";

    private static final String HTTP_PUT_METHOD = "PUT";

    private Logger log = LoggerFactory.getLogger( ArchivaDavResourceFactory.class );

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

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @plexus.requirement
     */
    private ServletAuthenticator servletAuth;

    /**
     * @plexus.requirement role-hint="basic"
     */
    private HttpAuthenticator httpAuth;

    /**
     * Lock Manager - use simple implementation from JackRabbit
     */
    private final LockManager lockManager = new SimpleLockManager();

    /** 
     * @plexus.requirement 
     */
    private RepositoryContentConsumers consumers;
    
    /**
     * @plexus.requirement
     */
    private ChecksumFile checksum;
        
    /**
     * @plexus.requirement role-hint="sha1"
     */
    private Digester digestSha1;

    /**
     * @plexus.requirement role-hint="md5";
     */
    private Digester digestMd5;
    
    /**
     * @plexus.requirement
     */
    private ArchivaXworkUser archivaXworkUser;
        
    public DavResource createResource( final DavResourceLocator locator, final DavServletRequest request,
                                       final DavServletResponse response )
        throws DavException
    {
        checkLocatorIsInstanceOfRepositoryLocator( locator );
        ArchivaDavResourceLocator archivaLocator = (ArchivaDavResourceLocator) locator;
        
        RepositoryGroupConfiguration repoGroupConfig =
            archivaConfiguration.getConfiguration().getRepositoryGroupsAsMap().get( archivaLocator.getRepositoryId() );
        List<String> repositories = new ArrayList<String>();

        boolean isGet = WebdavMethodUtil.isReadMethod( request.getMethod() );
        boolean isPut = WebdavMethodUtil.isWriteMethod( request.getMethod() );
        
        if ( repoGroupConfig != null )
        {
            if( WebdavMethodUtil.isWriteMethod( request.getMethod() ) )
            {
                throw new DavException( HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                                        "Write method not allowed for repository groups." );
            }
            repositories.addAll( repoGroupConfig.getRepositories() );

            // handle browse requests for virtual repos
            if ( RepositoryPathUtil.getLogicalResource( locator.getResourcePath() ).endsWith( "/" ) )
            {
                return getResource( request, repositories, archivaLocator );
            }
        }
        else
        {
            repositories.add( archivaLocator.getRepositoryId() );
        }

        //MRM-419 - Windows Webdav support. Should not 404 if there is no content.
        if (StringUtils.isEmpty(archivaLocator.getRepositoryId()))
        {
            throw new DavException(HttpServletResponse.SC_NO_CONTENT);
        }

        List<DavResource> availableResources = new ArrayList<DavResource>();
        List<String> resourcesInAbsolutePath = new ArrayList<String>();
        DavException e = null;
        
        for ( String repositoryId : repositories )
        {
            ManagedRepositoryContent managedRepository = null;

            try
            {
                managedRepository = getManagedRepository( repositoryId );                
            }
            catch ( DavException de )
            {
                throw new DavException( HttpServletResponse.SC_NOT_FOUND, "Invalid managed repository <" +
                    repositoryId + ">" );
            }
            
            DavResource resource = null;
            
            if ( !locator.getResourcePath().startsWith( ArchivaDavResource.HIDDEN_PATH_PREFIX ) )
            {                
                if ( managedRepository != null )
                {
                    try
                    {
                        if( isAuthorized( request, repositoryId ) )
                        {   
                            LogicalResource logicalResource =
                                new LogicalResource( RepositoryPathUtil.getLogicalResource( locator.getResourcePath() ) );

                            if ( isGet )
                            {
                                resource = doGet( managedRepository, request, archivaLocator, logicalResource );
                            }

                            if ( isPut )
                            {
                                resource = doPut( managedRepository, request, archivaLocator, logicalResource );                                
                            }
                        }
                    }
                    catch ( DavException de ) 
                    {                        
                        e = de;
                        continue;
                    }

                    if( resource == null )
                    {
                        e = new DavException( HttpServletResponse.SC_NOT_FOUND, "Resource does not exist" );
                    }
                    else
                    {                           
                        availableResources.add( resource );

                        String logicalResource = RepositoryPathUtil.getLogicalResource( locator.getResourcePath() );
                        resourcesInAbsolutePath.add( managedRepository.getRepoRoot() + logicalResource );                        
                    }
                }
                else
                {
                    e = new DavException( HttpServletResponse.SC_NOT_FOUND, "Repository does not exist" );
                }
            }
        }        
        
        if ( availableResources.isEmpty() )
        {
            throw e;
        }
        
        String requestedResource = request.getRequestURI();
        
        // MRM-872 : merge all available metadata
        // merge metadata only when requested via the repo group        
        if ( ( repositoryRequest.isMetadata( requestedResource ) || ( requestedResource.endsWith( "metadata.xml.sha1" ) || requestedResource.endsWith( "metadata.xml.md5" ) ) ) &&
            repoGroupConfig != null )
        {   
            // this should only be at the project level not version level!
            if( isProjectReference( requestedResource ) )
            {
                String artifactId = StringUtils.substringBeforeLast( requestedResource.replace( '\\', '/' ), "/" );
                artifactId = StringUtils.substringAfterLast( artifactId, "/" );
                
                ArchivaDavResource res = ( ArchivaDavResource ) availableResources.get( 0 );
                String filePath = StringUtils.substringBeforeLast( res.getLocalResource().getAbsolutePath().replace( '\\', '/' ), "/" );                                
                filePath = filePath + "/maven-metadata-" + repoGroupConfig.getId() + ".xml";
                
                // for MRM-872 handle checksums of the merged metadata files 
                if( repositoryRequest.isSupportFile( requestedResource ) )
                {
                    File metadataChecksum = new File( filePath + "." 
                              + StringUtils.substringAfterLast( requestedResource, "." ) );                    
                    if( metadataChecksum.exists() )
                    {
                        LogicalResource logicalResource =
                            new LogicalResource( RepositoryPathUtil.getLogicalResource( locator.getResourcePath() ) );
                                        
                        ArchivaDavResource metadataChecksumResource =
                            new ArchivaDavResource( metadataChecksum.getAbsolutePath(), logicalResource.getPath(), null,
                                                    request.getRemoteAddr(), request.getDavSession(), archivaLocator, this,
                                                    mimeTypes, auditListeners, consumers, archivaXworkUser );
                        availableResources.add( 0, metadataChecksumResource );
                    }
                }
                else
                {   // merge the metadata of all repos under group
                    ArchivaRepositoryMetadata mergedMetadata = new ArchivaRepositoryMetadata();
                    for ( String resourceAbsPath : resourcesInAbsolutePath )    
                    {   
                        try
                        {   
                            File metadataFile = new File( resourceAbsPath );
                            ArchivaRepositoryMetadata repoMetadata = RepositoryMetadataReader.read( metadataFile );
                            mergedMetadata = RepositoryMetadataMerge.merge( mergedMetadata, repoMetadata );
                        }
                        catch ( RepositoryMetadataException r )
                        {
                            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                                    "Error occurred while reading metadata file." );
                        }                
                    }        
                    
                    try
                    {   
                        File resourceFile = writeMergedMetadataToFile( mergedMetadata, filePath );   
                        
                        LogicalResource logicalResource =
                            new LogicalResource( RepositoryPathUtil.getLogicalResource( locator.getResourcePath() ) );
                                        
                        ArchivaDavResource metadataResource =
                            new ArchivaDavResource( resourceFile.getAbsolutePath(), logicalResource.getPath(), null,
                                                    request.getRemoteAddr(), request.getDavSession(), archivaLocator, this,
                                                    mimeTypes, auditListeners, consumers, archivaXworkUser );
                        availableResources.add( 0, metadataResource );
                    }
                    catch ( RepositoryMetadataException r )
                    {                
                        throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                                "Error occurred while writing metadata file." );
                    }
                    catch ( IOException ie )
                    {
                        throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Error occurred while generating checksum files." );
                    }
                    catch ( DigesterException de )
                    {
                        throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Error occurred while generating checksum files." );
                    }
                }
            }
        }
                
        DavResource resource = availableResources.get( 0 );               
        setHeaders(response, locator, resource );

        // compatibility with MRM-440 to ensure browsing the repository works ok
        if ( resource.isCollection() && !request.getRequestURI().endsWith("/" ) )
        {
            throw new BrowserRedirectException( resource.getHref() );
        }
        resource.addLockManager(lockManager);
        return resource;
    }

    public DavResource createResource( final DavResourceLocator locator, final DavSession davSession )
        throws DavException
    {
        checkLocatorIsInstanceOfRepositoryLocator( locator );
        ArchivaDavResourceLocator archivaLocator = (ArchivaDavResourceLocator) locator;

        DavResource resource = null;
        if ( !locator.getResourcePath().startsWith( ArchivaDavResource.HIDDEN_PATH_PREFIX ) )
        {
            ManagedRepositoryContent managedRepository = getManagedRepository( archivaLocator.getRepositoryId() );
            String logicalResource = RepositoryPathUtil.getLogicalResource( locator.getResourcePath() );
            File resourceFile = new File( managedRepository.getRepoRoot(), logicalResource );
            resource =
                new ArchivaDavResource( resourceFile.getAbsolutePath(), logicalResource,
                                        managedRepository.getRepository(), davSession, archivaLocator, this, mimeTypes,
                                        auditListeners, consumers, archivaXworkUser );
        }
        resource.addLockManager(lockManager);
        return resource;
    }

    private DavResource doGet( ManagedRepositoryContent managedRepository, DavServletRequest request,
                               ArchivaDavResourceLocator locator, LogicalResource logicalResource )
        throws DavException
    {
        File resourceFile = new File( managedRepository.getRepoRoot(), logicalResource.getPath() );
        
        //MRM-893, dont send back a file when user intentionally wants a directory
        if ( locator.getHref( false ).endsWith( "/" ) )
        {
            if ( ! resourceFile.isDirectory() )
            {
                //force a resource not found 
                return null;
            }
        }

        ArchivaDavResource resource =
            new ArchivaDavResource( resourceFile.getAbsolutePath(), logicalResource.getPath(),
                                    managedRepository.getRepository(), request.getRemoteAddr(),
                                    request.getDavSession(), locator, this, mimeTypes, auditListeners, consumers, archivaXworkUser );

        if ( !resource.isCollection() )
        {
            boolean previouslyExisted = resourceFile.exists();

            // At this point the incoming request can either be in default or
            // legacy layout format.
            boolean fromProxy = fetchContentFromProxies( managedRepository, request, logicalResource );

            try
            {
                // Perform an adjustment of the resource to the managed
                // repository expected path.
                String localResourcePath =
                    repositoryRequest.toNativePath( logicalResource.getPath(), managedRepository );
                resourceFile = new File( managedRepository.getRepoRoot(), localResourcePath );
            }
            catch ( LayoutException e )
            {
                if ( previouslyExisted )
                {
                    return resource;
                }
                throw new DavException( HttpServletResponse.SC_NOT_FOUND, e );
            }

            // Attempt to fetch the resource from any defined proxy.
            if ( fromProxy )
            {
                String repositoryId = locator.getRepositoryId();
                String event = ( previouslyExisted ? AuditEvent.MODIFY_FILE : AuditEvent.CREATE_FILE ) + PROXIED_SUFFIX;
                triggerAuditEvent( request.getRemoteAddr(), repositoryId, logicalResource.getPath(), event );
            }

            if ( !resourceFile.exists() )
            {
                resource = null;
            }
            else
            {
                resource =
                    new ArchivaDavResource( resourceFile.getAbsolutePath(), logicalResource.getPath(),
                                            managedRepository.getRepository(), request.getRemoteAddr(),
                                            request.getDavSession(), locator, this, mimeTypes, auditListeners,
                                            consumers, archivaXworkUser );
            }
        }
        return resource;
    }

    private DavResource doPut( ManagedRepositoryContent managedRepository, DavServletRequest request,
                               ArchivaDavResourceLocator locator, LogicalResource logicalResource )
        throws DavException
    {
        /*
         * Create parent directories that don't exist when writing a file This actually makes this implementation not
         * compliant to the WebDAV RFC - but we have enough knowledge about how the collection is being used to do this
         * reasonably and some versions of Maven's WebDAV don't correctly create the collections themselves.
         */

        File rootDirectory = new File( managedRepository.getRepoRoot() );
        File destDir = new File( rootDirectory, logicalResource.getPath() ).getParentFile();
        
        if ( request.getMethod().equals(HTTP_PUT_METHOD) && !destDir.exists() )
        {
            destDir.mkdirs();
            String relPath = PathUtil.getRelative( rootDirectory.getAbsolutePath(), destDir );
            triggerAuditEvent( request.getRemoteAddr(), logicalResource.getPath(), relPath, AuditEvent.CREATE_DIR );
        }
        
        File resourceFile = new File( managedRepository.getRepoRoot(), logicalResource.getPath() );        
                
        return new ArchivaDavResource( resourceFile.getAbsolutePath(), logicalResource.getPath(),
                                       managedRepository.getRepository(), request.getRemoteAddr(),
                                       request.getDavSession(), locator, this, mimeTypes, auditListeners, consumers, archivaXworkUser );
    }

    private boolean fetchContentFromProxies( ManagedRepositoryContent managedRepository, DavServletRequest request,
                                             LogicalResource resource )
        throws DavException
    {
        if ( repositoryRequest.isSupportFile( resource.getPath() ) )
        {
            File proxiedFile = connectors.fetchFromProxies( managedRepository, resource.getPath() );

            return ( proxiedFile != null );
        }

        // Is it a Metadata resource?
        if ( repositoryRequest.isDefault( resource.getPath() ) && repositoryRequest.isMetadata( resource.getPath() ) )
        {
            return connectors.fetchMetatadaFromProxies(managedRepository, resource.getPath()) != null;
        }

        // Not any of the above? Then it's gotta be an artifact reference.
        try
        {
            // Get the artifact reference in a layout neutral way.
            ArtifactReference artifact = repositoryRequest.toArtifactReference( resource.getPath() );

            if ( artifact != null )
            {
                applyServerSideRelocation( managedRepository, artifact );

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
            log.error( e.getMessage(), e );
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to fetch artifact resource." );
        }
        return false;
    }

    /**
     * A relocation capable client will request the POM prior to the artifact, and will then read meta-data and do
     * client side relocation. A simplier client (like maven 1) will only request the artifact and not use the
     * metadatas.
     * <p>
     * For such clients, archiva does server-side relocation by reading itself the &lt;relocation&gt; element in
     * metadatas and serving the expected artifact.
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
            // MavenXpp3Reader leaves the file open, so we need to close it ourselves.
            FileReader reader = new FileReader( pom );
            Model model = null;
            try
            {
                model = new MavenXpp3Reader().read( reader );
            }
            finally
            {
                if (reader != null)
                {
                    reader.close();
                }
            }

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

    // TODO: remove?
    private void triggerAuditEvent( String remoteIP, String repositoryId, String resource, String action )
    {
        String activePrincipal = archivaXworkUser.getActivePrincipal( ActionContext.getContext().getSession() );
        AuditEvent event = new AuditEvent( repositoryId, activePrincipal, resource, action );
        event.setRemoteIP( remoteIP );

        for ( AuditListener listener : auditListeners )
        {
            listener.auditEvent( event );
        }
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

    private void setHeaders( DavServletResponse response, DavResourceLocator locator, DavResource resource )
    {
        // [MRM-503] - Metadata file need Pragma:no-cache response
        // header.
        if ( locator.getResourcePath().endsWith( "/maven-metadata.xml" ) )
        {
            response.addHeader( "Pragma", "no-cache" );
            response.addHeader( "Cache-Control", "no-cache" );
        }

        //We need to specify this so connecting wagons can work correctly
        response.addDateHeader("last-modified", resource.getModificationTime());

        // TODO: [MRM-524] determine http caching options for other types of files (artifacts, sha1, md5, snapshots)
    }

    private ManagedRepositoryContent getManagedRepository( String respositoryId )
        throws DavException
    {
        if ( respositoryId != null )
        {
            try
            {
                return repositoryFactory.getManagedRepositoryContent( respositoryId );
            }
            catch ( RepositoryNotFoundException e )
            {
                throw new DavException( HttpServletResponse.SC_NOT_FOUND, e );
            }
            catch ( RepositoryException e )
            {
                throw new DavException( HttpServletResponse.SC_NOT_FOUND, e );
            }
        }
        return null;
    }

    private void checkLocatorIsInstanceOfRepositoryLocator( DavResourceLocator locator )
        throws DavException
    {
        if ( !( locator instanceof RepositoryLocator ) )
        {
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                    "Locator does not implement RepositoryLocator" );
        }
    }

    class LogicalResource
    {
        private String path;

        public LogicalResource( String path )
        {
            this.path = path;
        }

        public String getPath()
        {
            return path;
        }

        public void setPath( String path )
        {
            this.path = path;
        }
    }

    protected boolean isAuthorized( DavServletRequest request, String repositoryId )
        throws DavException
    {   
        try
        {     
            AuthenticationResult result = httpAuth.getAuthenticationResult( request, null );
            SecuritySession securitySession = httpAuth.getSecuritySession( request.getSession( true ) );

            return servletAuth.isAuthenticated( request, result ) &&
                servletAuth.isAuthorized( request, securitySession, repositoryId,
                                          WebdavMethodUtil.isWriteMethod( request.getMethod() ) );
        }
        catch ( AuthenticationException e )
        {            
            boolean isPut = WebdavMethodUtil.isWriteMethod( request.getMethod() );
            
            // safety check for MRM-911            
            String guest = archivaXworkUser.getGuest();
            try
            {
                if( servletAuth.isAuthorized( guest, 
                      ( ( ArchivaDavResourceLocator ) request.getRequestLocator() ).getRepositoryId(), isPut ) )
                {   
                    return true;
                }
            }
            catch ( UnauthorizedException ae )
            {
                throw new UnauthorizedDavException( repositoryId,
                        "You are not authenticated and authorized to access any repository." );
            }
                        
            throw new UnauthorizedDavException( repositoryId, "You are not authenticated" );
        }
        catch ( MustChangePasswordException e )
        {
            throw new UnauthorizedDavException( repositoryId, "You must change your password." );
        }
        catch ( AccountLockedException e )
        {
            throw new UnauthorizedDavException( repositoryId, "User account is locked." );
        }
        catch ( AuthorizationException e )
        {
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                    "Fatal Authorization Subsystem Error." );
        }
        catch ( UnauthorizedException e )
        {
            throw new UnauthorizedDavException( repositoryId, e.getMessage() );
        }
    }

    private DavResource getResource( DavServletRequest request, List<String> repositories, ArchivaDavResourceLocator locator )
        throws DavException
    {
        List<File> mergedRepositoryContents = new ArrayList<File>();
        LogicalResource logicalResource =
            new LogicalResource( RepositoryPathUtil.getLogicalResource( locator.getResourcePath() ) );

        // flow:
        // if the current user logged in has permission to any of the repositories, allow user to
        // browse the repo group but displaying only the repositories which the user has permission to access.
        // otherwise, prompt for authentication.

        // put the current session in the session map which will be passed to ArchivaXworkUser
        Map<String, Object> sessionMap = new HashMap<String, Object>();
        if( request.getSession().getAttribute( SecuritySystemConstants.SECURITY_SESSION_KEY ) != null )
        {
            sessionMap.put( SecuritySystemConstants.SECURITY_SESSION_KEY,
                            request.getSession().getAttribute( SecuritySystemConstants.SECURITY_SESSION_KEY ) );
        }

        String activePrincipal = archivaXworkUser.getActivePrincipal( sessionMap );
        boolean allow = isAllowedToContinue( request, repositories, activePrincipal );

        if( allow )
        {
            boolean isPut = WebdavMethodUtil.isWriteMethod( request.getMethod() );
            
            for( String repository : repositories )
            {
                // for prompted authentication
                if( httpAuth.getSecuritySession( request.getSession( true ) ) != null )
                {
                    try
                    {
                        if( isAuthorized( request, repository ) )
                        {
                            getResource( locator, mergedRepositoryContents, logicalResource, repository );
                        }
                    }
                    catch ( DavException e )
                    {
                        continue;
                    }
                }
                else
                {
                    // for the current user logged in
                    try
                    {
                        if( servletAuth.isAuthorized( activePrincipal, repository, isPut ) )
                        {
                            getResource( locator, mergedRepositoryContents, logicalResource, repository );
                        }
                    }
                    catch ( UnauthorizedException e )
                    {
                        continue;
                    }
                }
            }
        }
        else
        {
            throw new UnauthorizedDavException( locator.getRepositoryId(), "User not authorized." );
        }

        ArchivaVirtualDavResource resource =
            new ArchivaVirtualDavResource( mergedRepositoryContents, logicalResource.getPath(), mimeTypes, locator, this );

        // compatibility with MRM-440 to ensure browsing the repository group works ok
        if ( resource.isCollection() && !request.getRequestURI().endsWith("/" ) )
        {
            throw new BrowserRedirectException( resource.getHref() );
        }

        return resource;
    }

    private void getResource( ArchivaDavResourceLocator locator, List<File> mergedRepositoryContents,
                              LogicalResource logicalResource, String repository )
        throws DavException
    {
        ManagedRepositoryContent managedRepository = null;

        try
        {
            managedRepository = getManagedRepository( repository );
        }
        catch ( DavException de )
        {
            throw new DavException( HttpServletResponse.SC_NOT_FOUND, "Invalid managed repository <" +
                repository + ">" );
        }

        if ( !locator.getResourcePath().startsWith( ArchivaVirtualDavResource.HIDDEN_PATH_PREFIX ) )
        {
            if( managedRepository != null )
            {
                File resourceFile = new File( managedRepository.getRepoRoot(), logicalResource.getPath() );
                if( resourceFile.exists() )
                {
                    mergedRepositoryContents.add( resourceFile );
                }
            }
        }
    }

    /**
     * Check if the current user is authorized to access any of the repos
     *
     * @param request
     * @param repositories
     * @param activePrincipal
     * @return
     */
    private boolean isAllowedToContinue( DavServletRequest request, List<String> repositories, String activePrincipal )
    {
        boolean allow = false;


        // if securitySession != null, it means that the user was prompted for authentication
        if( httpAuth.getSecuritySession( request.getSession() ) != null )
        {
            for( String repository : repositories )
            {
                try
                {
                    if( isAuthorized( request, repository ) )
                    {
                        allow = true;
                        break;
                    }
                }
                catch( DavException e )
                {
                    continue;
                }
            }
        }
        else
        {
            boolean isPut = WebdavMethodUtil.isWriteMethod( request.getMethod() );
            for( String repository : repositories )
            {
                try
                {   
                    if( servletAuth.isAuthorized( activePrincipal, repository, isPut ) )
                    {
                        allow = true;
                        break;
                    }
                }
                catch ( UnauthorizedException e )
                {
                    continue;
                }
            }
        }

        return allow;
    }

    private File writeMergedMetadataToFile( ArchivaRepositoryMetadata mergedMetadata, String outputFilename )
        throws RepositoryMetadataException, DigesterException, IOException
    {  
        File outputFile = new File( outputFilename );        
        if( outputFile.exists() )
        {
            FileUtils.deleteQuietly( outputFile );
        }
        
        outputFile.getParentFile().mkdirs();
        RepositoryMetadataWriter.write( mergedMetadata, outputFile );
        
        createChecksumFile( outputFilename, digestSha1 );
        createChecksumFile( outputFilename, digestMd5 );
        
        return outputFile;
    }
    
    private void createChecksumFile( String path, Digester digester )
        throws DigesterException, IOException
    {   
        File checksumFile = new File( path + digester.getFilenameExtension() );        
        if ( !checksumFile.exists() )
        {
            FileUtils.deleteQuietly( checksumFile );
            checksum.createChecksum( new File( path ), digester );            
        }
        else if ( !checksumFile.isFile() )
        {
            log.error( "Checksum file is not a file." );
        }
    }
    
    private boolean isProjectReference( String requestedResource )
    {  
       try
       {           
           VersionedReference versionRef = metadataTools.toVersionedReference( requestedResource );           
           return false;
       }
       catch ( RepositoryMetadataException re )
       {
           return true;
       }
    }
    
    public void setServletAuth( ServletAuthenticator servletAuth )
    {
        this.servletAuth = servletAuth;
    }
    
    public void setHttpAuth( HttpAuthenticator httpAuth )
    {
        this.httpAuth = httpAuth;
    }
}
