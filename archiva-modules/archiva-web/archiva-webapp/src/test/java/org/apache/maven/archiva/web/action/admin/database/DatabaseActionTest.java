package org.apache.maven.archiva.web.action.admin.database;

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

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.DatabaseScanningConfiguration;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.easymock.MockControl;

/**
 * DatabaseActionTest
 */
public class DatabaseActionTest
    extends PlexusInSpringTestCase 
{   
    private DatabaseAction action;
    
    private MockControl archivaConfigControl;
    
    private ArchivaConfiguration archivaConfig;
    
    private Configuration config;
    
    protected void setUp() 
        throws Exception
    {
        super.setUp();

        archivaConfigControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfig = (ArchivaConfiguration) archivaConfigControl.getMock();

        action = new DatabaseAction();

        config = new Configuration();
        
        DatabaseScanningConfiguration databaseScanningConfig = new DatabaseScanningConfiguration();
        
        List<String> cleanUpConsumers = new ArrayList<String>();
        cleanUpConsumers.add( "not-present-remove-db-artifact" );
        cleanUpConsumers.add( "not-present-remove-db-project" );
        cleanUpConsumers.add( "not-present-remove-indexed" );
        
        List<String> unprocessedConsumers = new ArrayList<String>();
        unprocessedConsumers.add( "update-db-bytecode-stats" );
        unprocessedConsumers.add( "update-db-project" );
        unprocessedConsumers.add( "validate-repository-metadata" );
        
        databaseScanningConfig.setCleanupConsumers( cleanUpConsumers );
        databaseScanningConfig.setUnprocessedConsumers( unprocessedConsumers );

        config.setDatabaseScanning( databaseScanningConfig );
        
        setUpEnabledUnproccessedConsumers();
        
        action.setArchivaConfiguration( archivaConfig );
    }
    
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
    }

    public void testUpdateUnprocessedConsumers()
    throws Exception
    {
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        
        archivaConfig.save( config );
        archivaConfigControl.replay();
        
        String returnString = action.updateUnprocessedConsumers();
        
        List<String> results = config.getDatabaseScanning().getUnprocessedConsumers();
        
        assertEquals( action.SUCCESS, returnString );
        assertEquals( 3, results.size() );
    }
    
    public void testDisableAllUnprocessedConsumers( )
        throws Exception
    {
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config );
        
        archivaConfig.save( config );
        archivaConfigControl.replay();
        
        action.setEnabledUnprocessedConsumers( null );
        
        String returnString = action.updateUnprocessedConsumers();
        
        List<String> results = config.getDatabaseScanning().getUnprocessedConsumers();
        
        assertEquals( action.SUCCESS, returnString );
        assertEquals( 0, results.size() );
    }

    private void setUpEnabledUnproccessedConsumers( )
    {
        List<String> enabledUnprocessedConsumer = new ArrayList<String>();
        
        enabledUnprocessedConsumer.add( "update-db-bytecode-stats" );
        enabledUnprocessedConsumer.add( "update-db-project" );
        enabledUnprocessedConsumer.add( "validate-repository-metadata" );
        
        action.setEnabledUnprocessedConsumers( enabledUnprocessedConsumer );
    }    
}
