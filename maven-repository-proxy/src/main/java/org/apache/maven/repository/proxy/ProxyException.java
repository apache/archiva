package org.apache.maven.repository.proxy;

/**
 * @author Edwin Punzalan
 */
public class ProxyException
    extends Exception
{
    public ProxyException( String message )
    {
        super( message );
    }

    public ProxyException( String message, Throwable t )
    {
        super( message, t );
    }
}
