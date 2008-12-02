package org.apache.maven.archiva.repository.events;

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

import java.util.List;

import org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @todo though we will eventually remove this altogether, an interim cleanup would be to genericise this
 * and replace the calls in RepositoryContentConsumers with calls to the same thing
 */
public class RepositoryListenerFactoryBean
    implements FactoryBean, ApplicationContextAware
{

    private ApplicationContext applicationContext;

    public void setApplicationContext( ApplicationContext applicationContext )
        throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    public Object getObject()
        throws Exception
    {
        return applicationContext.getBeansOfType( RepositoryListener.class ).values();
    }

    public Class getObjectType()
    {
        return List.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

    
}
