package org.apache.archiva.repository.features;

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


import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryEventListener;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

import static org.apache.archiva.indexer.ArchivaIndexManager.DEFAULT_INDEX_PATH;
import static org.apache.archiva.indexer.ArchivaIndexManager.DEFAULT_PACKED_INDEX_PATH;

/**
 *
 * This feature provides some information about index creation.
 *
 */
public class IndexCreationFeature extends AbstractFeature implements RepositoryFeature<IndexCreationFeature>{


    private boolean skipPackedIndexCreation = false;

    private URI indexPath;

    private URI packedIndexPath;

    private StorageAsset localIndexPath;

    private StorageAsset localPackedIndexPath;

    private Repository repo;

    public IndexCreationFeature(Repository repoId, RepositoryEventListener listener) {
        super(listener);
        this.repo = repoId;
        try {
            this.indexPath = new URI(DEFAULT_INDEX_PATH);
            this.packedIndexPath = new URI(DEFAULT_PACKED_INDEX_PATH);
        } catch (URISyntaxException e) {
            // Does not happen
            e.printStackTrace();
        }
    }

    public IndexCreationFeature(boolean skipPackedIndexCreation) {
        this.skipPackedIndexCreation = skipPackedIndexCreation;
        try {
            this.indexPath = new URI(DEFAULT_INDEX_PATH);
            this.packedIndexPath = new URI(DEFAULT_PACKED_INDEX_PATH);
        } catch (URISyntaxException e) {
            // Does not happen
            e.printStackTrace();
        }
    }

    @Override
    public IndexCreationFeature get() {
        return this;
    }

    /**
     * Returns true, if no packed index files should be created.
     * @return True, if no packed index files are created, otherwise false.
     */
    public boolean isSkipPackedIndexCreation() {
        return skipPackedIndexCreation;
    }

    /**
     * Sets the flag for packed index creation.
     *
     * @param skipPackedIndexCreation
     */
    public void setSkipPackedIndexCreation(boolean skipPackedIndexCreation) {
        this.skipPackedIndexCreation = skipPackedIndexCreation;
    }

    /**
     * Returns the path that is used to store the index.
     * @return the uri (may be relative or absolute)
     */
    public URI getIndexPath( )
    {
        return indexPath;
    }

    /**
     * Sets the path that is used to store the index.
     * @param indexPath the uri to the index path (may be relative)
     */
    public void setIndexPath( URI indexPath )
    {
        URI oldVal = this.indexPath;
        this.indexPath = indexPath;
        raiseEvent(IndexCreationEvent.indexUriChange(repo, oldVal, this.indexPath));

    }


    public boolean hasIndex() {
        return this.indexPath!=null && !StringUtils.isEmpty( this.indexPath.getPath() );
    }

    /**
     * Returns the path where the index is stored physically.
     *
     * @return
     */
    public StorageAsset getLocalIndexPath() {
        return localIndexPath;
    }

    /**
     * Sets the path where the index is stored physically. This method should only be used by the
     * MavenIndexProvider implementations.
     *
     * @param localIndexPath
     */
    public void setLocalIndexPath(StorageAsset localIndexPath) {
        this.localIndexPath = localIndexPath;
    }


    /**
     * Returns the path of the packed index.
     * @return
     */
    public URI getPackedIndexPath() {
        return packedIndexPath;
    }

    /**
     * Sets the path (relative or absolute) of the packed index.
     * @param packedIndexPath
     */
    public void setPackedIndexPath(URI packedIndexPath) {
        URI oldVal = this.packedIndexPath;
        this.packedIndexPath = packedIndexPath;
        raiseEvent(IndexCreationEvent.packedIndexUriChange(repo, oldVal, this.packedIndexPath));
    }

    /**
     * Returns the directory where the packed index is stored.
     * @return
     */
    public StorageAsset getLocalPackedIndexPath() {
        return localPackedIndexPath;
    }

    /**
     * Sets the path where the packed index is stored physically. This method should only be used by the
     * MavenIndexProvider implementations.
     *
     * @param localPackedIndexPath
     */
    public void setLocalPackedIndexPath(StorageAsset localPackedIndexPath) {
        this.localPackedIndexPath = localPackedIndexPath;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("IndexCreationFeature:{").append("skipPackedIndexCreation=").append(skipPackedIndexCreation)
                .append(",indexPath=").append(indexPath).append(",packedIndexPath=").append(packedIndexPath).append("}");
        return sb.toString();
    }
}
