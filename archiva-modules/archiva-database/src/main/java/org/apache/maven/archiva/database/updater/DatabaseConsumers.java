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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.OrPredicate;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.DatabaseScanningConfiguration;
import org.apache.maven.archiva.consumers.functors.PermanentConsumerPredicate;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * DatabaseConsumers 
 *
 * @version $Id$
 */
public class DatabaseConsumers
    implements ApplicationContextAware
{    
    private Logger log = LoggerFactory.getLogger( DatabaseConsumers.class );
    
    private ArchivaConfiguration archivaConfiguration;

    private Predicate selectedCleanupConsumers;

    private Predicate selectedUnprocessedConsumers;
    
    private ApplicationContext applicationContext;

    public DatabaseConsumers( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
        
        Predicate permanentConsumers = new PermanentConsumerPredicate();

        selectedCleanupConsumers = new OrPredicate( permanentConsumers, new SelectedCleanupConsumersPredicate() );
        selectedUnprocessedConsumers = new OrPredicate( permanentConsumers, new SelectedUnprocessedConsumersPredicate() );
    }
    
    class SelectedUnprocessedConsumersPredicate
        implements Predicate
    {
        public boolean evaluate( Object object )
        {
            boolean satisfies = false;

            if ( object instanceof DatabaseUnprocessedArtifactConsumer )
            {
                DatabaseUnprocessedArtifactConsumer consumer = (DatabaseUnprocessedArtifactConsumer) object;
                DatabaseScanningConfiguration config = archivaConfiguration.getConfiguration().getDatabaseScanning();

                return config.getUnprocessedConsumers().contains( consumer.getId() );
            }

            return satisfies;
        }
    }

    class SelectedCleanupConsumersPredicate
        implements Predicate
    {
        public boolean evaluate( Object object )
        {
            boolean satisfies = false;

            if ( object instanceof DatabaseCleanupConsumer )
            {
                DatabaseCleanupConsumer consumer = (DatabaseCleanupConsumer) object;
                DatabaseScanningConfiguration config = archivaConfiguration.getConfiguration().getDatabaseScanning();

                return config.getCleanupConsumers().contains( consumer.getId() );
            }

            return satisfies;
        }
    }

    public void setApplicationContext( ApplicationContext applicationContext )
        throws BeansException
    {
        this.applicationContext = applicationContext;
    }
    
    /**
     * Get the {@link List} of {@link DatabaseUnprocessedArtifactConsumer} objects
     * for those consumers selected due to the configuration.
     * 
     * @return the list of selected {@link DatabaseUnprocessedArtifactConsumer} objects.
     */
    @SuppressWarnings("unchecked")
    public List<ArchivaArtifactConsumer> getSelectedUnprocessedConsumers()
    {
        List<ArchivaArtifactConsumer> ret = new ArrayList<ArchivaArtifactConsumer>();
        ret.addAll( CollectionUtils.select( getAvailableUnprocessedConsumers(), selectedUnprocessedConsumers ) );
        return ret;
    }

    /**
     * Get the {@link List} of {@link DatabaseCleanupConsumer} objects for those
     * consumers selected due to the configuration.
     * 
     * @return the list of selected {@link DatabaseCleanupConsumer} objects.
     */
    @SuppressWarnings("unchecked")
    public List<ArchivaArtifactConsumer> getSelectedCleanupConsumers()
    {
        List<ArchivaArtifactConsumer> ret = new ArrayList<ArchivaArtifactConsumer>();
        ret.addAll( CollectionUtils.select( getAvailableCleanupConsumers(), selectedCleanupConsumers ) );
        return ret;
    }

    /**
     * Get the complete {@link List} of {@link DatabaseUnprocessedArtifactConsumer} objects
     * that are available in the system, regardless of configuration.
     * 
     * @return the list of all available {@link DatabaseUnprocessedArtifactConsumer} objects.
     */
    @SuppressWarnings("unchecked")
    public List<DatabaseUnprocessedArtifactConsumer> getAvailableUnprocessedConsumers()
    {       
        return new ArrayList<DatabaseUnprocessedArtifactConsumer>( applicationContext.getBeansOfType( DatabaseUnprocessedArtifactConsumer.class ).values() );
    }

    /**
     * Get the complete {@link List} of {@link DatabaseCleanupConsumer} objects
     * that are available in the system, regardless of configuration.
     * 
     * @return the list of all available {@link DatabaseCleanupConsumer} objects.
     */
    @SuppressWarnings("unchecked")
    public List<DatabaseCleanupConsumer> getAvailableCleanupConsumers()
    {
        return new ArrayList<DatabaseCleanupConsumer>( applicationContext.getBeansOfType( DatabaseCleanupConsumer.class ).values() );
    }
    
    /**
     * Execute the cleanup consumers to cleanup the specified artifact from the database and index.
     * 
     * @param artifact
     */
    public void executeCleanupConsumer( ArchivaArtifact artifact )
    {
        List<ArchivaArtifactConsumer> consumers = getSelectedCleanupConsumers();
        for ( ArchivaArtifactConsumer consumer : consumers )
        {
            consumer.beginScan();
        }
        
        if ( CollectionUtils.isEmpty( consumers ) )
        {
            log.warn( "There are no selected consumers for artifact cleanup." );
            return;
        }
        
        ProcessArchivaArtifactClosure processArtifactClosure = new ProcessArchivaArtifactClosure();
        processArtifactClosure.setArtifact( artifact );
        
        CollectionUtils.forAllDo( consumers, processArtifactClosure );
        
        for ( ArchivaArtifactConsumer consumer : consumers )
        {
            consumer.completeScan();
        }
    }
}
