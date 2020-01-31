package org.apache.archiva.repository.metadata;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.model.ArchivaRepositoryMetadata;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.storage.StorageAsset;

/**
 * Interface for reading metadata from a given file
 */
public interface MetadataReader
{
    /**
     * Reads the given metadata file and returns the corresponding metadata object.
     * @param asset The asset where the metadata should be read from
     * @return The parsed metadata
     * @throws RepositoryMetadataException if the metadata could not be read
     */
    ArchivaRepositoryMetadata read( StorageAsset asset ) throws RepositoryMetadataException;

    /**
     * Returns <code>true</code>, if the given path is a valid path for a metadata file, otherwise <code>false</code>
     * The implementation should not access the file directly, just use the path for validation.
     * @param path the path to the metadata file / asset
     * @return <code>true</code>, if the path is valid for a metadata file otherwise <code>false</code>
     */
    boolean isValidMetadataPath(String path);

    /**
     * Returns <code>true</code>, if this metadata reader instance can be used to read metadata for the
     * given repository type, otherwise <code>false</code>.
     *
     * @param repositoryType the repository type to check for
     * @return <code>true</code>, if this is a implementation for the given type, otherwise <code>false</code>
     */
    boolean isValidForType(RepositoryType repositoryType);
}
