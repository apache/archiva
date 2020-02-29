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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.repository.storage.RepositoryStorage;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.repository.storage.util.VisitStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.CopyOption;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class MockStorage implements RepositoryStorage
{
    public static final String ADD = "ADD";
    public static final String REMOVE = "REMOVE";
    private MockAsset root;
    private LinkedHashMap<String, MockAsset> assets = new LinkedHashMap<>( );

    private VisitStatus status = new VisitStatus( );

    public MockStorage( MockAsset root )
    {
        this.root = root;
        root.setStorage( this );
    }

    public MockStorage() {
        this.root = new MockAsset( "" );
        this.root.setStorage( this );
    }

    public VisitStatus getStatus() {
        return status;
    }

    @Override
    public URI getLocation( )
    {
        return null;
    }

    @Override
    public void updateLocation( URI newLocation ) throws IOException
    {

    }

    private String[] splitPath(String path) {
        if (path.equals("/")) {
            return new String[0];
        } else
        {
            if (path.startsWith( "/" )) {
                return path.substring( 1, path.length( ) ).split( "/" );
            }
            return path.split( "/" );
        }
    }

    @Override
    public StorageAsset getAsset( String path )
    {
        if (assets.containsKey( path )) {
            return assets.get( path );
        }
        String[] pathArr = splitPath( path );
        StorageAsset parent = root;
        for (String pathElement : pathArr) {
            Optional<? extends StorageAsset> next = parent.list( ).stream( ).filter( a -> a.getName( ).equals( pathElement ) ).findFirst( );
            if (next.isPresent()) {
                parent = next.get( );
            } else {
                MockAsset asset = new MockAsset( (MockAsset)parent, pathElement );
                assets.put( asset.getPath( ), asset );
                parent = asset;
            }
        }
        return parent;
    }

    @Override
    public void consumeData( StorageAsset asset, Consumer<InputStream> consumerFunction, boolean readLock ) throws IOException
    {

    }

    @Override
    public void consumeDataFromChannel( StorageAsset asset, Consumer<ReadableByteChannel> consumerFunction, boolean readLock ) throws IOException
    {

    }

    @Override
    public void writeData( StorageAsset asset, Consumer<OutputStream> consumerFunction, boolean writeLock ) throws IOException
    {

    }

    @Override
    public void writeDataToChannel( StorageAsset asset, Consumer<WritableByteChannel> consumerFunction, boolean writeLock ) throws IOException
    {

    }

    @Override
    public StorageAsset addAsset( String path, boolean container )
    {
        String[] pathArr = splitPath( path );
        StorageAsset parent = root;
        for (String pathElement : pathArr) {
            Optional<? extends StorageAsset> next = parent.list( ).stream( ).filter( a -> a.getName( ).equals( pathElement ) ).findFirst( );
            if (next.isPresent()) {
                parent = next.get( );
            } else {
                MockAsset asset = new MockAsset( (MockAsset)parent, pathElement );
                assets.put( asset.getPath( ), asset );
                parent = asset;
            }
        }
        status.add( ADD, parent );
        return parent;
    }

    @Override
    public void removeAsset( StorageAsset assetArg ) throws IOException
    {
        MockAsset asset = (MockAsset) assetArg;
        if (asset.hasParent())
        {
            asset.getParent( ).unregisterChild( asset );
        }
        assets.remove( asset.getPath( ) );
        status.add( REMOVE, asset );
        if (asset.isThrowException()) {
            throw new IOException( "Mocked IOException for " + asset.getPath( ) );
        }
    }

    @Override
    public StorageAsset moveAsset( StorageAsset origin, String destination, CopyOption... copyOptions ) throws IOException
    {
        return null;
    }

    @Override
    public void moveAsset( StorageAsset origin, StorageAsset destination, CopyOption... copyOptions ) throws IOException
    {

    }

    @Override
    public StorageAsset copyAsset( StorageAsset origin, String destination, CopyOption... copyOptions ) throws IOException
    {
        return null;
    }

    @Override
    public void copyAsset( StorageAsset origin, StorageAsset destination, CopyOption... copyOptions ) throws IOException
    {

    }
}
