package org.apache.archiva.repository.base;

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

import org.apache.archiva.repository.EditableRepositoryGroup;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RepositoryCapabilities;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.storage.RepositoryStorage;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Abstract repository group implementation.
 *
 */
public class AbstractRepositoryGroup extends AbstractRepository implements EditableRepositoryGroup
{

    private ListOrderedMap<String, ManagedRepository> repositories = new ListOrderedMap<>();

    private int mergedIndexTTL;

    private final ReadWriteLock rwl = new ReentrantReadWriteLock();

    private RepositoryCapabilities capabilities;

    public AbstractRepositoryGroup( RepositoryType type, String id, String name, RepositoryStorage storage) {
        super(type, id, name, storage);
    }

    public AbstractRepositoryGroup(Locale primaryLocale, RepositoryType type, String id, String name, RepositoryStorage storage) {
        super(primaryLocale, type, id, name, storage);
    }

    @Override
    public boolean hasIndex() {
        return true;
    }

    @Override
    public RepositoryCapabilities getCapabilities() {
        return capabilities;
    }


    @Override
    public void clearRepositories() {
        rwl.writeLock().lock();
        try {
            repositories.clear();
        } finally {
            rwl.writeLock().unlock();
        }
    }

    @Override
    public void setRepositories(List<ManagedRepository> newRepositories) {
        rwl.writeLock().lock();
        try {
            repositories.clear();
            for(ManagedRepository repo : newRepositories) {
                if (repo!=null)
                    repositories.put(repo.getId(), repo);
            }
        } finally {
            rwl.writeLock().unlock();
        }
    }

    @Override
    public void addRepository(ManagedRepository repository) {
        rwl.writeLock().lock();
        try {
            if (repository!=null)
                repositories.put(repository.getId(), repository);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    @Override
    public void addRepository(int index, ManagedRepository repository) {
        rwl.writeLock().lock();
        try {
            if (repository!=null)
                repositories.put(index, repository.getId(), repository);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    @Override
    public boolean removeRepository(ManagedRepository repository) {
        rwl.writeLock().lock();
        try {
            return repositories.remove(repository.getId(), repository);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    @Override
    public ManagedRepository removeRepository(String repoId) {
        rwl.writeLock().lock();
        try {
            return repositories.remove(repoId);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    @Override
    public void setMergedIndexTTL(int timeInSeconds) {
        this.mergedIndexTTL = timeInSeconds;
    }

    @Override
    public List<ManagedRepository> getRepositories() {
        rwl.readLock().lock();
        try {
            return repositories.valueList();
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public boolean contains(ManagedRepository repository) {
        rwl.readLock().lock();
        try {
            return repositories.containsValue(repository);
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public boolean contains(String id) {
        rwl.readLock().lock();
        try {
            return repositories.containsKey(id);
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public int getMergedIndexTTL() {
        return mergedIndexTTL;
    }

    protected void setCapabilities(RepositoryCapabilities capabilities) {
        this.capabilities = capabilities;
    }
}
