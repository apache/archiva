package org.apache.archiva.web.action.admin.scanning;

import org.apache.archiva.admin.repository.admin.DefaultArchivaAdministration;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.RepositoryScanningConfiguration;
import org.apache.archiva.web.action.AbstractActionTestCase;
import org.easymock.MockControl;

import java.util.ArrayList;
import java.util.List;

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

public class RepositoryScanningActionTest
    extends AbstractActionTestCase
{
    private RepositoryScanningAction action;

    private MockControl archivaConfigControl;

    private ArchivaConfiguration archivaConfig;

    private Configuration config;

    protected void setUp()
        throws Exception
    {

        super.setUp();

        archivaConfigControl = MockControl.createControl( ArchivaConfiguration.class );
        archivaConfig = (ArchivaConfiguration) archivaConfigControl.getMock();

        action = new RepositoryScanningAction();

        config = new Configuration();

        RepositoryScanningConfiguration repositoryScanningConfig = new RepositoryScanningConfiguration();

        repositoryScanningConfig.setKnownContentConsumers( createKnownContentConsumersList() );

        config.setRepositoryScanning( repositoryScanningConfig );

        DefaultArchivaAdministration archivaAdministration = new DefaultArchivaAdministration();
        archivaAdministration.setArchivaConfiguration( archivaConfig );
        action.setArchivaAdministration( archivaAdministration );

    }

    public void testUpdateKnownConsumers()
        throws Exception
    {
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config, 10 );

        archivaConfig.save( config );
        archivaConfig.save( config );
        archivaConfig.save( config );
        archivaConfig.save( config );
        archivaConfig.save( config );
        archivaConfig.save( config );
        archivaConfig.save( config );
        archivaConfig.save( config );
        archivaConfigControl.replay();

        setEnabledKnownContentConsumers();

        String returnString = action.updateKnownConsumers();

        List<String> results = config.getRepositoryScanning().getKnownContentConsumers();

        assertEquals( action.SUCCESS, returnString );
        assertEquals( "results " + results, 8, results.size() );
    }

    public void testDisableAllKnownConsumers()
        throws Exception
    {
        archivaConfigControl.expectAndReturn( archivaConfig.getConfiguration(), config, 10 );

        archivaConfig.save( config );
        archivaConfig.save( config );
        archivaConfig.save( config );
        archivaConfig.save( config );
        archivaConfig.save( config );
        archivaConfig.save( config );
        archivaConfig.save( config );
        archivaConfig.save( config );
        archivaConfigControl.replay();

        action.setEnabledKnownContentConsumers( null );

        String returnString = action.updateKnownConsumers();

        List<String> results = config.getRepositoryScanning().getKnownContentConsumers();

        assertEquals( action.SUCCESS, returnString );
        assertEquals( 0, results.size() );
    }

    private void setEnabledKnownContentConsumers()
    {
        action.setEnabledKnownContentConsumers( createKnownContentConsumersList() );
    }

    private List<String> createKnownContentConsumersList()
    {
        List<String> knownContentConsumers = new ArrayList<String>();
        knownContentConsumers.add( "auto-remove" );
        knownContentConsumers.add( "auto-rename" );
        knownContentConsumers.add( "create-missing-checksums" );
        knownContentConsumers.add( "index-content" );
        knownContentConsumers.add( "metadata-updater" );
        knownContentConsumers.add( "repository-purge" );
        knownContentConsumers.add( "update-db-artifact" );
        knownContentConsumers.add( "validate-checksums" );

        return knownContentConsumers;
    }
}
