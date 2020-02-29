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

import org.apache.archiva.common.filelock.DefaultFileLockManager;
import org.apache.archiva.common.filelock.FileLockManager;
import org.apache.archiva.repository.ReleaseScheme;
import org.apache.archiva.repository.RepositoryCapabilities;
import org.apache.archiva.repository.RepositoryRequestInfo;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.StandardCapabilities;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.apache.archiva.repository.storage.RepositoryStorage;
import org.apache.archiva.repository.features.ArtifactCleanupFeature;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.features.StagingRepositoryFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/**
 *
 * Just a helper class, mainly used for unit tests.
 *
 *
 */
public class BasicManagedRepository extends AbstractManagedRepository

{
    Logger log = LoggerFactory.getLogger(BasicManagedRepository.class);
    ArtifactCleanupFeature artifactCleanupFeature = new ArtifactCleanupFeature(  );
    StagingRepositoryFeature stagingRepositoryFeature = new StagingRepositoryFeature( );

    static final StandardCapabilities CAPABILITIES = new StandardCapabilities( new ReleaseScheme[] {
        ReleaseScheme.RELEASE, ReleaseScheme.SNAPSHOT
    }, new String[] {"default"}, new String[0], new String[] {
        ArtifactCleanupFeature.class.toString(), IndexCreationFeature.class.toString(),
        StagingRepositoryFeature.class.toString()
    }, true, true, true, true, true  );

    public BasicManagedRepository( String id, String name, RepositoryStorage repositoryStorage )
    {
        super( RepositoryType.MAVEN, id, name, repositoryStorage );
        initFeatures();
    }

    public BasicManagedRepository( Locale primaryLocale, RepositoryType type, String id, String name, RepositoryStorage repositoryStorage )
    {
        super( primaryLocale, type, id, name, repositoryStorage);
        initFeatures();
    }

    private void initFeatures() {
        IndexCreationFeature indexCreationFeature = new IndexCreationFeature(this, this);
        addFeature( artifactCleanupFeature );
        addFeature( indexCreationFeature );
        addFeature( stagingRepositoryFeature );
    }

    @Override
    public boolean hasIndex( )
    {
        return true;
    }

    @Override
    public RepositoryCapabilities getCapabilities( )
    {
        return CAPABILITIES;
    }


    @Override
    public RepositoryRequestInfo getRequestInfo() {
        return null;
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
    public static BasicManagedRepository newFilesystemInstance(String id, String name, Path repositoryPath) throws IOException {
        FileLockManager lockManager = new DefaultFileLockManager();
        FilesystemStorage storage = new FilesystemStorage(repositoryPath, lockManager);
        return new BasicManagedRepository(id, name, storage);
    }

}
