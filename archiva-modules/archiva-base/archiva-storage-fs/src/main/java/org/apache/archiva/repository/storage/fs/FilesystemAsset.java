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

import org.apache.archiva.repository.storage.RepositoryStorage;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of an asset that is stored on the filesystem.
 * <p>
 * The implementation does not check the given paths. Caller should normalize the asset path
 * and check, if the base path is a parent of the resulting path.
 * <p>
 * The file must not exist for all operations.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class FilesystemAsset implements StorageAsset, Comparable {

    private final static Logger log = LoggerFactory.getLogger(FilesystemAsset.class);

    private final Path basePath;
    private final Path assetPath;
    private final String relativePath;

    public static final String DEFAULT_POSIX_FILE_PERMS = "rw-rw----";
    public static final String DEFAULT_POSIX_DIR_PERMS = "rwxrwx---";

    public static final Set<PosixFilePermission> DEFAULT_POSIX_FILE_PERMISSIONS;
    public static final Set<PosixFilePermission> DEFAULT_POSIX_DIR_PERMISSIONS;

    public static final AclEntryPermission[] DEFAULT_ACL_FILE_PERMISSIONS = new AclEntryPermission[]{
            AclEntryPermission.DELETE, AclEntryPermission.READ_ACL, AclEntryPermission.READ_ATTRIBUTES, AclEntryPermission.READ_DATA, AclEntryPermission.WRITE_ACL,
            AclEntryPermission.WRITE_ATTRIBUTES, AclEntryPermission.WRITE_DATA, AclEntryPermission.APPEND_DATA
    };

    public static final AclEntryPermission[] DEFAULT_ACL_DIR_PERMISSIONS = new AclEntryPermission[]{
            AclEntryPermission.ADD_FILE, AclEntryPermission.ADD_SUBDIRECTORY, AclEntryPermission.DELETE_CHILD,
            AclEntryPermission.DELETE, AclEntryPermission.READ_ACL, AclEntryPermission.READ_ATTRIBUTES, AclEntryPermission.READ_DATA, AclEntryPermission.WRITE_ACL,
            AclEntryPermission.WRITE_ATTRIBUTES, AclEntryPermission.WRITE_DATA, AclEntryPermission.APPEND_DATA
    };

    static {

        DEFAULT_POSIX_FILE_PERMISSIONS = PosixFilePermissions.fromString(DEFAULT_POSIX_FILE_PERMS);
        DEFAULT_POSIX_DIR_PERMISSIONS = PosixFilePermissions.fromString(DEFAULT_POSIX_DIR_PERMS);
    }

    Set<PosixFilePermission> defaultPosixFilePermissions = DEFAULT_POSIX_FILE_PERMISSIONS;
    Set<PosixFilePermission> defaultPosixDirectoryPermissions = DEFAULT_POSIX_DIR_PERMISSIONS;

    List<AclEntry> defaultFileAcls;
    List<AclEntry> defaultDirectoryAcls;

    boolean supportsAcl = false;
    boolean supportsPosix = false;
    final boolean setPermissionsForNew;
    final RepositoryStorage storage;

    boolean directoryHint = false;

    private static final OpenOption[] REPLACE_OPTIONS = new OpenOption[]{StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE};
    private static final OpenOption[] APPEND_OPTIONS = new OpenOption[]{StandardOpenOption.APPEND};


    FilesystemAsset(RepositoryStorage storage, String path, Path assetPath, Path basePath) {
        this.assetPath = assetPath;
        this.relativePath = normalizePath(path);
        this.setPermissionsForNew=false;
        this.basePath = basePath;
        this.storage = storage;
        init();
    }

    /**
     * Creates an asset for the given path. The given paths are not checked.
     * The base path should be an absolute path.
     *
     * @param path The logical path for the asset relative to the repository.
     * @param assetPath The asset path.
     */
    public FilesystemAsset(RepositoryStorage storage, String path, Path assetPath) {
        this.assetPath = assetPath;
        this.relativePath = normalizePath(path);
        this.setPermissionsForNew = false;
        this.basePath = null;
        this.storage = storage;
        // The base directory is always a directory
        if ("".equals(path) || "/".equals(path)) {
            this.directoryHint = true;
        }
        init();
    }

    /**
     * Creates an asset for the given path. The given paths are not checked.
     * The base path should be an absolute path.
     *
     * @param path The logical path for the asset relative to the repository
     * @param assetPath The asset path.
     * @param directory This is only relevant, if the represented file or directory does not exist yet and
     *                  is a hint.
     */
    public FilesystemAsset(RepositoryStorage storage, String path, Path assetPath, Path basePath, boolean directory) {
        this.assetPath = assetPath;
        this.relativePath = normalizePath(path);
        this.directoryHint = directory;
        this.setPermissionsForNew = false;
        this.basePath = basePath;
        this.storage = storage;
        init();
    }

    /**
     * Creates an asset for the given path. The given paths are not checked.
     * The base path should be an absolute path.
     *
     * @param path The logical path for the asset relative to the repository
     * @param assetPath The asset path.
     * @param directory This is only relevant, if the represented file or directory does not exist yet and
     *                  is a hint.
     */
    public FilesystemAsset(RepositoryStorage storage, String path, Path assetPath, Path basePath, boolean directory, boolean setPermissionsForNew) {
        this.assetPath = assetPath;
        this.relativePath = normalizePath(path);
        this.directoryHint = directory;
        this.setPermissionsForNew = setPermissionsForNew;
        this.basePath = basePath;
        this.storage = storage;
        init();
    }

    private String normalizePath(final String path) {
        if (!path.startsWith("/")) {
            return "/"+path;
        } else {
            String tmpPath = path;
            while (tmpPath.startsWith("//")) {
                tmpPath = tmpPath.substring(1);
            }
            return tmpPath;
        }
    }

    private void init() {

        if (setPermissionsForNew) {
            try {
                supportsAcl = Files.getFileStore(assetPath.getRoot()).supportsFileAttributeView(AclFileAttributeView.class);
            } catch (IOException e) {
                log.error("Could not check filesystem capabilities {}", e.getMessage());
            }
            try {
                supportsPosix = Files.getFileStore(assetPath.getRoot()).supportsFileAttributeView(PosixFileAttributeView.class);
            } catch (IOException e) {
                log.error("Could not check filesystem capabilities {}", e.getMessage());
            }

            if (supportsAcl) {
                AclFileAttributeView aclView = Files.getFileAttributeView(assetPath.getParent(), AclFileAttributeView.class);
                UserPrincipal owner = null;
                try {
                    owner = aclView.getOwner();
                    setDefaultFileAcls(processPermissions(owner, DEFAULT_ACL_FILE_PERMISSIONS));
                    setDefaultDirectoryAcls(processPermissions(owner, DEFAULT_ACL_DIR_PERMISSIONS));

                } catch (IOException e) {
                    supportsAcl = false;
                }


            }
        }
    }

    private List<AclEntry> processPermissions(UserPrincipal owner, AclEntryPermission[] defaultAclFilePermissions) {
        AclEntry.Builder aclBuilder = AclEntry.newBuilder();
        aclBuilder.setPermissions(defaultAclFilePermissions);
        aclBuilder.setType(AclEntryType.ALLOW);
        aclBuilder.setPrincipal(owner);
        ArrayList<AclEntry> aclList = new ArrayList<>();
        aclList.add(aclBuilder.build());
        return aclList;
    }


    @Override
    public RepositoryStorage getStorage( )
    {
        return storage;
    }

    @Override
    public String getPath() {
        return relativePath;
    }

    @Override
    public String getName() {
        return assetPath.getFileName().toString();
    }

    @Override
    public Instant getModificationTime() {
        try {
            return Files.getLastModifiedTime(assetPath).toInstant();
        } catch (IOException e) {
            log.error("Could not read modification time of {}", assetPath);
            return Instant.now();
        }
    }

    /**
     * Returns true, if the path of this asset points to a directory
     *
     * @return
     */
    @Override
    public boolean isContainer() {
        if (Files.exists(assetPath)) {
            return Files.isDirectory(assetPath);
        } else {
            return directoryHint;
        }
    }

    @Override
    public boolean isLeaf( )
    {
        if (Files.exists( assetPath )) {
            return Files.isRegularFile( assetPath );
        } else {
            return !directoryHint;
        }
    }

    /**
     * Returns the list of directory entries, if this asset represents a directory.
     * Otherwise a empty list will be returned.
     *
     * @return The list of entries in the directory, if it exists.
     */
    @Override
    public List<StorageAsset> list() {
        try {
            return Files.list(assetPath).map(p -> new FilesystemAsset(storage, relativePath + "/" + p.getFileName().toString(), assetPath.resolve(p), this.basePath))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Returns the size of the represented file. If it cannot be determined, -1 is returned.
     *
     * @return
     */
    @Override
    public long getSize() {
        try {
            return Files.size(assetPath);
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * Returns a input stream to the underlying file, if it exists. The caller has to make sure, that
     * the stream is closed after it was used.
     *
     * @return
     * @throws IOException
     */
    @Override
    public InputStream getReadStream() throws IOException {
        if (isContainer()) {
            throw new IOException("Can not create input stream for container");
        }
        return Files.newInputStream(assetPath);
    }

    @Override
    public ReadableByteChannel getReadChannel( ) throws IOException
    {
        return FileChannel.open( assetPath, StandardOpenOption.READ );
    }

    private OpenOption[] getOpenOptions(boolean replace) {
        return replace ? REPLACE_OPTIONS : APPEND_OPTIONS;
    }

    @Override
    public OutputStream getWriteStream( boolean replace) throws IOException {
        OpenOption[] options = getOpenOptions( replace );
        if (!Files.exists( assetPath )) {
            create();
        }
        return Files.newOutputStream(assetPath, options);
    }

    @Override
    public WritableByteChannel getWriteChannel( boolean replace ) throws IOException
    {
        OpenOption[] options = getOpenOptions( replace );
        return FileChannel.open( assetPath, options );
    }

    @Override
    public boolean replaceDataFromFile( Path newData) throws IOException {
        final boolean createNew = !Files.exists(assetPath);
        Path backup = null;
        if (!createNew) {
            backup = findBackupFile(assetPath);
        }
        try {
            if (!createNew) {
                Files.move(assetPath, backup);
            }
            Files.move(newData, assetPath, StandardCopyOption.REPLACE_EXISTING);
            applyDefaultPermissions(assetPath);
            return true;
        } catch (IOException e) {
            log.error("Could not overwrite file {}", assetPath);
            // Revert if possible
            if (backup != null && Files.exists(backup)) {
                Files.move(backup, assetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            throw e;
        } finally {
            if (backup != null) {
                try {
                    Files.deleteIfExists(backup);
                } catch (IOException e) {
                    log.error("Could not delete backup file {}", backup);
                }
            }
        }

    }

    private void applyDefaultPermissions(Path filePath) {
        try {
            if (supportsPosix) {
                Set<PosixFilePermission> perms;
                if (Files.isDirectory(filePath)) {
                    perms = defaultPosixFilePermissions;
                } else {
                    perms = defaultPosixDirectoryPermissions;
                }
                Files.setPosixFilePermissions(filePath, perms);
            } else if (supportsAcl) {
                List<AclEntry> perms;
                if (Files.isDirectory(filePath)) {
                    perms = getDefaultDirectoryAcls();
                } else {
                    perms = getDefaultFileAcls();
                }
                AclFileAttributeView aclAttr = Files.getFileAttributeView(filePath, AclFileAttributeView.class);
                aclAttr.setAcl(perms);
            }
        } catch (IOException e) {
            log.error("Could not set permissions for {}: {}", filePath, e.getMessage());
        }
    }

    private Path findBackupFile(Path file) {
        String ext = ".bak";
        Path backupPath = file.getParent().resolve(file.getFileName().toString() + ext);
        int idx = 0;
        while (Files.exists(backupPath)) {
            backupPath = file.getParent().resolve(file.getFileName().toString() + ext + idx++);
        }
        return backupPath;
    }

    @Override
    public boolean exists() {
        return Files.exists(assetPath);
    }

    @Override
    public Path getFilePath() throws UnsupportedOperationException {
        return assetPath;
    }

    @Override
    public boolean isFileBased( )
    {
        return true;
    }

    @Override
    public boolean hasParent( )
    {
        if (basePath!=null && assetPath.equals(basePath)) {
                return false;
        }
        return assetPath.getParent()!=null;
    }

    @Override
    public StorageAsset getParent( )
    {
        Path parentPath;
        if (basePath!=null && assetPath.equals( basePath )) {
            parentPath=null;
        } else
        {
            parentPath = assetPath.getParent( );
        }
        String relativeParent = StringUtils.substringBeforeLast( relativePath,"/");
        if (parentPath!=null) {
            return new FilesystemAsset(storage, relativeParent, parentPath, basePath, true, setPermissionsForNew );
        } else {
            return null;
        }
    }

    @Override
    public StorageAsset resolve(String toPath) {
        return storage.getAsset(this.getPath()+"/"+toPath);
    }


    public void setDefaultFileAcls(List<AclEntry> acl) {
        defaultFileAcls = acl;
    }

    public List<AclEntry> getDefaultFileAcls() {
        return defaultFileAcls;
    }

    public void setDefaultPosixFilePermissions(Set<PosixFilePermission> perms) {
        defaultPosixFilePermissions = perms;
    }

    public Set<PosixFilePermission> getDefaultPosixFilePermissions() {
        return defaultPosixFilePermissions;
    }

    public void setDefaultDirectoryAcls(List<AclEntry> acl) {
        defaultDirectoryAcls = acl;
    }

    public List<AclEntry> getDefaultDirectoryAcls() {
        return defaultDirectoryAcls;
    }

    public void setDefaultPosixDirectoryPermissions(Set<PosixFilePermission> perms) {
        defaultPosixDirectoryPermissions = perms;
    }

    public Set<PosixFilePermission> getDefaultPosixDirectoryPermissions() {
        return defaultPosixDirectoryPermissions;
    }

    @Override
    public void create() throws IOException {
        if (!Files.exists(assetPath)) {
            if (directoryHint) {
                Files.createDirectories(assetPath);
            } else {
                if (!Files.exists( assetPath.getParent() )) {
                    Files.createDirectories( assetPath.getParent( ) );
                }
                Files.createFile(assetPath);
            }
            if (setPermissionsForNew) {
                applyDefaultPermissions(assetPath);
            }
        }
    }

    @Override
    public String toString() {
        return relativePath+":"+assetPath;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof FilesystemAsset) {
            if (this.getPath()!=null) {
                return this.getPath().compareTo(((FilesystemAsset) o).getPath());
            } else {
                return 0;
            }
        }
        return 0;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;

        FilesystemAsset that = (FilesystemAsset) o;

        if ( !assetPath.equals( that.assetPath ) ) return false;
        return storage.equals( that.storage );
    }

    @Override
    public int hashCode( )
    {
        int result = assetPath.hashCode( );
        result = 31 * result + storage.hashCode( );
        return result;
    }
}
