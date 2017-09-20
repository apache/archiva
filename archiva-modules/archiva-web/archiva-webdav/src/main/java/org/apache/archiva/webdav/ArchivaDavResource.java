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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.common.filelock.FileLockException;
import org.apache.archiva.common.filelock.FileLockManager;
import org.apache.archiva.common.filelock.FileLockTimeoutException;
import org.apache.archiva.common.filelock.Lock;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.redback.components.taskqueue.TaskQueueException;
import org.apache.archiva.repository.events.AuditListener;
import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.model.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.model.RepositoryTask;
import org.apache.archiva.webdav.util.IndexWriter;
import org.apache.archiva.webdav.util.MimeTypes;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.*;
import org.apache.jackrabbit.webdav.property.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 */
public class ArchivaDavResource
    implements DavResource
{
    public static final String HIDDEN_PATH_PREFIX = ".";

    private final ArchivaDavResourceLocator locator;

    private final DavResourceFactory factory;

    private final Path localResource;

    private final String logicalResource;

    private DavPropertySet properties = null;

    private LockManager lockManager;

    private final DavSession session;

    private String remoteAddr;

    private final ManagedRepository repository;

    private final MimeTypes mimeTypes;

    private List<AuditListener> auditListeners;

    private String principal;

    public static final String COMPLIANCE_CLASS = "1, 2";

    private final ArchivaTaskScheduler scheduler;

    private final FileLockManager fileLockManager;

    private Logger log = LoggerFactory.getLogger( ArchivaDavResource.class );

    public ArchivaDavResource( String localResource, String logicalResource, ManagedRepository repository,
                               DavSession session, ArchivaDavResourceLocator locator, DavResourceFactory factory,
                               MimeTypes mimeTypes, List<AuditListener> auditListeners,
                               RepositoryArchivaTaskScheduler scheduler, FileLockManager fileLockManager )
    {
        this.localResource = Paths.get( localResource );
        this.logicalResource = logicalResource;
        this.locator = locator;
        this.factory = factory;
        this.session = session;

        // TODO: push into locator as well as moving any references out of the resource factory
        this.repository = repository;

        // TODO: these should be pushed into the repository layer, along with the physical file operations in this class
        this.mimeTypes = mimeTypes;
        this.auditListeners = auditListeners;
        this.scheduler = scheduler;
        this.fileLockManager = fileLockManager;
    }

    public ArchivaDavResource( String localResource, String logicalResource, ManagedRepository repository,
                               String remoteAddr, String principal, DavSession session,
                               ArchivaDavResourceLocator locator, DavResourceFactory factory, MimeTypes mimeTypes,
                               List<AuditListener> auditListeners, RepositoryArchivaTaskScheduler scheduler,
                               FileLockManager fileLockManager )
    {
        this( localResource, logicalResource, repository, session, locator, factory, mimeTypes, auditListeners,
              scheduler, fileLockManager );

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
        return Files.exists(localResource);
    }

    @Override
    public boolean isCollection()
    {
        return Files.isDirectory(localResource);
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

    public Path getLocalResource()
    {
        return localResource;
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
        try
        {
            return Files.getLastModifiedTime(localResource).toMillis();
        }
        catch ( IOException e )
        {
            log.error("Could not get modification time of {}: {}", localResource, e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public void spool( OutputContext outputContext )
        throws IOException
    {
        if ( !isCollection() )
        {
            outputContext.setContentLength( Files.size( localResource ) );
            outputContext.setContentType( mimeTypes.getMimeType( localResource.getFileName().toString() ) );
        }

        try
        {
            if ( !isCollection() && outputContext.hasStream() )
            {
                Lock lock = fileLockManager.readFileLock( localResource );
                try (InputStream is = Files.newInputStream( lock.getFile()))
                {
                    IOUtils.copy( is, outputContext.getOutputStream() );
                }
            }
            else if ( outputContext.hasStream() )
            {
                IndexWriter writer = new IndexWriter( this, localResource, logicalResource );
                writer.write( outputContext );
            }
        }
        catch ( FileLockException e )
        {
            throw new IOException( e.getMessage(), e );
        }
        catch ( FileLockTimeoutException e )
        {
            throw new IOException( e.getMessage(), e );
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
        Path localFile = localResource.resolve( resource.getDisplayName() );
        boolean exists = Files.exists(localFile);

        if ( isCollection() && inputContext.hasStream() ) // New File
        {
            try (OutputStream stream = Files.newOutputStream( localFile ))
            {
                IOUtils.copy( inputContext.getInputStream(), stream );
            }
            catch ( IOException e )
            {
                throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
            }

            // TODO: a bad deployment shouldn't delete an existing file - do we need to write to a temporary location first?
            long expectedContentLength = inputContext.getContentLength();
            long actualContentLength = 0;
            try
            {
                actualContentLength = Files.size(localFile);
            }
            catch ( IOException e )
            {
                log.error( "Could not get length of file {}: {}", localFile, e.getMessage(), e );
            }
            // length of -1 is given for a chunked request or unknown length, in which case we accept what was uploaded
            if ( expectedContentLength >= 0 && expectedContentLength != actualContentLength )
            {
                String msg = "Content Header length was " + expectedContentLength + " but was " + actualContentLength;
                log.debug( "Upload failed: {}", msg );

                org.apache.archiva.common.utils.FileUtils.deleteQuietly( localFile );
                throw new DavException( HttpServletResponse.SC_BAD_REQUEST, msg );
            }

            queueRepositoryTask( localFile );

            log.debug( "File '{}{}(current user '{}')", resource.getDisplayName(),
                       ( exists ? "' modified " : "' created " ), this.principal );

            triggerAuditEvent( resource, exists ? AuditEvent.MODIFY_FILE : AuditEvent.CREATE_FILE );
        }
        else if ( !inputContext.hasStream() && isCollection() ) // New directory
        {
            try
            {
                Files.createDirectories( localFile );
            }
            catch ( IOException e )
            {
                log.error("Could not create directory {}: {}", localFile, e.getMessage(), e);
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

    @Override
    public DavResourceIterator getMembers()
    {
        List<DavResource> list = new ArrayList<>();
        if ( exists() && isCollection() )
        {
            try ( Stream<Path> stream = Files.list(localResource))
            {
                stream.forEach ( p ->
                {
                    String item = p.toString();
                    try
                    {
                        if ( !item.startsWith( HIDDEN_PATH_PREFIX ) )
                        {
                            String path = locator.getResourcePath( ) + '/' + item;
                            DavResourceLocator resourceLocator =
                                locator.getFactory( ).createResourceLocator( locator.getPrefix( ), path );
                            DavResource resource = factory.createResource( resourceLocator, session );

                            if ( resource != null )
                            {
                                list.add( resource );
                            }
                            log.debug( "Resource '{}' retrieved by '{}'", item, this.principal );
                        }
                    }
                    catch ( DavException e )
                    {
                        // Should not occur
                    }
                });
            } catch (IOException e) {
                log.error("Error while listing {}", localResource);
            }
        }
        return new DavResourceIteratorImpl( list );
    }

    @Override
    public void removeMember( DavResource member )
        throws DavException
    {
        Path resource = checkDavResourceIsArchivaDavResource( member ).getLocalResource();

        if ( Files.exists(resource) )
        {
            try
            {
                if ( Files.isDirectory(resource) )
                {
                    org.apache.archiva.common.utils.FileUtils.deleteDirectory( resource );
                    triggerAuditEvent( member, AuditEvent.REMOVE_DIR );
                }
                else
                {
                    Files.deleteIfExists( resource );
                    triggerAuditEvent( member, AuditEvent.REMOVE_FILE );
                }

                log.debug( "{}{}' removed (current user '{}')", ( Files.isDirectory(resource) ? "Directory '" : "File '" ),
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
                FileUtils.moveDirectory( getLocalResource().toFile(), resource.getLocalResource().toFile() );

                triggerAuditEvent( remoteAddr, locator.getRepositoryId(), logicalResource, AuditEvent.MOVE_DIRECTORY );
            }
            else
            {
                FileUtils.moveFile( getLocalResource().toFile(), resource.getLocalResource().toFile() );

                triggerAuditEvent( remoteAddr, locator.getRepositoryId(), logicalResource, AuditEvent.MOVE_FILE );
            }

            log.debug( "{}{}' moved to '{}' (current user '{}')", ( isCollection() ? "Directory '" : "File '" ),
                       getLocalResource().getFileName(), destination, this.principal );

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
                FileUtils.copyDirectory( getLocalResource().toFile(), resource.getLocalResource().toFile() );

                triggerAuditEvent( remoteAddr, locator.getRepositoryId(), logicalResource, AuditEvent.COPY_DIRECTORY );
            }
            else
            {
                FileUtils.copyFile( getLocalResource().toFile(), resource.getLocalResource().toFile() );

                triggerAuditEvent( remoteAddr, locator.getRepositoryId(), logicalResource, AuditEvent.COPY_FILE );
            }

            log.debug( "{}{}' copied to '{}' (current user '{}')", ( isCollection() ? "Directory '" : "File '" ),
                       getLocalResource().getFileName(), destination, this.principal );

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
            properties.add( new DefaultDavProperty( DavPropertyName.DISPLAYNAME, getDisplayName() ) );
        }
        if ( isCollection() )
        {
            properties.add( new ResourceType( ResourceType.COLLECTION ) );
            // Windows XP support
            properties.add( new DefaultDavProperty( DavPropertyName.ISCOLLECTION, "1" ) );
        }
        else
        {
            properties.add( new ResourceType( ResourceType.DEFAULT_RESOURCE ) );

            // Windows XP support
            properties.add( new DefaultDavProperty( DavPropertyName.ISCOLLECTION, "0" ) );
        }

        // Need to get the ISO8601 date for properties
        DateTime dt = null;
        try
        {
            dt = new DateTime( Files.getLastModifiedTime( localResource ).toMillis() );
        }
        catch ( IOException e )
        {
            log.error("Could not get modification time of {}: {}", localResource, e.getMessage(), e);
            dt = new DateTime();
        }
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String modifiedDate = fmt.print( dt );

        properties.add( new DefaultDavProperty( DavPropertyName.GETLASTMODIFIED, modifiedDate ) );

        properties.add( new DefaultDavProperty( DavPropertyName.CREATIONDATE, modifiedDate ) );

        try
        {
            properties.add( new DefaultDavProperty( DavPropertyName.GETCONTENTLENGTH, Files.size(localResource) ) );
        }
        catch ( IOException e )
        {
            log.error("Could not get file size of {}: {}", localResource, e.getMessage(), e);
            properties.add( new DefaultDavProperty( DavPropertyName.GETCONTENTLENGTH, 0 ) );
        }

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
}
