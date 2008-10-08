package org.apache.maven.archiva.web.xmlrpc.services;

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

import org.apache.maven.archiva.web.xmlrpc.api.AdministrationService;
import org.apache.maven.archiva.web.xmlrpc.api.ManagedRepository;
import org.apache.maven.archiva.web.xmlrpc.api.RemoteRepository;

public class AdministrationServiceImpl
    implements AdministrationService
{

    public boolean configureDatabaseConsumer( String consumerId, boolean enable )
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean configureRepositoryConsumer( String repoId, String consumerId, boolean enable )
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean deleteArtifact( String repoId, String groupId, String artifactId, String version )
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean executeDatabaseScanner()
    {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean executeRepositoryScanner( String repoId )
    {
        // TODO Auto-generated method stub
        return false;
    }

    public List<String> getAllDatabaseConsumers()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public List<String> getAllRepositoryConsumers( String repoId )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public List<ManagedRepository> getAllManagedRepositories()
    {
        return null;
    }

    public List<RemoteRepository> getAllRemoteRepositories()
    {
        return null;
    }
}
