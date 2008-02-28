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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;

/**
 * Utility method to convert plexus descriptors to spring bean context.
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class PlexusApplicationContextDelegate
{
    /** Logger used by this class. */
    protected final Log logger = LogFactory.getLog(getClass());

    private PlexusLifecycleBeanPostProcessor lifecycleBeanPostProcessor;

    /**
     * @see org.springframework.context.support.AbstractXmlApplicationContext#loadBeanDefinitions(org.springframework.beans.factory.xml.XmlBeanDefinitionReader)
     */
    protected void loadBeanDefinitions( XmlBeanDefinitionReader reader )
        throws BeansException, IOException
    {
        logger.info( "Registering plexus to spring XML translation" );
        reader.setDocumentReaderClass( PlexusBeanDefinitionDocumentReader.class );
        reader.setValidationMode( XmlBeanDefinitionReader.VALIDATION_NONE );
    }

    /**
     * @see org.springframework.context.support.AbstractApplicationContext#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
     */
    protected void postProcessBeanFactory( ConfigurableListableBeanFactory beanFactory, ApplicationContext context )
    {
        // Register a PlexusContainerAdapter bean to allow context lookups using plexus API
        PlexusContainerAdapter plexus = new PlexusContainerAdapter();
        plexus.setApplicationContext( context );
        beanFactory.registerSingleton( "plexusContainer", plexus );

        // Register a beanPostProcessor to handle plexus interface-based lifecycle management
        lifecycleBeanPostProcessor = new PlexusLifecycleBeanPostProcessor();
        lifecycleBeanPostProcessor.setBeanFactory( context );
        beanFactory.addBeanPostProcessor( lifecycleBeanPostProcessor );

        // Register a PorpertyEditor to support plexus XML <configuration> set as CDATA in
        // a spring context XML file.
        beanFactory.addPropertyEditorRegistrar( new PlexusConfigurationPropertyEditor() );
        beanFactory.addPropertyEditorRegistrar( new PropertiesPropertyEditor() );
    }

    /**
     * @see org.springframework.context.support.AbstractApplicationContext#doClose()
     */
    protected void doClose()
    {
        try
        {
            lifecycleBeanPostProcessor.destroy();
        }
        catch ( Throwable ex )
        {
            logger.error( "Exception thrown from PlexusLifecycleBeanPostProcessor handling ContextClosedEvent", ex );
        }
    }
}
