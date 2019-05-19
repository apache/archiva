package org.apache.archiva.repository.maven2;

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

import org.apache.archiva.common.filelock.FileLockManager;
import org.apache.archiva.repository.*;
import org.apache.archiva.repository.content.FilesystemStorage;
import org.apache.archiva.repository.features.ArtifactCleanupFeature;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.features.StagingRepositoryFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

public class MavenRepositoryGroup extends AbstractRepositoryGroup implements EditableRepositoryGroup {

    private static final RepositoryCapabilities CAPABILITIES = new StandardCapabilities(
            new ReleaseScheme[] { ReleaseScheme.RELEASE, ReleaseScheme.SNAPSHOT },
            new String[] { MavenManagedRepository.DEFAULT_LAYOUT, MavenManagedRepository.LEGACY_LAYOUT},
            new String[] {},
            new String[] {ArtifactCleanupFeature.class.getName(), IndexCreationFeature.class.getName(),
                    StagingRepositoryFeature.class.getName()},
            true,
            true,
            true,
            true,
            false
    );

    private final Logger log = LoggerFactory.getLogger(MavenRepositoryGroup.class);

    private FileLockManager lockManager;
    private FilesystemStorage fsStorage;

    public MavenRepositoryGroup(String id, String name, Path repositoryBase, FileLockManager lockManager) {
        super(RepositoryType.MAVEN, id, name, repositoryBase);
        this.lockManager = lockManager;
        init();
    }

    public MavenRepositoryGroup(Locale primaryLocale, String id, String name, Path repositoryBase, FileLockManager lockManager) {
        super(primaryLocale, RepositoryType.MAVEN, id, name, repositoryBase);
        this.lockManager = lockManager;
        init();
    }

    private Path getRepositoryPath() {
        return getRepositoryBase().resolve(getId());
    }

    private void init() {
        setCapabilities(CAPABILITIES);
        try {
            fsStorage = new FilesystemStorage(getRepositoryPath(), lockManager);
        } catch (IOException e) {
            log.error("IOException while initializing repository group with path {}",getRepositoryBase());
            throw new RuntimeException("Fatal error while accessing repository path "+ getRepositoryBase(), e);
        }
        setStorage(fsStorage);
    }
}
