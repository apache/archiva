package org.apache.archiva.repository.mock;

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

import org.apache.archiva.indexer.ArchivaIndexManager;
import org.apache.archiva.indexer.ArchivaIndexingContext;
import org.apache.archiva.indexer.IndexCreationFailedException;
import org.apache.archiva.indexer.IndexUpdateFailedException;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryType;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service("archivaIndexManager#maven")
public class ArchivaIndexManagerMock implements ArchivaIndexManager {



    @Override
    public void pack(ArchivaIndexingContext context) throws IndexUpdateFailedException {

    }

    @Override
    public void scan(ArchivaIndexingContext context) throws IndexUpdateFailedException {

    }

    @Override
    public void update(ArchivaIndexingContext context, boolean fullUpdate) throws IndexUpdateFailedException {

    }

    @Override
    public void addArtifactsToIndex(ArchivaIndexingContext context, Collection<URI> artifactReference) throws IndexUpdateFailedException {

    }

    @Override
    public void removeArtifactsFromIndex(ArchivaIndexingContext context, Collection<URI> artifactReference) throws IndexUpdateFailedException {

    }

    @Override
    public boolean supportsRepository(RepositoryType type) {
        return true;
    }

    @Override
    public ArchivaIndexingContext createContext(Repository repository) throws IndexCreationFailedException {
        return null;
    }

    @Override
    public ArchivaIndexingContext reset(ArchivaIndexingContext context) throws IndexUpdateFailedException {
        return null;
    }

    @Override
    public ArchivaIndexingContext move(ArchivaIndexingContext context, Repository repo) throws IndexCreationFailedException {
        return null;
    }

    @Override
    public void updateLocalIndexPath(Repository repo) {

    }

    @Override
    public ArchivaIndexingContext mergeContexts(Repository destinationRepo, List<ArchivaIndexingContext> contexts, boolean packIndex) throws UnsupportedOperationException, IndexCreationFailedException {
        return null;
    }
}
