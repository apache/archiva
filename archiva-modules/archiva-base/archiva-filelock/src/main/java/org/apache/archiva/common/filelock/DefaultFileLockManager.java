package org.apache.archiva.common.filelock;

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

import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Olivier Lamy
 */
@Service( "fileLockManager#default" )
public class DefaultFileLockManager
    implements FileLockManager
{
    // TODO currently we create lock for read and write!!
    // the idea could be to store lock here with various clients read/write
    // only read could be a more simple lock and acquire a write lock means waiting the end of all reading threads
    //private static final ConcurrentMap<File, Lock> lockFiles = new ConcurrentHashMap<File, Lock>( 64 );

    private boolean skipLocking = false;

    private Logger log = LoggerFactory.getLogger( getClass() );

    private int timeout = 0;


    @Override
    public Lock readFileLock( File file )
        throws FileLockException, FileLockTimeoutException
    {
        if ( skipLocking )
        {
            return new Lock( file );

        }
        StopWatch stopWatch = new StopWatch();
        boolean acquired = false;
        mkdirs( file.getParentFile() );
        try
        {
            Lock lock = new Lock( file, false );

            stopWatch.start();

            while ( !acquired )
            {
                if ( timeout > 0 )
                {
                    long delta = stopWatch.getTime();
                    log.debug( "delta {}, timeout {}", delta, timeout );
                    if ( delta > timeout )
                    {
                        log.warn( "Cannot acquire read lock within {} millis. Will skip the file: {}", timeout, file );
                        // we could not get the lock within the timeout period, so  throw  FileLockTimeoutException
                        throw new FileLockTimeoutException();
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
        catch ( FileNotFoundException e )
        {
            throw new FileLockException( e.getMessage(), e );
        }
    }


    @Override
    public Lock writeFileLock( File file )
        throws FileLockException, FileLockTimeoutException
    {
        if ( skipLocking )
        {
            return new Lock( file );
        }

        mkdirs( file.getParentFile() );

        StopWatch stopWatch = new StopWatch();
        boolean acquired = false;

        try
        {
            Lock lock = new Lock( file, true );

            stopWatch.start();

            while ( !acquired )
            {
                if ( timeout > 0 )
                {
                    long delta = stopWatch.getTime();
                    log.debug( "delta {}, timeout {}", delta, timeout );
                    if ( delta > timeout )
                    {
                        log.warn( "Cannot acquire read lock within {} millis. Will skip the file: {}", timeout, file );
                        // we could not get the lock within the timeout period, so throw FileLockTimeoutException
                        throw new FileLockTimeoutException();
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
        catch ( FileNotFoundException e )
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
            lock.close();
        }
        catch ( IOException e )
        {
            throw new FileLockException( e.getMessage(), e );
        }
    }

    private boolean mkdirs( File directory )
    {
        if ( directory == null )
        {
            return false;
        }

        if ( directory.exists() )
        {
            return false;
        }
        if ( directory.mkdir() )
        {
            return true;
        }

        File canonDir = null;
        try
        {
            canonDir = directory.getCanonicalFile();
        }
        catch ( IOException e )
        {
            return false;
        }

        File parentDir = canonDir.getParentFile();
        return ( parentDir != null && ( mkdirs( parentDir ) || parentDir.exists() ) && canonDir.mkdir() );
    }

    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout( int timeout )
    {
        this.timeout = timeout;
    }

    public boolean isSkipLocking()
    {
        return skipLocking;
    }

    public void setSkipLocking( boolean skipLocking )
    {
        this.skipLocking = skipLocking;
    }
}
