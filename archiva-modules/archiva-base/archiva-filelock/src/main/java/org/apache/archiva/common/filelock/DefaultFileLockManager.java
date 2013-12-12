package org.apache.archiva.common.filelock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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

    private int timeout = 0;

    @Override
    public Lock readFileLock( File file )
        throws FileLockException, FileNotFoundException
    {
        if ( skipLocking )
        {
            return new Lock( file );

        }
        StopWatch stopWatch = new StopWatch();
        boolean acquired = false;

        Lock lock = new Lock( file, false );

        stopWatch.start();

        while ( !acquired )
        {
            if ( timeout > 0 )
            {
                long delta = stopWatch.getTotalTimeMillis();
                if ( delta > timeout )
                {
                    log.warn( "Cannot acquire read lock within {} millis. Will skip the file: {}", timeout, file );
                    // we could not get the lock within the timeout period, so return null
                    return null;
                }
            }
            try
            {
                lock.openLock( false, timeout > 0 );
                acquired = true;
            }
            catch ( IOException e )
            {
                throw new FileLockException( e.getMessage(), e );
            }
            catch ( IllegalStateException e )
            {
                log.debug( "openLock {}:{}", e.getClass(), e.getMessage() );
            }
        }
        return lock;
    }


    @Override
    public Lock writeFileLock( File file )
        throws FileLockException, FileNotFoundException
    {
        if ( skipLocking )
        {
            return new Lock( file );
        }

        StopWatch stopWatch = new StopWatch();
        boolean acquired = false;

        Lock lock = new Lock( file, true );

        stopWatch.start();

        while ( !acquired )
        {
            if ( timeout > 0 )
            {
                long delta = stopWatch.getTotalTimeMillis();
                if ( delta > timeout )
                {
                    log.warn( "Cannot acquire read lock within {} millis. Will skip the file: {}", timeout, file );
                    // we could not get the lock within the timeout period, so return null
                    return null;
                }
            }
            try
            {
                lock.openLock( true, timeout > 0 );
                acquired = true;
            }
            catch ( IOException e )
            {
                throw new FileLockException( e.getMessage(), e );
            }
            catch ( IllegalStateException e )
            {
                log.debug( "openLock {}:{}", e.getClass(), e.getMessage() );
            }
        }
        return lock;

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
            lock.close();
        }
        catch ( IOException e )
        {
            throw new FileLockException( e.getMessage(), e );
        }
    }

    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout( int timeout )
    {
        this.timeout = timeout;
    }


}
