package org.apache.archiva.stagerepository.merge;

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

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.filter.Filter;

import java.util.List;

public interface RepositoryMerger
{
    void merge( MetadataRepository metadataRepository, String sourceRepoId, String targetRepoId )
        throws Exception;

    void merge( MetadataRepository metadataRepository, String sourceRepoId, String targetRepoId,
                Filter<ArtifactMetadata> filter )
        throws Exception;

    public List<ArtifactMetadata> getConflictingArtifacts( MetadataRepository metadataRepository, String sourceRepo,
                                                           String targetRepo )
        throws Exception;
}