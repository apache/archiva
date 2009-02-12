package org.apache.archiva.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.apache.archiva.repository.api.RepositoryManager;
import org.apache.archiva.repository.api.RepositoryManagerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;

public class DefaultRepositoryManagerFactory implements RepositoryManagerFactory, BeanFactoryAware
{
    private ListableBeanFactory beanFactory;

    private final ArrayList<RepositoryManager> repositoryManagers = new ArrayList<RepositoryManager>();

    public void init()
    {
        Map beans = beanFactory.getBeansOfType(RepositoryManager.class);
        if (beans != null)
        {
            repositoryManagers.addAll(beans.values());
        }
    }

    public Collection<RepositoryManager> getRepositoryManagers()
    {
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
