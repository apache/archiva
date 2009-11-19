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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import org.apache.archiva.scheduler.ArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.RepositoryTask;
import org.apache.commons.io.FileUtils;
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
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.repository.audit.AuditEvent;
import org.apache.maven.archiva.repository.audit.AuditListener;
import org.apache.maven.archiva.webdav.util.IndexWriter;
import org.apache.maven.archiva.webdav.util.MimeTypes;
import org.codehaus.plexus.taskqueue.TaskQueueException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class ArchivaDavResource
    implements DavResource
{
    public static final String HIDDEN_PATH_PREFIX = ".";

    private final ArchivaDavResourceLocator locator;

    private final DavResourceFactory factory;

    private final File localResource;

    private final String logicalResource;

    private DavPropertySet properties = null;

    private LockManager lockManager;
    
    private final DavSession session;
    
    private String remoteAddr;

    private final ManagedRepositoryConfiguration repository;

    private final MimeTypes mimeTypes;

    private List<AuditListener> auditListeners;

    private String principal;
    
    public static final String COMPLIANCE_CLASS = "1, 2";
    
    private ArchivaTaskScheduler scheduler;
    
    private Logger log = LoggerFactory.getLogger( ArchivaDavResource.class );

    public ArchivaDavResource( String localResource, String logicalResource, ManagedRepositoryConfiguration repository,
                               DavSession session, ArchivaDavResourceLocator locator, DavResourceFactory factory,
                               MimeTypes mimeTypes, List<AuditListener> auditListeners,
                               RepositoryArchivaTaskScheduler scheduler )
    {
        this.localResource = new File( localResource ); 
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
    }

    public ArchivaDavResource( String localResource, String logicalResource, ManagedRepositoryConfiguration repository,
                               String remoteAddr, String principal, DavSession session, ArchivaDavResourceLocator locator,
                               DavResourceFactory factory, MimeTypes mimeTypes, List<AuditListener> auditListeners,
                               RepositoryArchivaTaskScheduler scheduler )
    {
        this( localResource, logicalResource, repository, session, locator, factory, mimeTypes, auditListeners,
              scheduler );

        this.remoteAddr = remoteAddr;
        this.principal = principal;
    }

    public String getComplianceClass()
    {
        return COMPLIANCE_CLASS;
    }

    public String getSupportedMethods()
    {
        return METHODS;
    }

    public boolean exists()
    {
        return localResource.exists();
    }

    public boolean isCollection()
    {
        return localResource.isDirectory();
    }

    public String getDisplayName()
    {
        String resPath = getResourcePath();
        return ( resPath != null ) ? Text.getName( resPath ) : resPath;
    }

    public DavResourceLocator getLocator()
    {
        return locator;
    }

    public File getLocalResource()
    {
        return localResource;
    }

    public String getResourcePath()
    {
        return locator.getResourcePath();
    }

    public String getHref()
    {
        return locator.getHref( isCollection() );
    }

    public long getModificationTime()
    {
        return localResource.lastModified();
    }

    public void spool( OutputContext outputContext )
        throws IOException
    {
        if ( !isCollection())
        {
            outputContext.setContentLength( localResource.length() );
            outputContext.setContentType( mimeTypes.getMimeType( localResource.getName() ) );
        }
        
        if ( !isCollection() && outputContext.hasStream() )
        {
            FileInputStream is = null;
            try
            {
                // Write content to stream
                is = new FileInputStream( localResource );
                IOUtils.copy( is, outputContext.getOutputStream() );
            }
            finally
            {
                IOUtils.closeQuietly( is );
            }
        }
        else if (outputContext.hasStream())
        {
            IndexWriter writer = new IndexWriter( this, localResource, logicalResource );
            writer.write( outputContext );
        }
    }

    public DavPropertyName[] getPropertyNames()
    {
        return getProperties().getPropertyNames();
    }

    public DavProperty getProperty( DavPropertyName name )
    {
        return getProperties().get( name );
    }

    public DavPropertySet getProperties()
    {
        return initProperties();
    }

    public void setProperty( DavProperty property )
        throws DavException
    {
    }

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
    public MultiStatusResponse alterProperties( List changeList )
        throws DavException
    {
        return null;
    }

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
            DavResourceLocator parentloc = locator.getFactory().createResourceLocator( locator.getPrefix(), parentPath );
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

    public void addMember( DavResource resource, InputContext inputContext )
        throws DavException
    {
        File localFile = new File( localResource, resource.getDisplayName() );
        boolean exists = localFile.exists();

        if ( isCollection() && inputContext.hasStream() ) // New File
        {
            FileOutputStream stream = null;
            try
            {
                stream = new FileOutputStream( localFile );
                IOUtils.copy( inputContext.getInputStream(), stream );
            }
            catch ( IOException e )
            {
                throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
            }
            finally
            {
                IOUtils.closeQuietly( stream );
            }
            
            // TODO: a bad deployment shouldn't delete an existing file - do we need to write to a temporary location first?
            if ( inputContext.getContentLength() != localFile.length() )
            {
                FileUtils.deleteQuietly( localFile );
                
                throw new DavException( HttpServletResponse.SC_BAD_REQUEST, "Content Header length was " +
                    inputContext.getContentLength() + " but was " + localFile.length() );
            }
            
            queueRepositoryTask( localFile );           
            
            log.debug( "File '" + resource.getDisplayName() + ( exists ? "' modified " : "' created ") + "(current user '" + this.principal + "')" );
            
            triggerAuditEvent( resource, exists ? AuditEvent.MODIFY_FILE : AuditEvent.CREATE_FILE );
        }
        else if ( !inputContext.hasStream() && isCollection() ) // New directory
        {
            localFile.mkdir();
            
            log.debug( "Directory '" + resource.getDisplayName() + "' (current user '" + this.principal + "')" );
            
            triggerAuditEvent( resource, AuditEvent.CREATE_DIR );
        }
        else
        {            
            throw new DavException( HttpServletResponse.SC_BAD_REQUEST, "Could not write member " +
                resource.getResourcePath() + " at " + getResourcePath() );
        }
    }

    public DavResourceIterator getMembers()
    {
        List<DavResource> list = new ArrayList<DavResource>();
        if ( exists() && isCollection() )
        {
            for ( String item : localResource.list() )
            {
                try
                {
                    if ( !item.startsWith( HIDDEN_PATH_PREFIX ) )
                    {
                        String path = locator.getResourcePath() + '/' + item;
                        DavResourceLocator resourceLocator =
                            locator.getFactory().createResourceLocator( locator.getPrefix(), path );
                        DavResource resource = factory.createResource( resourceLocator, session );
                        
                        if ( resource != null )
                        {
                            list.add( resource );
                        }
                        log.debug( "Resource '" + item + "' retrieved by '" + this.principal + "'" );
                    }
                }
                catch ( DavException e )
                {
                    // Should not occur
                }
            }
        }
        return new DavResourceIteratorImpl( list );
    }

    public void removeMember( DavResource member )
        throws DavException
    {
        File resource = checkDavResourceIsArchivaDavResource( member ).getLocalResource();
        
        if ( resource.exists() )
        {
            try
            {
                if ( resource.isDirectory() )
                {
                    FileUtils.deleteDirectory( resource );

                    triggerAuditEvent( member, AuditEvent.REMOVE_DIR );
                }
                else
                {
                    if ( !resource.delete() )
                    {
                        throw new IOException( "Could not remove file" );
                    }

                    triggerAuditEvent( member, AuditEvent.REMOVE_FILE );
                }
                log.debug( ( resource.isDirectory() ? "Directory '" : "File '" ) + member.getDisplayName() + "' removed (current user '" + this.principal + "')" );
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

    private void triggerAuditEvent( DavResource member, String event ) throws DavException
    {
        String path = logicalResource + "/" + member.getDisplayName();
        
        triggerAuditEvent( checkDavResourceIsArchivaDavResource( member ).remoteAddr, locator.getRepositoryId(), path,
                           event );
    }

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
                FileUtils.moveDirectory( getLocalResource(), resource.getLocalResource() );

                triggerAuditEvent( remoteAddr, locator.getRepositoryId(), logicalResource, AuditEvent.MOVE_DIRECTORY );
            }
            else
            {
                FileUtils.moveFile( getLocalResource(), resource.getLocalResource() );

                triggerAuditEvent( remoteAddr, locator.getRepositoryId(), logicalResource, AuditEvent.MOVE_FILE );
            }
            
            log.debug( ( isCollection() ? "Directory '" : "File '" ) + getLocalResource().getName() + "' moved to '" +
            		   destination + "' (current user '" + this.principal + "')" );
        }
        catch ( IOException e )
        {
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
        }
    }

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
                FileUtils.copyDirectory( getLocalResource(), resource.getLocalResource() );

                triggerAuditEvent( remoteAddr, locator.getRepositoryId(), logicalResource, AuditEvent.COPY_DIRECTORY );
            }
            else
            {
                FileUtils.copyFile( getLocalResource(), resource.getLocalResource() );

                triggerAuditEvent( remoteAddr, locator.getRepositoryId(), logicalResource, AuditEvent.COPY_FILE );
            }
            log.debug( ( isCollection() ? "Directory '" : "File '" ) + getLocalResource().getName() + "' copied to '" +
            		   destination + "' (current user '" + this.principal + "')" );
        }
        catch ( IOException e )
        {
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
        }
    }

    public boolean isLockable( Type type, Scope scope )
    {
        return Type.WRITE.equals(type) && Scope.EXCLUSIVE.equals(scope);
    }

    public boolean hasLock( Type type, Scope scope )
    {
        return getLock(type, scope) != null;
    }

    public ActiveLock getLock( Type type, Scope scope )
    {
        ActiveLock lock = null;
        if (exists() && Type.WRITE.equals(type) && Scope.EXCLUSIVE.equals(scope)) 
        {
            lock = lockManager.getLock(type, scope, this);
        }
        return lock;
    }

    public ActiveLock[] getLocks()
    {
        ActiveLock writeLock = getLock(Type.WRITE, Scope.EXCLUSIVE);
        return (writeLock != null) ? new ActiveLock[]{writeLock} : new ActiveLock[0];
    }

    public ActiveLock lock( LockInfo lockInfo )
        throws DavException
    {
        ActiveLock lock = null;
        if (isLockable(lockInfo.getType(), lockInfo.getScope())) 
        {
            lock = lockManager.createLock(lockInfo, this);
        }
        else 
        {
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "Unsupported lock type or scope.");
        }
        return lock;
    }

    public ActiveLock refreshLock( LockInfo lockInfo, String lockToken )
        throws DavException
    {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        ActiveLock lock = getLock(lockInfo.getType(), lockInfo.getScope());
        if (lock == null) {
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED, "No lock with the given type/scope present on resource " + getResourcePath());
        }

        lock = lockManager.refreshLock(lockInfo, lockToken, this);

        return lock;
    }

    public void unlock( String lockToken )
        throws DavException
    {
        ActiveLock lock = getLock(Type.WRITE, Scope.EXCLUSIVE);
        if (lock == null)
        {
            throw new DavException(HttpServletResponse.SC_PRECONDITION_FAILED);
        }
        else if (lock.isLockedByToken(lockToken))
        {
            lockManager.releaseLock(lockToken, this);
        }
        else
        {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
    }

    public void addLockManager( LockManager lockManager )
    {
        this.lockManager = lockManager;
    }

    public DavResourceFactory getFactory()
    {
        return factory;
    }

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
        DateTime dt = new DateTime( localResource.lastModified() );
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String modifiedDate = fmt.print( dt );

        properties.add( new DefaultDavProperty( DavPropertyName.GETLASTMODIFIED, modifiedDate ) );

        properties.add( new DefaultDavProperty( DavPropertyName.CREATIONDATE, modifiedDate ) );

        properties.add( new DefaultDavProperty( DavPropertyName.GETCONTENTLENGTH, localResource.length() ) );
        
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
    
    private void queueRepositoryTask( File localFile )
    {        
        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repository.getId() );
        task.setResourceFile( localFile );
        task.setUpdateRelatedArtifacts( false );
        task.setScanAll( true );

        try
        {
            scheduler.queueTask( task );
        }
        catch ( TaskQueueException e )
        {
            log.error( "Unable to queue repository task to execute consumers on resource file ['" +
                localFile.getName() + "']." );
        }
    }
}
