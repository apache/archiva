package org.apache.archiva.indexer;

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

import org.apache.archiva.common.utils.PathUtil;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryEvent;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

@Service("indexManager#none")
public class GenericIndexManager implements ArchivaIndexManager {

    private final Logger log = LoggerFactory.getLogger(GenericIndexManager.class);

    public static final String DEFAULT_INDEXER_DIR = ".indexer";

    @Override
    public void pack(ArchivaIndexingContext context) {

    }

    @Override
    public void scan(ArchivaIndexingContext context) {

    }

    @Override
    public void update(ArchivaIndexingContext context, boolean fullUpdate) {

    }

    @Override
    public void addArtifactsToIndex(ArchivaIndexingContext context, Collection<URI> artifactReference) {

    }

    @Override
    public void removeArtifactsFromIndex(ArchivaIndexingContext context, Collection<URI> artifactReference) {

    }

    @Override
    public boolean supportsRepository(RepositoryType type) {
        return false;
    }

    @Override
    public ArchivaIndexingContext createContext(Repository repository) {
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
        if (repo.supportsFeature(IndexCreationFeature.class)) {
            IndexCreationFeature icf = repo.getFeature(IndexCreationFeature.class).get();
            try {
                icf.setLocalIndexPath(getIndexPath(repo));
            } catch (IOException e) {
                log.error("Could not set local index path for {}. New URI: {}", repo.getId(), icf.getIndexPath());
            }
        }
    }

    private Path getIndexPath(Repository repo) throws IOException {
        IndexCreationFeature icf = repo.getFeature(IndexCreationFeature.class).get();
        Path repoDir = repo.getLocalPath();
        URI indexDir = icf.getIndexPath();
        Path indexDirectory = null;
        if ( ! StringUtils.isEmpty(indexDir.toString( ) ) )
        {

            indexDirectory = PathUtil.getPathFromUri( indexDir );
            // not absolute so create it in repository directory
            if ( !indexDirectory.isAbsolute( ) )
            {
                indexDirectory = repoDir.resolve( indexDirectory );
            }
        }
        else
        {
            indexDirectory = repoDir.resolve( DEFAULT_INDEXER_DIR);
        }

        if ( !Files.exists( indexDirectory ) )
        {
            Files.createDirectories( indexDirectory );
        }
        return indexDirectory;
    }

}
