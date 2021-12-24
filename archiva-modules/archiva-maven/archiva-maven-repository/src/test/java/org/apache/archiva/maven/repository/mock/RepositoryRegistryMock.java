package org.apache.archiva.maven.repository.mock;

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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.base.ArchivaRepositoryRegistry;
import org.apache.archiva.repository.base.ConfigurationHandler;
import org.apache.archiva.repository.validation.RepositoryValidator;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RepositoryRegistryMock extends ArchivaRepositoryRegistry
{

    private Map<String, ManagedRepository> managedRepositories = new TreeMap<>();

    public RepositoryRegistryMock( ConfigurationHandler configurationHandler, List<RepositoryValidator<? extends Repository>> validatorList )
    {
        super( configurationHandler, validatorList );
    }

    @Override
    public ManagedRepository putRepository(ManagedRepository managedRepository) throws RepositoryException
    {
        managedRepositories.put(managedRepository.getId(), managedRepository);
        return managedRepository;
    }

    @Override
    public ManagedRepository getManagedRepository(String repoId) {
        return managedRepositories.get(repoId);
    }

    @Override
    public Repository getRepository( String repoId) {
        if (managedRepositories.containsKey(repoId)) {
            return managedRepositories.get(repoId);
        } else {
            return null;
        }
    }
}
