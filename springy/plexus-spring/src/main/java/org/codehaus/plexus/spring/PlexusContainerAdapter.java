package org.codehaus.plexus.spring;

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

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.composition.CompositionException;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryListener;
import org.codehaus.plexus.component.repository.ComponentDescriptor;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.component.repository.exception.ComponentRepositoryException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.DefaultContext;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LoggerManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * An adapter to access Spring ApplicationContext from a plexus component
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class PlexusContainerAdapter
    implements PlexusContainer, ApplicationContextAware, InitializingBean
{
    private Context context = new DefaultContext();

    private ApplicationContext applicationContext;


    /**
     * {@inheritDoc}
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet()
        throws Exception
    {
        context.put( "plexus", this );
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#addComponentDescriptor(org.codehaus.plexus.component.repository.ComponentDescriptor)
     */
    public void addComponentDescriptor( ComponentDescriptor componentDescriptor )
        throws ComponentRepositoryException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#addContextValue(java.lang.Object, java.lang.Object)
     */
    public void addContextValue( Object key, Object value )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#addJarRepository(java.io.File)
     */
    public void addJarRepository( File repository )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#addJarResource(java.io.File)
     */
    public void addJarResource( File resource )
        throws PlexusContainerException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#autowire(java.lang.Object)
     */
    public Object autowire( Object component )
        throws CompositionException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#createAndAutowire(java.lang.String)
     */
    public Object createAndAutowire( String clazz )
        throws CompositionException, ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#createChildContainer(java.lang.String, java.util.List, java.util.Map)
     */
    public PlexusContainer createChildContainer( String name, List classpathJars, Map context )
        throws PlexusContainerException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#createChildContainer(java.lang.String, java.util.List, java.util.Map, java.util.List)
     */
    public PlexusContainer createChildContainer( String name, List classpathJars, Map context, List discoveryListeners )
        throws PlexusContainerException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#createComponentRealm(java.lang.String, java.util.List)
     */
    public ClassRealm createComponentRealm( String id, List jars )
        throws PlexusContainerException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#dispose()
     */
    public void dispose()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#getChildContainer(java.lang.String)
     */
    public PlexusContainer getChildContainer( String name )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#getComponentDescriptor(java.lang.String)
     */
    public ComponentDescriptor getComponentDescriptor( String role )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#getComponentDescriptor(java.lang.String, java.lang.String)
     */
    public ComponentDescriptor getComponentDescriptor( String role, String roleHint )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#getComponentDescriptor(java.lang.String, org.codehaus.plexus.classworlds.realm.ClassRealm)
     */
    public ComponentDescriptor getComponentDescriptor( String role, ClassRealm realm )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#getComponentDescriptor(java.lang.String, java.lang.String, org.codehaus.plexus.classworlds.realm.ClassRealm)
     */
    public ComponentDescriptor getComponentDescriptor( String role, String roleHint, ClassRealm realm )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#getComponentDescriptorList(java.lang.String)
     */
    public List getComponentDescriptorList( String role )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#getComponentDescriptorList(java.lang.String, org.codehaus.plexus.classworlds.realm.ClassRealm)
     */
    public List getComponentDescriptorList( String role, ClassRealm componentRealm )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#getComponentDescriptorMap(java.lang.String)
     */
    public Map getComponentDescriptorMap( String role )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#getComponentDescriptorMap(java.lang.String, org.codehaus.plexus.classworlds.realm.ClassRealm)
     */
    public Map getComponentDescriptorMap( String role, ClassRealm componentRealm )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#getComponentRealm(java.lang.String)
     */
    public ClassRealm getComponentRealm( String realmId )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#getContainerRealm()
     */
    public ClassRealm getContainerRealm()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#getContext()
     */
    public Context getContext()
    {
        return context;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#getCreationDate()
     */
    public Date getCreationDate()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#getLogger()
     */
    public Logger getLogger()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#getLoggerManager()
     */
    public LoggerManager getLoggerManager()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#getLookupRealm()
     */
    public ClassRealm getLookupRealm()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#getLookupRealm(java.lang.Object)
     */
    public ClassRealm getLookupRealm( Object component )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#getName()
     */
    public String getName()
    {
        return "plexus spring adapter";
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#hasChildContainer(java.lang.String)
     */
    public boolean hasChildContainer( String name )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#hasComponent(java.lang.String)
     */
    public boolean hasComponent( String role )
    {
        return false;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#hasComponent(java.lang.String, java.lang.String)
     */
    public boolean hasComponent( String role, String roleHint )
    {
        return applicationContext.containsBean( PlexusToSpringUtils.buildSpringId( role, roleHint ) );
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#isReloadingEnabled()
     */
    public boolean isReloadingEnabled()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#lookup(java.lang.String)
     */
    public Object lookup( String componentKey )
        throws ComponentLookupException
    {
        return lookup( componentKey, (String) null );
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#lookup(java.lang.Class)
     */
    public Object lookup( Class componentClass )
        throws ComponentLookupException
    {
        return lookup( componentClass.getName(), (String) null );

    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#lookup(java.lang.String, org.codehaus.plexus.classworlds.realm.ClassRealm)
     */
    public Object lookup( String componentKey, ClassRealm realm )
        throws ComponentLookupException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#lookup(java.lang.String, java.lang.String)
     */
    public Object lookup( String role, String roleHint )
        throws ComponentLookupException
    {
        return applicationContext.getBean( PlexusToSpringUtils.buildSpringId( role, roleHint ) );
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#lookup(java.lang.Class, org.codehaus.plexus.classworlds.realm.ClassRealm)
     */
    public Object lookup( Class componentClass, ClassRealm realm )
        throws ComponentLookupException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#lookup(java.lang.Class, java.lang.String)
     */
    public Object lookup( Class role, String roleHint )
        throws ComponentLookupException
    {
        return lookup( role.getName(), roleHint );
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#lookup(java.lang.String, java.lang.String, org.codehaus.plexus.classworlds.realm.ClassRealm)
     */
    public Object lookup( String role, String roleHint, ClassRealm realm )
        throws ComponentLookupException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#lookup(java.lang.Class, java.lang.String, org.codehaus.plexus.classworlds.realm.ClassRealm)
     */
    public Object lookup( Class role, String roleHint, ClassRealm realm )
        throws ComponentLookupException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#lookupList(java.lang.String)
     */
    public List lookupList( String role )
        throws ComponentLookupException
    {
        return PlexusToSpringUtils.LookupList( role, applicationContext );
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#lookupList(java.lang.Class)
     */
    public List lookupList( Class role )
        throws ComponentLookupException
    {
        return lookupList( role.getName() );
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#lookupList(java.lang.String, org.codehaus.plexus.classworlds.realm.ClassRealm)
     */
    public List lookupList( String role, ClassRealm realm )
        throws ComponentLookupException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#lookupList(java.lang.Class, org.codehaus.plexus.classworlds.realm.ClassRealm)
     */
    public List lookupList( Class role, ClassRealm realm )
        throws ComponentLookupException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#lookupMap(java.lang.String)
     */
    public Map lookupMap( String role )
        throws ComponentLookupException
    {
        return PlexusToSpringUtils.lookupMap( role, applicationContext );
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#lookupMap(java.lang.Class)
     */
    public Map lookupMap( Class role )
        throws ComponentLookupException
    {
        return lookupMap( role.getName() );
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#lookupMap(java.lang.String, org.codehaus.plexus.classworlds.realm.ClassRealm)
     */
    public Map lookupMap( String role, ClassRealm realm )
        throws ComponentLookupException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#lookupMap(java.lang.Class, org.codehaus.plexus.classworlds.realm.ClassRealm)
     */
    public Map lookupMap( Class role, ClassRealm realm )
        throws ComponentLookupException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#registerComponentDiscoveryListener(org.codehaus.plexus.component.discovery.ComponentDiscoveryListener)
     */
    public void registerComponentDiscoveryListener( ComponentDiscoveryListener listener )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#release(java.lang.Object)
     */
    public void release( Object component )
        throws ComponentLifecycleException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#releaseAll(java.util.Map)
     */
    public void releaseAll( Map components )
        throws ComponentLifecycleException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#releaseAll(java.util.List)
     */
    public void releaseAll( List components )
        throws ComponentLifecycleException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#removeChildContainer(java.lang.String)
     */
    public void removeChildContainer( String name )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#removeComponentDiscoveryListener(org.codehaus.plexus.component.discovery.ComponentDiscoveryListener)
     */
    public void removeComponentDiscoveryListener( ComponentDiscoveryListener listener )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#resume(java.lang.Object)
     */
    public void resume( Object component )
        throws ComponentLifecycleException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#setLoggerManager(org.codehaus.plexus.logging.LoggerManager)
     */
    public void setLoggerManager( LoggerManager loggerManager )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#setLookupRealm(org.codehaus.plexus.classworlds.realm.ClassRealm)
     */
    public ClassRealm setLookupRealm( ClassRealm realm )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#setName(java.lang.String)
     */
    public void setName( String name )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#setParentPlexusContainer(org.codehaus.plexus.PlexusContainer)
     */
    public void setParentPlexusContainer( PlexusContainer container )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#setReloadingEnabled(boolean)
     */
    public void setReloadingEnabled( boolean reloadingEnabled )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusContainer#suspend(java.lang.Object)
     */
    public void suspend( Object component )
        throws ComponentLifecycleException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    public void setApplicationContext( ApplicationContext applicationContext )
        throws BeansException
    {
        this.applicationContext = applicationContext;
    }


}
