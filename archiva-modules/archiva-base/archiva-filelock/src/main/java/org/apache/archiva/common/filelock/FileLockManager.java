package org.apache.archiva.common.filelock;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * @author Olivier Lamy
 */
public interface FileLockManager
{
    Lock writeFileLock( File file )
        throws FileLockException, FileNotFoundException;

    Lock readFileLock( File file )
        throws FileLockException, FileNotFoundException;

    void release( Lock lock )
        throws FileLockException, FileNotFoundException;

    int getTimeout();

    void setTimeout( int timeout );
}
