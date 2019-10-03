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

import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.repository.LayoutException;
import org.apache.archiva.repository.storage.RepositoryStorage;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.metadata.audit.AuditListener;
import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.model.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.model.RepositoryTask;
import org.apache.archiva.webdav.util.IndexWriter;
import org.apache.archiva.webdav.util.MimeTypes;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceIteratorImpl;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 */
public class ArchivaDavResource
    implements DavResource
{
    public static final String HIDDEN_PATH_PREFIX = ".";

    private final ArchivaDavResourceLocator locator;

    private final DavResourceFactory factory;

    // private final Path localResource;

    private final String logicalResource;

    private DavPropertySet properties = null;

    private LockManager lockManager;

    private final DavSession session;

    private String remoteAddr;

    private final RepositoryStorage repositoryStorage;

    private final MimeTypes mimeTypes;

    private List<AuditListener> auditListeners;

    private String principal;

    public static final String COMPLIANCE_CLASS = "1, 2";

    private final ArchivaTaskScheduler<RepositoryTask> scheduler;

    private Logger log = LoggerFactory.getLogger( ArchivaDavResource.class );

    private StorageAsset asset;

    public ArchivaDavResource( StorageAsset localResource, String logicalResource, RepositoryStorage repositoryStorage,
                               DavSession session, ArchivaDavResourceLocator locator, DavResourceFactory factory,
                               MimeTypes mimeTypes, List<AuditListener> auditListeners,
                               RepositoryArchivaTaskScheduler scheduler) throws LayoutException
    {
        // this.localResource = Paths.get( localResource );
        this.asset = localResource;
        this.logicalResource = logicalResource;
        this.locator = locator;
        this.factory = factory;
        this.session = session;

        // TODO: push into locator as well as moving any references out of the resource factory
        this.repositoryStorage = repositoryStorage;

        // TODO: these should be pushed into the repository layer, along with the physical file operations in this class
        this.mimeTypes = mimeTypes;
        this.auditListeners = auditListeners;
        this.scheduler = scheduler;

    }

    public ArchivaDavResource( StorageAsset localResource, String logicalResource, RepositoryStorage repositoryStorage,
                               String remoteAddr, String principal, DavSession session,
                               ArchivaDavResourceLocator locator, DavResourceFactory factory, MimeTypes mimeTypes,
                               List<AuditListener> auditListeners, RepositoryArchivaTaskScheduler scheduler) throws LayoutException
    {
        this( localResource, logicalResource, repositoryStorage, session, locator, factory, mimeTypes, auditListeners,
              scheduler );

        this.remoteAddr = remoteAddr;
        this.principal = principal;
    }


    @Override
    public String getComplianceClass()
    {
        return COMPLIANCE_CLASS;
    }

    @Override
    public String getSupportedMethods()
    {
        return METHODS;
    }

    @Override
    public boolean exists()
    {
        return asset.exists();
    }

    @Override
    public boolean isCollection()
    {
        return asset.isContainer();
    }

    @Override
    public String getDisplayName()
    {
        String resPath = getResourcePath();
        return ( resPath != null ) ? Text.getName( resPath ) : resPath;
    }

    @Override
    public DavResourceLocator getLocator()
    {
        return locator;
    }

    @Override
    public String getResourcePath()
    {
        return locator.getResourcePath();
    }

    @Override
    public String getHref()
    {
        return locator.getHref( isCollection() );
    }

    @Override
    public long getModificationTime()
    {
        return asset.getModificationTime().toEpochMilli();
    }

    @Override
    public void spool( OutputContext outputContext )
        throws IOException
    {
        if ( !isCollection() )
        {
            outputContext.setContentLength( asset.getSize());
            outputContext.setContentType( mimeTypes.getMimeType( asset.getName() ) );
        }

        if ( !isCollection() && outputContext.hasStream() )
        {
            repositoryStorage.consumeData( asset, is -> {copyStream(is, outputContext.getOutputStream());}, true );
        }
        else if ( outputContext.hasStream() )
        {
            IndexWriter writer = new IndexWriter( asset, logicalResource );
            writer.write( outputContext );
        }
    }

    private void copyStream(InputStream is, OutputStream os) throws RuntimeException {
        try
        {
            IOUtils.copy(is, os);
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Copy failed "+e.getMessage(), e );
        }
    }

    @Override
    public DavPropertyName[] getPropertyNames()
    {
        return getProperties().getPropertyNames();
    }

    @Override
    public DavProperty getProperty( DavPropertyName name )
    {
        return getProperties().get( name );
    }

    @Override
    public DavPropertySet getProperties()
    {
        return initProperties();
    }

    @Override
    public void setProperty( DavProperty property )
        throws DavException
    {
    }

    @Override
    public void removeProperty( DavPropertyName propertyName )
        throws DavException
    {
    }

    public MultiStatusResponse alterProperties( DavPropertySet setProperties, DavPropertyNameSet removePropertyNames )
        throws DavException
    {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public MultiStatusResponse alterProperties( List changeList )
        throws DavException
    {
        return null;
    }

    @Override
    public DavResource getCollection()
    {
        DavResource parent = null;
        if ( getResourcePath() != null && !getResourcePath().equals( "/" ) )
        {
            String parentPath = Text.getRelativeParent( getResourcePath(), 1 );
            if ( parentPath.equals( "" ) )
            {
                parentPath = "/";
            }
            DavResourceLocator parentloc =
                locator.getFactory().createResourceLocator( locator.getPrefix(), parentPath );
            try
            {
                parent = factory.createResource( parentloc, session );
            }
            catch ( DavException e )
            {
                // should not occur
            }
        }
        return parent;
    }

    @Override
    public void addMember( DavResource resource, InputContext inputContext )
        throws DavException
    {
        // Path localFile = localResource.resolve( resource.getDisplayName() );
        boolean exists = asset.exists();
        final String newPath = asset.getPath()+"/"+resource.getDisplayName();

        if ( isCollection() && inputContext.hasStream() ) // New File
        {
            Path tempFile = null;
            try
            {
                tempFile = Files.createTempFile( "archiva_upload","dat" );
                try(OutputStream os = Files.newOutputStream( tempFile, StandardOpenOption.CREATE ))
                {
                    IOUtils.copy( inputContext.getInputStream( ), os );
                }
                long expectedContentLength = inputContext.getContentLength();
                long actualContentLength = 0;
                try
                {
                    actualContentLength = Files.size(tempFile);
                }
                catch ( IOException e )
                {
                    log.error( "Could not get length of file {}: {}", tempFile, e.getMessage(), e );
                }
                // length of -1 is given for a chunked request or unknown length, in which case we accept what was uploaded
                if ( expectedContentLength >= 0 && expectedContentLength != actualContentLength )
                {
                    String msg = "Content Header length was " + expectedContentLength + " but was " + actualContentLength;
                    log.debug( "Upload failed: {}", msg );
                    throw new DavException( HttpServletResponse.SC_BAD_REQUEST, msg );
                }
                StorageAsset member = repositoryStorage.addAsset( newPath, false );
                member.create();
                member.replaceDataFromFile( tempFile );
            }
            catch ( IOException e )
            {
                throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
            } finally {
                if (tempFile!=null)
                {
                    try
                    {
                        Files.deleteIfExists( tempFile );
                    }
                    catch ( IOException e )
                    {
                        log.error("Could not delete temporary file {}", tempFile);
                    }
                }
            }

            // queueRepositoryTask( asset );

            log.debug( "File '{}{}(current user '{}')", resource.getDisplayName(),
                       ( exists ? "' modified " : "' created " ), this.principal );

            // triggerAuditEvent( resource, exists ? AuditEvent.MODIFY_FILE : AuditEvent.CREATE_FILE );
        }
        else if ( !inputContext.hasStream() && isCollection() ) // New directory
        {
            try
            {
                StorageAsset member = repositoryStorage.addAsset( newPath, true );
                member.create();
            }
            catch ( IOException e )
            {
                log.error("Could not create directory {}: {}", newPath, e.getMessage(), e);
            }

            log.debug( "Directory '{}' (current user '{}')", resource.getDisplayName(), this.principal );

            triggerAuditEvent( resource, AuditEvent.CREATE_DIR );
        }
        else
        {
            String msg = "Could not write member " + resource.getResourcePath() + " at " + getResourcePath()
                + " as this is not a DAV collection";
            log.debug( msg );
            throw new DavException( HttpServletResponse.SC_BAD_REQUEST, msg );
        }
    }

    public StorageAsset getAsset() {
        return asset;
    }

    @Override
    public DavResourceIterator getMembers()
    {
        List<DavResource> list;
        if ( exists() && isCollection() )
        {
            list = asset.list().stream().filter( m -> !m.getName().startsWith( HIDDEN_PATH_PREFIX ) )
                .map(m -> {
                    String path = locator.getResourcePath( ) + '/' + m.getName();
                    DavResourceLocator resourceLocator =
                        locator.getFactory( ).createResourceLocator( locator.getPrefix( ), path );
                    try
                    {
                        return factory.createResource( resourceLocator, session );
                    }
                    catch ( DavException e )
                    {
                        return null;
                    }

                }).filter( Objects::nonNull ).collect( Collectors.toList());
        } else {
            list = Collections.emptyList( );
        }
        return new DavResourceIteratorImpl( list );
    }

    @Override
    public void removeMember( DavResource member )
        throws DavException
    {
        StorageAsset resource = checkDavResourceIsArchivaDavResource( member ).getAsset( );

        if ( resource.exists() )
        {
            try
            {
                if ( resource.isContainer() )
                {
                    repositoryStorage.removeAsset( resource );
                    triggerAuditEvent( member, AuditEvent.REMOVE_DIR );
                }
                else
                {
                    repositoryStorage.removeAsset( resource );
                    triggerAuditEvent( member, AuditEvent.REMOVE_FILE );
                }

                log.debug( "{}{}' removed (current user '{}')", ( resource.isContainer() ? "Directory '" : "File '" ),
                           member.getDisplayName(), this.principal );

            }
            catch ( IOException e )
            {
                throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
            }
        }
        else
        {
            throw new DavException( HttpServletResponse.SC_NOT_FOUND );
        }
    }

    private void triggerAuditEvent( DavResource member, String action )
        throws DavException
    {
        String path = logicalResource + "/" + member.getDisplayName();

        ArchivaDavResource resource = checkDavResourceIsArchivaDavResource( member );
        AuditEvent auditEvent = new AuditEvent( locator.getRepositoryId(), resource.principal, path, action );
        auditEvent.setRemoteIP( resource.remoteAddr );

        for ( AuditListener listener : auditListeners )
        {
            listener.auditEvent( auditEvent );
        }
    }

    @Override
    public void move( DavResource destination )
        throws DavException
    {
        if ( !exists() )
        {
            throw new DavException( HttpServletResponse.SC_NOT_FOUND, "Resource to copy does not exist." );
        }

        try
        {
            ArchivaDavResource resource = checkDavResourceIsArchivaDavResource( destination );
            if ( isCollection() )
            {
                this.asset = repositoryStorage.moveAsset( asset, destination.getResourcePath() );
                triggerAuditEvent( remoteAddr, locator.getRepositoryId(), logicalResource, AuditEvent.MOVE_DIRECTORY );
            }
            else
            {
                this.asset = repositoryStorage.moveAsset( asset, destination.getResourcePath() );
                triggerAuditEvent( remoteAddr, locator.getRepositoryId(), logicalResource, AuditEvent.MOVE_FILE );
            }

            log.debug( "{}{}' moved to '{}' (current user '{}')", ( isCollection() ? "Directory '" : "File '" ),
                       asset.getPath(), destination, this.principal );

        }
        catch ( IOException e )
        {
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
        }
    }

    @Override
    public void copy( DavResource destination, boolean shallow )
        throws DavException
    {
        if ( !exists() )
        {
            throw new DavException( HttpServletResponse.SC_NOT_FOUND, "Resource to copy does not exist." );
        }

        if ( shallow && isCollection() )
        {
            throw new DavException( DavServletResponse.SC_FORBIDDEN, "Unable to perform shallow copy for collection" );
        }

        try
        {
            ArchivaDavResource resource = checkDavResourceIsArchivaDavResource( destination );
            if ( isCollection() )
            {
                repositoryStorage.copyAsset( asset, destination.getResourcePath() );

                triggerAuditEvent( remoteAddr, locator.getRepositoryId(), logicalResource, AuditEvent.COPY_DIRECTORY );
            }
            else
            {
                repositoryStorage.copyAsset( asset, destination.getResourcePath() );

                triggerAuditEvent( remoteAddr, locator.getRepositoryId(), logicalResource, AuditEvent.COPY_FILE );
            }

            log.debug( "{}{}' copied to '{}' (current user '{}')", ( isCollection() ? "Directory '" : "File '" ),
                       asset.getPath(), destination, this.principal );

        }
        catch ( IOException e )
        {
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
        }
    }

    @Override
    public boolean isLockable( Type type, Scope scope )
    {
        return Type.WRITE.equals( type ) && Scope.EXCLUSIVE.equals( scope );
    }

    @Override
    public boolean hasLock( Type type, Scope scope )
    {
        return getLock( type, scope ) != null;
    }

    @Override
    public ActiveLock getLock( Type type, Scope scope )
    {
        ActiveLock lock = null;
        if ( exists() && Type.WRITE.equals( type ) && Scope.EXCLUSIVE.equals( scope ) )
        {
            lock = lockManager.getLock( type, scope, this );
        }
        return lock;
    }

    @Override
    public ActiveLock[] getLocks()
    {
        ActiveLock writeLock = getLock( Type.WRITE, Scope.EXCLUSIVE );
        return ( writeLock != null ) ? new ActiveLock[]{ writeLock } : new ActiveLock[0];
    }

    @Override
    public ActiveLock lock( LockInfo lockInfo )
        throws DavException
    {
        ActiveLock lock = null;
        if ( isLockable( lockInfo.getType(), lockInfo.getScope() ) )
        {
            lock = lockManager.createLock( lockInfo, this );
        }
        else
        {
            throw new DavException( DavServletResponse.SC_PRECONDITION_FAILED, "Unsupported lock type or scope." );
        }
        return lock;
    }

    @Override
    public ActiveLock refreshLock( LockInfo lockInfo, String lockToken )
        throws DavException
    {
        if ( !exists() )
        {
            throw new DavException( DavServletResponse.SC_NOT_FOUND );
        }
        ActiveLock lock = getLock( lockInfo.getType(), lockInfo.getScope() );
        if ( lock == null )
        {
            throw new DavException( DavServletResponse.SC_PRECONDITION_FAILED,
                                    "No lock with the given type/scope present on resource " + getResourcePath() );
        }

        lock = lockManager.refreshLock( lockInfo, lockToken, this );

        return lock;
    }

    @Override
    public void unlock( String lockToken )
        throws DavException
    {
        ActiveLock lock = getLock( Type.WRITE, Scope.EXCLUSIVE );
        if ( lock == null )
        {
            throw new DavException( HttpServletResponse.SC_PRECONDITION_FAILED );
        }
        else if ( lock.isLockedByToken( lockToken ) )
        {
            lockManager.releaseLock( lockToken, this );
        }
        else
        {
            throw new DavException( DavServletResponse.SC_LOCKED );
        }
    }

    @Override
    public void addLockManager( LockManager lockManager )
    {
        this.lockManager = lockManager;
    }

    @Override
    public DavResourceFactory getFactory()
    {
        return factory;
    }

    @Override
    public DavSession getSession()
    {
        return session;
    }

    /**
     * Fill the set of properties
     */
    protected DavPropertySet initProperties()
    {
        if ( !exists() )
        {
            properties = new DavPropertySet();
        }

        if ( properties != null )
        {
            return properties;
        }

        DavPropertySet properties = new DavPropertySet();

        // set (or reset) fundamental properties
        if ( getDisplayName() != null )
        {
            properties.add( new DefaultDavProperty<>( DavPropertyName.DISPLAYNAME, getDisplayName() ) );
        }
        if ( isCollection() )
        {
            properties.add( new ResourceType( ResourceType.COLLECTION ) );
            // Windows XP support
            properties.add( new DefaultDavProperty<>( DavPropertyName.ISCOLLECTION, "1" ) );
        }
        else
        {
            properties.add( new ResourceType( ResourceType.DEFAULT_RESOURCE ) );

            // Windows XP support
            properties.add( new DefaultDavProperty<>( DavPropertyName.ISCOLLECTION, "0" ) );
        }

        // Need to get the ISO8601 date for properties
        String modifiedDate = DateTimeFormatter.ISO_INSTANT.format( asset.getModificationTime() );
        properties.add( new DefaultDavProperty<>( DavPropertyName.GETLASTMODIFIED, modifiedDate ) );
        properties.add( new DefaultDavProperty<>( DavPropertyName.CREATIONDATE, modifiedDate ) );

        properties.add( new DefaultDavProperty<>( DavPropertyName.GETCONTENTLENGTH, asset.getSize() ) );

        this.properties = properties;

        return properties;
    }

    private ArchivaDavResource checkDavResourceIsArchivaDavResource( DavResource resource )
        throws DavException
    {
        if ( !( resource instanceof ArchivaDavResource ) )
        {
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                    "DavResource is not instance of ArchivaDavResource" );
        }
        return (ArchivaDavResource) resource;
    }

    private void triggerAuditEvent( String remoteIP, String repositoryId, String resource, String action )
    {
        AuditEvent event = new AuditEvent( repositoryId, principal, resource, action );
        event.setRemoteIP( remoteIP );

        for ( AuditListener listener : auditListeners )
        {
            listener.auditEvent( event );
        }
    }

    /**
    private void queueRepositoryTask( Path localFile )
    {
        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repository.getId() );
        task.setResourceFile( localFile );
        task.setUpdateRelatedArtifacts( false );
        task.setScanAll( false );

        try
        {
            scheduler.queueTask( task );
        }
        catch ( TaskQueueException e )
        {
            log.error( "Unable to queue repository task to execute consumers on resource file ['{}"
                           + "'].", localFile.getFileName() );
        }
    }
     **/
}
