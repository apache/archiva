package org.apache.maven.archiva.common.spring;

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

import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.logging.slf4j.Slf4jLogger;
import org.codehaus.plexus.logging.slf4j.Slf4jLoggerManager;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * A Spring bean postPorcessor to apply Plexu LogEnabled lifecycle interface
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @since 1.1
 */
public class PlexusLogEnabledBeanPostProcessor
    implements BeanPostProcessor, InitializingBean
{
    private LoggerManager loggerManager = new Slf4jLoggerManager();

    /**
     * {@inheritDoc}
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet()
        throws Exception
    {
        if ( loggerManager instanceof Initializable )
        {
            ( (Initializable) loggerManager ).initialize();
        }
    }

    /**
     * {@inheritDoc}
     * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization(java.lang.Object, java.lang.String)
     */
    public Object postProcessAfterInitialization( Object bean, String beanName )
        throws BeansException
    {
        return bean;
    }

    /**
     * {@inheritDoc}
     * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization(java.lang.Object, java.lang.String)
     */
    public Object postProcessBeforeInitialization( Object bean, String beanName )
        throws BeansException
    {
        if ( bean instanceof LogEnabled )
        {
            ( (LogEnabled) bean ).enableLogging( loggerManager.getLoggerForComponent( beanName ) );
        }
        return bean;
    }

    protected void setLoggerManager( LoggerManager loggerManager )
    {
        this.loggerManager = loggerManager;
    }

}
