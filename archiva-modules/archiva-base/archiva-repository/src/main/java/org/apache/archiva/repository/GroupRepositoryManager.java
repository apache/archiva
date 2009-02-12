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
import java.util.List;
import org.apache.archiva.repository.api.RepositoryManager;
import org.apache.archiva.repository.api.RepositoryManagerWeight;
import org.apache.archiva.repository.api.ResourceContext;
import org.apache.archiva.repository.api.Status;
import org.apache.archiva.repository.api.SystemRepositoryManager;

@RepositoryManagerWeight(400)
public class GroupRepositoryManager implements RepositoryManager
{
    private final RepositoryManager proxyRepositoryManager;

    private final SystemRepositoryManager repositoryManager;

    public GroupRepositoryManager(RepositoryManager proxyRepositoryManager, SystemRepositoryManager repositoryManager)
    {
        this.proxyRepositoryManager = proxyRepositoryManager;
        this.repositoryManager = repositoryManager;
    }

    public boolean exists(String repositoryId)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ResourceContext handles(ResourceContext context)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean read(ResourceContext context, OutputStream os)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<Status> stat(ResourceContext context)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean write(ResourceContext context, InputStream is)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
