package org.apache.archiva.web.startup;

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

import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import java.util.Properties;

/**
 * @author Olivier Lamy
 * @since 2.0.2
 */
public class RepositorySessionFactoryBean
    extends AbstractFactoryBean<RepositorySessionFactory>
{

    private Logger logger = LoggerFactory.getLogger( getClass() );

    //@Value( "repositorySessionFactory#${archiva.repositorySessionFactory.id}" )
    //private String beanName;

    private Properties properties;

    public RepositorySessionFactoryBean( Properties properties )
    {
        this.properties = properties;
        // we can override with system props
        this.properties.putAll( System.getProperties() );
    }

    @Override
    public Class<RepositorySessionFactory> getObjectType()
    {
        return RepositorySessionFactory.class;
    }

    @Override
    protected RepositorySessionFactory createInstance()
        throws Exception
    {
        String id = properties.getProperty( "archiva.repositorySessionFactory.id" );
        RepositorySessionFactory repositorySessionFactory =
            getBeanFactory().getBean( "repositorySessionFactory#" + id, RepositorySessionFactory.class );
        logger.info( "create RepositorySessionFactory instance of {}", repositorySessionFactory.getClass().getName() );
        return repositorySessionFactory;
    }

    public Properties getProperties()
    {
        return properties;
    }

    public void setProperties( Properties properties )
    {
        this.properties = properties;
    }
}
