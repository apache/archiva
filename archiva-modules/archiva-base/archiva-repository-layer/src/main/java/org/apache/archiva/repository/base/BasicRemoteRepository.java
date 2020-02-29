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
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.StandardCapabilities;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.apache.archiva.repository.storage.RepositoryStorage;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.features.RemoteIndexFeature;
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
public class BasicRemoteRepository extends AbstractRemoteRepository

{
    Logger log = LoggerFactory.getLogger(BasicRemoteRepository.class);

    RemoteIndexFeature remoteIndexFeature = new RemoteIndexFeature();
    IndexCreationFeature indexCreationFeature = new IndexCreationFeature(true);


    static final StandardCapabilities CAPABILITIES = new StandardCapabilities( new ReleaseScheme[] {
        ReleaseScheme.RELEASE, ReleaseScheme.SNAPSHOT
    }, new String[] {"default"}, new String[0], new String[] {
        RemoteIndexFeature.class.toString(),
            IndexCreationFeature.class.toString()
    }, true, true, true, true, true  );

    public BasicRemoteRepository( String id, String name, RepositoryStorage storage)
    {
        super( RepositoryType.MAVEN, id, name, storage);
        initFeatures();
    }

    public BasicRemoteRepository( Locale primaryLocale, RepositoryType type, String id, String name, RepositoryStorage storage )
    {
        super( primaryLocale, type, id, name, storage );
        initFeatures();
    }

    private void initFeatures() {
        addFeature( remoteIndexFeature );
        addFeature( indexCreationFeature );
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


    public static BasicRemoteRepository newFilesystemInstance(String id, String name, Path basePath) throws IOException {
        FileLockManager lockManager = new DefaultFileLockManager();
        FilesystemStorage storage = new FilesystemStorage(basePath.resolve(id), lockManager);
        return new BasicRemoteRepository(id, name, storage);
    }
}
