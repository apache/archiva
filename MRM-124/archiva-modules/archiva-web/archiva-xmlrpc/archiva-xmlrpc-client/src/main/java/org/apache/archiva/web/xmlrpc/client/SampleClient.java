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

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.archiva.web.xmlrpc.api.AdministrationService;
import org.apache.archiva.web.xmlrpc.api.beans.ManagedRepository;
import org.apache.archiva.web.xmlrpc.api.beans.RemoteRepository;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcClientRequestImpl;
import org.apache.xmlrpc.client.util.ClientFactory;

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
 * @author 
 * @version $Id$
 */
public class SampleClient
{   
    public static void main( String[] args ) 
    {       
        try
        {
            XmlRpcClient client = new XmlRpcClient();
            
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL( new URL( args[0] ) );
            config.setBasicUserName( args[1] );
            config.setBasicPassword( args[2] );
            config.setEnabledForExtensions( true );
            
            client.setConfig( config );
            
            /* managed repositories */
            Object[] params = new Object[]{};
            Object[] managedRepos = (Object[])
                 client.execute( "AdministrationService.getAllManagedRepositories", params );                        
            
            System.out.println( "\n******** Managed Repositories ********" );
            for( int i = 0; i < managedRepos.length; i++ )
            {
                System.out.println( "=================================" );
                ManagedRepository managedRepo = new ManagedRepository(); 
                try
                {   
                    BeanUtils.populate( managedRepo, (Map)managedRepos[i] );
                }
                catch ( IllegalAccessException e )
                {
                    e.printStackTrace();
                }
                catch ( InvocationTargetException e )
                {
                    e.printStackTrace();
                }
                System.out.println( "Id: " + managedRepo.getId() );
                System.out.println( "Name: " + managedRepo.getName() );
                System.out.println( "Layout: " + managedRepo.getLayout() );
                System.out.println( "URL: " + managedRepo.getUrl() );
                System.out.println( "Releases: " + managedRepo.isReleases() );
                System.out.println( "Snapshots: " + managedRepo.isSnapshots() );
            }
                        
            /* remote repositories */
            params = new Object[]{};
            Object[] remoteRepos = (Object[])
                 client.execute( "AdministrationService.getAllRemoteRepositories", params );
            
            System.out.println( "\n******** Remote Repositories ********" );
            for( int i = 0; i < remoteRepos.length; i++ )
            {
                System.out.println( "=================================" );
                RemoteRepository remoteRepo = new RemoteRepository();
                
                try
                {   
                    BeanUtils.populate( remoteRepo, (Map) remoteRepos[i] );
                }
                catch ( IllegalAccessException e )
                {
                    e.printStackTrace();
                }
                catch ( InvocationTargetException e )
                {
                    e.printStackTrace();
                }
                System.out.println( "Id: " + remoteRepo.getId() );
                System.out.println( "Name: " + remoteRepo.getName() );
                System.out.println( "Layout: " + remoteRepo.getLayout() );
                System.out.println( "URL: " + remoteRepo.getUrl() );                    
            }
            
            /* repo consumers */
            params = new Object[]{};
            Object[] repoConsumers = (Object[])
                 client.execute( "AdministrationService.getAllRepositoryConsumers", params );
            
            System.out.println( "\n******** Repository Consumers ********" );
            for( int i = 0; i < repoConsumers.length; i++ )
            {   
                System.out.println( repoConsumers[i] );                    
            }
            
            /* db consumers */
            params = new Object[]{};
            Object[] dbConsumers = (Object[])
                 client.execute( "AdministrationService.getAllDatabaseConsumers", params );
            
            System.out.println( "\n******** Database Consumers ********" );
            for( int i = 0; i < dbConsumers.length; i++ )
            {   
                System.out.println( dbConsumers[i] );                    
            }
            
            /* configure repo consumer */
            Object[] configureRepoConsumerParams = new Object[] { "internal", "repository-purge", true };            
            Object configured = client.execute( "AdministrationService.configureRepositoryConsumer", configureRepoConsumerParams );            
            System.out.println( "\nConfigured repo consumer 'repository-purge' : " + ( ( Boolean ) configured ).booleanValue() );
            
            
            /* configure db consumer */
            Object[] configureDbConsumerParams = new Object[] { "update-db-bytecode-stats", false };            
            configured = client.execute( "AdministrationService.configureDatabaseConsumer", configureDbConsumerParams );            
            System.out.println( "\nConfigured db consumer 'update-db-bytecode-stats' : " + ( ( Boolean ) configured ).booleanValue() );            
            
            
            /* execute repo scanner */
            Object[] executeRepoScanParams = new Object[] { "internal" };            
            configured = client.execute( "AdministrationService.executeRepositoryScanner", executeRepoScanParams );            
            System.out.println( "\nExecuted repo scanner of repository 'internal' : " + ( ( Boolean ) configured ).booleanValue() );
            
            
            /* execute db scanner */
            Object[] executeDbScanParams = new Object[] {};            
            configured = client.execute( "AdministrationService.executeDatabaseScanner", executeDbScanParams );
            System.out.println( "\nExecuted database scanner : " + ( ( Boolean ) configured ).booleanValue() );            
            
        }
        catch ( MalformedURLException e )
        {
            e.printStackTrace();
        }
        catch ( XmlRpcException e )
        {
            e.printStackTrace();
        }           
    }
}
