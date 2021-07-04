package org.apache.archiva.repository.base.group;
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

import org.apache.archiva.common.filelock.DefaultFileLockManager;
import org.apache.archiva.common.filelock.FileLockManager;
import org.apache.archiva.repository.EditableRepositoryGroup;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.ReleaseScheme;
import org.apache.archiva.repository.RepositoryCapabilities;
import org.apache.archiva.repository.RepositoryGroup;
import org.apache.archiva.repository.RepositoryState;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.StandardCapabilities;
import org.apache.archiva.repository.base.AbstractRepository;
import org.apache.archiva.repository.base.managed.BasicManagedRepository;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.storage.RepositoryStorage;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class BasicRepositoryGroup extends AbstractRepository implements EditableRepositoryGroup
{
    private static final RepositoryCapabilities CAPABILITIES = new StandardCapabilities(
        new ReleaseScheme[] { ReleaseScheme.RELEASE, ReleaseScheme.SNAPSHOT },
        new String[] {},
        new String[] {},
        new String[] {IndexCreationFeature.class.getName()},
        false,
        false,
        false,
        false,
        false
    );

    private int mergedIndexTtl  = 0;
    private boolean hasIndex = false;

    private final Logger log = LoggerFactory.getLogger(BasicRepositoryGroup.class);

    private List<ManagedRepository> repositories = new ArrayList<>(  );

    public BasicRepositoryGroup( String id, String name, RepositoryStorage repositoryStorage )
    {
        super( RepositoryType.MAVEN, id, name, repositoryStorage );
        IndexCreationFeature feature = new IndexCreationFeature( this, null );
        feature.setLocalIndexPath( repositoryStorage.getRoot( ).resolve(".indexer") );
        feature.setLocalPackedIndexPath( repositoryStorage.getRoot( ).resolve(".index") );
        addFeature( feature );
        setLastState( RepositoryState.CREATED );
    }

    @Override
    public List<ManagedRepository> getRepositories( )
    {
        return repositories;
    }

    @Override
    public boolean contains( ManagedRepository repository )
    {
        return repositories.contains( repository );
    }

    @Override
    public boolean contains( String id )
    {
        return repositories.stream( ).anyMatch( v -> id.equals( v.getId( ) ) );
    }

    @Override
    public int getMergedIndexTTL( )
    {
        return mergedIndexTtl;
    }

    @Override
    public boolean hasIndex( )
    {
        return hasIndex;
    }

    @Override
    public RepositoryCapabilities getCapabilities( )
    {
        return CAPABILITIES;
    }

    @Override
    public void clearRepositories( )
    {
        this.repositories.clear( );
    }

    @Override
    public void setRepositories( List<ManagedRepository> repositories )
    {
        this.repositories.clear();
        this.repositories.addAll( repositories );
    }

    @Override
    public void addRepository( ManagedRepository repository )
    {
        if ( !this.repositories.contains( repository ) )
        {
            this.repositories.add( repository );
        }
    }

    @Override
    public void addRepository( int index, ManagedRepository repository )
    {
        if (!this.repositories.contains( repository )) {
            this.repositories.add( index, repository );
        }
    }

    @Override
    public boolean removeRepository( ManagedRepository repository )
    {
        return this.repositories.remove( repository );
    }

    @Override
    public ManagedRepository removeRepository( String repoId )
    {
        for (ManagedRepository repo : this.repositories) {

            if (repoId.equals( repo.getId() )) {
                this.repositories.remove( repo );
                return repo;
            }
        }
        return null;
    }

    @Override
    public void setMergedIndexTTL( int timeInSeconds )
    {
        this.mergedIndexTtl = timeInSeconds;
    }

    /**
     * Creates a filesystem based repository instance. The path is built by basePath/repository-id
     *
     * @param id The repository id
     * @param name The name of the repository
     * @param repositoryPath The path to the repository
     * @return The repository instance
     * @throws IOException
     */
    public static BasicRepositoryGroup newFilesystemInstance( String id, String name, Path repositoryPath) throws IOException {
        FileLockManager lockManager = new DefaultFileLockManager();
        FilesystemStorage storage = new FilesystemStorage(repositoryPath, lockManager);
        return new BasicRepositoryGroup(id, name, storage);
    }

}
