package org.apache.maven.archiva.repository;

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

import java.util.List;

/**
 * DefinedRepositories - maintains the list of defined repositories. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public interface DefinedRepositories
{
    /**
     * Get the entire list of repositories.
     * 
     * @return the list of repositories.
     */
    public List getAllRepositories();
    
    /**
     * Get the list of managed (local) repositories.
     * 
     * @return the list of managed (local) repositories.
     */
    public List getManagedRepositories();
    
    /**
     * Get the list of remote repositories.
     * 
     * @return the list of remote repositories.
     */
    public List getRemoteRepositories();
    
    /**
     * Add a repository.
     * 
     * @param repository the repository to add.
     */
    public void addRepository(Repository repository);
    
    /**
     * Remove a repository.
     * 
     * @param repository the repository to add.
     */
    public void removeRepository(Repository repository);
    
    /**
     * Get a repository using the provided repository key.
     *  
     * @param repositoryKey the repository key to find the repository via.
     * @return the repository associated with the provided Repository Key, or null if not found.
     */
    public Repository getRepository( String repositoryKey );
}
