package org.apache.archiva.repository;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.apache.archiva.repository.api.interceptor.RepositoryInterceptor;
import org.apache.archiva.repository.api.interceptor.RepositoryInterceptorFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;

public class DefaultRepositoryInterceptorFactory implements RepositoryInterceptorFactory<RepositoryInterceptor>, BeanFactoryAware
{
    private ListableBeanFactory beanFactory;

    private ArrayList<RepositoryInterceptor> repositoryInterceptors;

    private final Class interceptorType;

    public DefaultRepositoryInterceptorFactory(Class interceptorType)
    {
        this.interceptorType = interceptorType;
    }

    public Collection<RepositoryInterceptor> getRepositoryInterceptors()
    {
        if (repositoryInterceptors == null)
        {
            repositoryInterceptors = new ArrayList<RepositoryInterceptor>();
            final Map beans = beanFactory.getBeansOfType(interceptorType);
            if (beans != null)
            {
                repositoryInterceptors.addAll(beans.values());
            }
        }
        return repositoryInterceptors;
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException
    {
        if (beanFactory instanceof ListableBeanFactory)
        {
            this.beanFactory = (ListableBeanFactory)beanFactory;
        }
        else
        {
            throw new RuntimeException("BeanFactory is not a ListableBeanFactory " + beanFactory.getClass());
        }
    }
}
