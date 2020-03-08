package org.apache.archiva.repository.storage.fs;

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

import org.apache.archiva.common.filelock.FileLockException;
import org.apache.archiva.common.filelock.FileLockManager;
import org.apache.archiva.common.filelock.FileLockTimeoutException;
import org.apache.archiva.common.filelock.Lock;
import org.apache.archiva.common.utils.PathUtil;
import org.apache.archiva.repository.storage.RepositoryStorage;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.*;
import java.util.function.Consumer;

/**
 * Implementation of <code>{@link RepositoryStorage}</code> where data is stored in the filesystem.
 *
 * All files are relative to a given base path. Path values are separated by '/', '..' is allowed to navigate
 * to a parent directory, but navigation out of the base path will lead to a exception.
 */
public class FilesystemStorage implements RepositoryStorage {

    private static final Logger log = LoggerFactory.getLogger(FilesystemStorage.class);

    private Path basePath;
    private final FileLockManager fileLockManager;

    public FilesystemStorage(Path basePath, FileLockManager fileLockManager) throws IOException {
        if (!Files.exists(basePath)) {
            Files.createDirectories(basePath);
        }
        this.basePath = basePath.normalize().toRealPath();
        this.fileLockManager = fileLockManager;
    }

    private Path normalize(final String path) {
        String nPath = path;
        while (nPath.startsWith("/")) {
            nPath = nPath.substring(1);
        }
        return Paths.get(nPath);
    }

    private Path getAssetPath(String path) throws IOException {
        Path assetPath = basePath.resolve(normalize(path)).normalize();
        if (!assetPath.startsWith(basePath))
        {
            throw new IOException("Path navigation out of allowed scope: "+path);
        }
        return assetPath;
    }

    @Override
    public void consumeData( StorageAsset asset, Consumer<InputStream> consumerFunction, boolean readLock ) throws IOException
    {
        final Path path = asset.getFilePath();
        try {
            if (readLock) {
                consumeDataLocked( path, consumerFunction );
            } else
            {
                try ( InputStream is = Files.newInputStream( path ) )
                {
                    consumerFunction.accept( is );
                }
                catch ( IOException e )
                {
                    log.error("Could not read the input stream from file {}", path);
                    throw e;
                }
            }
        } catch (RuntimeException e)
        {
            log.error( "Runtime exception during data consume from artifact {}. Error: {}", path, e.getMessage() );
            throw new IOException( e );
        }

    }

    @Override
    public void consumeDataFromChannel( StorageAsset asset, Consumer<ReadableByteChannel> consumerFunction, boolean readLock ) throws IOException
    {
        final Path path = asset.getFilePath();
        try {
            if (readLock) {
                consumeDataFromChannelLocked( path, consumerFunction );
            } else
            {
                try ( FileChannel is = FileChannel.open( path, StandardOpenOption.READ ) )
                {
                    consumerFunction.accept( is );
                }
                catch ( IOException e )
                {
                    log.error("Could not read the input stream from file {}", path);
                    throw e;
                }
            }
        } catch (RuntimeException e)
        {
            log.error( "Runtime exception during data consume from artifact {}. Error: {}", path, e.getMessage() );
            throw new IOException( e );
        }
    }

    @Override
    public void writeData( StorageAsset asset, Consumer<OutputStream> consumerFunction, boolean writeLock ) throws IOException
    {
        final Path path = asset.getFilePath();
        try {
            if (writeLock) {
                writeDataLocked( path, consumerFunction );
            } else
            {
                try ( OutputStream is = Files.newOutputStream( path ) )
                {
                    consumerFunction.accept( is );
                }
                catch ( IOException e )
                {
                    log.error("Could not write the output stream to file {}", path);
                    throw e;
                }
            }
        } catch (RuntimeException e)
        {
            log.error( "Runtime exception during data consume from artifact {}. Error: {}", path, e.getMessage() );
            throw new IOException( e );
        }

    }

    @Override
    public void writeDataToChannel( StorageAsset asset, Consumer<WritableByteChannel> consumerFunction, boolean writeLock ) throws IOException
    {
        final Path path = asset.getFilePath();
        try {
            if (writeLock) {
                writeDataToChannelLocked( path, consumerFunction );
            } else
            {
                try ( FileChannel os = FileChannel.open( path, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE ))
                {
                    consumerFunction.accept( os );
                }
                catch ( IOException e )
                {
                    log.error("Could not write the data to file {}", path);
                    throw e;
                }
            }
        } catch (RuntimeException e)
        {
            log.error( "Runtime exception during data consume from artifact {}. Error: {}", path, e.getMessage() );
            throw new IOException( e );
        }
    }

    private void consumeDataLocked( Path file, Consumer<InputStream> consumerFunction) throws IOException
    {

        final Lock lock;
        try
        {
            lock = fileLockManager.readFileLock( file );
            try ( InputStream is = Files.newInputStream( lock.getFile()))
            {
                consumerFunction.accept( is );
            }
            catch ( IOException e )
            {
                log.error("Could not read the input stream from file {}", file);
                throw e;
            } finally
            {
                fileLockManager.release( lock );
            }
        }
        catch ( FileLockException | FileNotFoundException | FileLockTimeoutException e)
        {
            log.error("Locking error on file {}", file);
            throw new IOException(e);
        }
    }

    private void consumeDataFromChannelLocked( Path file, Consumer<ReadableByteChannel> consumerFunction) throws IOException
    {

        final Lock lock;
        try
        {
            lock = fileLockManager.readFileLock( file );
            try ( FileChannel is = FileChannel.open( lock.getFile( ), StandardOpenOption.READ ))
            {
                consumerFunction.accept( is );
            }
            catch ( IOException e )
            {
                log.error("Could not read the input stream from file {}", file);
                throw e;
            } finally
            {
                fileLockManager.release( lock );
            }
        }
        catch ( FileLockException | FileNotFoundException | FileLockTimeoutException e)
        {
            log.error("Locking error on file {}", file);
            throw new IOException(e);
        }
    }


    private void writeDataLocked( Path file, Consumer<OutputStream> consumerFunction) throws IOException
    {

        final Lock lock;
        try
        {
            lock = fileLockManager.writeFileLock( file );
            try ( OutputStream is = Files.newOutputStream( lock.getFile()))
            {
                consumerFunction.accept( is );
            }
            catch ( IOException e )
            {
                log.error("Could not write the output stream to file {}", file);
                throw e;
            } finally
            {
                fileLockManager.release( lock );
            }
        }
        catch ( FileLockException | FileNotFoundException | FileLockTimeoutException e)
        {
            log.error("Locking error on file {}", file);
            throw new IOException(e);
        }
    }

    private void writeDataToChannelLocked( Path file, Consumer<WritableByteChannel> consumerFunction) throws IOException
    {

        final Lock lock;
        try
        {
            lock = fileLockManager.writeFileLock( file );
            try ( FileChannel is = FileChannel.open( lock.getFile( ), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE ))
            {
                consumerFunction.accept( is );
            }
            catch ( IOException e )
            {
                log.error("Could not write to file {}", file);
                throw e;
            } finally
            {
                fileLockManager.release( lock );
            }
        }
        catch ( FileLockException | FileNotFoundException | FileLockTimeoutException e)
        {
            log.error("Locking error on file {}", file);
            throw new IOException(e);
        }
    }

    @Override
    public URI getLocation() {
        return basePath.toUri();
    }

    /**
     * Updates the location and releases all locks.
     *
     * @param newLocation The URI to the new location
     *
     * @throws IOException If the directory cannot be created.
     */
    @Override
    public void updateLocation(URI newLocation) throws IOException {
        Path newPath = PathUtil.getPathFromUri(newLocation).toAbsolutePath();
        if (!Files.exists(newPath)) {
            Files.createDirectories(newPath);
        }
        basePath = newPath;
        if (fileLockManager!=null) {
            fileLockManager.clearLockFiles();
        }
    }

    @Override
    public StorageAsset getAsset( String path )
    {
        try {
            return new FilesystemAsset(this, path, getAssetPath(path), basePath);
        } catch (IOException e) {
            throw new IllegalArgumentException("Path navigates outside of base directory "+path);
        }
    }

    @Override
    public StorageAsset addAsset( String path, boolean container )
    {
        try {
            return new FilesystemAsset(this, path, getAssetPath(path), basePath, container);
        } catch (IOException e) {
            throw new IllegalArgumentException("Path navigates outside of base directory "+path);
        }
    }

    @Override
    public void removeAsset( StorageAsset asset ) throws IOException
    {
        Files.delete(asset.getFilePath());
    }

    @Override
    public StorageAsset moveAsset( StorageAsset origin, String destination, CopyOption... copyOptions ) throws IOException
    {
        boolean container = origin.isContainer();
        FilesystemAsset newAsset = new FilesystemAsset(this, destination, getAssetPath(destination), basePath, container );
        moveAsset( origin, newAsset, copyOptions );
        return newAsset;
    }

    @Override
    public void moveAsset( StorageAsset origin, StorageAsset destination, CopyOption... copyOptions ) throws IOException
    {
        if (origin.getStorage()!=this) {
            throw new IOException("The origin asset does not belong to this storage instance. Cannot copy between different storage instances.");
        }
        if (destination.getStorage()!=this) {
            throw new IOException("The destination asset does not belong to this storage instance. Cannot copy between different storage instances.");
        }
        Files.move(origin.getFilePath(), destination.getFilePath(), copyOptions);
    }

    @Override
    public StorageAsset copyAsset( StorageAsset origin, String destination, CopyOption... copyOptions ) throws IOException
    {
        boolean container = origin.isContainer();
        FilesystemAsset newAsset = new FilesystemAsset(this, destination, getAssetPath(destination), basePath, container );
        copyAsset( origin, newAsset, copyOptions );
        return newAsset;
    }

    @Override
    public void copyAsset( StorageAsset origin, StorageAsset destination, CopyOption... copyOptions ) throws IOException
    {
        if (origin.getStorage()!=this) {
            throw new IOException("The origin asset does not belong to this storage instance. Cannot copy between different storage instances.");
        }
        if (destination.getStorage()!=this) {
            throw new IOException("The destination asset does not belong to this storage instance. Cannot copy between different storage instances.");
        }
        Path destinationPath = destination.getFilePath();
        boolean overwrite = false;
        for (int i=0; i<copyOptions.length; i++) {
            if (copyOptions[i].equals( StandardCopyOption.REPLACE_EXISTING )) {
                overwrite=true;
            }
        }
        if (Files.exists(destinationPath) && !overwrite) {
            throw new IOException("Destination file exists already "+ destinationPath);
        }
        if (Files.isDirectory( origin.getFilePath() ))
        {
            FileUtils.copyDirectory(origin.getFilePath( ).toFile(), destinationPath.toFile() );
        } else if (Files.isRegularFile( origin.getFilePath() )) {
            if (!Files.exists( destinationPath )) {
                Files.createDirectories( destinationPath );
            }
            Files.copy( origin.getFilePath( ), destinationPath, copyOptions );
        }
    }

    public FileLockManager getFileLockManager() {
        return fileLockManager;
    }

}
