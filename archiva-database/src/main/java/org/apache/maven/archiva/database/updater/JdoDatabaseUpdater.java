package org.apache.maven.archiva.database.updater;

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

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.DatabaseScanningConfiguration;
import org.apache.maven.archiva.consumers.ArchivaArtifactConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.constraints.ArtifactsProcessedConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * JdoDatabaseUpdater
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.database.updater.DatabaseUpdater" 
 *   role-hint="jdo" 
 */
public class JdoDatabaseUpdater
    extends AbstractLogEnabled
    implements DatabaseUpdater, RegistryListener, Initializable
{
    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration configuration;

    /**
     * The collection of available consumers.
     * @plexus.requirement role="org.apache.maven.archiva.consumers.ArchivaArtifactConsumer"
     */
    private Map availableConsumers;

    /**
     * The list of active consumers for unprocessed content.
     */
    private List activeUnprocessedConsumers = new ArrayList();

    /**
     * The list of active consumers for processed content.
     */
    private List activeProcessedConsumers = new ArrayList();

    /**
     * The list of registry (configuration) property names that will trigger a refresh of the activeConsumers list.
     */
    private List propertyNameTriggers = new ArrayList();

    public void update()
        throws ArchivaDatabaseException
    {
        updateAllUnprocessed();
        updateAllProcessed();
    }

    public void updateAllUnprocessed()
        throws ArchivaDatabaseException
    {
        List unprocessedArtifacts = dao.getArtifactDAO().queryArtifacts( new ArtifactsProcessedConstraint( false ) );

        beginConsumerLifecycle( this.activeUnprocessedConsumers );

        try
        {
            // Process each consumer.
            Iterator it = unprocessedArtifacts.iterator();
            while ( it.hasNext() )
            {
                ArchivaArtifact artifact = (ArchivaArtifact) it.next();

                if ( !artifact.getModel().isProcessed() )
                {
                    updateUnprocessed( artifact );
                }
            }
        }
        finally
        {
            consumerConsumerLifecycle( this.activeUnprocessedConsumers );
        }
    } 

    public void updateAllProcessed()
        throws ArchivaDatabaseException
    {
        List processedArtifacts = dao.getArtifactDAO().queryArtifacts( new ArtifactsProcessedConstraint( true ) );

        beginConsumerLifecycle( this.activeProcessedConsumers );

        try
        {
            // Process each consumer.
            Iterator it = processedArtifacts.iterator();
            while ( it.hasNext() )
            {
                ArchivaArtifact artifact = (ArchivaArtifact) it.next();

                if ( !artifact.getModel().isProcessed() )
                {
                    updateProcessed( artifact );
                }
            }
        }
        finally
        {
            consumerConsumerLifecycle( this.activeProcessedConsumers );
        }
    }

    private void consumerConsumerLifecycle( List consumers )
    {
        Iterator it = consumers.iterator();
        while ( it.hasNext() )
        {
            ArchivaArtifactConsumer consumer = (ArchivaArtifactConsumer) it.next();
            consumer.completeScan();
        }
    }

    private void beginConsumerLifecycle( List consumers )
    {
        Iterator it = consumers.iterator();
        while ( it.hasNext() )
        {
            ArchivaArtifactConsumer consumer = (ArchivaArtifactConsumer) it.next();
            consumer.beginScan();
        }
    }

    public void updateUnprocessed( ArchivaArtifact artifact )
        throws ArchivaDatabaseException
    {
        Iterator it = this.activeUnprocessedConsumers.iterator();
        while ( it.hasNext() )
        {
            ArchivaArtifactConsumer consumer = (ArchivaArtifactConsumer) it.next();
            try
            {
                consumer.processArchivaArtifact( artifact );
            }
            catch ( ConsumerException e )
            {
                getLogger().warn( "Unable to consume (unprocessed) artifact: " + artifact );
            }
        }

        artifact.getModel().setWhenProcessed( new Date() );
        dao.getArtifactDAO().saveArtifact( artifact );
    }

    public void updateProcessed( ArchivaArtifact artifact )
        throws ArchivaDatabaseException
    {
        Iterator it = this.activeProcessedConsumers.iterator();
        while ( it.hasNext() )
        {
            ArchivaArtifactConsumer consumer = (ArchivaArtifactConsumer) it.next();
            try
            {
                consumer.processArchivaArtifact( artifact );
            }
            catch ( ConsumerException e )
            {
                getLogger().warn( "Unable to consume (processed)  artifact: " + artifact );
            }
        }
    }

    private void updateActiveConsumers()
    {
        this.activeUnprocessedConsumers.clear();
        this.activeProcessedConsumers.clear();

        DatabaseScanningConfiguration dbScanning = configuration.getConfiguration().getDatabaseScanning();
        if ( dbScanning == null )
        {
            getLogger().error( "No Database Consumers found!" );
            return;
        }

        this.activeUnprocessedConsumers.addAll( getActiveConsumerList( dbScanning.getUnprocessedConsumers() ) );
        this.activeProcessedConsumers.addAll( getActiveConsumerList( dbScanning.getCleanupConsumers() ) );
    }

    private List getActiveConsumerList( List potentialConsumerList )
    {
        if ( ( potentialConsumerList == null ) || ( potentialConsumerList.isEmpty() ) )
        {
            return Collections.EMPTY_LIST;
        }

        List ret = new ArrayList();

        Iterator it = potentialConsumerList.iterator();
        while ( it.hasNext() )
        {
            String consumerName = (String) it.next();
            if ( !availableConsumers.containsKey( consumerName ) )
            {
                getLogger().warn( "Requested Consumer [" + consumerName + "] does not exist.  Disabling." );
                continue;
            }

            ret.add( consumerName );
        }

        return ret;
    }

    public void initialize()
        throws InitializationException
    {
        propertyNameTriggers = new ArrayList();
        propertyNameTriggers.add( "databaseScanning" );
        propertyNameTriggers.add( "unprocessedConsumers" );
        propertyNameTriggers.add( "unprocessedConsumer" );
        propertyNameTriggers.add( "processedConsumers" );
        propertyNameTriggers.add( "processedConsumer" );

        configuration.addChangeListener( this );
        updateActiveConsumers();
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( propertyNameTriggers.contains( propertyName ) )
        {
            updateActiveConsumers();
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* nothing to do here */
    }
}
