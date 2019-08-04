package org.apache.archiva.webtest.memory;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Service;

@Service("repositorySessionFactory#test")
public class TestRepositorySessionFactory
    extends AbstractFactoryBean<RepositorySessionFactory>
    implements RepositorySessionFactory
{
    private RepositorySession repositorySession;

    public void setRepositorySession( RepositorySession repositorySession )
    {
        this.repositorySession = repositorySession;
    }

    @Override
    public void open() {

    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public RepositorySession createSession() throws MetadataRepositoryException
    {
        return repositorySession != null ? repositorySession : new RepositorySession( new TestMetadataRepository(),
                                                                                      new TestMetadataResolver() );
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
        return this;
    }

    @Override
    public void close()
    {
        // no op
    }
}
