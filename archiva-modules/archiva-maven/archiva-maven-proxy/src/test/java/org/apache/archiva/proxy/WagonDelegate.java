package org.apache.archiva.proxy;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A dummy wagon implementation
 */
@Service ("wagon#test")
public class WagonDelegate
    implements Wagon
{
    private Logger log = LoggerFactory.getLogger( WagonDelegate.class );

    private Wagon delegate;

    private String contentToGet;

    @Override
    public void get( String resourceName, File destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        log.debug( ".get({}, {})", resourceName, destination );
        delegate.get( resourceName, destination );
        create( destination.toPath() );
    }

    @Override
    public boolean getIfNewer( String resourceName, File destination, long timestamp )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        log.info( ".getIfNewer({}, {}, {})", resourceName, destination, timestamp );

        boolean result = delegate.getIfNewer( resourceName, destination, timestamp );
        createIfMissing( destination.toPath() );
        return result;
    }

    @Override
    public void put( File source, String destination )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        delegate.put( source, destination );
    }

    @Override
    public void putDirectory( File sourceDirectory, String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        delegate.putDirectory( sourceDirectory, destinationDirectory );
    }

    @Override
    public boolean resourceExists( String resourceName )
        throws TransferFailedException, AuthorizationException
    {
        return delegate.resourceExists( resourceName );
    }

    @SuppressWarnings ("unchecked")
    @Override
    public List<String> getFileList( String destinationDirectory )
        throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException
    {
        return delegate.getFileList( destinationDirectory );
    }

    @Override
    public boolean supportsDirectoryCopy()
    {
        return delegate.supportsDirectoryCopy();
    }

    @Override
    public void setTimeout( int val )
    {
        // ignore
    }

    @Override
    public int getTimeout()
    {
        return 0;
    }

    @Override
    public void setReadTimeout( int timeoutValue )
    {
        // ignore
    }

    @Override
    public int getReadTimeout()
    {
        return 0;
    }

    @Override
    public Repository getRepository()
    {
        return delegate.getRepository();
    }

    @Override
    public void connect( Repository source )
        throws ConnectionException, AuthenticationException
    {
        delegate.connect( source );
    }

    @Override
    public void connect( Repository source, ProxyInfo proxyInfo )
        throws ConnectionException, AuthenticationException
    {
        delegate.connect( source, proxyInfo );
    }

    @Override
    public void connect( Repository source, ProxyInfoProvider proxyInfoProvider )
        throws ConnectionException, AuthenticationException
    {
        delegate.connect( source, proxyInfoProvider );
    }

    @Override
    public void connect( Repository source, AuthenticationInfo authenticationInfo )
        throws ConnectionException, AuthenticationException
    {
        delegate.connect( source, authenticationInfo );
    }

    @Override
    public void connect( Repository source, AuthenticationInfo authenticationInfo, ProxyInfo proxyInfo )
        throws ConnectionException, AuthenticationException
    {
        delegate.connect( source, authenticationInfo, proxyInfo );
    }

    @Override
    public void connect( Repository source, AuthenticationInfo authenticationInfo, ProxyInfoProvider proxyInfoProvider )
        throws ConnectionException, AuthenticationException
    {
        delegate.connect( source, authenticationInfo, proxyInfoProvider );
    }

    @SuppressWarnings ("deprecation")
    @Override
    public void openConnection()
        throws ConnectionException, AuthenticationException
    {
        delegate.openConnection();
    }

    @Override
    public void disconnect()
        throws ConnectionException
    {
        delegate.disconnect();
    }

    @Override
    public void addSessionListener( SessionListener listener )
    {
        delegate.addSessionListener( listener );
    }

    @Override
    public void removeSessionListener( SessionListener listener )
    {
        delegate.removeSessionListener( listener );
    }

    @Override
    public boolean hasSessionListener( SessionListener listener )
    {
        return delegate.hasSessionListener( listener );
    }

    @Override
    public void addTransferListener( TransferListener listener )
    {
        delegate.addTransferListener( listener );
    }

    @Override
    public void removeTransferListener( TransferListener listener )
    {
        delegate.removeTransferListener( listener );
    }

    @Override
    public boolean hasTransferListener( TransferListener listener )
    {
        return delegate.hasTransferListener( listener );
    }

    @Override
    public boolean isInteractive()
    {
        return delegate.isInteractive();
    }

    @Override
    public void setInteractive( boolean interactive )
    {
        delegate.setInteractive( interactive );
    }

    public void setDelegate( Wagon delegate )
    {
        this.delegate = delegate;
    }

    void setContentToGet( String content )
    {
        contentToGet = content;
    }

    private void createIfMissing( Path destination )
    {
        // since the mock won't actually copy a file, create an empty one to simulate file existence
        if ( !Files.exists(destination) )
        {
            create( destination );
        }
    }

    private void create( Path destination )
    {
        try
        {
            Files.createDirectories(destination.getParent());
            if ( contentToGet == null && !Files.exists(destination))
            {
                Files.createFile(destination);
            }
            else if (contentToGet != null)
            {
                org.apache.archiva.common.utils.FileUtils.writeStringToFile(destination, Charset.defaultCharset(), contentToGet);
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }
    }

    public void cleanup() {

    }
}
