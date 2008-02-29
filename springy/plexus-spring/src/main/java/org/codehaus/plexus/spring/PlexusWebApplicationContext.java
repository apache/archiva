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

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * A custom XmlWebApplicationContext to support plexus
 * <tr>components.xml</tt> descriptors in Spring, with no changes required to
 * neither plexus nor spring beans.
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class PlexusWebApplicationContext
    extends XmlWebApplicationContext
{
    private static PlexusApplicationContextDelegate delegate = new PlexusApplicationContextDelegate();

    /**
     * {@inheritDoc}
     *
     * @see org.springframework.context.support.AbstractXmlApplicationContext#loadBeanDefinitions(org.springframework.beans.factory.xml.XmlBeanDefinitionReader)
     */
    protected void loadBeanDefinitions( XmlBeanDefinitionReader reader )
        throws BeansException, IOException
    {
        delegate.loadBeanDefinitions( reader );
        super.loadBeanDefinitions( reader );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.springframework.context.support.AbstractApplicationContext#postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory)
     */
    protected void postProcessBeanFactory( ConfigurableListableBeanFactory beanFactory )
    {
        delegate.postProcessBeanFactory( beanFactory, this );
        getServletContext().setAttribute( "webwork.plexus.container", beanFactory.getBean( "plexusContainer" ) );
        super.postProcessBeanFactory( beanFactory );
    }

    /**
     * {@inheritDoc}
     *
     * @see org.springframework.context.support.AbstractApplicationContext#doClose()
     */
    protected void doClose()
    {
        delegate.doClose();
        super.doClose();
    }

}
