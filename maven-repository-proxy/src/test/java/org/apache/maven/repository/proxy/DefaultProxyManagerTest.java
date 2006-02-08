package org.apache.maven.repository.proxy;

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.apache.maven.repository.proxy.configuration.ProxyConfiguration;
import org.apache.maven.wagon.ResourceDoesNotExistException;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Edwin Punzalan
 */
public class DefaultProxyManagerTest
    extends PlexusTestCase
{
    private ProxyManager proxy;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        ProxyManagerFactory factory = (ProxyManagerFactory) container.lookup( ProxyManagerFactory.ROLE );
        proxy = factory.getProxyManager( "default", getTestConfiguration() );
    }

    public void testExceptions()
    {
        proxy.setConfiguration( null );

        try
        {
            proxy.get( "/invalid" );
            fail( "Expected empty configuration error." );
        }
        catch ( ProxyException e )
        {
            assertEquals( "Expected Exception not thrown.", "No proxy configuration defined.", e.getMessage() );
        }
        catch ( ResourceDoesNotExistException e )
        {
            fail( "Expected Exception not thrown." );
        }

        try
        {
            proxy.getRemoteFile( "/invalid" );
            fail( "Expected empty configuration error." );
        }
        catch ( ProxyException e )
        {
            assertEquals( "Expected Exception not thrown.", "No proxy configuration defined.", e.getMessage() );
        }
        catch ( ResourceDoesNotExistException e )
        {
            fail( "Expected Exception not thrown." );
        }
    }

    public void testCache()
    {
        
    }

    protected void tearDown()
        throws Exception
    {
        container.release( proxy );

        super.tearDown();
    }

    private ProxyConfiguration getTestConfiguration()
        throws ComponentLookupException
    {
        ProxyConfiguration config = (ProxyConfiguration) container.lookup( ProxyConfiguration.ROLE );

        config.setRepositoryCachePath( "target/proxy-cache" );

        return config;
    }
}
