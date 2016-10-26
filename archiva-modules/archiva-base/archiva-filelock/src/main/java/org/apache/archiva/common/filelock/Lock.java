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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Olivier Lamy
 * @since 2.0.0
 */
public class Lock
{
    private File file;

    private AtomicBoolean write;

    private final Map<Thread, AtomicInteger> fileClients = new HashMap<>();

    private FileLock fileLock;

    private RandomAccessFile randomAccessFile;

    private FileChannel fileChannel;

    public Lock( File file )
    {
        this.file = file;
    }

    public Lock( File file, boolean write )
        throws FileNotFoundException
    {
        this.file = file;
        this.write = new AtomicBoolean( write );
        randomAccessFile = new RandomAccessFile( file, write ? "rw" : "r" );
        fileChannel = randomAccessFile.getChannel();
    }

    public File getFile()
    {
        return file;
    }

    public AtomicBoolean isWrite()
    {
        return write;
    }

    public void setFile( File file )
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

    protected void close()
        throws IOException
    {
        IOException ioException = null;
        try
        {
            this.fileLock.release();
        }
        catch ( IOException e )
        {
            ioException = e;
        }

        closeQuietly( fileChannel );
        closeQuietly( randomAccessFile );

        fileClients.remove( Thread.currentThread() );

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

    protected RandomAccessFile getRandomAccessFile()
    {
        return randomAccessFile;
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
