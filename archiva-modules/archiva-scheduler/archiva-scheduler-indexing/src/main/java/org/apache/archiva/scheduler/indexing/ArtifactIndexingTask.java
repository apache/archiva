package org.apache.archiva.scheduler.indexing;

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

import org.apache.archiva.indexer.ArchivaIndexingContext;
import org.apache.archiva.components.taskqueue.Task;
import org.apache.archiva.repository.ManagedRepository;

import java.nio.file.Path;


public class ArtifactIndexingTask
    implements Task
{
    public enum Action
    {
        ADD,
        DELETE,
        FINISH
    }

    private final ManagedRepository repository;

    private final Path resourceFile;

    private final Action action;

    private final ArchivaIndexingContext context;

    private boolean executeOnEntireRepo = true;

    /**
     * @since 1.4-M1
     */
    private boolean onlyUpdate = false;

    public ArtifactIndexingTask( ManagedRepository repository, Path resourceFile, Action action,
                                 ArchivaIndexingContext context )
    {
        this.repository = repository;
        this.resourceFile = resourceFile;
        this.action = action;
        this.context = context;
    }

    public ArtifactIndexingTask( ManagedRepository repository, Path resourceFile, Action action,
                                 ArchivaIndexingContext context, boolean executeOnEntireRepo )
    {
        this( repository, resourceFile, action, context );
        this.executeOnEntireRepo = executeOnEntireRepo;
    }

    public ArtifactIndexingTask( ManagedRepository repository, Path resourceFile, Action action,
                                 ArchivaIndexingContext context, boolean executeOnEntireRepo, boolean onlyUpdate )
    {
        this( repository, resourceFile, action, context, executeOnEntireRepo );
        this.onlyUpdate = onlyUpdate;
    }

    public boolean isExecuteOnEntireRepo()
    {
        return executeOnEntireRepo;
    }

    public void setExecuteOnEntireRepo( boolean executeOnEntireRepo )
    {
        this.executeOnEntireRepo( executeOnEntireRepo );
    }

    public ArtifactIndexingTask executeOnEntireRepo( boolean executeOnEntireRepo )
    {
        this.executeOnEntireRepo = executeOnEntireRepo;
        return this;
    }

    @Override
    public long getMaxExecutionTime()
    {
        return 0;
    }

    public Path getResourceFile()
    {
        return resourceFile;
    }

    public Action getAction()
    {
        return action;
    }

    public ManagedRepository getRepository()
    {
        return repository;
    }

    public ArchivaIndexingContext getContext()
    {
        return context;
    }

    public boolean isOnlyUpdate()
    {
        return onlyUpdate;
    }

    public void setOnlyUpdate( boolean onlyUpdate )
    {
        this.onlyUpdate = onlyUpdate;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + action.hashCode();
        result = prime * result + repository.getId().hashCode();
        result = prime * result + ( ( resourceFile == null ) ? 0 : resourceFile.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        ArtifactIndexingTask other = (ArtifactIndexingTask) obj;
        if ( !action.equals( other.action ) )
        {
            return false;
        }
        if ( !repository.getId().equals( other.repository.getId() ) )
        {
            return false;
        }
        if ( resourceFile == null )
        {
            if ( other.resourceFile != null )
            {
                return false;
            }
        }
        else if ( !resourceFile.equals( other.resourceFile ) )
        {
            return false;
        }
        return true;
    }


    @Override
    public String toString()
    {
        return "ArtifactIndexingTask [action=" + action + ", repositoryId=" + repository.getId() + ", resourceFile="
            + resourceFile + "]";
    }

}
