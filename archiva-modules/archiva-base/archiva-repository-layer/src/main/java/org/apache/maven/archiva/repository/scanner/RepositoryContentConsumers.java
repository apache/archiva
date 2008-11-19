package org.apache.maven.archiva.repository.scanner;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.functors.IfClosure;
import org.apache.maven.archiva.common.utils.BaseFile;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RepositoryScanningConfiguration;
import org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.repository.scanner.functors.ConsumerProcessFileClosure;
import org.apache.maven.archiva.repository.scanner.functors.ConsumerWantsFilePredicate;
import org.apache.maven.archiva.repository.scanner.functors.TriggerBeginScanClosure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RepositoryContentConsumerUtil 
 *
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.repository.scanner.RepositoryContentConsumers"
 */
public class RepositoryContentConsumers
{
    private Logger log = LoggerFactory.getLogger( RepositoryContentConsumers.class );
    
    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer"
     */
    private List<KnownRepositoryContentConsumer> availableKnownConsumers;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer"
     */
    private List<InvalidRepositoryContentConsumer> availableInvalidConsumers;

    private List<KnownRepositoryContentConsumer> selectedKnownConsumers;

    private List<InvalidRepositoryContentConsumer> selectedInvalidConsumers;
    
    /**
     * <p>
     * Get the list of Ids associated with those {@link KnownRepositoryContentConsumer} that have
     * been selected in the configuration to execute.
     * </p>
     * 
     * <p>
     * NOTE: This list can be larger and contain entries that might not exist or be available
     * in the classpath, or as a component.
     * </p>
     * 
     * @return the list of consumer ids that have been selected by the configuration.
     */
    public List<String> getSelectedKnownConsumerIds()
    {
        RepositoryScanningConfiguration scanning = archivaConfiguration.getConfiguration().getRepositoryScanning();
        return scanning.getKnownContentConsumers();
    }

    /**
     * <p>
     * Get the list of Ids associated with those {@link InvalidRepositoryContentConsumer} that have
     * been selected in the configuration to execute.
     * </p>
     * 
     * <p>
     * NOTE: This list can be larger and contain entries that might not exist or be available
     * in the classpath, or as a component.
     * </p>
     * 
     * @return the list of consumer ids that have been selected by the configuration.
     */
    public List<String> getSelectedInvalidConsumerIds()
    {
        RepositoryScanningConfiguration scanning = archivaConfiguration.getConfiguration().getRepositoryScanning();
        return scanning.getInvalidContentConsumers();
    }

    /**
     * Get the map of {@link String} ids to {@link KnownRepositoryContentConsumer} implementations,
     * for those consumers that have been selected according to the active configuration. 
     * 
     * @return the map of String ids to {@link KnownRepositoryContentConsumer} objects.
     */
    public Map<String, KnownRepositoryContentConsumer> getSelectedKnownConsumersMap()
    {
        Map<String, KnownRepositoryContentConsumer> consumerMap = new HashMap<String, KnownRepositoryContentConsumer>();

        for ( KnownRepositoryContentConsumer consumer : getSelectedKnownConsumers() )
        {
            consumerMap.put( consumer.getId(), consumer );
        }

        return consumerMap;
    }

    /**
     * Get the map of {@link String} ids to {@link InvalidRepositoryContentConsumer} implementations,
     * for those consumers that have been selected according to the active configuration. 
     * 
     * @return the map of String ids to {@link InvalidRepositoryContentConsumer} objects.
     */
    public Map<String, InvalidRepositoryContentConsumer> getSelectedInvalidConsumersMap()
    {
        Map<String, InvalidRepositoryContentConsumer> consumerMap = new HashMap<String, InvalidRepositoryContentConsumer>();

        for ( InvalidRepositoryContentConsumer consumer : getSelectedInvalidConsumers() )
        {
            consumerMap.put( consumer.getId(), consumer );
        }

        return consumerMap;
    }

    /**
     * Get the list of {@link KnownRepositoryContentConsumer} objects that are
     * selected according to the active configuration.
     * 
     * @return the list of {@link KnownRepositoryContentConsumer} that have been selected
     *         by the active configuration.
     */
    public synchronized List<KnownRepositoryContentConsumer> getSelectedKnownConsumers()
    {
        if ( selectedKnownConsumers == null )
        {
            List<KnownRepositoryContentConsumer> ret = new ArrayList<KnownRepositoryContentConsumer>();

            List<String> knownSelected = getSelectedKnownConsumerIds();

            for ( KnownRepositoryContentConsumer consumer : availableKnownConsumers )
            {
                if ( knownSelected.contains( consumer.getId() ) || consumer.isPermanent() )
                {
                    ret.add( consumer );
                }
            }
            this.selectedKnownConsumers = ret;
        }
        return selectedKnownConsumers;
    }

    /**
     * Get the list of {@link InvalidRepositoryContentConsumer} objects that are
     * selected according to the active configuration.
     * 
     * @return the list of {@link InvalidRepositoryContentConsumer} that have been selected
     *         by the active configuration.
     */
    public synchronized List<InvalidRepositoryContentConsumer> getSelectedInvalidConsumers()
    {
        if ( selectedInvalidConsumers == null )
        {
            List<InvalidRepositoryContentConsumer> ret = new ArrayList<InvalidRepositoryContentConsumer>();

            List<String> invalidSelected = getSelectedInvalidConsumerIds();

            for ( InvalidRepositoryContentConsumer consumer : availableInvalidConsumers )
            {
                if ( invalidSelected.contains( consumer.getId() ) || consumer.isPermanent() )
                {
                    ret.add( consumer );
                }
            }
            selectedInvalidConsumers = ret;
        }
        return selectedInvalidConsumers;
    }

    /**
     * Get the list of {@link KnownRepositoryContentConsumer} objects that are
     * available and present in the classpath and as components in the IoC.
     * 
     * @return the list of all available {@link KnownRepositoryContentConsumer} present in the classpath 
     *         and as a component in the IoC.
     */
    public List<KnownRepositoryContentConsumer> getAvailableKnownConsumers()
    {
        return availableKnownConsumers;
    }

    /**
     * Get the list of {@link InvalidRepositoryContentConsumer} objects that are
     * available and present in the classpath and as components in the IoC.
     * 
     * @return the list of all available {@link InvalidRepositoryContentConsumer} present in the classpath 
     *         and as a component in the IoC.
     */
    public List<InvalidRepositoryContentConsumer> getAvailableInvalidConsumers()
    {
        return availableInvalidConsumers;
    }

    /**
     * Set the list of {@link KnownRepositoryContentConsumer} objects that are
     * available.
     * 
     * NOTE: This is an override for the base functionality as a component, this
     * is used by archiva-cli and the unit testing framework.
     * 
     * @return the list of available {@link KnownRepositoryContentConsumer}.
     */
    public void setAvailableKnownConsumers( List<KnownRepositoryContentConsumer> availableKnownConsumers )
    {
        this.availableKnownConsumers = availableKnownConsumers;
    }

    /**
     * Set the list of {@link InvalidRepositoryContentConsumer} objects that are
     * available.
     * 
     * NOTE: This is an override for the base functionality as a component, this
     * is used by archiva-cli and the unit testing framework.
     * 
     * @return the list of available {@link InvalidRepositoryContentConsumer}.
     */
    public void setAvailableInvalidConsumers( List<InvalidRepositoryContentConsumer> availableInvalidConsumers )
    {
        this.availableInvalidConsumers = availableInvalidConsumers;
    }

    /**
     * A convienence method to execute all of the active selected consumers for a 
     * particular arbitrary file.
     * 
     * @param repository the repository configuration to use.
     * @param localFile the local file to execute the consumers against.
     */
    public void executeConsumers( ManagedRepositoryConfiguration repository, File localFile )
    {
        // Run the repository consumers
        try
        {   
            Closure triggerBeginScan = new TriggerBeginScanClosure( repository, getStartTime() );

            List<KnownRepositoryContentConsumer> selectedKnownConsumers = getSelectedKnownConsumers();
            List<InvalidRepositoryContentConsumer> selectedInvalidConsumers = getSelectedInvalidConsumers();
            CollectionUtils.forAllDo( selectedKnownConsumers, triggerBeginScan );
            CollectionUtils.forAllDo( selectedInvalidConsumers, triggerBeginScan );

            // yuck. In case you can't read this, it says
            // "process the file if the consumer has it in the includes list, and not in the excludes list"
            BaseFile baseFile = new BaseFile( repository.getLocation(), localFile );
            ConsumerWantsFilePredicate predicate = new ConsumerWantsFilePredicate();
            predicate.setBasefile( baseFile );
            ConsumerProcessFileClosure closure = new ConsumerProcessFileClosure();
            closure.setBasefile( baseFile );
            predicate.setCaseSensitive( false );
            Closure processIfWanted = IfClosure.getInstance( predicate, closure );

            CollectionUtils.forAllDo( selectedKnownConsumers, processIfWanted );

            if ( predicate.getWantedFileCount() <= 0 )
            {
                // Nothing known processed this file.  It is invalid!
                CollectionUtils.forAllDo( selectedInvalidConsumers, closure );
            }
        }
        finally
        {
            /* TODO: This is never called by the repository scanner instance, so not calling here either - but it probably should be?
                        CollectionUtils.forAllDo( availableKnownConsumers, triggerCompleteScan );
                        CollectionUtils.forAllDo( availableInvalidConsumers, triggerCompleteScan );
            */
        }
    }

    public void setSelectedKnownConsumers( List<KnownRepositoryContentConsumer> selectedKnownConsumers )
    {
        this.selectedKnownConsumers = selectedKnownConsumers;
    }

    public void setSelectedInvalidConsumers( List<InvalidRepositoryContentConsumer> selectedInvalidConsumers )
    {
        this.selectedInvalidConsumers = selectedInvalidConsumers;
    }
    
    protected Date getStartTime()
    {   
        return new Date( System.currentTimeMillis() );
    }
    
    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }
}
