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

import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.DavResourceLocator;
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
import org.apache.archiva.webdav.util.IndexWriter;
import org.apache.archiva.webdav.util.MimeTypes;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DavResource for virtual repositories
 */
public class ArchivaVirtualDavResource
    implements DavResource
{
    private static final String COMPLIANCE_CLASS = "1";

    private ArchivaDavResourceLocator locator;

    private DavResourceFactory factory;

    private String logicalResource;

    private DavPropertySet properties;

    private boolean propsInitialized = false;

    private static final String METHODS = "OPTIONS, GET, HEAD, POST, TRACE, PROPFIND, PROPPATCH, MKCOL";

    private final List<File> localResources;

    public ArchivaVirtualDavResource( List<File> localResources, String logicalResource, MimeTypes mimeTypes,
                                      ArchivaDavResourceLocator locator, DavResourceFactory factory )
    {
        this.localResources = localResources;
        this.logicalResource = logicalResource;
        this.locator = locator;
        this.factory = factory;
        this.properties = new DavPropertySet();
    }

    @Override
    public void spool( OutputContext outputContext )
        throws IOException
    {
        if ( outputContext.hasStream() )
        {
            Collections.sort( localResources );
            List<File> localResourceFiles = new ArrayList<>();

            for ( File resourceFile : localResources )
            {
                if ( resourceFile.exists() )
                {
                    localResourceFiles.add( resourceFile );
                }
            }

            IndexWriter writer = new IndexWriter( this, localResourceFiles, logicalResource );
            writer.write( outputContext );
        }
    }

    @Override
    public void addLockManager( LockManager arg0 )
    {

    }

    @Override
    public void addMember( DavResource arg0, InputContext arg1 )
        throws DavException
    {

    }

    @SuppressWarnings( "unchecked" )
    @Override
    public MultiStatusResponse alterProperties( List arg0 )
        throws DavException
    {
        return null;
    }

    public MultiStatusResponse alterProperties( DavPropertySet arg0, DavPropertyNameSet arg1 )
        throws DavException
    {
        return null;
    }

    @Override
    public void copy( DavResource arg0, boolean arg1 )
        throws DavException
    {

    }

    @Override
    public boolean exists()
    {
        // localResources are already filtered (all files in the list are already existing)
        return true;
    }

    @Override
    public ActiveLock getLock( Type arg0, Scope arg1 )
    {
        return null;
    }

    @Override
    public ActiveLock[] getLocks()
    {
        return null;
    }

    @Override
    public DavResourceIterator getMembers()
    {
        return null;
    }

    @Override
    public String getSupportedMethods()
    {
        return METHODS;
    }

    @Override
    public long getModificationTime()
    {
        return 0;
    }

    @Override
    public boolean hasLock( Type arg0, Scope arg1 )
    {
        return false;
    }

    @Override
    public boolean isCollection()
    {
        return true;
    }

    @Override
    public boolean isLockable( Type arg0, Scope arg1 )
    {
        return false;
    }

    @Override
    public ActiveLock lock( LockInfo arg0 )
        throws DavException
    {
        return null;
    }

    @Override
    public void move( DavResource arg0 )
        throws DavException
    {

    }

    @Override
    public ActiveLock refreshLock( LockInfo arg0, String arg1 )
        throws DavException
    {
        return null;
    }

    @Override
    public void removeMember( DavResource arg0 )
        throws DavException
    {

    }

    @Override
    public void unlock( String arg0 )
        throws DavException
    {

    }

    @Override
    public String getComplianceClass()
    {
        return COMPLIANCE_CLASS;
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
    public DavResourceFactory getFactory()
    {
        return factory;
    }

    @Override
    public String getDisplayName()
    {
        String resPath = getResourcePath();

        return ( resPath != null ) ? Text.getName( resPath ) : resPath;
    }

    @Override
    public DavSession getSession()
    {
        return null;
    }

    @Override
    public DavPropertyName[] getPropertyNames()
    {
        return getProperties().getPropertyNames();
    }

    @Override
    public DavProperty getProperty( DavPropertyName name )
    {
        initProperties();
        return properties.get( name );
    }

    @Override
    public DavPropertySet getProperties()
    {
        initProperties();
        return properties;
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
                // go back to ArchivaDavResourceFactory!
                parent = factory.createResource( parentloc, null );
            }
            catch ( DavException e )
            {
                // should not occur
            }
        }
        return parent;
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
        DateTime dt = new DateTime( 0 );
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String modifiedDate = fmt.print( dt );

        properties.add( new DefaultDavProperty( DavPropertyName.GETLASTMODIFIED, modifiedDate ) );

        properties.add( new DefaultDavProperty( DavPropertyName.CREATIONDATE, modifiedDate ) );

        properties.add( new DefaultDavProperty( DavPropertyName.GETCONTENTLENGTH, 0 ) );

        propsInitialized = true;
    }

    public String getLogicalResource()
    {
        return logicalResource;
    }

    public void setLogicalResource( String logicalResource )
    {
        this.logicalResource = logicalResource;
    }
}
