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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * A FactoryBean to port to Spring the plexus "get all components with role ..." feature.
 * <p>
 * Plexus allows to define a requirement this way :
 * <pre>
 *   \/**
 *    * @plexus.requirement role="org.apache.maven.archiva.policies.PreDownloadPolicy"
 *    *\/
 *   private Map<String, PreDownloadPolicy> prePolicies;
 * </pre>
 * This FactoryBean generates the expected Map from a ListableBeanFactory, based on the role
 * to be the FQCN of the component interface.
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @since 1.1
 */
public class BeansOfTypeFactoryBean
    extends AbstractFactoryBean
    implements InitializingBean
{
    private Class type;

    private Map<String, Object> beansOfType;

    /**
     * {@inheritDoc}
     * @see org.springframework.beans.factory.config.AbstractFactoryBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet()
        throws Exception
    {
        beansOfType = new HashMap<String, Object>();
        if ( !( getBeanFactory() instanceof ListableBeanFactory ) )
        {
            String error = "A ListableBeanFactory bean factory is required to create a bean-of-types Map";
            logger.error( error );
            throw new BeanInitializationException( error );
        }
        Map beans = ((ListableBeanFactory) getBeanFactory()).getBeansOfType( type );
        for ( Iterator iterator = beans.entrySet().iterator(); iterator.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) iterator.next();
            beansOfType.put( getRoleHint( (String) entry.getKey() ), entry.getValue() );
        }
    }

    /**
     * @param key
     * @return
     */
    private String getRoleHint( String key )
    {
        int i =key.indexOf( '#' );
        if (i >= 0 )
        {
            return key.substring( i + 1 );
        }
        return key;
    }

    /**
     * {@inheritDoc}
     * @see org.springframework.beans.factory.config.AbstractFactoryBean#createInstance()
     */
    @Override
    protected Object createInstance()
        throws Exception
    {
        return beansOfType;
    }

    /**
     * {@inheritDoc}
     * @see org.springframework.beans.factory.config.AbstractFactoryBean#getObjectType()
     */
    @Override
    public Class getObjectType()
    {
        return Map.class;
    }

    public void setType( Class type )
    {
        this.type = type;
    }


}
