package org.apache.archiva.repository;

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

    private final ArrayList<RepositoryInterceptor> repositoryInterceptors;

    private final Class interceptorType;

    public DefaultRepositoryInterceptorFactory(Class interceptorType)
    {
        this.interceptorType = interceptorType;
        this.repositoryInterceptors = new ArrayList<RepositoryInterceptor>();
    }

    public void init()
    {
        Map beans = beanFactory.getBeansOfType(interceptorType);
        if (beans != null)
        {
            repositoryInterceptors.addAll(repositoryInterceptors);
        }
    }

    public Collection<RepositoryInterceptor> getRepositoryInterceptors()
    {
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
