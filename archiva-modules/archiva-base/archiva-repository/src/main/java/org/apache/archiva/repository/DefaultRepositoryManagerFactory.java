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
import java.util.Collections;
import java.util.Map;
import org.apache.archiva.repository.api.RepositoryManager;
import org.apache.archiva.repository.api.RepositoryManagerFactory;
import org.apache.archiva.repository.api.RepositoryWeightComparitor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;

public class DefaultRepositoryManagerFactory implements RepositoryManagerFactory, BeanFactoryAware
{
    private ListableBeanFactory beanFactory;

    private ArrayList<RepositoryManager> repositoryManagers;

    public Collection<RepositoryManager> getRepositoryManagers()
    {
        if (repositoryManagers == null)
        {
            repositoryManagers = new ArrayList<RepositoryManager>();
            Map beans = beanFactory.getBeansOfType(RepositoryManager.class);
            if (beans != null)
            {
                repositoryManagers.addAll(beans.values());
            }
            Collections.sort(repositoryManagers, new RepositoryWeightComparitor());
        }
        return repositoryManagers;
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
