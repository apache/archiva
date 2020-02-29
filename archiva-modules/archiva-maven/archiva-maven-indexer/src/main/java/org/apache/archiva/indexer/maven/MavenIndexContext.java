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

import org.apache.archiva.common.filelock.DefaultFileLockManager;
import org.apache.archiva.indexer.ArchivaIndexingContext;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.maven.index.context.IndexingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.sql.Date;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Maven implementation of index context
 */
public class MavenIndexContext implements ArchivaIndexingContext {

    private static final Logger log = LoggerFactory.getLogger(ArchivaIndexingContext.class);


    private AtomicBoolean openStatus = new AtomicBoolean(false);
    private IndexingContext delegate;
    private Repository repository;
    private StorageAsset dir = null;

    protected MavenIndexContext(Repository repository, IndexingContext delegate) {
        this.delegate = delegate;
        this.repository = repository;
        this.openStatus.set(true);

    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public Repository getRepository() {
        return repository;
    }

    @Override
    public StorageAsset getPath() {
        if (dir==null) {
            StorageAsset repositoryDirAsset = repository.getAsset("");
            Path repositoryDir = repositoryDirAsset.getFilePath().toAbsolutePath();
            Path indexDir = delegate.getIndexDirectoryFile().toPath();
            if (indexDir.startsWith(repositoryDir)) {
                dir = repository.getAsset(repositoryDir.relativize(indexDir).toString());
            } else {
                try {
                    FilesystemStorage storage = new FilesystemStorage(indexDir, new DefaultFileLockManager());
                    dir = storage.getAsset("");
                } catch (IOException e) {
                    log.error("Error occured while creating storage for index dir");
                }
            }
        }
        return dir;
    }

    @Override
    public boolean isEmpty() throws IOException {
        return Files.list(delegate.getIndexDirectoryFile().toPath()).count()==0;
    }

    @Override
    public void commit() throws IOException {
        delegate.commit();
    }

    @Override
    public void rollback() throws IOException {
        delegate.rollback();
    }

    @Override
    public void optimize() throws IOException {
        delegate.optimize();
    }

    @Override
    public void close(boolean deleteFiles) throws IOException {
        if (openStatus.compareAndSet(true,false)) {
            try {
                delegate.close(deleteFiles);
            } catch (NoSuchFileException e) {
                // Ignore missing directory
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (openStatus.compareAndSet(true,false)) {
            try {
                delegate.close(false);
            } catch (NoSuchFileException e) {
                // Ignore missing directory
            }
        }
    }

    @Override
    public boolean isOpen() {
        return openStatus.get();
    }

    @Override
    public void purge() throws IOException {
        delegate.purge();
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return IndexingContext.class.equals(clazz);
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <T> T getBaseContext(Class<T> clazz) throws UnsupportedOperationException {
        if (IndexingContext.class.equals(clazz)) {
            return (T) delegate;
        } else {
            throw new UnsupportedOperationException("The class "+clazz+" is not supported by the maven indexer");
        }
    }

    @Override
    public Set<String> getGroups() throws IOException {
        return delegate.getAllGroups();
    }

    @Override
    public void updateTimestamp(boolean save) throws IOException {
        delegate.updateTimestamp(save);
    }

    @Override
    public void updateTimestamp(boolean save, ZonedDateTime time) throws IOException {
        delegate.updateTimestamp(save, Date.from(time.toInstant()));
    }


}
