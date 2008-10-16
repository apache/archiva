package org.apache.archiva.web.xmlrpc.api;

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

import org.apache.archiva.web.xmlrpc.api.beans.ManagedRepository;
import org.apache.archiva.web.xmlrpc.api.beans.RemoteRepository;

import com.atlassian.xmlrpc.ServiceObject;

@ServiceObject( "AdministrationService" )
public interface AdministrationService
{    
    /**
     * Executes repository scanner on the given repository.
     *  
     * @param repoId id of the repository to be scanned
     * @return
     * @throws Exception
     */
    public Boolean executeRepositoryScanner( String repoId ) throws Exception;
   
    /**
     * Executes the database scanner.
     * 
     * @return
     * @throws Exception
     */
    public Boolean executeDatabaseScanner() throws Exception;

    /**
     * Gets all available database consumers.
     * @return
     */
    public List<String> getAllDatabaseConsumers();
   
    /**
     * Configures (enable or disable) database consumer.
     * 
     * @param consumerId id of the database consumer
     * @param enable flag whether to enable or disable the specified consumer
     * @return
     * @throws Exception
     */
    public Boolean configureDatabaseConsumer( String consumerId, boolean enable ) throws Exception;

    /**
     * Gets all available repository consumers.
     * 
     * @return
     */
    public List<String> getAllRepositoryConsumers();
    
 // TODO should we already implement config of consumers per repository?
    /**
     * Configures (enable or disable) repository consumer.
     * 
     * @param repoId
     * @param consumerId
     * @param enable
     * @return
     * @throws Exception
     */    
    public Boolean configureRepositoryConsumer( String repoId, String consumerId, boolean enable ) throws Exception;

    /**
     * Gets all managed repositories.
     * 
     * @return
     */
    public List<ManagedRepository> getAllManagedRepositories();

    /**
     * Gets all remote repositories.
     * 
     * @return
     */
    public List<RemoteRepository> getAllRemoteRepositories();

    /**
     * Deletes given artifact from the specified repository.
     * 
     * @param repoId id of the repository where the artifact to be deleted resides
     * @param groupId groupId of the artifact to be deleted
     * @param artifactId artifactId of the artifact to be deleted
     * @param version version of the artifact to be deleted
     * @return
     * @throws Exception
     */
    public Boolean deleteArtifact( String repoId, String groupId, String artifactId, String version )
        throws Exception;    
    
    //TODO 
    // consider the following as additional services:
    // - getAllConfiguredRepositoryConsumers( String repoId ) - list all enabled consumers for the repo
    // - getAllConfiguredDatabaseConsumers() - list all enabled db consumers
}
