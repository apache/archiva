package org.apache.archiva;

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
import org.apache.archiva.metadata.repository.RepositorySessionFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * @author Olivier Lamy
 */
public class TestRepositorySessionFactoryBean
    extends RepositorySessionFactoryBean
{

    private Logger logger = LoggerFactory.getLogger( getClass() );

    private String beanId;

    public TestRepositorySessionFactoryBean( String beanId )
    {
        super( new Properties(  ) );
        this.beanId = beanId;
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
            getBeanFactory().getBean( "repositorySessionFactory#" + this.beanId, RepositorySessionFactory.class );
        logger.info( "create RepositorySessionFactory instance of {}", repositorySessionFactory.getClass().getName() );
        if (!repositorySessionFactory.isOpen()) {
            repositorySessionFactory.open();
        }
        return repositorySessionFactory;
    }

    @Override
    public String getId()
    {
        return this.beanId;
    }
}
