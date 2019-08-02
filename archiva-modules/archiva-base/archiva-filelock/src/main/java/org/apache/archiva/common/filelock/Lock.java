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

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Olivier Lamy
 * @since 2.0.0
 */
public class Lock implements Closeable
{
    private Path file;

    private AtomicBoolean write;

    private final Map<Thread, AtomicInteger> fileClients = new HashMap<>();

    private FileLock fileLock;

    private FileChannel fileChannel;

    public Lock( Path file )
    {
        this.file = file;
    }

    public Lock( Path file, boolean write )
            throws IOException
    {
        this.file = file;
        this.write = new AtomicBoolean( write );
        fileChannel = write ? FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.READ) : FileChannel.open(file, StandardOpenOption.READ);
    }

    public Path getFile()
    {
        return file;
    }

    public AtomicBoolean isWrite()
    {
        return write;
    }

    public void setFile( Path file )
    {
        this.file = file;
    }

    public void setWrite( boolean write )
    {
        this.write.set( write );
    }

    public boolean isShared()
    {
        return this.fileLock.isValid() && this.fileLock.isShared();
    }

    public boolean isValid()
    {
        return this.fileLock!=null && this.fileLock.isValid();
    }

    public Map<Thread, AtomicInteger> getFileClients()
    {
        return fileClients;
    }

    public void addFileClient( Thread thread )
    {
        this.fileClients.put( thread, new AtomicInteger( 1 ) );
    }

    public boolean removeFileClient( Thread thread )
    {
        return this.fileClients.remove( thread ) != null;
    }

    public void close()
        throws IOException
    {
        IOException ioException = null;
        try
        {
            if (this.fileLock!=null) {
                this.fileLock.release();
            }
        }
        catch ( IOException e )
        {
            ioException = e;
        } finally {
            closeQuietly( fileChannel );
            fileClients.remove( Thread.currentThread() );
        }

        if ( ioException != null )
        {
            throw ioException;
        }

    }

    protected void openLock( boolean write, boolean timeout )
        throws IOException
    {
        fileClients.put( Thread.currentThread(), new AtomicInteger( 1 ) );

        this.fileLock = timeout
            ? fileChannel.tryLock( 0L, Long.MAX_VALUE, write ? false : true )
            : fileChannel.lock( 0L, Long.MAX_VALUE, write ? false : true );

    }



    private void closeQuietly( Closeable closeable )
    {
        try
        {
            closeable.close();
        }
        catch ( IOException e )
        {
            // ignore
        }
    }


    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder( "Lock{" );
        sb.append( "file=" ).append( file );
        sb.append( '}' );
        return sb.toString();
    }
}
