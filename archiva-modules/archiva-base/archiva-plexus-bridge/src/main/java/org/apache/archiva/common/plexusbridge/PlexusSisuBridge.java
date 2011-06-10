package org.apache.archiva.common.plexusbridge;

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

import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;

/**
 * Simple component which will initiate the plexus shim component
 * to see plexus components inside a guice container.<br/>
 * So move all of this here to be able to change quickly if needed.
 *
 * @author Olivier Lamy
 */
@Service( "plexusSisuBridge" )
public class PlexusSisuBridge
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    private boolean containerAutoWiring = true;

    private String containerClassPathScanning = PlexusConstants.SCANNING_ON;

    private String containerComponentVisibility = PlexusConstants.REALM_VISIBILITY;

    private URL overridingComponentsXml;

    private ClassRealm containerRealm;

    private DefaultPlexusContainer plexusContainer;

    @PostConstruct
    public void initialize()
        throws PlexusSisuBridgeException
    {
        DefaultContainerConfiguration conf = new DefaultContainerConfiguration();

        conf.setAutoWiring( containerAutoWiring );
        conf.setClassPathScanning( containerClassPathScanning );
        conf.setComponentVisibility( containerComponentVisibility );

        conf.setContainerConfigurationURL( overridingComponentsXml );

        ClassWorld classWorld = new ClassWorld();

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        containerRealm = new ClassRealm( classWorld, "maven", tccl );

        // olamy hackhish but plexus-sisu need a URLClassLoader with URL filled

        if ( tccl instanceof URLClassLoader )
        {
            URL[] urls = ( (URLClassLoader) tccl ).getURLs();
            for ( URL url : urls )
            {
                containerRealm.addURL( url );
            }
        }

        conf.setRealm( containerRealm );

        //conf.setClassWorld( classWorld );

        ClassLoader ori = Thread.currentThread().getContextClassLoader();

        try
        {
            Thread.currentThread().setContextClassLoader( containerRealm );
            plexusContainer = new DefaultPlexusContainer( conf );
        }
        catch ( PlexusContainerException e )
        {
            throw new PlexusSisuBridgeException( e.getMessage(), e );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( ori );
        }
    }


    private URL[] getClassLoaderURLs( ClassLoader classLoader )
    {
        try
        {
            // can be WebappClassLoader when using tomcat maven plugin
            //java.net.URL[] getURLs
            Method method = classLoader.getClass().getMethod( "getURLs", new Class[]{ } );
            if ( method != null )
            {
                return (URL[]) method.invoke( classLoader, null );
            }
        }
        catch ( Exception e )
        {
            log.info( "ignore issue trying to find url[] from classloader {}", e.getMessage() );
        }
        return new URL[]{ };
    }

    public <T> T lookup( Class<T> clazz )
        throws PlexusSisuBridgeException
    {
        ClassLoader ori = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader( containerRealm );
            return plexusContainer.lookup( clazz );
        }
        catch ( ComponentLookupException e )
        {
            throw new PlexusSisuBridgeException( e.getMessage(), e );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( ori );
        }
    }

    public <T> T lookup( Class<T> clazz, String hint )
        throws PlexusSisuBridgeException
    {
        ClassLoader ori = Thread.currentThread().getContextClassLoader();

        try
        {
            Thread.currentThread().setContextClassLoader( containerRealm );
            return plexusContainer.lookup( clazz, hint );
        }
        catch ( ComponentLookupException e )
        {
            throw new PlexusSisuBridgeException( e.getMessage(), e );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( ori );
        }
    }

    public <T> List<T> lookupList( Class<T> clazz )
        throws PlexusSisuBridgeException
    {
        ClassLoader ori = Thread.currentThread().getContextClassLoader();

        try
        {
            Thread.currentThread().setContextClassLoader( containerRealm );
            return plexusContainer.lookupList( clazz );
        }
        catch ( ComponentLookupException e )
        {
            throw new PlexusSisuBridgeException( e.getMessage(), e );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( ori );
        }
    }

    public <T> Map<String, T> lookupMap( Class<T> clazz )
        throws PlexusSisuBridgeException
    {
        ClassLoader ori = Thread.currentThread().getContextClassLoader();

        try
        {
            Thread.currentThread().setContextClassLoader( containerRealm );
            return plexusContainer.lookupMap( clazz );
        }
        catch ( ComponentLookupException e )
        {
            throw new PlexusSisuBridgeException( e.getMessage(), e );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( ori );
        }
    }
}
