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

import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;

/**
 * Obtain an index instance.
 *
 */
public interface RepositoryContentIndexFactory
{
    /**
     * Method to create an instance of the bytecode index.
     *
     * @param repository the repository to create the content index from.
     * @return the index instance
     */
    RepositoryContentIndex createBytecodeIndex( ManagedRepositoryConfiguration repository );
    
    /**
     * Method to create an instance of the file content index.
     *
     * @param repository the repository to create the file content index from.
     * @return the index instance
     */
    RepositoryContentIndex createFileContentIndex( ManagedRepositoryConfiguration repository );

    /**
     * Method to create an instance of the hashcode index.
     *
     * @param repository the repository to create the content index from.
     * @return the index instance
     */
    RepositoryContentIndex createHashcodeIndex( ManagedRepositoryConfiguration repository );
}
