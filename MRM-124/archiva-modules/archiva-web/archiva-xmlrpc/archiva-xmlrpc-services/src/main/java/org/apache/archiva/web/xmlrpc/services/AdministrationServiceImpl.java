package org.apache.archiva.web.xmlrpc.services;

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

import java.util.ArrayList;
import java.util.List;

import org.apache.archiva.web.xmlrpc.api.AdministrationService;
import org.apache.archiva.web.xmlrpc.api.beans.ManagedRepository;
import org.apache.archiva.web.xmlrpc.api.beans.RemoteRepository;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.DatabaseScanningConfiguration;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.RepositoryScanningConfiguration;
import org.apache.maven.archiva.consumers.DatabaseCleanupConsumer;
import org.apache.maven.archiva.consumers.DatabaseUnprocessedArtifactConsumer;
import org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.database.updater.DatabaseConsumers;
import org.apache.maven.archiva.repository.scanner.RepositoryContentConsumers;
import org.codehaus.plexus.registry.RegistryException;

/**
 * AdministrationServiceImpl
 * 
 * @version $Id: AdministrationServiceImpl.java
 */
public class AdministrationServiceImpl
    implements AdministrationService
{
    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;
    
    private RepositoryContentConsumers repoConsumersUtil;
    
    private DatabaseConsumers dbConsumersUtil;
    
    /**
     * @see AdministrationService#configureDatabaseConsumer(String, boolean)
     */
    public boolean configureDatabaseConsumer( String consumerId, boolean enable ) throws Exception
    {
        List<DatabaseCleanupConsumer> cleanupConsumers = dbConsumersUtil.getAvailableCleanupConsumers();
        List<DatabaseUnprocessedArtifactConsumer> unprocessedConsumers =
            dbConsumersUtil.getAvailableUnprocessedConsumers();
        
        boolean found = false;
        boolean isCleanupConsumer = false;        
        for( DatabaseCleanupConsumer consumer : cleanupConsumers )
        {
            if( consumer.getId().equals( consumerId ) )
            {
                found = true;
                isCleanupConsumer = true;
                break;
            }
        }
        
        if( !found )
        {
            for( DatabaseUnprocessedArtifactConsumer consumer : unprocessedConsumers )
            {
                if( consumer.getId().equals( consumerId ) )
                {
                    found = true;
                    break;
                }
            }
        }
        
        if( !found )
        {
            throw new Exception( "Invalid database consumer." );
        }
        
        Configuration config = archivaConfiguration.getConfiguration();
        DatabaseScanningConfiguration dbScanningConfig = config.getDatabaseScanning();
        
        if( isCleanupConsumer )
        {
            dbScanningConfig.addCleanupConsumer( consumerId );            
        }
        else
        {
            dbScanningConfig.addUnprocessedConsumer( consumerId );
        }
        
        config.setDatabaseScanning( dbScanningConfig );        
        saveConfiguration( config );
        
        return true;
    }

    /**
     * @see AdministrationService#configureRepositoryConsumer(String, String, boolean)
     */
    public boolean configureRepositoryConsumer( String repoId, String consumerId, boolean enable )
        throws Exception
    {
        // TODO use repoId once consumers are configured per repository! (MRM-930)
        
        List<KnownRepositoryContentConsumer> knownConsumers = repoConsumersUtil.getAvailableKnownConsumers();
        List<InvalidRepositoryContentConsumer> invalidConsumers = repoConsumersUtil.getAvailableInvalidConsumers();
        
        boolean found = false;
        boolean isKnownContentConsumer = false;
        for( KnownRepositoryContentConsumer consumer : knownConsumers )
        {
            if( consumer.getId().equals( consumerId ) )
            {
                found = true;
                isKnownContentConsumer = true;
                break;
            }
        }
        
        if( !found )
        {
            for( InvalidRepositoryContentConsumer consumer : invalidConsumers )
            {
                if( consumer.getId().equals( consumerId ) )
                {
                    found = true;
                    break;
                }
            }
        }
        
        if( !found )
        {
            throw new Exception( "Invalid repository consumer." );
        }
        
        Configuration config = archivaConfiguration.getConfiguration();
        RepositoryScanningConfiguration repoScanningConfig = config.getRepositoryScanning();
        
        if( isKnownContentConsumer )
        {
            repoScanningConfig.addKnownContentConsumer( consumerId );
        }
        else
        {
            repoScanningConfig.addInvalidContentConsumer( consumerId );
        }
        
        config.setRepositoryScanning( repoScanningConfig );        
        saveConfiguration( config );
        
        return true;
    }
    
    /**
     * @see AdministrationService#deleteArtifact(String, String, String, String)
     */
    public boolean deleteArtifact( String repoId, String groupId, String artifactId, String version ) throws Exception
    {
        // TODO implement delete artifact in Archiva
        
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * @see AdministrationService#executeDatabaseScanner()
     */
    public boolean executeDatabaseScanner() throws Exception
    {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * @see AdministrationService#executeRepositoryScanner(String)
     */
    public boolean executeRepositoryScanner( String repoId ) throws Exception
    {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * @see AdministrationService#getAllDatabaseConsumers()
     */
    public List<String> getAllDatabaseConsumers()
    {
        List<String> consumers = new ArrayList<String>();
        
        List<DatabaseCleanupConsumer> cleanupConsumers = dbConsumersUtil.getAvailableCleanupConsumers();
        List<DatabaseUnprocessedArtifactConsumer> unprocessedConsumers = dbConsumersUtil.getAvailableUnprocessedConsumers();
        
        for( DatabaseCleanupConsumer consumer : cleanupConsumers )
        {
            consumers.add( consumer.getId() );
        }  
        
        for( DatabaseUnprocessedArtifactConsumer consumer : unprocessedConsumers )
        {
            consumers.add( consumer.getId() );
        } 
        
        return consumers;
    }

    /**
     * @see AdministrationService#getAllRepositoryConsumers()
     */
    public List<String> getAllRepositoryConsumers()
    {
        List<String> consumers = new ArrayList<String>();
                
        List<KnownRepositoryContentConsumer> knownConsumers = repoConsumersUtil.getAvailableKnownConsumers();
        List<InvalidRepositoryContentConsumer> invalidConsumers = repoConsumersUtil.getAvailableInvalidConsumers();
        
        for( KnownRepositoryContentConsumer consumer : knownConsumers )
        {
            consumers.add( consumer.getId() );
        }
        
        for( InvalidRepositoryContentConsumer consumer : invalidConsumers )
        {
            consumers.add( consumer.getId() );
        }

        return consumers;
    }

    /**
     * @see AdministrationService#getAllManagedRepositories()
     */
    public List<ManagedRepository> getAllManagedRepositories()
    {
        return null;
    }

    /**
     * @see AdministrationService#getAllRemoteRepositories()
     */
    public List<RemoteRepository> getAllRemoteRepositories()
    {
        return null;
    }

    private void saveConfiguration( Configuration config )
        throws Exception
    {
        try
        {
            archivaConfiguration.save( config );
        }
        catch(  RegistryException e )
        {
            throw new Exception( "Error occurred in the registry." );
        }
        catch ( IndeterminateConfigurationException e )
        {
            throw new Exception( "Error occurred while saving the configuration." );    
        }
    }
    
    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }

    public void setRepoConsumersUtil( RepositoryContentConsumers consumerUtil )
    {
        this.repoConsumersUtil = consumerUtil;
    }    
    
    public void setDbConsumersUtil( DatabaseConsumers consumerUtil )
    {
        this.dbConsumersUtil = consumerUtil;
    }   
}
