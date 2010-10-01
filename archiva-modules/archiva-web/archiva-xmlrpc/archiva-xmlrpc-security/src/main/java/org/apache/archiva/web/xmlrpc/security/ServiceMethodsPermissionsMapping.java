package org.apache.archiva.web.xmlrpc.security;

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

import java.util.Arrays;
import java.util.List;

/**
 * ServiceMethodsPermissionsMapping
 *
 * Used by the XmlRpcAuthenticationHandler to check the permissions specific to the requested service method.
 * New methods in exposed services must be registered in the appropriate operation below.
 *
 * @version $Id: ServiceMethodsPermissionsMapping.java
 */
public class ServiceMethodsPermissionsMapping
{
    public static final List<String> SERVICE_METHODS_FOR_OPERATION_MANAGE_CONFIGURATION =
        Arrays.asList( "AdministrationService.configureRepositoryConsumer",
                       "AdministrationService.configureDatabaseConsumer",
                       "AdministrationService.executeDatabaseScanner",
                       "AdministrationService.getAllManagedRepositories",
                       "AdministrationService.getAllRemoteRepositories",
                       "AdministrationService.getAllDatabaseConsumers",
                       "AdministrationService.getAllRepositoryConsumers", 
                       "AdministrationService.deleteArtifact", 
                       "AdministrationService.addManagedRepository",
                       "AdministrationService.deleteManagedRepository", "AdministrationService.getManagedRepository",
                       "AdministrationService.merge");

    public static final List<String> SERVICE_METHODS_FOR_OPERATION_RUN_INDEXER =
        Arrays.asList( "AdministrationService.executeRepositoryScanner" );

    public static final List<String> SERVICE_METHODS_FOR_OPERATION_REPOSITORY_ACCESS =
        Arrays.asList( "SearchService.quickSearch", "SearchService.getArtifactByChecksum",
                       "SearchService.getArtifactVersions", "SearchService.getArtifactVersionsByDate",
                       "SearchService.getDependencies", "SearchService.getDependencyTree",
                       "SearchService.getDependees" );
    
    public static final String PING = "PingService.ping";
}
