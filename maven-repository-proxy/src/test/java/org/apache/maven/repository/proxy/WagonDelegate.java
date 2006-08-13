package org.apache.maven.repository.proxy;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.events.SessionListener;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.authorization.AuthorizationException;

import java.io.File;

/**
 * A dummy wagon implementation
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class WagonDelegate
    implements Wagon
{
    private Wagon delegate;

    public void get( String resourceName, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        delegate.get( resourceName, destination );
    }

    public boolean getIfNewer( String resourceName, File destination, long timestamp )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        return delegate.getIfNewer( resourceName, destination, timestamp );
    }

    public void put( File source, String destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        delegate.put( source, destination );
    }

    public void putDirectory( File sourceDirectory, String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        delegate.putDirectory( sourceDirectory, destinationDirectory );
    }

    public boolean supportsDirectoryCopy()
    {
        return delegate.supportsDirectoryCopy();
    }

    public Repository getRepository()
    {
        return delegate.getRepository();
    }

    public void connect( Repository source )
        throws ConnectionException, AuthenticationException
    {
        delegate.connect( source );
    }

    public void connect( Repository source, ProxyInfo proxyInfo )
        throws ConnectionException, AuthenticationException
    {
        delegate.connect( source, proxyInfo );
    }

    public void connect( Repository source, AuthenticationInfo authenticationInfo )
        throws ConnectionException, AuthenticationException
    {
        delegate.connect( source, authenticationInfo );
    }

    public void connect( Repository source, AuthenticationInfo authenticationInfo, ProxyInfo proxyInfo )
        throws ConnectionException, AuthenticationException
    {
        delegate.connect( source, authenticationInfo, proxyInfo );
    }

    public void openConnection()
        throws ConnectionException, AuthenticationException
    {
        delegate.openConnection();
    }

    public void disconnect()
        throws ConnectionException
    {
        delegate.disconnect();
    }

    public void addSessionListener( SessionListener listener )
    {
        delegate.addSessionListener( listener );
    }

    public void removeSessionListener( SessionListener listener )
    {
        delegate.removeSessionListener( listener );
    }

    public boolean hasSessionListener( SessionListener listener )
    {
        return delegate.hasSessionListener( listener );
    }

    public void addTransferListener( TransferListener listener )
    {
        delegate.addTransferListener( listener );
    }

    public void removeTransferListener( TransferListener listener )
    {
        delegate.removeTransferListener( listener );
    }

    public boolean hasTransferListener( TransferListener listener )
    {
        return delegate.hasTransferListener( listener );
    }

    public boolean isInteractive()
    {
        return delegate.isInteractive();
    }

    public void setInteractive( boolean interactive )
    {
        delegate.setInteractive( interactive );
    }

    public void setDelegate( Wagon delegate )
    {
        this.delegate = delegate;
    }
}
