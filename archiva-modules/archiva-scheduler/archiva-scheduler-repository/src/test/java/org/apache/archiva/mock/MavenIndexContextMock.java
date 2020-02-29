package org.apache.archiva.mock;

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
import org.apache.archiva.repository.storage.fs.FilesystemAsset;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.maven.index.context.IndexingContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.sql.Date;
import java.time.ZonedDateTime;
import java.util.Set;

/**
 * Maven implementation of index context
 */
public class MavenIndexContextMock implements ArchivaIndexingContext {

    private boolean open = true;

    private IndexingContext delegate;
    private Repository repository;
    private FilesystemStorage filesystemStorage;

    MavenIndexContextMock( Repository repository, IndexingContext delegate) {
        this.delegate = delegate;
        this.repository = repository;
        try {
            filesystemStorage = new FilesystemStorage(delegate.getIndexDirectoryFile().toPath().getParent(), new DefaultFileLockManager());
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        return new FilesystemAsset(filesystemStorage, delegate.getIndexDirectoryFile().toPath().getFileName().toString(), delegate.getIndexDirectoryFile().toPath());

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
        this.open = false;
        try {
            delegate.close(deleteFiles);
        } catch (NoSuchFileException e) {
            // Ignore missing directory
        }
    }

    @Override
    public void close() throws IOException {
        this.open = false;
        try {
            delegate.close(false);
        } catch (NoSuchFileException e) {
            // Ignore missing directory
        }
    }

    @Override
    public boolean isOpen() {
        return open;
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
