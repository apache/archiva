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

import org.apache.archiva.repository.api.Repository;
import org.apache.archiva.repository.api.RepositoryFactory;
import org.apache.archiva.repository.api.RepositoryManagerException;
import org.apache.archiva.repository.api.ResourceContext;
import org.apache.archiva.repository.api.Status;
import org.apache.archiva.repository.api.SystemRepositoryManager;
import org.apache.archiva.repository.api.RepositoryManagerWeight;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

@RepositoryManagerWeight(9000)
public class DefaultRepositoryManager implements SystemRepositoryManager
{
    private final RepositoryFactory repositoryFactory;

    public DefaultRepositoryManager(RepositoryFactory repositoryFactory)
    {
        this.repositoryFactory = repositoryFactory;
    }

    public ResourceContext handles(ResourceContext context)
    {
        return context;
    }

    public boolean exists(String repositoryId)
    {
        return repositoryFactory.getRepositories().containsKey(repositoryId);
    }

    public boolean read(ResourceContext context, OutputStream os)
    {
        final Repository repository = repositoryFactory.getRepositories().get(context.getRepositoryId());
        if (repository == null)
        {
            return false;
        }

        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream(new File(repository.getLocalPath(), context.getLogicalPath()));
            IOUtils.copyLarge(fis, os);
            return true;
        }
        catch (IOException e)
        {
            throw new RepositoryManagerException("Could not read resource from " + context.getLogicalPath(), e);
        }
        finally
        {
            IOUtils.closeQuietly(fis);
        }
    }

    public boolean write(ResourceContext context, InputStream is)
    {
        final Repository repository = repositoryFactory.getRepositories().get(context.getRepositoryId());
        if (repository == null)
        {
            return false;
        }

        final File newResource = new File(repository.getLocalPath(), context.getLogicalPath());
        newResource.getParentFile().mkdirs();
        
        FileOutputStream fos = null;
        try
        {
            newResource.createNewFile();
            fos = new FileOutputStream(newResource);
            IOUtils.copy(is, fos);
            return true;
        }
        catch (IOException e)
        {
            FileUtils.deleteQuietly(newResource);
            throw new RepositoryManagerException("Could not write resource to " + context.getLogicalPath(), e);
        }
        finally
        {
            IOUtils.closeQuietly(fos);
        }
    }

    public List<Status> stat(ResourceContext context)
    {
        final Repository repository = repositoryFactory.getRepositories().get(context.getRepositoryId());
        if (repository != null)
        {
            final File file = new File(repository.getLocalPath(), context.getLogicalPath());
            if (file.exists())
            {
                final ArrayList<Status> result = new ArrayList<Status>();
                if (file.isDirectory())
                {
                    result.add(Status.fromFile(file));
                    for (final File child : file.listFiles())
                    {
                        result.add(Status.fromFile(child));
                    }
                }

                if (file.isFile())
                {
                    result.add(Status.fromFile(file));
                }
                return result;
            }
            return Collections.EMPTY_LIST;
        }
        return null;
    }
}
