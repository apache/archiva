package org.apache.archiva.common.filelock;

/**
 * @author Olivier Lamy
 */
public class FileLockException
    extends Exception
{
    public FileLockException( String s, Throwable throwable )
    {
        super( s, throwable );
    }
}
