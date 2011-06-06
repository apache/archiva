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

import junit.framework.TestCase;
import org.apache.archiva.audit.AuditListener;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.SimpleLockManager;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.webdav.util.MimeTypes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.File;
import java.util.Collections;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class DavResourceTest
    extends TestCase
{
    private DavSession session;

    @Inject
    private MimeTypes mimeTypes;

    private ArchivaDavResourceLocator resourceLocator;

    private DavResourceFactory resourceFactory;

    private File baseDir;

    private final String REPOPATH = "myresource.jar";

    private File myResource;

    private DavResource resource;

    private LockManager lockManager;

    private ManagedRepositoryConfiguration repository = new ManagedRepositoryConfiguration();
    
    @Override
    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();
        session = new ArchivaDavSession();
        baseDir = new File( "target/DavResourceTest" );
        baseDir.mkdirs();
        myResource = new File( baseDir, "myresource.jar" );
        assertTrue( "Could not create " + myResource.getAbsolutePath(), myResource.createNewFile() );
        resourceFactory = new RootContextDavResourceFactory();
        
        resourceLocator =
            (ArchivaDavResourceLocator) new ArchivaDavLocatorFactory().createResourceLocator( "/", REPOPATH );        
        resource = getDavResource( resourceLocator.getHref( false ), myResource );
        lockManager = new SimpleLockManager();
        resource.addLockManager( lockManager );        
    }

    @After
    public void tearDown()
        throws Exception
    {
        super.tearDown();
        FileUtils.deleteDirectory( baseDir );
    }

    private DavResource getDavResource( String logicalPath, File file )
    {
        return new ArchivaDavResource( file.getAbsolutePath(), logicalPath, repository, session, resourceLocator,
                                       resourceFactory, mimeTypes, Collections.<AuditListener> emptyList(), null );
    }

    @Test
    public void testDeleteNonExistantResourceShould404()
        throws Exception
    {
        File dir = new File( baseDir, "testdir" );
        try
        {
            DavResource directoryResource = getDavResource( "/testdir", dir );
            directoryResource.getCollection().removeMember( directoryResource );
            fail( "Did not throw DavException" );
        }
        catch ( DavException e )
        {
            assertEquals( DavServletResponse.SC_NOT_FOUND, e.getErrorCode() );
        }
    }

    @Test
    public void testDeleteCollection()
        throws Exception
    {
        File dir = new File( baseDir, "testdir" );
        try
        {
            assertTrue( dir.mkdir() );
            DavResource directoryResource = getDavResource( "/testdir", dir );
            directoryResource.getCollection().removeMember( directoryResource );
            assertFalse( dir.exists() );
        }
        finally
        {
            FileUtils.deleteDirectory( dir );
        }
    }

    @Test
    public void testDeleteResource()
        throws Exception
    {
        assertTrue( myResource.exists() );
        resource.getCollection().removeMember( resource );
        assertFalse( myResource.exists() );
    }

    @Test
    public void testIsLockable()
    {
        assertTrue( resource.isLockable( Type.WRITE, Scope.EXCLUSIVE ) );
        assertFalse( resource.isLockable( Type.WRITE, Scope.SHARED ) );
    }

    @Test
    public void testLock()
        throws Exception
    {
        assertEquals( 0, resource.getLocks().length );

        LockInfo info = new LockInfo( Scope.EXCLUSIVE, Type.WRITE, "/", 0, false );
        lockManager.createLock( info, resource );

        assertEquals( 1, resource.getLocks().length );
    }

    @Test
    public void testLockIfResourceUnlockable()
        throws Exception
    {
        assertEquals( 0, resource.getLocks().length );

        LockInfo info = new LockInfo( Scope.SHARED, Type.WRITE, "/", 0, false );
        try
        {
            lockManager.createLock( info, resource );
            fail( "Did not throw dav exception" );
        }
        catch ( Exception e )
        {
            // Simple lock manager will die
        }
        assertEquals( 0, resource.getLocks().length );
    }

    @Test
    public void testGetLock()
        throws Exception
    {
        LockInfo info = new LockInfo( Scope.EXCLUSIVE, Type.WRITE, "/", 0, false );
        lockManager.createLock( info, resource );

        assertEquals( 1, resource.getLocks().length );

        // Lock should exist
        assertNotNull( resource.getLock( Type.WRITE, Scope.EXCLUSIVE ) );

        // Lock should not exist
        assertNull( resource.getLock( Type.WRITE, Scope.SHARED ) );
    }

    @Test
    public void testRefreshLockThrowsExceptionIfNoLockIsPresent()
        throws Exception
    {
        LockInfo info = new LockInfo( Scope.EXCLUSIVE, Type.WRITE, "/", 0, false );

        assertEquals( 0, resource.getLocks().length );

        try
        {
            lockManager.refreshLock( info, "notoken", resource );
            fail( "Did not throw dav exception" );
        }
        catch ( DavException e )
        {
            assertEquals( DavServletResponse.SC_PRECONDITION_FAILED, e.getErrorCode() );
        }

        assertEquals( 0, resource.getLocks().length );
    }

    @Test
    public void testRefreshLock()
        throws Exception
    {
        LockInfo info = new LockInfo( Scope.EXCLUSIVE, Type.WRITE, "/", 0, false );

        assertEquals( 0, resource.getLocks().length );

        lockManager.createLock( info, resource );

        assertEquals( 1, resource.getLocks().length );

        ActiveLock lock = resource.getLocks()[0];

        lockManager.refreshLock( info, lock.getToken(), resource );

        assertEquals( 1, resource.getLocks().length );
    }

    @Test
    public void testUnlock()
        throws Exception
    {
        LockInfo info = new LockInfo( Scope.EXCLUSIVE, Type.WRITE, "/", 0, false );

        assertEquals( 0, resource.getLocks().length );

        lockManager.createLock( info, resource );

        assertEquals( 1, resource.getLocks().length );

        ActiveLock lock = resource.getLocks()[0];

        lockManager.releaseLock( lock.getToken(), resource );

        assertEquals( 0, resource.getLocks().length );
    }

    @Test
    public void testUnlockThrowsDavExceptionIfNotLocked()
        throws Exception
    {
        LockInfo info = new LockInfo( Scope.EXCLUSIVE, Type.WRITE, "/", 0, false );

        assertEquals( 0, resource.getLocks().length );

        lockManager.createLock( info, resource );

        assertEquals( 1, resource.getLocks().length );

        try
        {
            lockManager.releaseLock( "BLAH", resource );
            fail( "Did not throw DavException" );
        }
        catch ( DavException e )
        {
            assertEquals( DavServletResponse.SC_LOCKED, e.getErrorCode() );
        }

        assertEquals( 1, resource.getLocks().length );
    }

    @Test
    public void testUnlockThrowsDavExceptionIfResourceNotLocked()
        throws Exception
    {
        assertEquals( 0, resource.getLocks().length );

        try
        {
            lockManager.releaseLock( "BLAH", resource );
            fail( "Did not throw DavException" );
        }
        catch ( DavException e )
        {
            assertEquals( DavServletResponse.SC_PRECONDITION_FAILED, e.getErrorCode() );
        }

        assertEquals( 0, resource.getLocks().length );
    }

    private class RootContextDavResourceFactory
        implements DavResourceFactory
    {
        public DavResource createResource( DavResourceLocator locator, DavServletRequest request,
                                           DavServletResponse response )
            throws DavException
        {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

        public DavResource createResource( DavResourceLocator locator, DavSession session )
            throws DavException
        {
            return new ArchivaDavResource( baseDir.getAbsolutePath(), "/", repository, session, resourceLocator,
                                           resourceFactory, mimeTypes, Collections.<AuditListener> emptyList(),
                                           null );
        }
    }
}
