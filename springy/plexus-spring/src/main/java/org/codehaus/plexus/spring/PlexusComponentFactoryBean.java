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
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.util.ReflectionUtils;

/**
 * A FactoryBean dedicated to building plexus components. This includes :
 * <ul>
 * <li>Support for direct field injection or "requirements"</li>
 * <li>Support for LogEnabled, Initializable and Disposable plexus interfaces</li>
 * <li>Support for plexus.requirement to get a Map<role-hint, component> for a
 * role
 * </ul>
 * If not set, the beanFActory will auto-detect the loggerManager to use by
 * searching for the adequate bean in the spring context.
 * <p>
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class PlexusComponentFactoryBean
    implements FactoryBean, BeanFactoryAware
{
    /** Logger available to subclasses */
    protected final Log logger = LogFactory.getLog( getClass() );

    /** The beanFactory */
    private BeanFactory beanFactory;

    /**
     * @todo isn't there a constant for this in plexus ?
     */
    private static final String SINGLETON = "singleton";

    /** The plexus component role */
    private Class role;

    /** The plexus component implementation class */
    private Class implementation;

    /** The plexus component instantiation strategy */
    private String instantiationStrategy;

    /** The plexus component requirements and configurations */
    private Map requirements;

    private Object singletonInstance;

    /**
     * {@inheritDoc}
     *
     * @see org.springframework.beans.factory.FactoryBean#getObject()
     */
    public Object getObject()
        throws Exception
    {
        if ( isSingleton() )
        {
            synchronized ( this )
            {
                if ( singletonInstance != null )
                {
                    return singletonInstance;
                }
                this.singletonInstance = createInstance();
                return singletonInstance;
            }
        }
        return createInstance();
    }

    /**
     * Create the plexus component instance. Inject dependencies declared as
     * requirements using direct field injection
     */
    public Object createInstance()
        throws Exception
    {
        logger.debug( "Creating plexus component " + implementation );
        final Object component = implementation.newInstance();
        if ( requirements != null )
        {
            for ( Iterator iterator = requirements.entrySet().iterator(); iterator.hasNext(); )
            {
                Map.Entry requirement = (Map.Entry) iterator.next();
                String fieldName = (String) requirement.getKey();

                if ( fieldName.startsWith( "#" ) )
                {
                    // implicit field injection : the field name was no
                    // specified in the plexus descriptor as only one filed
                    // matches Dependency type

                    RuntimeBeanReference ref = (RuntimeBeanReference) requirement.getValue();
                    Object dependency = beanFactory.getBean( ref.getBeanName() );

                    Field[] fields = implementation.getDeclaredFields();
                    for ( int i = 0; i < fields.length; i++ )
                    {
                        Field field = fields[i];
                        if ( ReflectionUtils.COPYABLE_FIELDS.matches( field )
                            && field.getType().isAssignableFrom( dependency.getClass() ) )
                        {
                            if ( logger.isTraceEnabled() )
                            {
                                logger.trace( "Injecting dependency " + dependency + " into field " + field.getName() );
                            }
                            ReflectionUtils.makeAccessible( field );
                            ReflectionUtils.setField( field, component, dependency );
                        }
                    }
                }
                else
                {
                    // explicit field injection
                    fieldName = PlexusToSpringUtils.toCamelCase( fieldName );
                    Field field = findField( fieldName );
                    Object dependency = resolveRequirement( field, requirement.getValue() );
                    if ( logger.isTraceEnabled() )
                    {
                        logger.trace( "Injecting dependency " + dependency + " into field " + field.getName() );
                    }
                    ReflectionUtils.makeAccessible( field );
                    ReflectionUtils.setField( field, component, dependency );
                }
            }
        }
        return component;
    }

    private Field findField( String fieldName )
    {
        Class clazz = implementation;
        while ( clazz != Object.class )
        {
            try
            {
                return clazz.getDeclaredField( fieldName );
            }
            catch ( NoSuchFieldException e )
            {
                clazz = clazz.getSuperclass();
            }
        }
        String error = "No field " + fieldName + " on implementation class " + implementation;
        logger.error( error );
        throw new BeanInitializationException( error );
    }

    /**
     * Resolve the requirement that this field exposes in the component
     *
     * @param field
     * @return
     */
    protected Object resolveRequirement( Field field, Object requirement )
    {
        if ( requirement instanceof RuntimeBeanReference )
        {
            String beanName = ( (RuntimeBeanReference) requirement ).getBeanName();
            if ( Map.class.isAssignableFrom( field.getType() ) )
            {
                // component ask plexus for a Map of all available
                // components for the role
                requirement = PlexusToSpringUtils.lookupMap( beanName, getListableBeanFactory() );
            }
            else if ( Collection.class.isAssignableFrom( field.getType() ) )
            {
                requirement = PlexusToSpringUtils.LookupList( beanName, getListableBeanFactory() );
            }
            else
            {
                requirement = beanFactory.getBean( beanName );
            }
        }
        if ( requirement != null )
        {
            requirement = getBeanTypeConverter().convertIfNecessary( requirement, field.getType() );
        }
        return requirement;

    }

    public Class getObjectType()
    {
        return role;
    }

    public boolean isSingleton()
    {
        return SINGLETON.equals( instantiationStrategy );
    }

    protected TypeConverter getBeanTypeConverter()
    {
        if ( beanFactory instanceof ConfigurableBeanFactory )
        {
            return ( (ConfigurableBeanFactory) beanFactory ).getTypeConverter();
        }
        else
        {
            return new SimpleTypeConverter();
        }
    }

    private ListableBeanFactory getListableBeanFactory()
    {
        if ( beanFactory instanceof ListableBeanFactory )
        {
            return (ListableBeanFactory) beanFactory;
        }
        throw new BeanInitializationException( "A ListableBeanFactory is required by the PlexusComponentFactoryBean" );
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
     * @param instantiationStrategy the instantiationStrategy to set
     */
    public void setInstantiationStrategy( String instantiationStrategy )
    {
        if ( instantiationStrategy.length() == 0 )
        {
            instantiationStrategy = SINGLETON;
        }
        if ( "poolable".equals( instantiationStrategy ) )
        {
            throw new BeanCreationException( "Plexus poolable instantiation-strategy is not supported" );
        }
        this.instantiationStrategy = instantiationStrategy;
    }

    /**
     * @param requirements the requirements to set
     */
    public void setRequirements( Map requirements )
    {
        this.requirements = requirements;
    }

    public void setBeanFactory( BeanFactory beanFactory )
    {
        this.beanFactory = beanFactory;
    }

}
