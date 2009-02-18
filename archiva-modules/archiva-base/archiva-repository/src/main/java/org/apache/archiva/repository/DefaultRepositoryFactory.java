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

import java.util.HashMap;
import java.util.Map;
import org.apache.archiva.repository.api.Repository;
import org.apache.archiva.repository.api.RepositoryFactory;
import org.apache.log4j.Logger;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;

public class DefaultRepositoryFactory implements RepositoryFactory
{
    private static final Logger log = Logger.getLogger(DefaultRepositoryFactory.class);

    private final RepositoryContentFactory repositoryContentFactory;

    public DefaultRepositoryFactory(RepositoryContentFactory repositoryContentFactory)
    {
        this.repositoryContentFactory = repositoryContentFactory;
    }

    public Map<String, Repository> getRepositories()
    {
        final Map<String, ManagedRepositoryContent> contentMap = repositoryContentFactory.getManagedContentMap();
        final HashMap<String, Repository> repositories = new HashMap<String, Repository>();
        for (final String repositoryId : contentMap.keySet())
        {
            final ManagedRepositoryContent content = contentMap.get(repositoryId);

            if (!content.getLocalPath().exists() && !content.getLocalPath().mkdirs())
            {
                log.error("Directory could not be created for repository <" + content.getId() + "> with missing directory");
            }

            repositories.put(repositoryId, content);
        }
        return repositories;
    }
}
