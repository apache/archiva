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

import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.admin.model.remote.RemoteRepositoryAdmin;
import org.apache.archiva.audit.Auditable;
import org.apache.archiva.checksum.ChecksumAlgorithm;
import org.apache.archiva.checksum.ChecksumUtil;
import org.apache.archiva.checksum.StreamingChecksum;
import org.apache.archiva.common.filelock.DefaultFileLockManager;
import org.apache.archiva.common.filelock.FileLockManager;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.archiva.common.utils.PathUtil;
import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.indexer.ArchivaIndexingContext;
import org.apache.archiva.indexer.merger.IndexMerger;
import org.apache.archiva.indexer.merger.IndexMergerException;
import org.apache.archiva.indexer.merger.IndexMergerRequest;
import org.apache.archiva.indexer.merger.base.MergedRemoteIndexesTask;
import org.apache.archiva.indexer.merger.base.MergedRemoteIndexesTaskRequest;
import org.apache.archiva.indexer.merger.TemporaryGroupIndex;
import org.apache.archiva.indexer.search.RepositorySearch;
import org.apache.archiva.indexer.search.RepositorySearchException;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.metadata.repository.storage.RelocationException;
import org.apache.archiva.metadata.repository.storage.RepositoryStorage;
import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.policies.ProxyDownloadException;
import org.apache.archiva.proxy.ProxyRegistry;
import org.apache.archiva.proxy.model.RepositoryProxyHandler;
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
import org.apache.archiva.repository.LayoutException;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.ReleaseScheme;
import org.apache.archiva.repository.RepositoryGroup;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.RepositoryRequestInfo;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.metadata.audit.AuditListener;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.metadata.base.MetadataTools;
import org.apache.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.archiva.repository.metadata.base.RepositoryMetadataMerge;
import org.apache.archiva.repository.metadata.base.RepositoryMetadataWriter;
import org.apache.archiva.scheduler.repository.model.RepositoryArchivaTaskScheduler;
import org.apache.archiva.security.ServletAuthenticator;
import org.apache.archiva.webdav.util.MimeTypes;
import org.apache.archiva.webdav.util.TemporaryGroupIndexSessionCleaner;
import org.apache.archiva.webdav.util.WebdavMethodUtil;
import org.apache.archiva.xml.XMLException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.SimpleLockManager;
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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    private ProxyRegistry proxyRegistry;

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
    private RepositoryRegistry repositoryRegistry;

    @Inject
    private IndexMerger indexMerger;

    @Inject
    private RepositorySearch repositorySearch;

    /**
     * Lock Manager - use simple implementation from JackRabbit
     */
    private final LockManager lockManager = new SimpleLockManager();

    @Inject
    @Named( value = "archivaTaskScheduler#repository" )
    private RepositoryArchivaTaskScheduler scheduler;

    @Inject
    @Named( value = "fileLockManager#default" )
    private FileLockManager fileLockManager;

    private ApplicationContext applicationContext;


    @Inject
    public ArchivaDavResourceFactory( ApplicationContext applicationContext, ArchivaConfiguration archivaConfiguration )
        throws PlexusSisuBridgeException
    {
        this.archivaConfiguration = archivaConfiguration;
        this.applicationContext = applicationContext;

    }

    @PostConstruct
    public void initialize() throws IOException
    {

    }


    @Override
    public DavResource createResource( final DavResourceLocator locator, final DavServletRequest request,
                                       final DavServletResponse response )
        throws DavException
    {
        final ArchivaDavResourceLocator archivaLocator = checkLocatorIsInstanceOfRepositoryLocator( locator );

        final String sRepoId = archivaLocator.getRepositoryId();

        RepositoryGroup repoGroup = repositoryRegistry.getRepositoryGroup(sRepoId);

        final boolean isGroupRepo = repoGroup != null;

        String activePrincipal = getActivePrincipal( request );

        List<String> resourcesInAbsolutePath = new ArrayList<>();

        boolean readMethod = WebdavMethodUtil.isReadMethod( request.getMethod() );
        RepositoryRequestInfo repositoryRequestInfo = null;
        DavResource resource;
        if ( isGroupRepo )
        {
            if ( !readMethod )
            {
                throw new DavException( HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                                        "Write method not allowed for repository groups." );
            }

            log.debug( "Repository group '{}' accessed by '{}", repoGroup.getId(), activePrincipal );

            // handle browse requests for virtual repos
            if ( getLogicalResource( archivaLocator, null, true ).endsWith( "/" ) )
            {
                DavResource davResource =
                    getResourceFromGroup( request, archivaLocator,
                                          repoGroup );

                setHeaders( response, locator, davResource, true );

                return davResource;

            }
            else
            {
                // make a copy to avoid potential concurrent modifications (eg. by configuration)
                // TODO: ultimately, locking might be more efficient than copying in this fashion since updates are
                //  infrequent
                resource = processRepositoryGroup( request, archivaLocator, activePrincipal,
                                                   resourcesInAbsolutePath, repoGroup );
                for (ManagedRepository repo : repoGroup.getRepositories() ) {
                    if (repo!=null) {
                        repositoryRequestInfo = repo.getRequestInfo();
                        break;
                    }
                }
            }
        }
        else
        {

            // We do not provide folders for remote repositories


            ManagedRepository repo = repositoryRegistry.getManagedRepository( sRepoId );
            if (repo==null) {
                throw new DavException( HttpServletResponse.SC_NOT_FOUND,
                    "Invalid repository: " + archivaLocator.getRepositoryId() );
            }
            ManagedRepositoryContent managedRepositoryContent = repo.getContent( );
            if (managedRepositoryContent==null) {
                log.error("Inconsistency detected. Repository content not found for '{}'", archivaLocator.getRepositoryId());
                throw new DavException( HttpServletResponse.SC_NOT_FOUND,
                    "Invalid repository: " + archivaLocator.getRepositoryId() );
            }

            log.debug( "Managed repository '{}' accessed by '{}'", managedRepositoryContent.getId(), activePrincipal );

            resource = processRepository( request, archivaLocator, activePrincipal, managedRepositoryContent,
                                          repo);
            repositoryRequestInfo = repo.getRequestInfo();
            String logicalResource = getLogicalResource( archivaLocator, null, false );
            resourcesInAbsolutePath.add(
                Paths.get( managedRepositoryContent.getRepoRoot(), logicalResource ).toAbsolutePath().toString() );

        }

        String requestedResource = request.getRequestURI();

        // MRM-872 : merge all available metadata
        // merge metadata only when requested via the repo group
        if ( ( repositoryRequestInfo.isMetadata( requestedResource ) || repositoryRequestInfo.isMetadataSupportFile(
            requestedResource ) ) && isGroupRepo )
        {
            // this should only be at the project level not version level!
            if ( isProjectReference( requestedResource ) )
            {

                ArchivaDavResource res = (ArchivaDavResource) resource;
                String newPath;
                if (res.getAsset().hasParent())
                {
                    newPath = res.getAsset( ).getParent( ).getPath( ) + "/maven-metadata-" + sRepoId + ".xml";
                } else {
                    newPath = StringUtils.substringBeforeLast( res.getAsset().getPath(), "/" ) + "/maven-metadata-" + sRepoId + ".xml";;
                }
                // for MRM-872 handle checksums of the merged metadata files
                if ( repositoryRequestInfo.isSupportFile( requestedResource ) )
                {
                    String metadataChecksumPath = newPath + "." + StringUtils.substringAfterLast( requestedResource, "." );
                    StorageAsset metadataChecksum = repoGroup.getAsset( metadataChecksumPath );
                    if ( repoGroup.getAsset( metadataChecksumPath ).exists() )
                    {
                        LogicalResource logicalResource =
                            new LogicalResource( getLogicalResource( archivaLocator, null, false ) );

                        try
                        {
                            resource =
                                new ArchivaDavResource( metadataChecksum, logicalResource.getPath(), repoGroup,
                                                        request.getRemoteAddr(), activePrincipal, request.getDavSession(),
                                                        archivaLocator, this, mimeTypes, auditListeners, scheduler);
                        }
                        catch ( LayoutException e )
                        {
                            log.error("Incompatible layout: {}", e.getMessage(), e);
                            throw new DavException( 500, e );
                        }
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
                                FilesystemStorage storage = new FilesystemStorage( metadataFile.getParent( ), new DefaultFileLockManager( ) );
                                ArchivaRepositoryMetadata repoMetadata = repositoryRegistry.getMetadataReader( repoGroup.getType( ) ).read( storage.getAsset( metadataFile.getFileName().toString() ) );
                                mergedMetadata = RepositoryMetadataMerge.merge( mergedMetadata, repoMetadata );
                            }
                            catch ( RepositoryMetadataException r )
                            {
                                throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                                        "Error occurred while merging metadata file." );
                            }
                            catch ( IOException e )
                            {
                                throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                    "Error occurred while merging metadata file." );
                            }
                        }

                        try
                        {
                            StorageAsset resourceFile = writeMergedMetadataToFile( repoGroup, mergedMetadata, newPath );

                            LogicalResource logicalResource =
                                new LogicalResource( getLogicalResource( archivaLocator, null, false ) );

                            resource =
                                new ArchivaDavResource( resourceFile, logicalResource.getPath(), repoGroup,
                                                        request.getRemoteAddr(), activePrincipal,
                                                        request.getDavSession(), archivaLocator, this, mimeTypes,
                                                        auditListeners, scheduler);
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
                        catch ( LayoutException e )
                        {
                            log.error("Incompatible layout: {}", e.getMessage(), e);
                            throw new DavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Incompatible layout for repository "+repoGroup.getId());
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
                                                ArchivaDavResourceLocator archivaLocator,
                                                String activePrincipal, List<String> resourcesInAbsolutePath,
                                                RepositoryGroup repoGroup )
        throws DavException
    {
        DavResource resource = null;
        List<DavException> storedExceptions = new ArrayList<>();

        String pathInfo = StringUtils.removeEnd( request.getPathInfo(), "/" );

        String rootPath = StringUtils.substringBeforeLast( pathInfo, "/" );

        String mergedIndexPath = "/";
        if (repoGroup.supportsFeature( IndexCreationFeature.class )) {
            mergedIndexPath = repoGroup.getFeature( IndexCreationFeature.class ).get().getIndexPath().getPath();
        }

        if ( StringUtils.endsWith( rootPath, mergedIndexPath ) )
        {
            // we are in the case of index file request
            String requestedFileName = StringUtils.substringAfterLast( pathInfo, "/" );
            StorageAsset temporaryIndexDirectory =
                buildMergedIndexDirectory( activePrincipal, request, repoGroup );
            StorageAsset asset = temporaryIndexDirectory.resolve(requestedFileName);

            try {
                resource = new ArchivaDavResource( asset, requestedFileName, repoGroup,
                                                   request.getRemoteAddr(), activePrincipal, request.getDavSession(),
                                                   archivaLocator, this, mimeTypes, auditListeners, scheduler );
            } catch (LayoutException e) {
                log.error("Bad layout: {}", e.getMessage(), e);
                throw new DavException(500, e);
            }

        }
        else
        {
            for ( ManagedRepository repository : repoGroup.getRepositories() )
            {
                String repositoryId = repository.getId();
                ManagedRepositoryContent managedRepositoryContent;
                ManagedRepository managedRepository = repositoryRegistry.getManagedRepository( repositoryId );
                if (managedRepository==null) {
                    throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not find repository with id "+repositoryId );
                }
                managedRepositoryContent = managedRepository.getContent();
                if (managedRepositoryContent==null) {
                    log.error("Inconsistency detected. Repository content not found for '{}'",repositoryId);
                    throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not find repository content with id "+repositoryId );
                }
                try
                {
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

    private String getLogicalResource( ArchivaDavResourceLocator archivaLocator, org.apache.archiva.repository.ManagedRepository managedRepository,
                                       boolean useOrigResourcePath )
    {
        // FIXME remove this hack
        // but currently managedRepository can be null in case of group
        String layout = managedRepository == null ? "default" : managedRepository.getLayout();
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
            ? "default"
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
        catch (XMLException | IOException e )
        {
            log.error( e.getMessage(), e );
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
        }
    }

    private DavResource processRepository( final DavServletRequest request, ArchivaDavResourceLocator archivaLocator,
                                           String activePrincipal, ManagedRepositoryContent managedRepositoryContent,
                                           org.apache.archiva.repository.ManagedRepository managedRepository )
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
            StorageAsset repoAsset = managedRepository.getAsset( path );
            // Path resourceFile = Paths.get( managedRepositoryContent.getRepoRoot(), path );
            try
            {
                resource =
                    new ArchivaDavResource( repoAsset, path, managedRepository,
                                            request.getRemoteAddr(), activePrincipal, request.getDavSession(),
                                            archivaLocator, this, mimeTypes, auditListeners, scheduler );
            }
            catch ( LayoutException e )
            {
                log.error("Incompatible layout: {}", e.getMessage(), e);
                throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
            }

            if ( WebdavMethodUtil.isReadMethod( request.getMethod() ) )
            {
                if ( archivaLocator.getHref( false ).endsWith( "/" ) && !repoAsset.isContainer() )
                {
                    // force a resource not found
                    throw new DavException( HttpServletResponse.SC_NOT_FOUND, "Resource does not exist" );
                }
                else
                {
                    if ( !resource.isCollection() )
                    {
                        boolean previouslyExisted = repoAsset.exists();

                        boolean fromProxy = fetchContentFromProxies( managedRepository, request, logicalResource );

                        StorageAsset resourceAsset=null;
                        // At this point the incoming request can either be in default or
                        // legacy layout format.
                        try
                        {
                            // Perform an adjustment of the resource to the managed
                            // repository expected path.
                            // String localResourcePath = managedRepository.getRequestInfo().toNativePath( logicalResource.getPath() );
                            resourceAsset = managedRepository.getAsset( logicalResource.getPath() );
                            resource =
                                new ArchivaDavResource( resourceAsset, logicalResource.getPath(),
                                                        managedRepository,
                                                        request.getRemoteAddr(), activePrincipal,
                                                        request.getDavSession(), archivaLocator, this, mimeTypes,
                                                        auditListeners, scheduler );
                        }
                        catch ( LayoutException e )
                        {
                            if ( resourceAsset==null || !resourceAsset.exists() )
                            {
                                throw new DavException( HttpServletResponse.SC_NOT_FOUND, e );
                            }
                        }

                        if ( fromProxy )
                        {
                            String action = ( previouslyExisted ? AuditEvent.MODIFY_FILE : AuditEvent.CREATE_FILE )
                                + PROXIED_SUFFIX;

                            log.debug( "Proxied artifact '{}' in repository '{}' (current user '{}')",
                                       resourceAsset.getName(), managedRepositoryContent.getId(), activePrincipal );

                            triggerAuditEvent( request.getRemoteAddr(), archivaLocator.getRepositoryId(),
                                               logicalResource.getPath(), action, activePrincipal );
                        }

                        if ( !resourceAsset.exists() )
                        {
                            throw new DavException( HttpServletResponse.SC_NOT_FOUND, "Resource does not exist" );
                        }
                    }
                }
            }

            if ( request.getMethod().equals( HTTP_PUT_METHOD ) )
            {
                String resourcePath = logicalResource.getPath();
                RepositoryRequestInfo repositoryRequestInfo = managedRepository.getRequestInfo();
                // check if target repo is enabled for releases
                // we suppose that release-artifacts can be deployed only to repos enabled for releases
                if ( managedRepositoryContent.getRepository().getActiveReleaseSchemes().contains( ReleaseScheme.RELEASE ) && !repositoryRequestInfo.isMetadata(
                    resourcePath ) && !repositoryRequestInfo.isSupportFile( resourcePath ) )
                {
                    ArtifactReference artifact = null;
                    try
                    {
                        artifact = managedRepositoryContent.toArtifactReference( resourcePath );

                        if ( !VersionUtil.isSnapshot( artifact.getVersion() ) )
                        {
                            // check if artifact already exists and if artifact re-deployment to the repository is allowed
                            if ( managedRepositoryContent.hasContent( artifact )
                                && managedRepositoryContent.getRepository().blocksRedeployments())
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
                    catch ( org.apache.archiva.repository.ContentAccessException e )
                    {
                        e.printStackTrace( );
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
        ManagedRepository repo = repositoryRegistry.getManagedRepository( archivaLocator.getRepositoryId( ) );
        if (repo==null) {
            throw new DavException( HttpServletResponse.SC_NOT_FOUND,
                "Invalid repository: " + archivaLocator.getRepositoryId() );
        }
        managedRepositoryContent = repo.getContent();
        if (managedRepositoryContent==null) {
            log.error("Inconsistency detected. Repository content not found for '{}'", archivaLocator.getRepositoryId());
            throw new DavException( HttpServletResponse.SC_NOT_FOUND,
                "Invalid repository: " + archivaLocator.getRepositoryId() );
        }

        DavResource resource = null;
        String logicalResource = getLogicalResource( archivaLocator, repo, false );
        if ( logicalResource.startsWith( "/" ) )
        {
            logicalResource = logicalResource.substring( 1 );
        }
        StorageAsset resourceAsset = repo.getAsset( logicalResource );
        try
        {
            resource = new ArchivaDavResource( resourceAsset, logicalResource,
                                               repo, davSession, archivaLocator,
                                               this, mimeTypes, auditListeners, scheduler);
        }
        catch ( LayoutException e )
        {
            log.error( "Incompatible layout: {}", e.getMessage( ), e );
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
        }

        resource.addLockManager( lockManager );
        return resource;
    }

    private boolean fetchContentFromProxies( ManagedRepository managedRepository, DavServletRequest request,
                                             LogicalResource resource )
        throws DavException
    {
        String path = resource.getPath();
        if (!proxyRegistry.hasHandler(managedRepository.getType())) {
            throw new DavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No proxy handler found for repository type "+managedRepository.getType());
        }
        RepositoryRequestInfo repositoryRequestInfo = managedRepository.getRequestInfo();
        RepositoryProxyHandler proxyHandler = proxyRegistry.getHandler(managedRepository.getType()).get(0);
        if ( repositoryRequestInfo.isSupportFile( path ) )
        {
            StorageAsset proxiedFile = proxyHandler.fetchFromProxies( managedRepository, path );

            return ( proxiedFile != null );
        }

        // Is it a Metadata resource?
        if ( "default".equals(repositoryRequestInfo.getLayout( path )) && repositoryRequestInfo.isMetadata( path ) )
        {
            return proxyHandler.fetchMetadataFromProxies( managedRepository, path ).isModified();
        }

        // Is it an Archetype Catalog?
        if ( repositoryRequestInfo.isArchetypeCatalog( path ) )
        {
            // FIXME we must implement a merge of remote archetype catalog from remote servers.
            StorageAsset proxiedFile = proxyHandler.fetchFromProxies( managedRepository, path );

            return ( proxiedFile != null );
        }

        // Not any of the above? Then it's gotta be an artifact reference.
        try
        {
            // Get the artifact reference in a layout neutral way.
            ArtifactReference artifact = repositoryRequestInfo.toArtifactReference( path );

            if ( artifact != null )
            {
                String repositoryLayout = managedRepository.getLayout();

                RepositoryStorage repositoryStorage =
                    this.applicationContext.getBean( "repositoryStorage#" + repositoryLayout, RepositoryStorage.class );
                repositoryStorage.applyServerSideRelocation( managedRepository, artifact );

                StorageAsset proxiedFile = proxyHandler.fetchFromProxies( managedRepository, artifact );

                resource.setPath( managedRepository.getContent().toPath( artifact ) );

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
            && ( ArchivaDavResource.class.cast( resource ).getAsset().isContainer() ) ) )
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

    public void setProxyRegistry(ProxyRegistry proxyRegistry) {
        this.proxyRegistry = proxyRegistry;
    }

    public ProxyRegistry getProxyRegistry() {
        return this.proxyRegistry;
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

    private DavResource getResourceFromGroup( DavServletRequest request,
                                              ArchivaDavResourceLocator locator,
                                              RepositoryGroup repositoryGroup )
        throws DavException
    {
        final String id = repositoryGroup.getId();
        final List<ManagedRepository> repositories = repositoryGroup.getRepositories();
        if ( repositories == null
            || repositories.isEmpty() )
        {
            try {
                return new ArchivaDavResource( repositoryGroup.getAsset("/"), "groups/" + id, null,
                                               request.getDavSession(), locator, this, mimeTypes, auditListeners, scheduler);
            } catch (LayoutException e) {
                log.error("Bad repository layout: {}", e.getMessage(), e);
                throw new DavException(500, e);
            }
        }
        List<StorageAsset> mergedRepositoryContents = new ArrayList<>();

        ManagedRepository firstRepo = repositories.get( 0 );

        String path = getLogicalResource( locator, firstRepo, false );
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
        String mergedIndexPath = "/";
        if (repositoryGroup.supportsFeature( IndexCreationFeature.class )) {
            IndexCreationFeature indexCreationFeature = repositoryGroup.getFeature( IndexCreationFeature.class ).get();
            mergedIndexPath = indexCreationFeature.getIndexPath().getPath();
        }

        if ( allow )
        {

            if ( StringUtils.endsWith( pathInfo, mergedIndexPath ) )
            {
                StorageAsset mergedRepoDirPath =
                    buildMergedIndexDirectory( activePrincipal, request, repositoryGroup );
                mergedRepositoryContents.add( mergedRepoDirPath );
            }
            else
            {
                if ( StringUtils.equalsIgnoreCase( pathInfo, "/" + id ) )
                {
                    Path tmpDirectory = Paths.get( SystemUtils.getJavaIoTmpDir().toString(),
                                                  id,
                                                      mergedIndexPath );
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
                    try {
                        FilesystemStorage storage = new FilesystemStorage(tmpDirectory.getParent(), new DefaultFileLockManager());
                        mergedRepositoryContents.add( storage.getAsset("") );
                    } catch (IOException e) {
                        throw new DavException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not create storage for " + tmpDirectory);
                    }
                }
                for ( ManagedRepository repo : repositories )
                {
                    ManagedRepositoryContent managedRepository = null;
                    if (repo == null) {
                        throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Invalid managed repository <" + repo.getId() + ">");
                    }
                    managedRepository = repo.getContent();
                    if (managedRepository==null) {
                        log.error("Inconsistency detected. Repository content not found for '{}'",repo.getId());
                        throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "Invalid managed repository <" + repo.getId() + ">");
                    }
                    // Path resourceFile = Paths.get( managedRepository.getRepoRoot(), logicalResource.getPath() );
                    StorageAsset resourceFile = repo.getAsset(logicalResource.getPath());
                    if ( resourceFile.exists() && managedRepository.getRepository().supportsFeature( IndexCreationFeature.class ))
                    {
                        // in case of group displaying index directory doesn't have sense !!
                        IndexCreationFeature idf = managedRepository.getRepository().getFeature(IndexCreationFeature.class).get();
                        StorageAsset repoIndexDirectory = idf.getLocalIndexPath();
                        if ( !StringUtils.equals( FilenameUtils.normalize( repoIndexDirectory.getPath() ),
                                                  FilenameUtils.normalize( logicalResource.getPath() ) ) )
                        {
                            // for prompted authentication
                            if ( httpAuth.getSecuritySession( request.getSession( true ) ) != null )
                            {
                                try
                                {
                                    if ( isAuthorized( request, repo.getId() ) )
                                    {
                                        mergedRepositoryContents.add( resourceFile );
                                        log.debug( "Repository '{}' accessed by '{}'", repo.getId(), activePrincipal );
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
                                    if ( servletAuth.isAuthorized( activePrincipal, repo.getId(),
                                                                   WebdavMethodUtil.getMethodPermission(
                                                                       request.getMethod() ) ) )
                                    {
                                        mergedRepositoryContents.add( resourceFile );
                                        log.debug( "Repository '{}' accessed by '{}'", repo.getId(), activePrincipal );
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
    private boolean isAllowedToContinue( DavServletRequest request, List<ManagedRepository> repositories, String activePrincipal )
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
            for ( ManagedRepository repository : repositories )
            {
                try
                {
                    if ( isAuthorized( request, repository.getId() ) )
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
            for ( ManagedRepository repository : repositories )
            {
                try
                {
                    if ( servletAuth.isAuthorized( activePrincipal, repository.getId(),
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

    private StorageAsset writeMergedMetadataToFile( RepositoryGroup repoGroup, ArchivaRepositoryMetadata mergedMetadata, String outputFilename )
        throws RepositoryMetadataException, IOException
    {
        StorageAsset asset = repoGroup.addAsset( outputFilename, false );
        OutputStream stream = asset.getWriteStream( true );
        OutputStreamWriter sw = new OutputStreamWriter( stream, "UTF-8" );
        RepositoryMetadataWriter.write( mergedMetadata, sw );

        createChecksumFiles( repoGroup, outputFilename );
        return asset;
    }


    private void createChecksumFiles(RepositoryGroup repo, String path) {
        List<ChecksumAlgorithm> algorithms = ChecksumUtil.getAlgorithms( archivaConfiguration.getConfiguration( ).getArchivaRuntimeConfiguration( ).getChecksumTypes( ) );
        List<OutputStream> outStreams = algorithms.stream( ).map( algo -> {
            String ext = algo.getDefaultExtension( );
            try
            {
                return repo.getAsset( path + "." + ext ).getWriteStream( true );
            }
            catch ( IOException e )
            {
                e.printStackTrace( );
                return null;
            }
        } ).filter( Objects::nonNull ).collect( Collectors.toList( ) );
        try
        {
            StreamingChecksum.updateChecksums( repo.getAsset(path).getReadStream(), algorithms, outStreams );
        }
        catch ( IOException e )
        {
            e.printStackTrace( );
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

    protected StorageAsset buildMergedIndexDirectory( String activePrincipal,
                                              DavServletRequest request,
                                              RepositoryGroup repositoryGroup )
        throws DavException
    {

        try
        {
            final List<ManagedRepository> repositories = repositoryGroup.getRepositories();
            HttpSession session = request.getSession();

            @SuppressWarnings( "unchecked" ) Map<String, TemporaryGroupIndex> temporaryGroupIndexMap =
                (Map<String, TemporaryGroupIndex>) session.getAttribute(
                    TemporaryGroupIndexSessionCleaner.TEMPORARY_INDEX_SESSION_KEY );
            if ( temporaryGroupIndexMap == null )
            {
                temporaryGroupIndexMap = new HashMap<>();
            }

            final String id = repositoryGroup.getId();
            TemporaryGroupIndex tmp = temporaryGroupIndexMap.get(id);

            if ( tmp != null && tmp.getDirectory() != null && tmp.getDirectory().exists())
            {
                if ( System.currentTimeMillis() - tmp.getCreationTime() > (
                    repositoryGroup.getMergedIndexTTL() * 60 * 1000 ) )
                {
                    log.debug( MarkerFactory.getMarker( "group.merged.index" ),
                               "tmp group index '{}' is too old so delete it", id);
                    indexMerger.cleanTemporaryGroupIndex( tmp );
                }
                else
                {
                    log.debug( MarkerFactory.getMarker( "group.merged.index" ),
                               "merged index for group '{}' found in cache", id);
                    return tmp.getDirectory();
                }
            }

            Set<String> authzRepos = new HashSet<String>();

            String permission = WebdavMethodUtil.getMethodPermission( request.getMethod() );

            for ( ManagedRepository repository : repositories )
            {
                try
                {
                    if ( servletAuth.isAuthorized( activePrincipal, repository.getId(), permission ) )
                    {
                        authzRepos.add( repository.getId() );
                        authzRepos.addAll( this.repositorySearch.getRemoteIndexingContextIds( repository.getId() ) );
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
                    id, authzRepos );

            IndexCreationFeature indexCreationFeature = repositoryGroup.getFeature( IndexCreationFeature.class ).get();
            Path indexPath = indexCreationFeature.getLocalIndexPath().getFilePath();
            if (indexPath!=null)
            {
                Path tempRepoFile = Files.createTempDirectory( "temp" );
                tempRepoFile.toFile( ).deleteOnExit( );
                FilesystemStorage storage = new FilesystemStorage(tempRepoFile, new DefaultFileLockManager());
                StorageAsset tmpAsset = storage.getAsset("");

                IndexMergerRequest indexMergerRequest =
                    new IndexMergerRequest( authzRepos, true, id,
                        indexPath.toString( ),
                        repositoryGroup.getMergedIndexTTL( ) ).mergedIndexDirectory(
                        tmpAsset ).temporary( true );

                MergedRemoteIndexesTaskRequest taskRequest =
                    new MergedRemoteIndexesTaskRequest( indexMergerRequest, indexMerger );

                MergedRemoteIndexesTask job = new MergedRemoteIndexesTask( taskRequest );

                ArchivaIndexingContext indexingContext = job.execute( ).getIndexingContext( );

                StorageAsset mergedRepoDir = indexingContext.getPath( );
                TemporaryGroupIndex temporaryGroupIndex =
                    new TemporaryGroupIndex( mergedRepoDir, indexingContext.getId( ), id,
                        repositoryGroup.getMergedIndexTTL( ) ) //
                        .setCreationTime( new Date( ).getTime( ) );
                temporaryGroupIndexMap.put( id, temporaryGroupIndex );
                session.setAttribute( TemporaryGroupIndexSessionCleaner.TEMPORARY_INDEX_SESSION_KEY,
                    temporaryGroupIndexMap );
                return mergedRepoDir;
            } else {
                log.error("Local index path for repository group {} does not exist.", repositoryGroup.getId());
                throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
            }
        }
        catch ( RepositorySearchException e )
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

    public RepositoryRegistry getRepositoryRegistry( )
    {
        return repositoryRegistry;
    }

    public void setRepositoryRegistry( RepositoryRegistry repositoryRegistry )
    {
        this.repositoryRegistry = repositoryRegistry;
    }
}
