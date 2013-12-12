package org.apache.archiva.common.filelock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Olivier Lamy
 */
public class Lock
{
    private File file;

    private AtomicBoolean write;

    private final Map<Thread, AtomicInteger> fileClients = new HashMap<Thread, AtomicInteger>();

    private FileLock fileLock;

    public Lock( File file, boolean write )
        throws FileNotFoundException, IOException
    {
        this.file = file;
        this.write = new AtomicBoolean( write );
        this.openLock( write );
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

    public FileLock getFileLock()
    {
        return fileLock;
    }

    public void setFileLock( FileLock fileLock )
    {
        this.fileLock = fileLock;
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
        if ( this.write.get() )
        {
            this.fileLock.release();
            fileClients.remove( Thread.currentThread() );
        }
    }

    public void openLock( boolean write )
        throws IOException
    {
        fileClients.put( Thread.currentThread(), new AtomicInteger( 1 ) );
        RandomAccessFile raf = new RandomAccessFile( file, write ? "rw" : "r" );
        this.fileLock = raf.getChannel().lock( 1, 1, !write );
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
