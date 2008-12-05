package org.apache.archiva.web.xmlrpc.client;

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

import java.net.URL;
import java.util.List;

import org.apache.archiva.web.xmlrpc.api.AdministrationService;
import org.apache.archiva.web.xmlrpc.api.beans.ManagedRepository;
import org.apache.archiva.web.xmlrpc.api.beans.RemoteRepository;

import com.atlassian.xmlrpc.AuthenticationInfo;
import com.atlassian.xmlrpc.Binder;
import com.atlassian.xmlrpc.BindingException;
import com.atlassian.xmlrpc.DefaultBinder;

/**
 * TestClient
 * 
 * Test client for Archiva Web Services. 
 * To execute:
 * 
 * 1. set the <arguments> in the exec-maven-plugin config in the pom.xml in the following order:
 *    - url
 *    - username
 *    - password
 * 2. execute 'mvn exec:java' from the command-line
 * 
 * @version $Id$
 */
public class SampleClient
{   
    public static void main( String[] args ) 
    {       
        Binder binder = new DefaultBinder();
        
        try
        {
            AuthenticationInfo authnInfo = new AuthenticationInfo( args[1], args[2] );
            AdministrationService adminService = binder.bind( AdministrationService.class, new URL( args[0] ), authnInfo );
            List<ManagedRepository> managedRepos = adminService.getAllManagedRepositories();
            
            System.out.println( "\n******** Managed Repositories ********" );
            for( ManagedRepository managedRepo : managedRepos )
            {
                System.out.println( "=================================" );
                System.out.println( "Id: " + managedRepo.getId() );
                System.out.println( "Name: " + managedRepo.getName() );
                System.out.println( "Layout: " + managedRepo.getLayout() );
                System.out.println( "URL: " + managedRepo.getUrl() );
                System.out.println( "Releases: " + managedRepo.isReleases() );
                System.out.println( "Snapshots: " + managedRepo.isSnapshots() );
            }                
            
            System.out.println( "\n******** Remote Repositories ********" );
            List<RemoteRepository> remoteRepos = adminService.getAllRemoteRepositories();
            for( RemoteRepository remoteRepo : remoteRepos )
            {
                System.out.println( "=================================" );
                System.out.println( "Id: " + remoteRepo.getId() );
                System.out.println( "Name: " + remoteRepo.getName() );
                System.out.println( "Layout: " + remoteRepo.getLayout() );
                System.out.println( "URL: " + remoteRepo.getUrl() );
            }
            
            System.out.println( "\n******** Repository Consumers ********" );
            List<String> repoConsumers = adminService.getAllRepositoryConsumers();
            for( String consumer : repoConsumers )
            {
                System.out.println( consumer );
            }
            
            System.out.println( "\n******** Database Consumers ********" );
            List<String> dbConsumers = adminService.getAllDatabaseConsumers();
            for( String consumer : dbConsumers )
            {
                System.out.println( consumer );
            }
            
            Boolean success = adminService.configureRepositoryConsumer( "internal", "repository-purge", true );
            System.out.println( "\nConfigured repo consumer 'repository-purge' : " +
                ( (Boolean) success ).booleanValue() );
            
            success = adminService.configureDatabaseConsumer( "update-db-bytecode-stats", false );
            System.out.println( "\nConfigured db consumer 'update-db-bytecode-stats' : " +
                ( (Boolean) success ).booleanValue() );
            
            success = adminService.executeRepositoryScanner( "internal" );
            System.out.println( "\nExecuted repo scanner of repository 'internal' : " +
                ( (Boolean) success ).booleanValue() );
            
            success = adminService.executeDatabaseScanner();
            System.out.println( "\nExecuted database scanner : " + ( (Boolean) success ).booleanValue() );
           
            /* delete artifact */
            /* 
             * NOTE: before enabling & invoking deleteArtifact, make sure that the repository and artifact exists first!
             *                      
            success = adminService.deleteArtifact( "internal", "javax.activation", "activation", "1.1" );
            System.out.println( "\nDeleted artifact 'javax.activation:activation:1.1' from repository 'internal' : " +
                ( (Boolean) success ).booleanValue() );
            */
        }
        catch ( BindingException e )
        {
            e.printStackTrace();             
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }
}
