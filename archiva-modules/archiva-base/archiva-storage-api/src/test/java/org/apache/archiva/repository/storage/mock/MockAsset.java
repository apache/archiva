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

import org.apache.archiva.repository.storage.RepositoryStorage;
import org.apache.archiva.repository.storage.StorageAsset;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MockAsset implements StorageAsset
{
    private MockAsset parent;
    private String path;
    private String name;
    private LinkedHashMap<String, MockAsset> children = new LinkedHashMap<>( );
    private boolean container = false;
    private RepositoryStorage storage;



    private boolean throwException;

    public MockAsset( String name ) {
        this.name = name;
        this.path = "/";
    }

    public MockAsset( MockAsset parent, String name ) {
        this.parent = parent;
        this.path = (parent.hasParent()?parent.getPath( ):"") + "/" + name;
        this.name = name;
        this.storage = parent.getStorage( );
        parent.registerChild( this );
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
        return null;
    }

    @Override
    public ReadableByteChannel getReadChannel( ) throws IOException
    {
        return null;
    }

    @Override
    public OutputStream getWriteStream( boolean replace ) throws IOException
    {
        return null;
    }

    @Override
    public WritableByteChannel getWriteChannel( boolean replace ) throws IOException
    {
        return null;
    }

    @Override
    public boolean replaceDataFromFile( Path newData ) throws IOException
    {
        return false;
    }

    @Override
    public boolean exists( )
    {
        return false;
    }

    @Override
    public void create( ) throws IOException
    {

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
            return null;
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
