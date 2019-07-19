package org.apache.archiva.repository.content;

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

import org.apache.archiva.common.filelock.FileLockException;
import org.apache.archiva.common.filelock.FileLockManager;
import org.apache.archiva.common.filelock.FileLockTimeoutException;
import org.apache.archiva.common.filelock.Lock;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class StorageUtil
{
    private static final int DEFAULT_BUFFER_SIZE = 4096;

    /**
     * Copies the source asset to the target. The assets may be from different RepositoryStorage instances.
     *
     * @param source The source asset
     * @param target The target asset
     * @param locked If true, a readlock is set on the source and a write lock is set on the target.
     * @param copyOptions Copy options
     * @throws IOException
     */
    public static final void copyAsset( final StorageAsset source,
                                        final StorageAsset target,
                                        boolean locked,
                                        final CopyOption... copyOptions ) throws IOException
    {
        if (source.isFileBased() && target.isFileBased()) {
            // Short cut for FS operations
            final Path sourcePath = source.getFilePath();
            final Path targetPath = target.getFilePath( );
            if (locked) {
                final FileLockManager lmSource = ((FilesystemStorage)source.getStorage()).getFileLockManager();
                final FileLockManager lmTarget = ((FilesystemStorage)target.getStorage()).getFileLockManager();
                try (Lock lockRead = lmSource.readFileLock( sourcePath ); Lock lockWrite = lmTarget.writeFileLock( targetPath ) )
                {
                    Files.copy( sourcePath, targetPath, copyOptions );
                }
                catch ( FileLockException e )
                {
                    throw new IOException( e );
                }
                catch ( FileLockTimeoutException e )
                {
                    throw new IOException( e );
                }
            } else
            {
                Files.copy( sourcePath, targetPath, copyOptions );
            }
        } else {
            try {
                final RepositoryStorage sourceStorage = source.getStorage();
                final RepositoryStorage targetStorage = target.getStorage();
                sourceStorage.consumeDataFromChannel( source, is -> wrapWriteFunction( is, targetStorage, target, locked ), locked);
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

    /**
     *
     * @param source
     * @param target
     * @param locked
     * @param copyOptions
     * @throws IOException
     */
    public static void moveAsset(StorageAsset source, StorageAsset target, boolean locked, CopyOption... copyOptions) throws IOException
    {
        if (source.isFileBased() && target.isFileBased()) {
            // Short cut for FS operations
            // Move is atomic operation
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

    private static void wrapWriteFunction(ReadableByteChannel is, RepositoryStorage targetStorage, StorageAsset target, boolean locked) {
        try {
            targetStorage.writeDataToChannel( target, os -> copy(is, os), locked );
        } catch (Exception e) {
            throw new RuntimeException( e );
        }
    }


    private static void copy( final ReadableByteChannel is, final WritableByteChannel os ) {
        if (is instanceof FileChannel) {
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

    private static void copy( final FileChannel is, final WritableByteChannel os ) {
        try
        {
            is.transferTo( 0, is.size( ), os );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    private static void copy( final ReadableByteChannel is, final FileChannel os ) {
        try
        {
            os.transferFrom( is, 0, Long.MAX_VALUE );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

}
