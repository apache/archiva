package org.apache.archiva.webdav;

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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.admin.model.remote.RemoteRepositoryAdmin;
import org.apache.archiva.audit.Auditable;
import org.apache.archiva.common.filelock.FileLockManager;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.archiva.common.utils.PathUtil;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.archiva.indexer.merger.*;
import org.apache.archiva.indexer.search.RepositorySearch;
import org.apache.archiva.maven2.metadata.MavenMetadataReader;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.metadata.repository.storage.RelocationException;
import org.apache.archiva.metadata.repository.storage.RepositoryStorage;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.policies.ProxyDownloadException;
import org.apache.archiva.proxy.model.RepositoryProxyConnectors;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authorization.AuthorizationException;
import org.apache.archiva.redback.authorization.UnauthorizedException;
import org.apache.archiva.redback.integration.filter.authentication.HttpAuthenticator;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RepositoryContentFactory;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryNotFoundException;
import org.apache.archiva.repository.content.maven2.RepositoryRequest;
import org.apache.archiva.repository.events.AuditListener;
import org.apache.archiva.repository.layout.LayoutException;
import org.apache.archiva.repository.metadata.MetadataTools;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.archiva.repository.metadata.RepositoryMetadataMerge;
import org.apache.archiva.repository.metadata.RepositoryMetadataWriter;
import org.apache.archiva.scheduler.repository.model.RepositoryArchivaTaskScheduler;
import org.apache.archiva.security.ServletAuthenticator;
import org.apache.archiva.webdav.util.MimeTypes;
import org.apache.archiva.webdav.util.TemporaryGroupIndexSessionCleaner;
import org.apache.archiva.webdav.util.WebdavMethodUtil;
import org.apache.archiva.xml.XMLException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.SimpleLockManager;
import org.apache.maven.index.context.IndexingContext;
import org.codehaus.plexus.digest.ChecksumFile;
import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.digest.DigesterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 *
 */
@Service( "davResourceFactory#archiva" )
public class ArchivaDavResourceFactory
    implements DavResourceFactory, Auditable
{
    private static final String PROXIED_SUFFIX = " (proxied)";

    private static final String HTTP_PUT_METHOD = "PUT";

    private Logger log = LoggerFactory.getLogger( ArchivaDavResourceFactory.class );

    @Inject
    private List<AuditListener> auditListeners = new ArrayList<>();

    @Inject
    private RepositoryContentFactory repositoryFactory;

    private RepositoryRequest repositoryRequest;

    @Inject
    @Named( value = "repositoryProxyConnectors#default" )
    private RepositoryProxyConnectors connectors;

    @Inject
    private MetadataTools metadataTools;

    @Inject
    private MimeTypes mimeTypes;

    private ArchivaConfiguration archivaConfiguration;

    @Inject
    private ServletAuthenticator servletAuth;

    @Inject
    @Named( value = "httpAuthenticator#basic" )
    private HttpAuthenticator httpAuth;

    @Inject
    private RemoteRepositoryAdmin remoteRepositoryAdmin;

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    private IndexMerger indexMerger;

    @Inject
    private RepositorySearch repositorySearch;

    /**
     * Lock Manager - use simple implementation from JackRabbit
     */
    private final LockManager lockManager = new SimpleLockManager();

    private ChecksumFile checksum;

    private Digester digestSha1;

    private Digester digestMd5;

    @Inject
    @Named( value = "archivaTaskScheduler#repository" )
    private RepositoryArchivaTaskScheduler scheduler;

    @Inject
    @Named( value = "fileLockManager#default" )
    private FileLockManager fileLockManager;

    private ApplicationContext applicationContext;

    @Inject
    public ArchivaDavResourceFactory( ApplicationContext applicationContext, PlexusSisuBridge plexusSisuBridge,
                                      ArchivaConfiguration archivaConfiguration )
        throws PlexusSisuBridgeException
    {
        this.archivaConfiguration = archivaConfiguration;
        this.applicationContext = applicationContext;
        this.checksum = plexusSisuBridge.lookup( ChecksumFile.class );

        this.digestMd5 = plexusSisuBridge.lookup( Digester.class, "md5" );
        this.digestSha1 = plexusSisuBridge.lookup( Digester.class, "sha1" );

        // TODO remove this hard dependency on maven !!
        repositoryRequest = new RepositoryRequest( );
    }

    @PostConstruct
    public void initialize()
    {
        // no op
    }

    @Override
    public DavResource createResource( final DavResourceLocator locator, final DavServletRequest request,
                                       final DavServletResponse response )
        throws DavException
    {
        ArchivaDavResourceLocator archivaLocator = checkLocatorIsInstanceOfRepositoryLocator( locator );

        RepositoryGroupConfiguration repoGroupConfig =
            archivaConfiguration.getConfiguration().getRepositoryGroupsAsMap().get( archivaLocator.getRepositoryId() );

        String activePrincipal = getActivePrincipal( request );

        List<String> resourcesInAbsolutePath = new ArrayList<>();

        boolean readMethod = WebdavMethodUtil.isReadMethod( request.getMethod() );
        DavResource resource;
        if ( repoGroupConfig != null )
        {
            if ( !readMethod )
            {
                throw new DavException( HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                                        "Write method not allowed for repository groups." );
            }

            log.debug( "Repository group '{}' accessed by '{}", repoGroupConfig.getId(), activePrincipal );

            // handle browse requests for virtual repos
            if ( getLogicalResource( archivaLocator, null, true ).endsWith( "/" ) )
            {
                try
                {
                    DavResource davResource =
                        getResourceFromGroup( request, repoGroupConfig.getRepositories(), archivaLocator,
                                              repoGroupConfig );

                    setHeaders( response, locator, davResource, true );

                    return davResource;

                }
                catch ( RepositoryAdminException e )
                {
                    throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
                }
            }
            else
            {
                // make a copy to avoid potential concurrent modifications (eg. by configuration)
                // TODO: ultimately, locking might be more efficient than copying in this fashion since updates are
                //  infrequent
                List<String> repositories = new ArrayList<>( repoGroupConfig.getRepositories() );
                resource = processRepositoryGroup( request, archivaLocator, repositories, activePrincipal,
                                                   resourcesInAbsolutePath, repoGroupConfig );
            }
        }
        else
        {

            try
            {
                RemoteRepository remoteRepository =
                    remoteRepositoryAdmin.getRemoteRepository( archivaLocator.getRepositoryId() );

                if ( remoteRepository != null )
                {
                    String logicalResource = getLogicalResource( archivaLocator, null, false );
                    IndexingContext indexingContext = remoteRepositoryAdmin.createIndexContext( remoteRepository );
                    Path resourceFile = StringUtils.equals( logicalResource, "/" )
                        ? Paths.get( indexingContext.getIndexDirectoryFile().getParent() )
                        : Paths.get( indexingContext.getIndexDirectoryFile().getParent(), logicalResource );
                    resource = new ArchivaDavResource( resourceFile.toAbsolutePath().toString(), //
                                                       locator.getResourcePath(), //
                                                       null, //
                                                       request.getRemoteAddr(), //
                                                       activePrincipal, //
                                                       request.getDavSession(), //
                                                       archivaLocator, //
                                                       this, //
                                                       mimeTypes, //
                                                       auditListeners, //
                                                       scheduler, //
                                                       fileLockManager );
                    setHeaders( response, locator, resource, false );
                    return resource;
                }
            }
            catch ( RepositoryAdminException e )
            {
                log.debug( "RepositoryException remote repository with d'{}' not found, msg: {}",
                           archivaLocator.getRepositoryId(), e.getMessage() );
            }

            ManagedRepositoryContent managedRepositoryContent = null;

            try
            {
                managedRepositoryContent =
                    repositoryFactory.getManagedRepositoryContent( archivaLocator.getRepositoryId() );
            }
            catch ( RepositoryNotFoundException e )
            {
                throw new DavException( HttpServletResponse.SC_NOT_FOUND,
                                        "Invalid repository: " + archivaLocator.getRepositoryId() );
            }
            catch ( RepositoryException e )
            {
                throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
            }

            log.debug( "Managed repository '{}' accessed by '{}'", managedRepositoryContent.getId(), activePrincipal );

            try
            {
                resource = processRepository( request, archivaLocator, activePrincipal, managedRepositoryContent,
                                              managedRepositoryAdmin.getManagedRepository(
                                                  archivaLocator.getRepositoryId() ) );

                String logicalResource = getLogicalResource( archivaLocator, null, false );
                resourcesInAbsolutePath.add(
                    Paths.get( managedRepositoryContent.getRepoRoot(), logicalResource ).toAbsolutePath().toString() );

            }
            catch ( RepositoryAdminException e )
            {
                throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
            }
        }

        String requestedResource = request.getRequestURI();

        // MRM-872 : merge all available metadata
        // merge metadata only when requested via the repo group
        if ( ( repositoryRequest.isMetadata( requestedResource ) || repositoryRequest.isMetadataSupportFile(
            requestedResource ) ) && repoGroupConfig != null )
        {
            // this should only be at the project level not version level!
            if ( isProjectReference( requestedResource ) )
            {

                ArchivaDavResource res = (ArchivaDavResource) resource;
                String filePath =
                    StringUtils.substringBeforeLast( res.getLocalResource().toAbsolutePath().toString().replace( '\\', '/' ),
                                                     "/" );
                filePath = filePath + "/maven-metadata-" + repoGroupConfig.getId() + ".xml";

                // for MRM-872 handle checksums of the merged metadata files
                if ( repositoryRequest.isSupportFile( requestedResource ) )
                {
                    Path metadataChecksum =
                        Paths.get( filePath + "." + StringUtils.substringAfterLast( requestedResource, "." ) );

                    if ( Files.exists(metadataChecksum) )
                    {
                        LogicalResource logicalResource =
                            new LogicalResource( getLogicalResource( archivaLocator, null, false ) );

                        resource =
                            new ArchivaDavResource( metadataChecksum.toAbsolutePath().toString(), logicalResource.getPath(), null,
                                                    request.getRemoteAddr(), activePrincipal, request.getDavSession(),
                                                    archivaLocator, this, mimeTypes, auditListeners, scheduler,
                                                    fileLockManager );
                    }
                }
                else
                {
                    if ( resourcesInAbsolutePath != null && resourcesInAbsolutePath.size() > 1 )
                    {
                        // merge the metadata of all repos under group
                        ArchivaRepositoryMetadata mergedMetadata = new ArchivaRepositoryMetadata();
                        for ( String resourceAbsPath : resourcesInAbsolutePath )
                        {
                            try
                            {
                                Path metadataFile = Paths.get( resourceAbsPath );
                                ArchivaRepositoryMetadata repoMetadata = MavenMetadataReader.read( metadataFile );
                                mergedMetadata = RepositoryMetadataMerge.merge( mergedMetadata, repoMetadata );
                            }
                            catch ( XMLException e )
                            {
                                throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                                        "Error occurred while reading metadata file." );
                            }
                            catch ( RepositoryMetadataException r )
                            {
                                throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                                        "Error occurred while merging metadata file." );
                            }
                        }

                        try
                        {
                            Path resourceFile = writeMergedMetadataToFile( mergedMetadata, filePath );

                            LogicalResource logicalResource =
                                new LogicalResource( getLogicalResource( archivaLocator, null, false ) );

                            resource =
                                new ArchivaDavResource( resourceFile.toAbsolutePath().toString(), logicalResource.getPath(), null,
                                                        request.getRemoteAddr(), activePrincipal,
                                                        request.getDavSession(), archivaLocator, this, mimeTypes,
                                                        auditListeners, scheduler, fileLockManager );
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
                                                    "Error occurred while generating checksum files."
                                                        + de.getMessage() );
                        }
                    }
                }
            }
        }

        setHeaders( response, locator, resource, false );

        // compatibility with MRM-440 to ensure browsing the repository works ok
        if ( resource.isCollection() && !request.getRequestURI().endsWith( "/" ) )
        {
            throw new BrowserRedirectException( resource.getHref() );
        }
        resource.addLockManager( lockManager );
        return resource;
    }

    private DavResource processRepositoryGroup( final DavServletRequest request,
                                                ArchivaDavResourceLocator archivaLocator, List<String> repositories,
                                                String activePrincipal, List<String> resourcesInAbsolutePath,
                                                RepositoryGroupConfiguration repoGroupConfig )
        throws DavException
    {
        DavResource resource = null;
        List<DavException> storedExceptions = new ArrayList<>();

        String pathInfo = StringUtils.removeEnd( request.getPathInfo(), "/" );

        String rootPath = StringUtils.substringBeforeLast( pathInfo, "/" );

        if ( StringUtils.endsWith( rootPath, repoGroupConfig.getMergedIndexPath() ) )
        {
            // we are in the case of index file request
            String requestedFileName = StringUtils.substringAfterLast( pathInfo, "/" );
            Path temporaryIndexDirectory =
                buildMergedIndexDirectory( repositories, activePrincipal, request, repoGroupConfig );

            Path resourceFile = temporaryIndexDirectory.resolve( requestedFileName );
            resource = new ArchivaDavResource( resourceFile.toAbsolutePath().toString(), requestedFileName, null,
                                               request.getRemoteAddr(), activePrincipal, request.getDavSession(),
                                               archivaLocator, this, mimeTypes, auditListeners, scheduler,
                                               fileLockManager );

        }
        else
        {
            for ( String repositoryId : repositories )
            {
                ManagedRepositoryContent managedRepositoryContent;
                try
                {
                    managedRepositoryContent = repositoryFactory.getManagedRepositoryContent( repositoryId );
                }
                catch ( RepositoryNotFoundException e )
                {
                    throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
                }
                catch ( RepositoryException e )
                {
                    throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
                }

                try
                {
                    ManagedRepository managedRepository = managedRepositoryAdmin.getManagedRepository( repositoryId );
                    DavResource updatedResource =
                        processRepository( request, archivaLocator, activePrincipal, managedRepositoryContent,
                                           managedRepository );
                    if ( resource == null )
                    {
                        resource = updatedResource;
                    }

                    String logicalResource = getLogicalResource( archivaLocator, null, false );
                    if ( logicalResource.endsWith( "/" ) )
                    {
                        logicalResource = logicalResource.substring( 1 );
                    }
                    resourcesInAbsolutePath.add(
                        Paths.get( managedRepositoryContent.getRepoRoot(), logicalResource ).toAbsolutePath().toString() );
                }
                catch ( DavException e )
                {
                    storedExceptions.add( e );
                }
                catch ( RepositoryAdminException e )
                {
                    storedExceptions.add( new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e ) );
                }
            }
        }
        if ( resource == null )
        {
            if ( !storedExceptions.isEmpty() )
            {
                // MRM-1232
                for ( DavException e : storedExceptions )
                {
                    if ( 401 == e.getErrorCode() )
                    {
                        throw e;
                    }
                }

                throw new DavException( HttpServletResponse.SC_NOT_FOUND );
            }
            else
            {
                throw new DavException( HttpServletResponse.SC_NOT_FOUND );
            }
        }
        return resource;
    }

    private String getLogicalResource( ArchivaDavResourceLocator archivaLocator, ManagedRepository managedRepository,
                                       boolean useOrigResourcePath )
    {
        // FIXME remove this hack
        // but currently managedRepository can be null in case of group
        String layout = managedRepository == null ? new ManagedRepository().getLayout() : managedRepository.getLayout();
        RepositoryStorage repositoryStorage =
            this.applicationContext.getBean( "repositoryStorage#" + layout, RepositoryStorage.class );
        String path = repositoryStorage.getFilePath(
            useOrigResourcePath ? archivaLocator.getOrigResourcePath() : archivaLocator.getResourcePath(),
            managedRepository );
        log.debug( "found path {} for resourcePath: '{}' with managedRepo '{}' and layout '{}'", path,
                   archivaLocator.getResourcePath(), managedRepository == null ? "null" : managedRepository.getId(),
                   layout );
        return path;
    }

    private String evaluatePathWithVersion( ArchivaDavResourceLocator archivaLocator, //
                                            ManagedRepositoryContent managedRepositoryContent, //
                                            String contextPath )
        throws DavException
    {
        String layout = managedRepositoryContent.getRepository() == null
            ? new ManagedRepository().getLayout()
            : managedRepositoryContent.getRepository().getLayout();
        RepositoryStorage repositoryStorage =
            this.applicationContext.getBean( "repositoryStorage#" + layout, RepositoryStorage.class );
        try
        {
            return repositoryStorage.getFilePathWithVersion( archivaLocator.getResourcePath(), //
                                                             managedRepositoryContent );
        }
        catch ( RelocationException e )
        {
            String path = e.getPath();
            log.debug( "Relocation to {}", path );

            throw new BrowserRedirectException( addHrefPrefix( contextPath, path ), e.getRelocationType() );
        }
        catch ( XMLException e )
        {
            log.error( e.getMessage(), e );
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
        }
    }

    private DavResource processRepository( final DavServletRequest request, ArchivaDavResourceLocator archivaLocator,
                                           String activePrincipal, ManagedRepositoryContent managedRepositoryContent,
                                           ManagedRepository managedRepository )
        throws DavException
    {
        DavResource resource = null;
        if ( isAuthorized( request, managedRepositoryContent.getId() ) )
        {
            boolean readMethod = WebdavMethodUtil.isReadMethod( request.getMethod() );
            // Maven Centric part ask evaluation if -SNAPSHOT
            // MRM-1846 test if read method to prevent issue with maven 2.2.1 and uniqueVersion false

            String path = readMethod
                ? evaluatePathWithVersion( archivaLocator, managedRepositoryContent, request.getContextPath() )
                : getLogicalResource( archivaLocator, managedRepository, false );
            if ( path.startsWith( "/" ) )
            {
                path = path.substring( 1 );
            }
            LogicalResource logicalResource = new LogicalResource( path );
            Path resourceFile = Paths.get( managedRepositoryContent.getRepoRoot(), path );
            resource =
                new ArchivaDavResource( resourceFile.toAbsolutePath().toString(), path, managedRepositoryContent.getRepository(),
                                        request.getRemoteAddr(), activePrincipal, request.getDavSession(),
                                        archivaLocator, this, mimeTypes, auditListeners, scheduler, fileLockManager );

            if ( WebdavMethodUtil.isReadMethod( request.getMethod() ) )
            {
                if ( archivaLocator.getHref( false ).endsWith( "/" ) && !Files.isDirectory( resourceFile ) )
                {
                    // force a resource not found
                    throw new DavException( HttpServletResponse.SC_NOT_FOUND, "Resource does not exist" );
                }
                else
                {
                    if ( !resource.isCollection() )
                    {
                        boolean previouslyExisted = Files.exists(resourceFile);

                        boolean fromProxy = fetchContentFromProxies( managedRepositoryContent, request, logicalResource );

                        // At this point the incoming request can either be in default or
                        // legacy layout format.
                        try
                        {
                            // Perform an adjustment of the resource to the managed
                            // repository expected path.
                            String localResourcePath =
                                repositoryRequest.toNativePath( logicalResource.getPath(), managedRepositoryContent );
                            resourceFile = Paths.get( managedRepositoryContent.getRepoRoot(), localResourcePath );
                            resource =
                                new ArchivaDavResource( resourceFile.toAbsolutePath().toString(), logicalResource.getPath(),
                                                        managedRepositoryContent.getRepository(),
                                                        request.getRemoteAddr(), activePrincipal,
                                                        request.getDavSession(), archivaLocator, this, mimeTypes,
                                                        auditListeners, scheduler, fileLockManager );
                        }
                        catch ( LayoutException e )
                        {
                            if ( !Files.exists(resourceFile) )
                            {
                                throw new DavException( HttpServletResponse.SC_NOT_FOUND, e );
                            }
                        }

                        if ( fromProxy )
                        {
                            String action = ( previouslyExisted ? AuditEvent.MODIFY_FILE : AuditEvent.CREATE_FILE )
                                + PROXIED_SUFFIX;

                            log.debug( "Proxied artifact '{}' in repository '{}' (current user '{}')",
                                       resourceFile.getFileName(), managedRepositoryContent.getId(), activePrincipal );

                            triggerAuditEvent( request.getRemoteAddr(), archivaLocator.getRepositoryId(),
                                               logicalResource.getPath(), action, activePrincipal );
                        }

                        if ( !Files.exists(resourceFile) )
                        {
                            throw new DavException( HttpServletResponse.SC_NOT_FOUND, "Resource does not exist" );
                        }
                    }
                }
            }

            if ( request.getMethod().equals( HTTP_PUT_METHOD ) )
            {
                String resourcePath = logicalResource.getPath();

                // check if target repo is enabled for releases
                // we suppose that release-artifacts can be deployed only to repos enabled for releases
                if ( managedRepositoryContent.getRepository().isReleases() && !repositoryRequest.isMetadata(
                    resourcePath ) && !repositoryRequest.isSupportFile( resourcePath ) )
                {
                    ArtifactReference artifact = null;
                    try
                    {
                        artifact = managedRepositoryContent.toArtifactReference( resourcePath );

                        if ( !VersionUtil.isSnapshot( artifact.getVersion() ) )
                        {
                            // check if artifact already exists and if artifact re-deployment to the repository is allowed
                            if ( managedRepositoryContent.hasContent( artifact )
                                && managedRepositoryContent.getRepository().isBlockRedeployments() )
                            {
                                log.warn( "Overwriting released artifacts in repository '{}' is not allowed.",
                                          managedRepositoryContent.getId() );
                                throw new DavException( HttpServletResponse.SC_CONFLICT,
                                                        "Overwriting released artifacts is not allowed." );
                            }
                        }
                    }
                    catch ( LayoutException e )
                    {
                        log.warn( "Artifact path '{}' is invalid.", resourcePath );
                    }
                }

                /*
                 * Create parent directories that don't exist when writing a file This actually makes this
                 * implementation not compliant to the WebDAV RFC - but we have enough knowledge about how the
                 * collection is being used to do this reasonably and some versions of Maven's WebDAV don't correctly
                 * create the collections themselves.
                 */

                Path rootDirectory = Paths.get( managedRepositoryContent.getRepoRoot() );
                Path destDir = rootDirectory.resolve( logicalResource.getPath() ).getParent();

                if ( !Files.exists(destDir) )
                {
                    try
                    {
                        Files.createDirectories( destDir );
                    }
                    catch ( IOException e )
                    {
                        log.error("Could not create directory {}: {}", destDir, e.getMessage(), e);
                        throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not create directory "+destDir );
                    }
                    String relPath = PathUtil.getRelative( rootDirectory.toAbsolutePath().toString(), destDir );

                    log.debug( "Creating destination directory '{}' (current user '{}')", destDir.getFileName(),
                               activePrincipal );

                    triggerAuditEvent( request.getRemoteAddr(), managedRepositoryContent.getId(), relPath,
                                       AuditEvent.CREATE_DIR, activePrincipal );
                }
            }
        }
        return resource;
    }

    @Override
    public DavResource createResource( final DavResourceLocator locator, final DavSession davSession )
        throws DavException
    {
        ArchivaDavResourceLocator archivaLocator = checkLocatorIsInstanceOfRepositoryLocator( locator );

        ManagedRepositoryContent managedRepositoryContent;
        try
        {
            managedRepositoryContent =
                repositoryFactory.getManagedRepositoryContent( archivaLocator.getRepositoryId() );
        }
        catch ( RepositoryNotFoundException e )
        {
            throw new DavException( HttpServletResponse.SC_NOT_FOUND,
                                    "Invalid repository: " + archivaLocator.getRepositoryId() );
        }
        catch ( RepositoryException e )
        {
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
        }

        DavResource resource = null;
        try
        {
            String logicalResource = getLogicalResource( archivaLocator, managedRepositoryAdmin.getManagedRepository(
                archivaLocator.getRepositoryId() ), false );
            if ( logicalResource.startsWith( "/" ) )
            {
                logicalResource = logicalResource.substring( 1 );
            }
            Path resourceFile = Paths.get( managedRepositoryContent.getRepoRoot(), logicalResource );
            resource = new ArchivaDavResource( resourceFile.toAbsolutePath().toString(), logicalResource,
                                               managedRepositoryContent.getRepository(), davSession, archivaLocator,
                                               this, mimeTypes, auditListeners, scheduler, fileLockManager );

            resource.addLockManager( lockManager );
        }
        catch ( RepositoryAdminException e )
        {
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
        }
        return resource;
    }

    private boolean fetchContentFromProxies( ManagedRepositoryContent managedRepository, DavServletRequest request,
                                             LogicalResource resource )
        throws DavException
    {
        String path = resource.getPath();
        if ( repositoryRequest.isSupportFile( path ) )
        {
            Path proxiedFile = connectors.fetchFromProxies( managedRepository, path );

            return ( proxiedFile != null );
        }

        // Is it a Metadata resource?
        if ( repositoryRequest.isDefault( path ) && repositoryRequest.isMetadata( path ) )
        {
            return connectors.fetchMetadataFromProxies( managedRepository, path ).isModified();
        }

        // Is it an Archetype Catalog?
        if ( repositoryRequest.isArchetypeCatalog( path ) )
        {
            // FIXME we must implement a merge of remote archetype catalog from remote servers.
            Path proxiedFile = connectors.fetchFromProxies( managedRepository, path );

            return ( proxiedFile != null );
        }

        // Not any of the above? Then it's gotta be an artifact reference.
        try
        {
            // Get the artifact reference in a layout neutral way.
            ArtifactReference artifact = repositoryRequest.toArtifactReference( path );

            if ( artifact != null )
            {
                String repositoryLayout = managedRepository.getRepository().getLayout();

                RepositoryStorage repositoryStorage =
                    this.applicationContext.getBean( "repositoryStorage#" + repositoryLayout, RepositoryStorage.class );
                repositoryStorage.applyServerSideRelocation( managedRepository, artifact );

                Path proxiedFile = connectors.fetchFromProxies( managedRepository, artifact );

                resource.setPath( managedRepository.toPath( artifact ) );

                log.debug( "Proxied artifact '{}:{}:{}'", artifact.getGroupId(), artifact.getArtifactId(),
                           artifact.getVersion() );

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
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                    "Unable to fetch artifact resource." );
        }
        return false;
    }

    // TODO: remove?

    private void triggerAuditEvent( String remoteIP, String repositoryId, String resource, String action,
                                    String principal )
    {
        AuditEvent event = new AuditEvent( repositoryId, principal, resource, action );
        event.setRemoteIP( remoteIP );

        for ( AuditListener listener : auditListeners )
        {
            listener.auditEvent( event );
        }
    }

    @Override
    public void addAuditListener( AuditListener listener )
    {
        this.auditListeners.add( listener );
    }

    @Override
    public void clearAuditListeners()
    {
        this.auditListeners.clear();
    }

    @Override
    public void removeAuditListener( AuditListener listener )
    {
        this.auditListeners.remove( listener );
    }

    private void setHeaders( DavServletResponse response, DavResourceLocator locator, DavResource resource,
                             boolean group )
    {
        // [MRM-503] - Metadata file need Pragma:no-cache response
        // header.
        if ( locator.getResourcePath().endsWith( "/maven-metadata.xml" ) || ( resource instanceof ArchivaDavResource
            && ( Files.isDirectory( ArchivaDavResource.class.cast( resource ).getLocalResource()) ) ) )
        {
            response.setHeader( "Pragma", "no-cache" );
            response.setHeader( "Cache-Control", "no-cache" );
            response.setDateHeader( "Last-Modified", new Date().getTime() );
        }
        // if the resource is a directory don't cache it as new groupId deployed will be available
        // without need of refreshing browser
        else if ( locator.getResourcePath().endsWith( "/maven-metadata.xml" ) || (
            resource instanceof ArchivaVirtualDavResource && ( Files.isDirectory(Paths.get(
                ArchivaVirtualDavResource.class.cast( resource ).getLogicalResource() )) ) ) )
        {
            response.setHeader( "Pragma", "no-cache" );
            response.setHeader( "Cache-Control", "no-cache" );
            response.setDateHeader( "Last-Modified", new Date().getTime() );
        }
        else if ( group )
        {
            if ( resource instanceof ArchivaVirtualDavResource )
            {
                //MRM-1854 here we have a directory so force "Last-Modified"
                response.setDateHeader( "Last-Modified", new Date().getTime() );
            }
        }
        else
        {
            // We need to specify this so connecting wagons can work correctly
            response.setDateHeader( "Last-Modified", resource.getModificationTime() );
        }
        // TODO: [MRM-524] determine http caching options for other types of files (artifacts, sha1, md5, snapshots)
    }

    private ArchivaDavResourceLocator checkLocatorIsInstanceOfRepositoryLocator( DavResourceLocator locator )
        throws DavException
    {
        if ( !( locator instanceof ArchivaDavResourceLocator ) )
        {
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                    "Locator does not implement RepositoryLocator" );
        }

        // Hidden paths
        if ( locator.getResourcePath().startsWith( ArchivaDavResource.HIDDEN_PATH_PREFIX ) )
        {
            throw new DavException( HttpServletResponse.SC_NOT_FOUND );
        }

        ArchivaDavResourceLocator archivaLocator = (ArchivaDavResourceLocator) locator;

        // MRM-419 - Windows Webdav support. Should not 404 if there is no content.
        if ( StringUtils.isEmpty( archivaLocator.getRepositoryId() ) )
        {
            throw new DavException( HttpServletResponse.SC_NO_CONTENT );
        }
        return archivaLocator;
    }

    private String addHrefPrefix( String contextPath, String path ) {
        String prefix = archivaConfiguration.getConfiguration().getWebapp().getUi().getApplicationUrl();
        if (prefix == null || prefix.isEmpty()) {
            prefix = contextPath;
        }
        return prefix + ( StringUtils.startsWith( path, "/" ) ? "" :
                        ( StringUtils.endsWith( prefix, "/" ) ? "" : "/" ) )
                      + path;
    }

    private static class LogicalResource
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

            return servletAuth.isAuthenticated( request, result ) //
                && servletAuth.isAuthorized( request, securitySession, repositoryId, //
                                             WebdavMethodUtil.getMethodPermission( request.getMethod() ) );
        }
        catch ( AuthenticationException e )
        {
            // safety check for MRM-911
            String guest = UserManager.GUEST_USERNAME;
            try
            {
                if ( servletAuth.isAuthorized( guest,
                                               ( (ArchivaDavResourceLocator) request.getRequestLocator() ).getRepositoryId(),
                                               WebdavMethodUtil.getMethodPermission( request.getMethod() ) ) )
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

    private DavResource getResourceFromGroup( DavServletRequest request, List<String> repositories,
                                              ArchivaDavResourceLocator locator,
                                              RepositoryGroupConfiguration repositoryGroupConfiguration )
        throws DavException, RepositoryAdminException
    {
        if ( repositoryGroupConfiguration.getRepositories() == null
            || repositoryGroupConfiguration.getRepositories().isEmpty() )
        {
            Path file =
                Paths.get( System.getProperty( "appserver.base" ), "groups/" + repositoryGroupConfiguration.getId() );

            return new ArchivaDavResource( file.toString(), "groups/" + repositoryGroupConfiguration.getId(), null,
                                           request.getDavSession(), locator, this, mimeTypes, auditListeners, scheduler,
                                           fileLockManager );
        }
        List<Path> mergedRepositoryContents = new ArrayList<>();
        // multiple repo types so we guess they are all the same type
        // so use the first one
        // FIXME add a method with group in the repository storage
        String firstRepoId = repositoryGroupConfiguration.getRepositories().get( 0 );

        String path = getLogicalResource( locator, managedRepositoryAdmin.getManagedRepository( firstRepoId ), false );
        if ( path.startsWith( "/" ) )
        {
            path = path.substring( 1 );
        }
        LogicalResource logicalResource = new LogicalResource( path );

        // flow:
        // if the current user logged in has permission to any of the repositories, allow user to
        // browse the repo group but displaying only the repositories which the user has permission to access.
        // otherwise, prompt for authentication.

        String activePrincipal = getActivePrincipal( request );

        boolean allow = isAllowedToContinue( request, repositories, activePrincipal );

        // remove last /
        String pathInfo = StringUtils.removeEnd( request.getPathInfo(), "/" );

        if ( allow )
        {

            if ( StringUtils.endsWith( pathInfo, repositoryGroupConfiguration.getMergedIndexPath() ) )
            {
                Path mergedRepoDir =
                    buildMergedIndexDirectory( repositories, activePrincipal, request, repositoryGroupConfiguration );
                mergedRepositoryContents.add( mergedRepoDir );
            }
            else
            {
                if ( StringUtils.equalsIgnoreCase( pathInfo, "/" + repositoryGroupConfiguration.getId() ) )
                {
                    Path tmpDirectory = Paths.get( SystemUtils.getJavaIoTmpDir().toString(),
                                                  repositoryGroupConfiguration.getId(),
                                                      repositoryGroupConfiguration.getMergedIndexPath() );
                    if ( !Files.exists(tmpDirectory) )
                    {
                        synchronized ( tmpDirectory.toAbsolutePath().toString() )
                        {
                            if ( !Files.exists(tmpDirectory) )
                            {
                                try
                                {
                                    Files.createDirectories( tmpDirectory );
                                }
                                catch ( IOException e )
                                {
                                    throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not create direcotory "+tmpDirectory );
                                }
                            }
                        }
                    }
                    mergedRepositoryContents.add( tmpDirectory.getParent() );
                }
                for ( String repository : repositories )
                {
                    ManagedRepositoryContent managedRepository = null;

                    try
                    {
                        managedRepository = repositoryFactory.getManagedRepositoryContent( repository );
                    }
                    catch ( RepositoryNotFoundException e )
                    {
                        throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                                "Invalid managed repository <" + repository + ">: " + e.getMessage() );
                    }
                    catch ( RepositoryException e )
                    {
                        throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                                "Invalid managed repository <" + repository + ">: " + e.getMessage() );
                    }

                    Path resourceFile = Paths.get( managedRepository.getRepoRoot(), logicalResource.getPath() );
                    if ( Files.exists(resourceFile) )
                    {
                        // in case of group displaying index directory doesn't have sense !!
                        String repoIndexDirectory = managedRepository.getRepository().getIndexDirectory();
                        if ( StringUtils.isNotEmpty( repoIndexDirectory ) )
                        {
                            if ( !Paths.get( repoIndexDirectory ).isAbsolute() )
                            {
                                repoIndexDirectory = Paths.get( managedRepository.getRepository().getLocation(),
                                                               StringUtils.isEmpty( repoIndexDirectory )
                                                                   ? ".indexer"
                                                                   : repoIndexDirectory ).toAbsolutePath().toString();
                            }
                        }
                        if ( StringUtils.isEmpty( repoIndexDirectory ) )
                        {
                            repoIndexDirectory = Paths.get( managedRepository.getRepository().getLocation(),
                                                           ".indexer" ).toAbsolutePath().toString();
                        }

                        if ( !StringUtils.equals( FilenameUtils.normalize( repoIndexDirectory ),
                                                  FilenameUtils.normalize( resourceFile.toAbsolutePath().toString() ) ) )
                        {
                            // for prompted authentication
                            if ( httpAuth.getSecuritySession( request.getSession( true ) ) != null )
                            {
                                try
                                {
                                    if ( isAuthorized( request, repository ) )
                                    {
                                        mergedRepositoryContents.add( resourceFile );
                                        log.debug( "Repository '{}' accessed by '{}'", repository, activePrincipal );
                                    }
                                }
                                catch ( DavException e )
                                {
                                    // TODO: review exception handling

                                    log.debug( "Skipping repository '{}' for user '{}': {}", managedRepository,
                                               activePrincipal, e.getMessage() );

                                }

                            }
                            else
                            {
                                // for the current user logged in
                                try
                                {
                                    if ( servletAuth.isAuthorized( activePrincipal, repository,
                                                                   WebdavMethodUtil.getMethodPermission(
                                                                       request.getMethod() ) ) )
                                    {
                                        mergedRepositoryContents.add( resourceFile );
                                        log.debug( "Repository '{}' accessed by '{}'", repository, activePrincipal );
                                    }
                                }
                                catch ( UnauthorizedException e )
                                {
                                    // TODO: review exception handling

                                    log.debug( "Skipping repository '{}' for user '{}': {}", managedRepository,
                                               activePrincipal, e.getMessage() );

                                }
                            }
                        }
                    }
                }
            }
        }
        else
        {
            throw new UnauthorizedDavException( locator.getRepositoryId(), "User not authorized." );
        }

        ArchivaVirtualDavResource resource =
            new ArchivaVirtualDavResource( mergedRepositoryContents, logicalResource.getPath(), mimeTypes, locator,
                                           this );

        // compatibility with MRM-440 to ensure browsing the repository group works ok
        if ( resource.isCollection() && !request.getRequestURI().endsWith( "/" ) )
        {
            throw new BrowserRedirectException( resource.getHref() );
        }

        return resource;
    }

    protected String getActivePrincipal( DavServletRequest request )
    {
        User sessionUser = httpAuth.getSessionUser( request.getSession() );
        return sessionUser != null ? sessionUser.getUsername() : UserManager.GUEST_USERNAME;
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
        // when no repositories configured it's impossible to browse nothing !
        // at least make possible to see nothing :-)
        if ( repositories == null || repositories.isEmpty() )
        {
            return true;
        }

        boolean allow = false;

        // if securitySession != null, it means that the user was prompted for authentication
        if ( httpAuth.getSecuritySession( request.getSession() ) != null )
        {
            for ( String repository : repositories )
            {
                try
                {
                    if ( isAuthorized( request, repository ) )
                    {
                        allow = true;
                        break;
                    }
                }
                catch ( DavException e )
                {
                    continue;
                }
            }
        }
        else
        {
            for ( String repository : repositories )
            {
                try
                {
                    if ( servletAuth.isAuthorized( activePrincipal, repository,
                                                   WebdavMethodUtil.getMethodPermission( request.getMethod() ) ) )
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

    private Path writeMergedMetadataToFile( ArchivaRepositoryMetadata mergedMetadata, String outputFilename )
        throws RepositoryMetadataException, DigesterException, IOException
    {
        Path outputFile = Paths.get( outputFilename );
        if ( Files.exists(outputFile) )
        {
            org.apache.archiva.common.utils.FileUtils.deleteQuietly( outputFile );
        }

        Files.createDirectories(outputFile.getParent());
        RepositoryMetadataWriter.write( mergedMetadata, outputFile );

        createChecksumFile( outputFilename, digestSha1 );
        createChecksumFile( outputFilename, digestMd5 );

        return outputFile;
    }

    private void createChecksumFile( String path, Digester digester )
        throws DigesterException, IOException
    {
        Path checksumFile = Paths.get( path + digester.getFilenameExtension() );
        if ( !Files.exists(checksumFile) )
        {
            org.apache.archiva.common.utils.FileUtils.deleteQuietly( checksumFile );
            checksum.createChecksum( Paths.get( path ).toFile(), digester );
        }
        else if ( !Files.isRegularFile( checksumFile) )
        {
            log.error( "Checksum file is not a file." );
        }
    }

    private boolean isProjectReference( String requestedResource )
    {
        try
        {
            metadataTools.toVersionedReference( requestedResource );
            return false;
        }
        catch ( RepositoryMetadataException re )
        {
            return true;
        }
    }

    protected Path buildMergedIndexDirectory( List<String> repositories, String activePrincipal,
                                              DavServletRequest request,
                                              RepositoryGroupConfiguration repositoryGroupConfiguration )
        throws DavException
    {

        try
        {
            HttpSession session = request.getSession();

            Map<String, TemporaryGroupIndex> temporaryGroupIndexMap =
                (Map<String, TemporaryGroupIndex>) session.getAttribute(
                    TemporaryGroupIndexSessionCleaner.TEMPORARY_INDEX_SESSION_KEY );
            if ( temporaryGroupIndexMap == null )
            {
                temporaryGroupIndexMap = new HashMap<>();
            }

            TemporaryGroupIndex tmp = temporaryGroupIndexMap.get( repositoryGroupConfiguration.getId() );

            if ( tmp != null && tmp.getDirectory() != null && Files.exists(tmp.getDirectory()))
            {
                if ( System.currentTimeMillis() - tmp.getCreationTime() > (
                    repositoryGroupConfiguration.getMergedIndexTtl() * 60 * 1000 ) )
                {
                    log.debug( MarkerFactory.getMarker( "group.merged.index" ),
                               "tmp group index '{}' is too old so delete it", repositoryGroupConfiguration.getId() );
                    indexMerger.cleanTemporaryGroupIndex( tmp );
                }
                else
                {
                    log.debug( MarkerFactory.getMarker( "group.merged.index" ),
                               "merged index for group '{}' found in cache", repositoryGroupConfiguration.getId() );
                    return tmp.getDirectory();
                }
            }

            Set<String> authzRepos = new HashSet<String>();

            String permission = WebdavMethodUtil.getMethodPermission( request.getMethod() );

            for ( String repository : repositories )
            {
                try
                {
                    if ( servletAuth.isAuthorized( activePrincipal, repository, permission ) )
                    {
                        authzRepos.add( repository );
                        authzRepos.addAll( this.repositorySearch.getRemoteIndexingContextIds( repository ) );
                    }
                }
                catch ( UnauthorizedException e )
                {
                    // TODO: review exception handling

                    log.debug( "Skipping repository '{}' for user '{}': {}", repository, activePrincipal,
                               e.getMessage() );
                }
            }
            log.info( "generate temporary merged index for repository group '{}' for repositories '{}'",
                      repositoryGroupConfiguration.getId(), authzRepos );

            Path tempRepoFile = Files.createTempDirectory( "temp" );
            tempRepoFile.toFile().deleteOnExit();

            IndexMergerRequest indexMergerRequest =
                new IndexMergerRequest( authzRepos, true, repositoryGroupConfiguration.getId(),
                                        repositoryGroupConfiguration.getMergedIndexPath(),
                                        repositoryGroupConfiguration.getMergedIndexTtl() ).mergedIndexDirectory(
                    tempRepoFile ).temporary( true );

            MergedRemoteIndexesTaskRequest taskRequest =
                new MergedRemoteIndexesTaskRequest( indexMergerRequest, indexMerger );

            MergedRemoteIndexesTask job = new MergedRemoteIndexesTask( taskRequest );

            IndexingContext indexingContext = job.execute().getIndexingContext();

            Path mergedRepoDir = indexingContext.getIndexDirectoryFile().toPath();
            TemporaryGroupIndex temporaryGroupIndex =
                new TemporaryGroupIndex( mergedRepoDir, indexingContext.getId(), repositoryGroupConfiguration.getId(),
                                         repositoryGroupConfiguration.getMergedIndexTtl() ) //
                    .setCreationTime( new Date().getTime() );
            temporaryGroupIndexMap.put( repositoryGroupConfiguration.getId(), temporaryGroupIndex );
            session.setAttribute( TemporaryGroupIndexSessionCleaner.TEMPORARY_INDEX_SESSION_KEY,
                                  temporaryGroupIndexMap );
            return mergedRepoDir;
        }
        catch ( RepositoryAdminException e )
        {
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
        }
        catch ( IndexMergerException e )
        {
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
        }
        catch ( IOException e )
        {
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
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

    public void setScheduler( RepositoryArchivaTaskScheduler scheduler )
    {
        this.scheduler = scheduler;
    }

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }

    public void setRepositoryFactory( RepositoryContentFactory repositoryFactory )
    {
        this.repositoryFactory = repositoryFactory;
    }

    public void setRepositoryRequest( RepositoryRequest repositoryRequest )
    {
        this.repositoryRequest = repositoryRequest;
    }

    public void setConnectors( RepositoryProxyConnectors connectors )
    {
        this.connectors = connectors;
    }

    public RemoteRepositoryAdmin getRemoteRepositoryAdmin()
    {
        return remoteRepositoryAdmin;
    }

    public void setRemoteRepositoryAdmin( RemoteRepositoryAdmin remoteRepositoryAdmin )
    {
        this.remoteRepositoryAdmin = remoteRepositoryAdmin;
    }

    public ManagedRepositoryAdmin getManagedRepositoryAdmin()
    {
        return managedRepositoryAdmin;
    }

    public void setManagedRepositoryAdmin( ManagedRepositoryAdmin managedRepositoryAdmin )
    {
        this.managedRepositoryAdmin = managedRepositoryAdmin;
    }
}
