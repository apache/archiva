package org.apache.maven.archiva.indexer;

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

import java.io.File;

/**
 * Obtain an index instance.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface RepositoryArtifactIndexFactory
{
    /**
     * Plexus role.
     */
    String ROLE = RepositoryArtifactIndexFactory.class.getName();

    /**
     * Method to create an instance of the standard index.
     *
     * @param indexPath the path where the index will be created/updated
     * @return the index instance
     */
    RepositoryArtifactIndex createStandardIndex( File indexPath );

    /**
     * Method to create an instance of the minimal index.
     *
     * @param indexPath the path where the index will be created/updated
     * @return the index instance
     */
    RepositoryArtifactIndex createMinimalIndex( File indexPath );
}
