package org.apache.archiva.metadata.repository.storage.maven2;

import org.apache.commons.io.FileUtils;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.SessionListener;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MockWagon
    implements Wagon
{
    public void get( String s, File file )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        String sourceFile = getBasedir() + "/target/test-classes/" + s;

        try
        {
            FileUtils.copyFile( new File( sourceFile ), file );
            assert( file.exists() );
        }
        catch( IOException e )
        {
            throw new ResourceDoesNotExistException( e.getMessage() );            
        }
    }

    public boolean getIfNewer( String s, File file, long l )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        return false;
    }

    public void put( File file, String s )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        
    }

    public void putDirectory( File file, String s )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {

    }

    public boolean resourceExists( String s )
        throws TransferFailedException, AuthorizationException
    {
        return false;
    }

    public List getFileList( String s )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        return null;
    }

    public boolean supportsDirectoryCopy()
    {
        return false;
    }

    public Repository getRepository()
    {
        return null;
    }

    public void connect( Repository repository )
        throws ConnectionException, AuthenticationException
    {

    }

    public void connect( Repository repository, ProxyInfo proxyInfo )
        throws ConnectionException, AuthenticationException
    {

    }

    public void connect( Repository repository, ProxyInfoProvider proxyInfoProvider )
        throws ConnectionException, AuthenticationException
    {

    }

    public void connect( Repository repository, AuthenticationInfo authenticationInfo )
        throws ConnectionException, AuthenticationException
    {

    }

    public void connect( Repository repository, AuthenticationInfo authenticationInfo, ProxyInfo proxyInfo )
        throws ConnectionException, AuthenticationException
    {

    }

    public void connect( Repository repository, AuthenticationInfo authenticationInfo,
                         ProxyInfoProvider proxyInfoProvider )
        throws ConnectionException, AuthenticationException
    {

    }

    public void openConnection()
        throws ConnectionException, AuthenticationException
    {

    }

    public void disconnect()
        throws ConnectionException
    {

    }

    public void setTimeout( int i )
    {

    }

    public int getTimeout()
    {
        return 0;
    }

    public void addSessionListener( SessionListener sessionListener )
    {

    }

    public void removeSessionListener( SessionListener sessionListener )
    {

    }

    public boolean hasSessionListener( SessionListener sessionListener )
    {
        return false;
    }

    public void addTransferListener( TransferListener transferListener )
    {

    }

    public void removeTransferListener( TransferListener transferListener )
    {

    }

    public boolean hasTransferListener( TransferListener transferListener )
    {
        return false;
    }

    public boolean isInteractive()
    {
        return false;
    }

    public void setInteractive( boolean b )
    {

    }

    public String getBasedir()
    {
        String basedir = System.getProperty( "basedir" );

        if ( basedir == null )
        {
            basedir = new File( "" ).getAbsolutePath();
        }

        return basedir;
    }
}
