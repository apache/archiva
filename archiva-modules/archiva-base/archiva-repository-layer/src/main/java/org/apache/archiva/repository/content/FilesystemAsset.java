package org.apache.archiva.repository.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class FilesystemAsset implements StorageAsset
{

    private final static Logger log = LoggerFactory.getLogger( FilesystemAsset.class );

    private final Path basePath;
    private final Path assetPath;
    private final Path completeAssetPath;

    public String DEFAULT_POSIX_FILE_PERMS = "rw-rw----";
    public String DEFAULT_POSIX_DIR_PERMS = "rwxrwx---";

    List<AclEntry> defaultFileAcls;
    Set<PosixFilePermission> defaultPosixFilePermissions;
    List<AclEntry> defaultDirectoryAcls;
    Set<PosixFilePermission> defaultPosixDirectoryPermissions;

    boolean supportsAcl = false;
    boolean supportsPosix = false;

    boolean directory = false;

    public FilesystemAsset( Path basePath, String assetPath )
    {
        this.basePath = basePath;
        this.assetPath = Paths.get( assetPath );
        this.completeAssetPath = basePath.resolve( assetPath ).toAbsolutePath( );
        init( );
    }

    public FilesystemAsset( Path basePath, String assetPath, boolean directory )
    {
        this.basePath = basePath;
        this.assetPath = Paths.get( assetPath );
        this.completeAssetPath = basePath.resolve( assetPath ).toAbsolutePath( );
        this.directory = directory;
        init( );
    }

    private void init( )
    {
        defaultFileAcls = new ArrayList<>( );
        AclEntry.Builder aclBuilder = AclEntry.newBuilder( );
        aclBuilder.setPermissions( AclEntryPermission.DELETE, AclEntryPermission.READ_ACL, AclEntryPermission.READ_ATTRIBUTES, AclEntryPermission.READ_DATA, AclEntryPermission.WRITE_ACL,
            AclEntryPermission.WRITE_ATTRIBUTES, AclEntryPermission.WRITE_DATA, AclEntryPermission.APPEND_DATA );
        aclBuilder.setType( AclEntryType.ALLOW );
        defaultFileAcls.add( aclBuilder.build( ) );
        AclEntry.Builder aclDirBuilder = AclEntry.newBuilder( );
        aclDirBuilder.setPermissions( AclEntryPermission.ADD_FILE, AclEntryPermission.ADD_SUBDIRECTORY, AclEntryPermission.DELETE_CHILD,
            AclEntryPermission.DELETE, AclEntryPermission.READ_ACL, AclEntryPermission.READ_ATTRIBUTES, AclEntryPermission.READ_DATA, AclEntryPermission.WRITE_ACL,
            AclEntryPermission.WRITE_ATTRIBUTES, AclEntryPermission.WRITE_DATA, AclEntryPermission.APPEND_DATA );
        aclDirBuilder.setType( AclEntryType.ALLOW );
        defaultDirectoryAcls.add( aclDirBuilder.build( ) );

        defaultPosixFilePermissions = PosixFilePermissions.fromString( DEFAULT_POSIX_FILE_PERMS );
        defaultPosixDirectoryPermissions = PosixFilePermissions.fromString( DEFAULT_POSIX_DIR_PERMS );

        try
        {
            supportsAcl = Files.getFileStore( completeAssetPath ).supportsFileAttributeView( AclFileAttributeView.class );
        }
        catch ( IOException e )
        {
            log.error( "Could not check filesystem capabilities {}", e.getMessage( ) );
        }
        try
        {
            supportsPosix = Files.getFileStore( completeAssetPath ).supportsFileAttributeView( PosixFileAttributeView.class );
        }
        catch ( IOException e )
        {
            log.error( "Could not check filesystem capabilities {}", e.getMessage( ) );
        }

    }


    @Override
    public String getPath( )
    {
        return assetPath.toString( );
    }

    @Override
    public String getName( )
    {
        return assetPath.getFileName( ).toString( );
    }

    @Override
    public Instant getModificationTime( )
    {
        try
        {
            return Files.getLastModifiedTime( completeAssetPath ).toInstant( );
        }
        catch ( IOException e )
        {
            log.error( "Could not read modification time of {}", completeAssetPath );
            return Instant.now( );
        }
    }

    @Override
    public boolean isContainer( )
    {
        return Files.isDirectory( completeAssetPath );
    }

    @Override
    public List<StorageAsset> list( )
    {
        try
        {
            return Files.list( completeAssetPath ).map( p -> new FilesystemAsset( basePath, basePath.relativize( p ).toString( ) ) )
                .collect( Collectors.toList( ) );
        }
        catch ( IOException e )
        {
            return Collections.EMPTY_LIST;
        }
    }

    @Override
    public long getSize( )
    {
        try
        {
            return Files.size( completeAssetPath );
        }
        catch ( IOException e )
        {
            return -1;
        }
    }

    @Override
    public InputStream getData( ) throws IOException
    {
        return Files.newInputStream( completeAssetPath );
    }

    @Override
    public OutputStream writeData( boolean replace ) throws IOException
    {
        OpenOption[] options;
        if ( replace )
        {
            options = new OpenOption[]{StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE};
        }
        else
        {
            options = new OpenOption[]{StandardOpenOption.APPEND};
        }
        return Files.newOutputStream( completeAssetPath, options );
    }

    @Override
    public boolean storeDataFile( Path newData ) throws IOException
    {
        final boolean createNew = !Files.exists( completeAssetPath );
        Path backup = null;
        if ( !createNew )
        {
            backup = findBackupFile( completeAssetPath );
        }
        try
        {
            if ( !createNew )
            {
                Files.move( completeAssetPath, backup );
            }
            Files.move( newData, completeAssetPath, StandardCopyOption.REPLACE_EXISTING );
            setDefaultPermissions( completeAssetPath );
            return true;
        }
        catch ( IOException e )
        {
            log.error( "Could not overwrite file {}", completeAssetPath );
            // Revert if possible
            if ( backup != null && Files.exists( backup ) )
            {
                Files.move( backup, completeAssetPath, StandardCopyOption.REPLACE_EXISTING );
            }
            throw e;
        }
        finally
        {
            if ( backup != null )
            {
                try
                {
                    Files.deleteIfExists( backup );
                }
                catch ( IOException e )
                {
                    log.error( "Could not delete backup file {}", backup );
                }
            }
        }

    }

    private void setDefaultPermissions(Path filePath) {
        try
        {
            if ( supportsPosix )
            {
                Set<PosixFilePermission> perms;
                if ( Files.isDirectory( filePath ) )
                {
                    perms = defaultPosixFilePermissions;
                }
                else
                {
                    perms = defaultPosixDirectoryPermissions;
                }
                Files.setPosixFilePermissions( filePath, perms );
            }
            else if ( supportsAcl )
            {
                List<AclEntry> perms;
                if ( Files.isDirectory( filePath ) )
                {
                    perms = defaultDirectoryAcls;
                }
                else
                {
                    perms = defaultFileAcls;
                }
                AclFileAttributeView aclAttr = Files.getFileAttributeView( filePath, AclFileAttributeView.class );
                aclAttr.setAcl( perms );
            }
        } catch (IOException e) {
            log.error("Could not set permissions for {}: {}", filePath, e.getMessage());
        }
    }

    private Path findBackupFile( Path file )
    {
        String ext = ".bak";
        Path backupPath = file.getParent( ).resolve( file.getFileName( ).toString( ) + ext );
        int idx = 0;
        while ( Files.exists( backupPath ) )
        {
            backupPath = file.getParent( ).resolve( file.getFileName( ).toString( ) + ext + idx++ );
        }
        return backupPath;
    }

    @Override
    public boolean exists( )
    {
        return Files.exists( completeAssetPath );
    }

    @Override
    public Path getFilePath( ) throws UnsupportedOperationException
    {
        return completeAssetPath;
    }


    public void setDefaultFileAcls( List<AclEntry> acl )
    {
        defaultFileAcls = acl;
    }

    public List<AclEntry> getDefaultFileAcls( )
    {
        return defaultFileAcls;
    }

    public void setDefaultPosixFilePermissions( Set<PosixFilePermission> perms )
    {
        defaultPosixFilePermissions = perms;
    }

    public Set<PosixFilePermission> getDefaultPosixFilePermissions( )
    {
        return defaultPosixFilePermissions;
    }

    public void setDefaultDirectoryAcls( List<AclEntry> acl )
    {
        defaultDirectoryAcls = acl;
    }

    public List<AclEntry> getDefaultDirectoryAcls( )
    {
        return defaultDirectoryAcls;
    }

    public void setDefaultPosixDirectoryPermissions( Set<PosixFilePermission> perms )
    {
        defaultPosixDirectoryPermissions = perms;
    }

    public Set<PosixFilePermission> getDefaultPosixDirectoryPermissions( )
    {
        return defaultPosixDirectoryPermissions;
    }

    @Override
    public void create( ) throws IOException
    {
        if ( !Files.exists( completeAssetPath ) )
        {
            if ( directory )
            {
                Files.createDirectories( completeAssetPath );
            } else {
                Files.createFile( completeAssetPath );
            }
            setDefaultPermissions( completeAssetPath );
        }
    }

    @Override
    public String toString( )
    {
        return assetPath.toString();
    }


}
