package org.apache.archiva.metadata.repository;

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

    private static final String BEAN_ID_SYS_PROPS = "archiva.repositorySessionFactory.id";

    private Properties properties;

    private String id;

    public RepositorySessionFactoryBean( Properties properties )
    {
        this.properties = properties;
        // we can override with system props
        String value = System.getProperty( BEAN_ID_SYS_PROPS );
        if ( value != null )
        {
            this.properties.put( BEAN_ID_SYS_PROPS, value );
        }
        id = properties.getProperty( BEAN_ID_SYS_PROPS );
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
        RepositorySessionFactory repositorySessionFactory =
            getBeanFactory().getBean( "repositorySessionFactory#" + id, RepositorySessionFactory.class );
        logger.info( "create RepositorySessionFactory with id {} instance of {}", //
                     id, //
                     repositorySessionFactory.getClass().getName() );
        if (!repositorySessionFactory.isOpen()) {
            repositorySessionFactory.open();
        }
        return repositorySessionFactory;
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
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
