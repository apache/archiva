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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.logging.console.ConsoleLoggerManager;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class PlexusLifecycleBeanPostProcessor
    implements BeanPostProcessor, BeanFactoryAware, DisposableBean
{
    /** Logger available to subclasses */
    protected final Log logger = LogFactory.getLog( getClass() );

    private BeanFactory beanFactory;

    private Context context;

    private LoggerManager loggerManager;

    /** The plexus Disposable components */
    private Map disposables = new HashMap();

    protected Context getContext()
    {
        if ( context == null )
        {
            PlexusContainer container = (PlexusContainer) beanFactory.getBean( "plexusContainer" );
            context = container.getContext();
        }
        return context;
    }

    /**
     * Retrieve the loggerManager instance to be used for LogEnabled components
     *
     * @return
     */
    protected LoggerManager getLoggerManager()
    {
        if ( loggerManager == null )
        {
            if ( beanFactory.containsBean( "loggerManager" ) )
            {
                loggerManager = (LoggerManager) beanFactory.getBean( "loggerManager" );
            }
            else
            {
                logger.warn( "No loggerManager set in context. Falling back to ConsoleLoggerManager" );
                ConsoleLoggerManager defaultLoggerManager = new ConsoleLoggerManager();
                defaultLoggerManager.initialize();
                loggerManager = defaultLoggerManager;
            }
        }
        return loggerManager;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization(java.lang.Object,
     * java.lang.String)
     */
    public Object postProcessBeforeInitialization( Object bean, String beanName )
        throws BeansException
    {
        return bean;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization(java.lang.Object,
     * java.lang.String)
     */
    public Object postProcessAfterInitialization( Object bean, String beanName )
        throws BeansException
    {
        if ( bean instanceof FactoryBean )
        {
            // only apply to beans
            return bean;
        }

        if ( bean instanceof LogEnabled )
        {
            logger.trace( "Enable Logging on plexus bean " + beanName );
            ( (LogEnabled) bean ).enableLogging( getLoggerManager().getLoggerForComponent( beanName ) );
        }

        if ( bean instanceof Contextualizable )
        {
            try
            {
                logger.trace( "Contextualize plexus bean " + beanName );
                ( (Contextualizable) bean ).contextualize( getContext() );
            }
            catch ( ContextException e )
            {
                throw new BeanInitializationException(
                    "Failed to invoke plexus lifecycle Contextualizable.contextualize on " + beanName, e );
            }
        }

        // TODO add support for Startable, Stopable -> LifeCycle ?

        if ( bean instanceof Initializable )
        {
            try
            {
                logger.trace( "Initialize plexus bean " + beanName );
                ( (Initializable) bean ).initialize();
            }
            catch ( InitializationException e )
            {
                throw new BeanInitializationException( "Failed to invoke plexus lifecycle Initializable.initialize on "
                    + beanName, e );
            }
        }

        if ( bean instanceof Disposable )
        {
            synchronized ( disposables )
            {
                disposables.put( beanName, bean );
            }
        }
        return bean;
    }

    public void setBeanFactory( BeanFactory beanFactory )
    {
        this.beanFactory = beanFactory;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.springframework.beans.factory.config.AbstractFactoryBean#destroy()
     */
    public void destroy()
        throws Exception
    {
        synchronized ( disposables )
        {
            for ( Iterator iterator = disposables.entrySet().iterator(); iterator.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) iterator.next();
                logger.debug( "Dispose plexus component " + entry.getKey() );
                ( (Disposable) entry.getValue() ).dispose();
            }
        }
    }

}
