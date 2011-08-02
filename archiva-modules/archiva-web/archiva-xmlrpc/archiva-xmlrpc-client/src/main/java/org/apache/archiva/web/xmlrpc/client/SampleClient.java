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

import com.atlassian.xmlrpc.ApacheBinder;
import com.atlassian.xmlrpc.Binder;
import com.atlassian.xmlrpc.BindingException;
import com.atlassian.xmlrpc.ConnectionInfo;
import org.apache.archiva.web.xmlrpc.api.AdministrationService;
import org.apache.archiva.web.xmlrpc.api.PingService;
import org.apache.archiva.web.xmlrpc.api.beans.ManagedRepository;
import org.apache.archiva.web.xmlrpc.api.beans.RemoteRepository;

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

        Binder binder = new ApacheBinder();
        ConnectionInfo info = new ConnectionInfo();
        info.setUsername( args[1] );
        info.setPassword( args[2] );

        try
        {
            AdministrationService adminService = binder.bind( AdministrationService.class, new URL( args[0] ), info );
            PingService pingService = binder.bind( PingService.class, new URL( args[0] ), info );
                       
            System.out.println( "Ping : " + pingService.ping() );
            
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
            
            Boolean success = adminService.configureRepositoryConsumer( "internal", "repository-purge", true );
            System.out.println( "\nConfigured repo consumer 'repository-purge' : " +
                ( (Boolean) success ).booleanValue() );
            
            success = adminService.executeRepositoryScanner( "internal" );
            System.out.println( "\nExecuted repo scanner of repository 'internal' : " +
                ( (Boolean) success ).booleanValue() );

            /** add, get and delete managed repo **/
            /*
             * NOTE: change the location of the repository to be added depending on your platform!
             *
            success = adminService.addManagedRepository( "test", "default", "Test Repo",
                                               "/tmp/archiva-repo/test/", true, true, false, "0 0 * * * ?" );
            System.out.println( "\nSuccessfully added managed repository 'test'" );

            ManagedRepository repo = adminService.getManagedRepository( "test" );
            System.out.println( "****** managed repo info ******" );
            System.out.println( "ID: " + repo.getId() );
            System.out.println( "NAME: " + repo.getName() );
            System.out.println( "LAYOUT: " + repo.getLayout() );
            System.out.println( "URL: " + repo.getUrl() );

            success = adminService.deleteManagedRepository( "test" );
            System.out.println( "\nSuccessfully deleted managed repository 'test'" );
            */

            /* delete artifact */
            /* 
             * NOTE: before enabling & invoking deleteArtifact, make sure that the repository and artifact exists first!
             *                      
            success = adminService.deleteArtifact( "internal", "javax.activation", "activation", "1.1" );
            System.out.println( "\nDeleted artifact 'javax.activation:activation:1.1' from repository 'internal' : " +
                ( (Boolean) success ).booleanValue() );
            */
            
            /* quick search */            
            /*
             * NOTE: before enabling & invoking search service, make sure that the artifacts you're searching
             *      for has been indexed already in order to get results
             *        
            SearchService searchService = binder.bind( SearchService.class, new URL( args[0] ), authnInfo );
            List<Artifact> artifacts = searchService.quickSearch( "org" );
            
            System.out.println( "\n************ Search Results for 'org' *************" );
            for( Artifact artifact : artifacts )
            {
                System.out.println( "Artifact: " + artifact.getGroupId() + ":" + artifact.getArtifactId() +
                                    ":" + artifact.getVersion() );
            }            
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
