package org.apache.archiva.repository.storage.mock;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.repository.storage.AssetType;
import org.apache.archiva.repository.storage.RepositoryStorage;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.repository.storage.util.StorageUtil;
import org.apache.commons.lang3.StringUtils;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.mockito.Matchers.any;

public class MockAsset implements StorageAsset
{
    private MockAsset parent;
    private String path;
    private String name;
    private LinkedHashMap<String, MockAsset> children = new LinkedHashMap<>( );
    private boolean container = false;
    private RepositoryStorage storage;
    private boolean exists = false;


    private boolean throwException;

    public MockAsset( String name ) {
        this.parent = null;
        this.name = name;
        this.path = "/";
    }

    public MockAsset( MockAsset parent, String name ) {
        if (parent!=null && "".equals(name)) {
            throw new RuntimeException( "Bad asset creation with empty name and parent" );
        }
        this.parent = parent;
        this.path = getPath( parent, name );
        this.name = name;
        this.storage = parent.getStorage( );
        parent.registerChild( this );
    }

    private String getPath(MockAsset parent, String name) {

        if (parent.hasParent() && !parent.getPath(  ).equals( "/" )) {
            return parent.getPath( ) + "/" + name;
        } else {
            return "/" + name;
        }
    }

    public void registerChild(MockAsset child) {
        children.putIfAbsent( child.getName(), child );
        this.container = true;
    }

    public void unregisterChild(MockAsset child) {
        children.remove( child.getName( ) );
    }


    public void setStorage(RepositoryStorage storage) {
        this.storage = storage;
    }

    public boolean isThrowException( )
    {
        return throwException;
    }

    public void setThrowException( boolean throwException )
    {
        this.throwException = throwException;
    }

    @Override
    public RepositoryStorage getStorage( )
    {
        return storage;
    }

    @Override
    public String getPath( )
    {
        return this.path;
    }

    @Override
    public String getName( )
    {
        return this.name;
    }

    @Override
    public Instant getModificationTime( )
    {
        return Instant.now();
    }

    @Override
    public boolean isContainer( )
    {
        return this.container;
    }

    @Override
    public boolean isLeaf( )
    {
        return !this.container;
    }

    @Override
    public List<MockAsset> list( )
    {
        return new ArrayList( children.values( ) );
    }

    @Override
    public long getSize( )
    {
        return 0;
    }

    @Override
    public InputStream getReadStream( ) throws IOException
    {
        return Mockito.mock( InputStream.class );
    }

    @Override
    public ReadableByteChannel getReadChannel( ) throws IOException
    {
        ReadableByteChannel channel = Mockito.mock( ReadableByteChannel.class );
        Mockito.when( channel.read( any( ByteBuffer.class ) ) ).thenReturn( -1 );
        return channel;
    }

    @Override
    public OutputStream getWriteStream( boolean replace ) throws IOException
    {
        return Mockito.mock( OutputStream.class );
    }

    @Override
    public WritableByteChannel getWriteChannel( boolean replace ) throws IOException
    {
        this.exists = true;
        return Mockito.mock( WritableByteChannel.class );
    }

    @Override
    public boolean replaceDataFromFile( Path newData ) throws IOException
    {
        return false;
    }

    @Override
    public boolean exists( )
    {
        return exists;
    }

    @Override
    public void create( ) throws IOException
    {
        this.exists = true;
    }

    @Override
    public void create( AssetType type ) throws IOException
    {
        if (type.equals( AssetType.CONTAINER )) {
            this.container = true;
        }
        this.exists = true;
    }

    @Override
    public Path getFilePath( ) throws UnsupportedOperationException
    {
        return null;
    }

    @Override
    public boolean isFileBased( )
    {
        return false;
    }

    @Override
    public boolean hasParent( )
    {
        return this.parent != null;
    }

    @Override
    public MockAsset getParent( )
    {
        return this.parent;
    }

    @Override
    public StorageAsset resolve( String toPath )
    {
        if (children.containsKey( toPath )) {
            return children.get( toPath );
        } else {
            if (toPath.startsWith( "/" )) {
                toPath = StringUtils.removeStart( toPath, "/" );
            }
            if ( "".equals( toPath ) )
            {
                return this;
            }
            String[] destPath = toPath.split( "/" );
            StringBuilder destPathStr = new StringBuilder( );
            MockAsset destParent = this;
            for (int i=0; i<destPath.length; i++) {
                destPathStr.append( "/" ).append( destPath[i] );
                StorageAsset child = storage.getAsset( destPathStr.toString( ) );
                if (child!=null) {
                    destParent = (MockAsset) child;
                } else
                {
                    System.out.println( "Resolve " + destParent.getPath( ) + " -- " + destPath[i] );
                    destParent = new MockAsset( destParent, destPath[i] );
                }
            }
            return destParent;
        }
    }

    @Override
    public String relativize( StorageAsset asset )
    {
        System.out.println( "relativize this " + this.getPath( ) + " -> other " + asset.getPath( ) );
        if (asset.isFileBased()) {
            return Paths.get( getPath( ) ).relativize( asset.getFilePath( ) ).toString();
        } else {
            return StringUtils.removeStart( asset.getPath( ), this.getPath( ) );
        }
    }

    @Override
    public String toString( )
    {
        return getPath();
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;

        MockAsset mockAsset = (MockAsset) o;

        return path.equals( mockAsset.path );
    }

    @Override
    public int hashCode( )
    {
        return path.hashCode( );
    }
}
