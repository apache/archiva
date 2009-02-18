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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.archiva.repository.api.InvalidOperationException;
import org.apache.archiva.repository.api.MutableResourceContext;
import org.apache.archiva.repository.api.RepositoryManager;
import org.apache.archiva.repository.api.RepositoryManagerWeight;
import org.apache.archiva.repository.api.ResourceContext;
import org.apache.archiva.repository.api.Status;
import org.apache.archiva.repository.api.SystemRepositoryManager;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.RepositoryGroupConfiguration;

@RepositoryManagerWeight(400)
public class GroupRepositoryManager implements RepositoryManager
{
    private final ArchivaConfiguration archivaConfiguration;

    private final RepositoryManager proxyRepositoryManager;

    private final SystemRepositoryManager systemRepositoryManager;

    public GroupRepositoryManager(ArchivaConfiguration archivaConfiguration, RepositoryManager proxyRepositoryManager, SystemRepositoryManager systemRepositoryManager)
    {
        this.archivaConfiguration = archivaConfiguration;
        this.proxyRepositoryManager = proxyRepositoryManager;
        this.systemRepositoryManager = systemRepositoryManager;
    }

    public boolean exists(String repositoryId)
    {
        return (getGroupConfiguration(repositoryId) != null);
    }

    public ResourceContext handles(ResourceContext context)
    {
        if (getGroupConfiguration(context.getRepositoryId()) != null)
        {
            return context;
        }
        return null;
    }

    public boolean read(ResourceContext context, OutputStream os)
    {
        final RepositoryGroupConfiguration groupConfiguration = getGroupConfiguration(context.getRepositoryId());
        for (String repositoryId : groupConfiguration.getRepositories() )
        {
            final MutableResourceContext resourceContext = new MutableResourceContext(context);
            resourceContext.setRepositoryId(repositoryId);
            if (systemRepositoryManager.read(context, os))
            {
                return true;
            }
        }
        return false;
    }
    
    public boolean write(ResourceContext context, InputStream is)
    {
        throw new InvalidOperationException("Repository Groups are not writable: " + context.getRepositoryId());
    }

    public List<Status> stat(ResourceContext context)
    {
        final RepositoryGroupConfiguration groupConfiguration = getGroupConfiguration(context.getRepositoryId());

        final LinkedHashMap<String, Status> statusMap = new LinkedHashMap<String, Status>();
        for (final String repositoryId : groupConfiguration.getRepositories())
        {
            final MutableResourceContext resourceContext = new MutableResourceContext(context);
            resourceContext.setRepositoryId(repositoryId);

            ResourceContext rc = proxyRepositoryManager.handles(resourceContext);
            if (rc != null)
            {
                addStatResultToMap(statusMap, rc, proxyRepositoryManager);
            }
            else
            {
                rc = systemRepositoryManager.handles(resourceContext);
                if (rc != null)
                {
                    addStatResultToMap(statusMap, rc, systemRepositoryManager);
                }
            }
        }
        return new ArrayList<Status>(statusMap.values());
    }

    private void addStatResultToMap(final Map<String, Status> statusMap, final ResourceContext resourceContext, final RepositoryManager repositoryManager)
    {
        for (final Status status : repositoryManager.stat(resourceContext))
        {
            statusMap.put(status.getName(), status);
        }
    }

    private RepositoryGroupConfiguration getGroupConfiguration(final String repositoryId)
    {
        return archivaConfiguration.getConfiguration().getRepositoryGroupsAsMap().get( repositoryId );
    }
}
