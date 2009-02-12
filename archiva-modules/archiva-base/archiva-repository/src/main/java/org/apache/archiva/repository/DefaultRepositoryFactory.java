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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.archiva.repository.api.Repository;
import org.apache.archiva.repository.api.RepositoryFactory;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;

public class DefaultRepositoryFactory implements RepositoryFactory
{
    private final ArchivaConfiguration archivaConfiguration;

    public DefaultRepositoryFactory(ArchivaConfiguration archivaConfiguration)
    {
        this.archivaConfiguration = archivaConfiguration;
    }

    public Map<String, Repository> getRepositories()
    {
        final HashMap<String, Repository> repositories = new HashMap<String, Repository>();
        for (final ManagedRepositoryConfiguration configuration : archivaConfiguration.getConfiguration().getManagedRepositories())
        {
            final DefaultRepository repository = new DefaultRepository(configuration.getId(), configuration.getName(), new File(configuration.getLocation()));
            repositories.put(configuration.getId(), repository);
            repository.getLocalPath().mkdirs();
        }
        return repositories;
    }
}
