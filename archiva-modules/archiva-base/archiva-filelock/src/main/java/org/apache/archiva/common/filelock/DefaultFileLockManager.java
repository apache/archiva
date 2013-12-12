package org.apache.archiva.common.filelock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

import java.nio.channels.OverlappingFileLockException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Olivier Lamy
 */
@Service("fileLockManager#default")
public class DefaultFileLockManager
    implements FileLockManager
{
    private static final ConcurrentMap<File, Lock> lockFiles = new ConcurrentHashMap<File, Lock>( 64 );

    private boolean skipLocking = false;

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Override
    public Lock readFileLock( File file )
        throws FileLockException
    {
        if ( skipLocking )
        {
            try
            {
                return new Lock( file, false );
            }
            catch ( IOException e )
            {
                throw new FileLockException( e.getMessage(), e );
            }
        }
        Lock lock = lockFiles.get( file );
        if ( lock == null )
        {
            try
            {
                lock = new Lock( file, false );
                Lock current = lockFiles.putIfAbsent( file, lock );
                if ( current != null )
                {
                    lock = current;
                }
                return lock;
            }
            catch ( IOException e )
            {
                throw new FileLockException( e.getMessage(), e );
            }
            catch ( OverlappingFileLockException e )
            {
                log.debug( "OverlappingFileLockException: {}", e.getMessage() );
                if ( lock == null )
                {
                    lock = lockFiles.get( file );
                }
            }
        }
        // FIXME add a timeout on getting that!!!
        while ( true )
        {
            log.debug( "wait read lock" );
            synchronized ( lock )
            {
                if ( lock.getFileLock().isShared() || !lock.getFileLock().isValid() )
                {
                    lock.addFileClient( Thread.currentThread() );
                    return lock;
                }
            }
        }
        //return lock;
    }

    @Override
    public Lock writeFileLock( File file )
        throws FileLockException
    {
        try
        {
            if ( skipLocking )
            {
                return new Lock( file, true );
            }

            // FIXME add a timeout on getting that!!!
            while ( true )
            {
                Lock lock = lockFiles.get( file );
                log.debug( "wait write lock" );
                if ( lock != null )
                {
                    synchronized ( lock )
                    {
                        if ( lock.getFileLock().isValid() || lock.getFileClients().size() > 0 )
                        {
                            continue;
                        }
                        return lock;
                    }
                }
                else
                {
                    try
                    {
                        lock = new Lock( file, true );
                    }
                    catch ( OverlappingFileLockException e )
                    {
                        log.debug( "OverlappingFileLockException: {}", e.getMessage() );
                        if ( lock == null )
                        {
                            lock = lockFiles.get( file );
                        }

                        lock = lockFiles.get( file );
                        log.debug( "OverlappingFileLockException get: {}", lock );
                    }
                    Lock current = lockFiles.putIfAbsent( file, lock );
                    if ( current != null )
                    {
                        lock = current;
                    }
                    return lock;
                }
            }

        }
        catch ( IOException e )
        {
            throw new FileLockException( e.getMessage(), e );
        }
    }

    @Override
    public void release( Lock lock )
        throws FileLockException
    {
        if ( lock == null )
        {
            log.debug( "skip releasing null" );
            return;
        }
        if ( skipLocking )
        {
            return;
        }
        try
        {
            if ( lock.isWrite().get() )
            {
                lock.getFileLock().release();
            }
            synchronized ( lock )
            {
                lock.close();
                if ( lock.getFileClients().size() < 1 )
                {
                    lockFiles.remove( lock.getFile() );
                }
            }
        }
        catch ( IOException e )
        {
            throw new FileLockException( e.getMessage(), e );
        }
    }
}
