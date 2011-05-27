package org.apache.maven.archiva.proxy;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
public class WagonFactoryException
    extends Exception
{
    public WagonFactoryException( String message, Throwable e )
    {
        super( message, e );
    }
}
