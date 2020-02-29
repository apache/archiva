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
import org.apache.archiva.common.filelock.Lock;
import org.apache.archiva.repository.storage.RepositoryStorage;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.repository.storage.util.StorageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.util.HashSet;

/**
 *
 * Utility class for assets. Allows to copy, move between different storage instances and
 * recursively consume the tree.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class FsStorageUtil
{
    private static final Logger log = LoggerFactory.getLogger( FsStorageUtil.class);

    /**
     * Copies the source asset to the target. The assets may be from different RepositoryStorage instances.
     * If you know that source and asset are from the same storage instance, the copy method of the storage
     * instance may be faster.
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
                Lock lockRead = null;
                Lock lockWrite = null;
                try {
                    lockRead = lmSource.readFileLock(sourcePath);
                } catch (Exception e) {
                    log.error("Could not create read lock on {}", sourcePath);
                    throw new IOException(e);
                }
                try {
                    lockWrite = lmTarget.writeFileLock(targetPath);
                } catch (Exception e) {
                    log.error("Could not create write lock on {}", targetPath);
                    throw new IOException(e);
                }
                try {
                    Files.copy(sourcePath, targetPath, copyOptions);
                } finally {
                    if (lockRead!=null) {
                        try {
                            lmSource.release(lockRead);
                        } catch (FileLockException e) {
                            log.error("Error during lock release of read lock {}", lockRead.getFile());
                        }
                    }
                    if (lockWrite!=null) {
                        try {
                            lmTarget.release(lockWrite);
                        } catch (FileLockException e) {
                            log.error("Error during lock release of write lock {}", lockWrite.getFile());
                        }
                    }
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

    private static final void wrapWriteFunction( ReadableByteChannel is, RepositoryStorage targetStorage, StorageAsset target, boolean locked) {
        try {
            targetStorage.writeDataToChannel( target, os -> StorageUtil.copy(is, os), locked );
        } catch (Exception e) {
            throw new RuntimeException( e );
        }
    }


    public static final void copyToLocalFile(StorageAsset asset, Path destination, CopyOption... copyOptions) throws IOException {
        if (asset.isFileBased()) {
            Files.copy(asset.getFilePath(), destination, copyOptions);
        } else {
            try {

                HashSet<OpenOption> openOptions = new HashSet<>();
                for (CopyOption option : copyOptions) {
                    if (option == StandardCopyOption.REPLACE_EXISTING) {
                        openOptions.add(StandardOpenOption.CREATE);
                        openOptions.add(StandardOpenOption.TRUNCATE_EXISTING);
                        openOptions.add(StandardOpenOption.WRITE);
                    } else {
                        openOptions.add(StandardOpenOption.WRITE);
                        openOptions.add(StandardOpenOption.CREATE_NEW);
                    }
                }
                asset.getStorage().consumeDataFromChannel(asset, channel -> {
                    try {
                        FileChannel.open(destination, openOptions).transferFrom(channel, 0, Long.MAX_VALUE);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, false);
            } catch (Throwable e) {
                if (e.getCause() instanceof IOException) {
                    throw (IOException)e.getCause();
                } else {
                    throw new IOException(e);
                }
            }
        }
    }

    public static class PathInformation {
        final Path path ;
        final boolean tmpFile;

        PathInformation(Path path, boolean tmpFile) {
            this.path = path;
            this.tmpFile = tmpFile;
        }

        public Path getPath() {
            return path;
        }

        public boolean isTmpFile() {
            return tmpFile;
        }

    }

    public static final PathInformation getAssetDataAsPath(StorageAsset asset) throws IOException {
        if (!asset.exists()) {
            throw new IOException("Asset does not exist");
        }
        if (asset.isFileBased()) {
            return new PathInformation(asset.getFilePath(), false);
        } else {
            Path tmpFile = Files.createTempFile(asset.getName(), org.apache.archiva.repository.storage.util.StorageUtil.getExtension(asset));
            copyToLocalFile(asset, tmpFile, StandardCopyOption.REPLACE_EXISTING);
            return new PathInformation(tmpFile, true);
        }
    }

}
