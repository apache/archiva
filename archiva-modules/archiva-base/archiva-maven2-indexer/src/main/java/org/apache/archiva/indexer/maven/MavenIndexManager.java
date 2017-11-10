package org.apache.archiva.indexer.maven;

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
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.indexer.ArchivaIndexManager;
import org.apache.archiva.indexer.ArchivaIndexingContext;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.features.RemoteIndexFeature;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index_shaded.lucene.index.IndexFormatTooOldException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Maven implementation of index manager
 */
@Service("archivaIndexManager#maven")
public class MavenIndexManager implements ArchivaIndexManager {

    private static final Logger log = LoggerFactory.getLogger(MavenIndexManager.class);

    @Inject
    private Indexer indexer;

    @Inject
    private List<? extends IndexCreator> indexCreators;

    @Inject
    private ArchivaConfiguration archivaConfiguration;

    @Override
    public void pack(ArchivaIndexingContext context) {

    }

    @Override
    public void scan(ArchivaIndexingContext context, boolean update) {

    }

    @Override
    public void update(ArchivaIndexingContext context, URI remoteUpdateUri, boolean fullUpdate) {

    }

    @Override
    public void addArtifactToIndex(ArchivaIndexingContext context, ArtifactReference artifactReference) {

    }

    @Override
    public void removeArtifactFromIndex(ArchivaIndexingContext context, ArtifactReference artifactReference) {

    }

    @Override
    public boolean supportsRepository(RepositoryType type) {
        return false;
    }

    @Override
    public ArchivaIndexingContext createContext(Repository remoteRepository) throws IOException {
        IndexingContext mvnCtx = null;
        if (remoteRepository instanceof RemoteRepository) {
            mvnCtx = createRemoteContext((RemoteRepository) remoteRepository);
        } else if (remoteRepository instanceof ManagedRepository) {
            // TODO: Implement managed repository index creation
            mvnCtx = null;
        }
        MavenIndexContext context = new MavenIndexContext(remoteRepository, mvnCtx);
        return null;
    }

    private IndexingContext createRemoteContext(RemoteRepository remoteRepository) throws IOException {
        Path appServerBase = archivaConfiguration.getAppServerBaseDir();

        String contextKey = "remote-" + remoteRepository.getId();

        // create remote repository path
        Path repoDir = appServerBase.resolve( "data").resolve( "remotes" ).resolve( remoteRepository.getId() );
        if ( !Files.exists(repoDir) )
        {
            Files.createDirectories(repoDir);
        }

        Path indexDirectory = null;

        // is there configured indexDirectory ?
        if (remoteRepository.supportsFeature(RemoteIndexFeature.class)) {
            RemoteIndexFeature rif = remoteRepository.getFeature(RemoteIndexFeature.class).get();
            indexDirectory = PathUtil.getPathFromUri(rif.getIndexUri());
            if (!indexDirectory.isAbsolute()) {
                indexDirectory = repoDir.resolve(indexDirectory);
            }

            // if not configured use a default value
            if (indexDirectory == null) {
                indexDirectory = repoDir.resolve(".index");
            }
            if (!Files.exists(indexDirectory)) {
                Files.createDirectories(indexDirectory);
            }

            try {

                return indexer.createIndexingContext(contextKey, remoteRepository.getId(), repoDir.toFile(), indexDirectory.toFile(),
                        remoteRepository.getLocation() == null ? null : remoteRepository.getLocation().toString(),
                        calculateIndexRemoteUrl(remoteRepository.getLocation(), rif),
                        true, false,
                        indexCreators);
            } catch (IndexFormatTooOldException e) {
                // existing index with an old lucene format so we need to delete it!!!
                // delete it first then recreate it.
                log.warn("the index of repository {} is too old we have to delete and recreate it", //
                        remoteRepository.getId());
                org.apache.archiva.common.utils.FileUtils.deleteDirectory(indexDirectory);
                return indexer.createIndexingContext(contextKey, remoteRepository.getId(), repoDir.toFile(), indexDirectory.toFile(),
                        remoteRepository.getLocation() == null ? null : remoteRepository.getLocation().toString(),
                        calculateIndexRemoteUrl(remoteRepository.getLocation(), rif),
                        true, false,
                        indexCreators);

            }
        } else {
            throw new IOException("No remote index defined");
        }
    }

    private String calculateIndexRemoteUrl(URI baseUri, RemoteIndexFeature rif) {
        if (rif.getIndexUri()==null) {
            return baseUri.resolve(".index").toString();
        } else {
            return baseUri.resolve(rif.getIndexUri()).toString();
        }
    }


}
