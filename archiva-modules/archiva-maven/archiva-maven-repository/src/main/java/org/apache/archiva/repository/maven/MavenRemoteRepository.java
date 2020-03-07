package org.apache.archiva.repository.maven;

import org.apache.archiva.common.filelock.DefaultFileLockManager;
import org.apache.archiva.common.filelock.FileLockManager;
import org.apache.archiva.repository.base.AbstractRemoteRepository;
import org.apache.archiva.repository.ReleaseScheme;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.RepositoryCapabilities;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.StandardCapabilities;
import org.apache.archiva.repository.UnsupportedFeatureException;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.features.RemoteIndexFeature;
import org.apache.archiva.repository.features.RepositoryFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

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

/**
 * Maven2 remote repository implementation
 */
public class MavenRemoteRepository extends AbstractRemoteRepository
    implements RemoteRepository
{

    Logger log = LoggerFactory.getLogger(MavenRemoteRepository.class);

    final private RemoteIndexFeature remoteIndexFeature = new RemoteIndexFeature();
    final private IndexCreationFeature indexCreationFeature;

    private static final RepositoryCapabilities CAPABILITIES = new StandardCapabilities(
        new ReleaseScheme[] { ReleaseScheme.RELEASE, ReleaseScheme.SNAPSHOT },
        new String[] { MavenManagedRepository.DEFAULT_LAYOUT, MavenManagedRepository.LEGACY_LAYOUT},
        new String[] {},
        new String[] {RemoteIndexFeature.class.getName(), IndexCreationFeature.class.getName()},
        true,
        true,
        true,
        true,
        false
    );

    public MavenRemoteRepository(String id, String name, FilesystemStorage storage)
    {
        super( RepositoryType.MAVEN, id, name, storage );
        this.indexCreationFeature = new IndexCreationFeature(this, this);

    }

    public MavenRemoteRepository( Locale primaryLocale, String id, String name, FilesystemStorage storage )
    {
        super( primaryLocale, RepositoryType.MAVEN, id, name, storage );
        this.indexCreationFeature = new IndexCreationFeature(this, this);
    }

    @Override
    public boolean hasIndex( )
    {
        return remoteIndexFeature.hasIndex();
    }

    @Override
    public RepositoryCapabilities getCapabilities( )
    {
        return CAPABILITIES;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <T extends RepositoryFeature<T>> RepositoryFeature<T> getFeature( Class<T> clazz ) throws UnsupportedFeatureException
    {
        if (RemoteIndexFeature.class.equals( clazz )) {
            return (RepositoryFeature<T>) remoteIndexFeature;
        } else if (IndexCreationFeature.class.equals(clazz)) {
            return (RepositoryFeature<T>) indexCreationFeature;
        } else {
            throw new UnsupportedFeatureException(  );
        }
    }

    @Override
    public <T extends RepositoryFeature<T>> boolean supportsFeature( Class<T> clazz )
    {
        if ( RemoteIndexFeature.class.equals(clazz) || IndexCreationFeature.class.equals(clazz)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return super.toString()+", remoteIndexFeature="+remoteIndexFeature.toString()+", indexCreationFeature="+indexCreationFeature.toString();
    }

    public static MavenRemoteRepository newLocalInstance(String id, String name, Path basePath) throws IOException {
        FileLockManager lockManager = new DefaultFileLockManager();
        FilesystemStorage storage = new FilesystemStorage(basePath.resolve(id), lockManager);
        return new MavenRemoteRepository(id, name, storage);
    }
}
