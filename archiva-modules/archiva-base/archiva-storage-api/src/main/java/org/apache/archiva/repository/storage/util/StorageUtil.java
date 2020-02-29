package org.apache.archiva.repository.storage.util;

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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *
 * Utility class for traversing the asset tree recursively and stream based access to the assets.
 *
 * @since 3.0
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class StorageUtil
{

    private static final Logger LOG = LoggerFactory.getLogger( StorageUtil.class );

    private static final int DEFAULT_BUFFER_SIZE = 4096;

    /**
     * Walk the tree starting at the given asset. The consumer is called for each asset found.
     * It runs a depth-first search where children are consumed before their parents.
     *
     * @param start the starting asset
     * @param consumer the consumer that is applied to each asset
     */
    public static void walk( StorageAsset start, Consumer<StorageAsset> consumer ) {
        try(Stream<StorageAsset> assetStream = newAssetStream( start, false )) {
            assetStream.forEach( consumer::accept );
        }
    }

    /**
     * Walk the tree starting at the given asset. The consumer function is called for each asset found
     * as long as it returns <code>true</code> as result. If the function returns <code>false</code> the
     * processing stops.
     * It runs a depth-first search where children are consumed before their parents.
     *
     * @param start the starting asset
     * @param consumer the consumer function that is applied to each asset and that has to return <code>true</code>,
     *                 if the walk should continue.
     */
    public static void walk( StorageAsset start, Function<StorageAsset, Boolean> consumer ) {
        try(Stream<StorageAsset> assetStream = newAssetStream( start, false )) {
            assetStream.anyMatch( a -> !consumer.apply( a ) );
        }
    }


    /**
     * Returns a stream of assets starting at the given start node. The returned stream returns a closable
     * stream and should always be used in a try-with-resources statement.
     *
     * @param start the starting asset
     * @param parallel <code>true</code>, if a parallel stream should be created, otherwise <code>false</code>
     * @return the newly created stream
     */
    public static Stream<StorageAsset> newAssetStream( StorageAsset start, boolean parallel )
    {
        return StreamSupport.stream( new AssetSpliterator( start ), parallel );
    }


    /**
     * Returns a non-parallel stream.
     * Calls {@link #newAssetStream(StorageAsset, boolean)} with <code>parallel=false</code>.
     *
     * @param start the starting asset
     * @return the returned stream object
     */
    public static Stream<StorageAsset> newAssetStream( StorageAsset start) {
        return newAssetStream( start, false );
    }

    /**
     * Deletes the given asset and all child assets recursively.
     * IOExceptions during deletion are ignored.
     *
     * @param baseDir The base asset to remove.
     *
     */
    public static final void deleteRecursively(StorageAsset baseDir) {
        RepositoryStorage storage = baseDir.getStorage( );
        walk( baseDir,  a -> {
            try {
                storage.removeAsset(a);
            } catch (IOException e) {
                LOG.error( "Could not delete asset {}: {}", a.getPath( ), e.getMessage( ), e );
            }
        });
    }

    /**
     * Deletes the given asset and all child assets recursively.
     * @param baseDir The base asset to remove.
     * @param stopOnError if <code>true</code> the traversal stops, if an exception is encountered
     * @return returns <code>true</code>, if every item was removed. If an IOException was encountered during
     * traversal it returns <code>false</code>
     */
    public static final boolean deleteRecursively(final StorageAsset baseDir, final boolean stopOnError) {
        final RepositoryStorage storage = baseDir.getStorage( );
        try(Stream<StorageAsset> stream = newAssetStream( baseDir ))
        {
            if ( stopOnError )
            {
                // Return true, if no exception occurred
                // anyMatch is short-circuiting, that means it stops if the condition matches
                return !stream.map( a -> {
                    try
                    {
                        storage.removeAsset( a );
                        // Returning false, if OK
                        return Boolean.FALSE;
                    }
                    catch ( IOException e )
                    {
                        LOG.error( "Could not delete asset {}: {}", a.getPath( ), e.getMessage( ), e );
                        // Returning true, if exception
                        return Boolean.TRUE;
                    }
                } ).anyMatch( r -> r );
            } else {
                // Return true, if all removals were OK
                // We want to consume all, so we use allMatch
                return stream.map( a -> {
                    try
                    {
                        storage.removeAsset( a );
                        // Returning true, if OK
                        return Boolean.TRUE;
                    }
                    catch ( IOException e )
                    {
                        LOG.error( "Could not delete asset {}: {}", a.getPath( ), e.getMessage( ), e );
                        // Returning false, if exception
                        return Boolean.FALSE;
                    }
                } ).allMatch( r -> r );
            }
        }
    }

    /**
     * Moves a asset between different storage instances.
     * If you know that source and asset are from the same storage instance, the move method of the storage
     * instance may be faster.
     *
     * @param source The source asset
     * @param target The target asset
     * @param locked If true, a lock is used for the move operation.
     * @param copyOptions Options for copying
     * @throws IOException If the move fails
     */
    public static final void moveAsset(StorageAsset source, StorageAsset target, boolean locked, CopyOption... copyOptions) throws IOException
    {
        if (source.isFileBased() && target.isFileBased()) {
            // Short cut for FS operations
            // Move is atomic operation
            if (!Files.exists(target.getFilePath().getParent())) {
                Files.createDirectories(target.getFilePath().getParent());
            }
            Files.move( source.getFilePath(), target.getFilePath(), copyOptions );
        } else {
            try {
                final RepositoryStorage sourceStorage = source.getStorage();
                final RepositoryStorage targetStorage = target.getStorage();
                sourceStorage.consumeDataFromChannel( source, is -> wrapWriteFunction( is, targetStorage, target, locked ), locked);
                sourceStorage.removeAsset( source );
            }  catch (IOException e) {
                throw e;
            }  catch (Throwable e) {
                Throwable cause = e.getCause();
                if (cause instanceof IOException) {
                    throw (IOException)cause;
                } else
                {
                    throw new IOException( e );
                }
            }
        }

    }

    private static final void wrapWriteFunction( ReadableByteChannel is, RepositoryStorage targetStorage, StorageAsset target, boolean locked) {
        try {
            targetStorage.writeDataToChannel( target, os -> copy(is, os), locked );
        } catch (Exception e) {
            throw new RuntimeException( e );
        }
    }

    public static final void copy( final ReadableByteChannel is, final WritableByteChannel os ) {
        if (is instanceof FileChannel ) {
            copy( (FileChannel) is, os );
        } else if (os instanceof FileChannel) {
            copy(is, (FileChannel)os);
        } else
        {
            try
            {
                ByteBuffer buffer = ByteBuffer.allocate( DEFAULT_BUFFER_SIZE );
                while ( is.read( buffer ) != -1 )
                {
                    buffer.flip( );
                    while ( buffer.hasRemaining( ) )
                    {
                        os.write( buffer );
                    }
                    buffer.clear( );
                }
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        }
    }

    public static final void copy( final FileChannel is, final WritableByteChannel os ) {
        try
        {
            is.transferTo( 0, is.size( ), os );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    public static final void copy( final ReadableByteChannel is, final FileChannel os ) {
        try
        {
            os.transferFrom( is, 0, Long.MAX_VALUE );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }


    /**
     * Returns the extension of the name of a given asset. Extension is the substring after the last occurence of '.' in the
     * string. If no '.' is found, the empty string is returned.
     *
     * @param asset The asset from which to return the extension string.
     * @return The extension.
     */
    public static final String getExtension(StorageAsset asset) {
        return StringUtils.substringAfterLast(asset.getName(),".");
    }
}
