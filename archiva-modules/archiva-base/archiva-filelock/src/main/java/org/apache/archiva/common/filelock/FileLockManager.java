package org.apache.archiva.common.filelock;

import java.io.File;

/**
 * @author Olivier Lamy
 */
public interface FileLockManager
{
    Lock writeFileLock( File file )
        throws FileLockException;

    Lock readFileLock( File file )
        throws FileLockException;

    void release( Lock lock )
        throws FileLockException;
}
