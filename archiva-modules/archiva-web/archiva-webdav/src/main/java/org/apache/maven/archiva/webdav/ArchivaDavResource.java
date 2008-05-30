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

import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.property.*;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.OutputContext;
import org.apache.jackrabbit.webdav.lock.*;
import org.apache.jackrabbit.util.Text;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.webdav.util.MimeTypes;
import org.apache.maven.archiva.webdav.util.IndexWriter;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.ArrayList;
import java.io.*;

/**
 * @author <a href="mailto:james@atlassian.com">James William Dumay</a> Portions from the Apache Jackrabbit Project
 */
public class ArchivaDavResource
    implements DavResource
{
    public static final String HIDDEN_PATH_PREFIX = ".";

    private final MimeTypes mimeTypes;

    private final ArchivaDavResourceLocator locator;

    private final DavResourceFactory factory;

    private final File localResource;

    private final String logicalResource;

    private DavPropertySet properties;

    private boolean propsInitialized = false;
    
    private LockManager lockManager;
    
    private final DavSession session;

    public ArchivaDavResource( String localResource, 
                               String logicalResource,
                               MimeTypes mimeTypes,
                               DavSession session,
                               ArchivaDavResourceLocator locator, 
                               DavResourceFactory factory )
    {
        this.mimeTypes = mimeTypes;
        this.localResource = new File( localResource );
        this.logicalResource = logicalResource;
        this.locator = locator;
        this.factory = factory;
        this.session = session;
        this.properties = new DavPropertySet();
    }

    public String getContentType()
    {
        return mimeTypes.getMimeType( localResource.getName() );
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
        initProperties();
        return localResource.lastModified();
    }

    public long getContentLength()
    {
        initProperties();
        return localResource.length();
    }

    public void spool( OutputContext outputContext )
        throws IOException
    {
        if ( !isCollection() )
        {
            FileInputStream is = null;
            try
            {
                outputContext.setContentLength( getContentLength() );
                outputContext.setContentType( getContentType() );

                // Write content to stream
                is = new FileInputStream( localResource );
                IOUtils.copy( is, outputContext.getOutputStream() );
            }
            finally
            {
                IOUtils.closeQuietly( is );
            }
        }
        else
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
        initProperties();
        return properties.get( name );
    }

    public DavPropertySet getProperties()
    {
        initProperties();
        return properties;
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
        if ( isCollection() && inputContext.hasStream() ) // New File
        {
            boolean deleteFile = false;
            FileOutputStream stream = null;
            try
            {
                stream = new FileOutputStream( localFile );
                IOUtils.copy( inputContext.getInputStream(), stream );
                if ( inputContext.getContentLength() != localFile.length() )
                {
                    deleteFile = true;
                    throw new DavException( HttpServletResponse.SC_BAD_REQUEST, "Content Header length was " +
                        inputContext.getContentLength() + " but was " + localFile.length() );
                }
            }
            catch ( IOException e )
            {
                throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
            }
            finally
            {
                IOUtils.closeQuietly( stream );
                if ( deleteFile )
                {
                    FileUtils.deleteQuietly( localFile );
                }
            }
        }
        else if ( !inputContext.hasStream() && isCollection() ) // New directory
        {
            localFile.mkdir();
        }
        else
        {
            throw new DavException( HttpServletResponse.SC_BAD_REQUEST, "Could not write member " +
                resource.getResourcePath() + " at " + getResourcePath() );
        }
    }

    public DavResourceIterator getMembers()
    {
        ArrayList list = new ArrayList();
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
                            list.add( resource );
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

        if ( !resource.exists() )
        {
            throw new DavException( HttpServletResponse.SC_NOT_FOUND, member.getResourcePath() );
        }

        boolean suceeded = false;

        if ( resource.isDirectory() )
        {
            try
            {
                FileUtils.deleteDirectory( resource );
                suceeded = true;
            }
            catch ( IOException e )
            {
                throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e );
            }
        }

        if ( !suceeded && resource.isFile() )
        {
            suceeded = resource.delete();
        }

        if ( !suceeded )
        {
            throw new DavException( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not delete resource " +
                member.getResourcePath() );
        }
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
            }
            else
            {
                FileUtils.moveFile( getLocalResource(), resource.getLocalResource() );
            }
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
            }
            else
            {
                FileUtils.copyFile( getLocalResource(), resource.getLocalResource() );
            }
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
    protected void initProperties()
    {
        if ( !exists() || propsInitialized )
        {
            return;
        }

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

        propsInitialized = true;
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
}
