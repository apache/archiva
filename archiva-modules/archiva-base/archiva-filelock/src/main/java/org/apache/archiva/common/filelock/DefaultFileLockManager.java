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

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Olivier Lamy
 * @since 2.0.0
 */
@Service("fileLockManager#default")
public class DefaultFileLockManager
    implements FileLockManager
{
    // TODO currently we create lock for read and write!!
    // the idea could be to store lock here with various clients read/write
    // only read could be a more simple lock and acquire a write lock means waiting the end of all reading threads
    private static final ConcurrentMap<Path, Lock> lockFiles = new ConcurrentHashMap<Path, Lock>( 64 );

    private boolean skipLocking = true;

    private Logger log = LoggerFactory.getLogger( getClass() );

    private int timeout = 0;


    @Override
    public Lock readFileLock( Path file )
        throws FileLockException, FileLockTimeoutException
    {
        if ( skipLocking )
        {
            return new Lock( file );

        }
        StopWatch stopWatch = new StopWatch();
        boolean acquired = false;
        try {
            mkdirs(file.getParent());
        } catch (IOException e) {
            throw new FileLockException("Could not create directories "+file.getParent(), e);
        }

        Lock lock = null;

        stopWatch.start();

        while ( !acquired )
        {
            // Make sure that not a bad lock is returned, if a exception was thrown.
            lock = null;

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

            Lock current = lockFiles.get( file );

            if ( current != null )
            {
                log.trace( "read lock file exist continue wait" );
                continue;
            }

            try
            {
                lock = new Lock( file, false );
                createNewFileQuietly( file );
                lock.openLock( false, timeout > 0 );
                // We are not returning an existing lock. If the lock is not
                // exclusive, another thread may release the lock and the client
                // knows nothing about it.
                // The only atomic operation is the putIfAbsent operation, so if
                // this returns null everything is OK, otherwise we should start at
                // the beginning.
                current = lockFiles.putIfAbsent( file, lock );
                if ( current == null )
                {
                    // Success
                    acquired = true;
                } else {
                    // We try again
                    lock.close();
                    lock=null;
                }
            }
            catch ( FileNotFoundException | NoSuchFileException e )
            {

                log.debug( "read Lock skip: {} try to create file", e.getMessage() );
                createNewFileQuietly( file );
            }
            catch ( IOException e )
            {
                throw new FileLockException( e.getMessage(), e );
            }
            catch ( IllegalStateException e )
            {
                log.trace( "openLock {}:{}", e.getClass(), e.getMessage() );
            }
        }

        return lock;

    }


    @Override
    public Lock writeFileLock( Path file )
        throws FileLockException, FileLockTimeoutException
    {
        if ( skipLocking )
        {
            return new Lock( file );
        }

        try {
            mkdirs( file.getParent() );
        } catch (IOException e) {
            throw new FileLockException("Could not create directory "+file.getParent(), e);
        }

        StopWatch stopWatch = new StopWatch();
        boolean acquired = false;

        Lock lock = null;

        stopWatch.start();

        while ( !acquired )
        {
            // Make sure that not a bad lock is returned, if a exception was thrown.
            lock = null;
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

            Lock current = lockFiles.get( file );

            try
            {

                if ( current != null )
                {
                    log.trace( "write lock file exist continue wait" );

                    continue;
                }
                lock = new Lock( file, true );
                createNewFileQuietly( file );
                lock.openLock( true, timeout > 0 );
                // We are not returning an existing lock. If the lock is not
                // exclusive, another thread may release the lock and the client
                // knows nothing about it.
                // The only atomic operation is the putIfAbsent operation, so if
                // this returns null everything is OK, otherwise we should start at
                // the beginning.
                current = lockFiles.putIfAbsent( file, lock );
                if ( current == null )
                {
                    // Success
                    acquired = true;
                } else {
                    // We try again
                    lock.close();
                    lock=null;
                }
            }
            catch ( FileNotFoundException | NoSuchFileException e )
            {

                log.debug( "write Lock skip: {} try to create file", e.getMessage() );
                createNewFileQuietly( file );
            }
            catch ( IOException e )
            {
                throw new FileLockException( e.getMessage(), e );
            }
            catch ( IllegalStateException e )
            {
                log.trace( "openLock {}:{}", e.getClass(), e.getMessage() );
            }
        }

        return lock;


    }

     private void createNewFileQuietly( Path file )
    {
        try
        {
            Files.createFile(file);
        }
        catch ( IOException e )
        {
            // skip that
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
            lockFiles.remove( lock.getFile() );
            lock.close();
        }
        catch ( ClosedChannelException e )
        {
            // skip this one
            log.debug( "ignore ClosedChannelException: {}", e.getMessage() );
        }
        catch ( IOException e )
        {
            throw new FileLockException( e.getMessage(), e );
        }
    }

    @Override
    public void clearLockFiles()
    {
        lockFiles.clear();
    }

    private Path mkdirs( Path directory ) throws IOException {
        return Files.createDirectories(directory);
    }

    @Override
    public int getTimeout()
    {
        return timeout;
    }

    @Override
    public void setTimeout( int timeout )
    {
        this.timeout = timeout;
    }

    @Override
    public boolean isSkipLocking()
    {
        return skipLocking;
    }

    @Override
    public void setSkipLocking( boolean skipLocking )
    {
        this.skipLocking = skipLocking;
    }
}
