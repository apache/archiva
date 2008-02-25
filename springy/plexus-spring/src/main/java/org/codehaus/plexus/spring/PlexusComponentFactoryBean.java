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

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.springframework.beans.BeansException;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.util.ReflectionUtils;

/**
 * A FactoryBean dedicated to building plexus components. This includes :
 * <ul>
 * <li>Support for direct field injection or "requirements"</li>
 * <li>Support for LogEnabled, Initializable and Disposable plexus interfaces</li>
 * <li>Support for plexus.requirement to get a Map<role-hint, component> for a role
 * </ul>
 * If not set, the beanFActory will auto-detect the loggerManager to use by searching for the adequate bean in the
 * spring context.
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class PlexusComponentFactoryBean
    implements FactoryBean, BeanFactoryAware, DisposableBean
{
    private Class role;

    private Class implementation;

    private String instanciationStrategy;

    private Map requirements;

    private ListableBeanFactory beanFactory;

    private LoggerManager loggerManager;

    private TypeConverter typeConverter = new SimpleTypeConverter();

    private List instances = new LinkedList();

    public void destroy()
        throws Exception
    {
        synchronized ( instances )
        {
            for ( Iterator iterator = instances.iterator(); iterator.hasNext(); )
            {
                Object component = (Object) iterator.next();
                if ( component instanceof Disposable )
                {
                    ( (Disposable) component ).dispose();

                }
            }
        }
    }

    public Object getObject()
        throws Exception
    {
        final Object component = implementation.newInstance();
        synchronized ( instances )
        {
            instances.add( component );
        }
        if ( requirements != null )
        {
            ReflectionUtils.doWithFields( implementation, new ReflectionUtils.FieldCallback()
            {
                public void doWith( Field field )
                    throws IllegalArgumentException, IllegalAccessException
                {
                    Object dependency = requirements.get( field.getName() );
                    if ( dependency instanceof RuntimeBeanReference )
                    {
                        String beanName = ( (RuntimeBeanReference) dependency ).getBeanName();
                        if ( Map.class.isAssignableFrom( field.getType() ) )
                        {
                            // component ask plexus for a Map of all available components for the role
                            Map map = new HashMap();
                            String mask = beanName + '#';
                            String[] beans = beanFactory.getBeanDefinitionNames();
                            for ( int i = 0; i < beans.length; i++ )
                            {
                                String name = beans[i];
                                if ( name.startsWith( mask ) )
                                {
                                    map.put( name.substring( mask.length() ), beanFactory.getBean( name ) );
                                }
                            }
                            if ( beanFactory.containsBean( beanName ) )
                            {
                                map.put( "default", beanFactory.getBean( beanName ) );
                            }
                            dependency = map;
                        }
                        else if ( Collection.class.isAssignableFrom( field.getType() ) )
                        {
                            List list = new LinkedList();
                            String mask = beanName + '#';
                            String[] beans = beanFactory.getBeanDefinitionNames();
                            for ( int i = 0; i < beans.length; i++ )
                            {
                                String name = beans[i];
                                if ( name.startsWith( mask ) )
                                {
                                    list.add( beanFactory.getBean( name ) );
                                }
                            }
                            if ( beanFactory.containsBean( beanName ) )
                            {
                                list.add( beanFactory.getBean( beanName ) );
                            }
                            dependency = list;
                        }
                        else
                        {
                            dependency = beanFactory.getBean( beanName );
                        }
                    }
                    if ( dependency != null )
                    {
                        dependency = typeConverter.convertIfNecessary( dependency, field.getType() );
                        ReflectionUtils.makeAccessible( field );
                        ReflectionUtils.setField( field, component, dependency );
                    }
                }
            }, ReflectionUtils.COPYABLE_FIELDS );
        }


        if ( component instanceof LogEnabled )
        {
            ( (LogEnabled) component ).enableLogging( getLoggerManager().getLoggerForComponent( role.getName() ) );
        }

        if ( component instanceof Initializable )
        {
            ( (Initializable) component ).initialize();
        }

        // TODO add support for Startable, Stopable -> LifeCycle ?

        return component;
    }

    public Class getObjectType()
    {
        return role;
    }

    public boolean isSingleton()
    {
        return "per-lookup".equals( instanciationStrategy );
    }

    private LoggerManager getLoggerManager()
    {
        if ( loggerManager == null )
        {
            if ( beanFactory.containsBean( "loggerManager" ) )
            {
                loggerManager = (LoggerManager) beanFactory.getBean( "loggerManager" );
            }
            Map loggers = beanFactory.getBeansOfType( LoggerManager.class );
            if ( loggers.size() == 1 )
            {
                loggerManager = (LoggerManager) loggers.values().iterator().next();
            }
            else
            {
                throw new BeanInitializationException(
                                                       "You must explicitly set a LoggerManager or define a unique one in bean context" );
            }
        }
        return loggerManager;
    }

    public void setBeanFactory( BeanFactory beanFactory )
        throws BeansException
    {
        this.beanFactory = (ListableBeanFactory) beanFactory;
    }

    /**
     * @param loggerManager the loggerManager to set
     */
    public void setLoggerManager( LoggerManager loggerManager )
    {
        this.loggerManager = loggerManager;
    }

    /**
     * @param role the role to set
     */
    public void setRole( Class role )
    {
        this.role = role;
    }

    /**
     * @param implementation the implementation to set
     */
    public void setImplementation( Class implementation )
    {
        this.implementation = implementation;
    }

    /**
     * @param instanciationStrategy the instanciationStrategy to set
     */
    public void setInstanciationStrategy( String instanciationStrategy )
    {
        this.instanciationStrategy = instanciationStrategy;
    }

    /**
     * @param requirements the requirements to set
     */
    public void setRequirements( Map requirements )
    {
        this.requirements = requirements;
    }

    protected void setTypeConverter( TypeConverter typeConverter )
    {
        this.typeConverter = typeConverter;
    }

}
