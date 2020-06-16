package org.apache.archiva.repository;

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
 * RemoteRepositoryContent interface for interacting with a remote repository in an abstract way, 
 * without the need for processing based on URLs, or working with the database. 
 *
 *
 */
public interface RemoteRepositoryContent extends RepositoryContent
{
    /**
     * <p>
     * Convenience method to get the repository id.
     * </p>
     * 
     * <p>
     * Equivalent to calling <code>.getRepository().getId()</code>
     * </p>
     * 
     * @return the repository id.
     */
    String getId();

    /**
     * Get the repository configuration associated with this
     * repository content.
     * 
     * @return the repository that is associated with this repository content.
     */
    RemoteRepository getRepository();

    /**
     * Set the repository configuration to associate with this
     * repository content.
     * 
     * @param repo the repository to associate with this repository content.
     */
    void setRepository( RemoteRepository repo );

}
