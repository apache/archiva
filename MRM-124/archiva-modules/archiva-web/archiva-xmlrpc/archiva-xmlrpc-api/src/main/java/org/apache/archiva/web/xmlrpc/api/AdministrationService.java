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

import com.atlassian.xmlrpc.ServiceObject;

@ServiceObject( "Administration" )
public interface AdministrationService
{
    public boolean executeRepositoryScanner( String repoId );

    public boolean executeDatabaseScanner();

    public List<String> getAllDatabaseConsumers();

    public boolean configureDatabaseConsumer( String consumerId, boolean enable );

    // TODO should we already implement config of consumers per repository?
    public boolean configureRepositoryConsumer( String repoId, String consumerId, boolean enable );

    public List<String> getAllRepositoryConsumers( String repoId );

    public List<ManagedRepository> getAllManagedRepositories();

    public List<RemoteRepository> getAllRemoteRepositories();

    public boolean deleteArtifact( String repoId, String groupId, String artifactId, String version );
}
