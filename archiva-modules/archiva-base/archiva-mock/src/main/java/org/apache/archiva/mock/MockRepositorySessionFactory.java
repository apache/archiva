package org.apache.archiva.mock;

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

import org.apache.archiva.metadata.repository.AbstractMetadataRepository;
import org.apache.archiva.metadata.repository.AbstractRepositorySessionFactory;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.MetadataSessionException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.springframework.stereotype.Service;

/**
 * @author Olivier Lamy
 */
@Service( "repositorySessionFactory#mock" )
public class MockRepositorySessionFactory extends AbstractRepositorySessionFactory
    implements RepositorySessionFactory
{
    private MetadataRepository repository = new AbstractMetadataRepository()
    {
    };

    private MetadataResolver resolver;

    public void setRepository( MetadataRepository repository )
    {
        this.repository = repository;
    }

    public void setResolver( MetadataResolver resolver )
    {
        this.resolver = resolver;
    }

    @Override
    public RepositorySession createSession() throws MetadataRepositoryException
    {
        return new RepositorySession( repository, resolver )
        {
            @Override
            public void close()
            {
                return;
            }

            @Override
            public void save() throws MetadataSessionException
            {
                // no op
            }

            @Override
            public MetadataRepository getRepository()
            {
                return repository;
            }
        };
    }

    @Override
    protected void initialize() {
        // noop
    }

    @Override
    protected void shutdown() {
        // noop
    }


}
