package org.apache.maven.archiva.discoverer.filter;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;

/**
 * Ability to filter repository metadata lists.
 *
 * @todo should be in maven-artifact
 */
public interface MetadataFilter
{
    /**
     * Whether to include this metadata in the filtered list.
     *
     * @param metadata  the metadata
     * @param timestamp the time to compare against - it will be included if it doesn't exist or is outdated
     * @return whether to include it
     */
    boolean include( RepositoryMetadata metadata, long timestamp );
}
