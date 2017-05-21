package org.apache.archiva.repository.scanner;

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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.admin.ArchivaAdministration;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.common.utils.BaseFile;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.consumers.functors.ConsumerWantsFilePredicate;
import org.apache.archiva.redback.components.registry.RegistryListener;
import org.apache.archiva.repository.scanner.functors.ConsumerProcessFileClosure;
import org.apache.archiva.repository.scanner.functors.TriggerBeginScanClosure;
import org.apache.archiva.repository.scanner.functors.TriggerScanCompletedClosure;
import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.functors.IfClosure;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RepositoryContentConsumerUtil
 */
@Service("repositoryContentConsumers")
public class RepositoryContentConsumers
    implements ApplicationContextAware
{

    @Inject
    private ApplicationContext applicationContext;

    private ArchivaAdministration archivaAdministration;

    private List<KnownRepositoryContentConsumer> selectedKnownConsumers;

    private List<InvalidRepositoryContentConsumer> selectedInvalidConsumers;

    @Inject
    private ArchivaConfiguration archivaConfiguration;

    @Inject
    public RepositoryContentConsumers( ArchivaAdministration archivaAdministration )
    {
        this.archivaAdministration = archivaAdministration;
    }

    @Override
    public void setApplicationContext( ApplicationContext applicationContext )
        throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    /**
     * <p>
     * Get the list of Ids associated with those {@link KnownRepositoryContentConsumer} that have
     * been selected in the configuration to execute.
     * </p>
     * <p>
     * NOTE: This list can be larger and contain entries that might not exist or be available
     * in the classpath, or as a component.
     * </p>
     *
     * @return the list of consumer ids that have been selected by the configuration.
     */
    public List<String> getSelectedKnownConsumerIds()
        throws RepositoryAdminException
    {
        return archivaAdministration.getKnownContentConsumers();
    }

    /**
     * <p>
     * Get the list of Ids associated with those {@link InvalidRepositoryContentConsumer} that have
     * been selected in the configuration to execute.
     * </p>
     * <p>
     * NOTE: This list can be larger and contain entries that might not exist or be available
     * in the classpath, or as a component.
     * </p>
     *
     * @return the list of consumer ids that have been selected by the configuration.
     */
    public List<String> getSelectedInvalidConsumerIds()
        throws RepositoryAdminException
    {
        return archivaAdministration.getInvalidContentConsumers();
    }

    /**
     * Get the map of {@link String} ids to {@link KnownRepositoryContentConsumer} implementations,
     * for those consumers that have been selected according to the active configuration.
     *
     * @return the map of String ids to {@link KnownRepositoryContentConsumer} objects.
     */
    public Map<String, KnownRepositoryContentConsumer> getSelectedKnownConsumersMap()
        throws RepositoryAdminException
    {
        Map<String, KnownRepositoryContentConsumer> consumerMap = new HashMap<>();

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
        throws RepositoryAdminException
    {
        Map<String, InvalidRepositoryContentConsumer> consumerMap = new HashMap<>();

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
     * by the active configuration.
     */
    public List<KnownRepositoryContentConsumer> getSelectedKnownConsumers()
        throws RepositoryAdminException
    {
        // FIXME only for testing
        if ( selectedKnownConsumers != null )
        {
            return selectedKnownConsumers;
        }
        List<KnownRepositoryContentConsumer> ret = new ArrayList<>();

        List<String> knownSelected = getSelectedKnownConsumerIds();

        for ( KnownRepositoryContentConsumer consumer : getAvailableKnownConsumers() )
        {
            if ( knownSelected.contains( consumer.getId() ) )
            {
                ret.add( consumer );
            }
        }
        return ret;
    }

    public void releaseSelectedKnownConsumers( List<KnownRepositoryContentConsumer> repositoryContentConsumers )
    {
        if ( repositoryContentConsumers == null )
        {
            return;
        }
        for ( KnownRepositoryContentConsumer knownRepositoryContentConsumer : repositoryContentConsumers )
        {
            if ( RegistryListener.class.isAssignableFrom( knownRepositoryContentConsumer.getClass() ) )
            {
                archivaConfiguration.removeChangeListener(
                    RegistryListener.class.cast( knownRepositoryContentConsumer ) );
            }
        }
    }

    /**
     * Get the list of {@link InvalidRepositoryContentConsumer} objects that are
     * selected according to the active configuration.
     *
     * @return the list of {@link InvalidRepositoryContentConsumer} that have been selected
     * by the active configuration.
     */
    public synchronized List<InvalidRepositoryContentConsumer> getSelectedInvalidConsumers()
        throws RepositoryAdminException
    {

        // FIXME only for testing
        if ( selectedInvalidConsumers != null )
        {
            return selectedInvalidConsumers;
        }

        List<InvalidRepositoryContentConsumer> ret = new ArrayList<>();

        List<String> invalidSelected = getSelectedInvalidConsumerIds();

        for ( InvalidRepositoryContentConsumer consumer : getAvailableInvalidConsumers() )
        {
            if ( invalidSelected.contains( consumer.getId() ) )
            {
                ret.add( consumer );
            }
        }
        return ret;
    }


    /**
     * Get the list of {@link KnownRepositoryContentConsumer} objects that are
     * available and present in the classpath and as components in the IoC.
     *
     * @return the list of all available {@link KnownRepositoryContentConsumer} present in the classpath
     * and as a component in the IoC.
     */
    public List<KnownRepositoryContentConsumer> getAvailableKnownConsumers()
    {
        return new ArrayList<>( applicationContext.getBeansOfType( KnownRepositoryContentConsumer.class ).values() );
    }

    /**
     * Get the list of {@link InvalidRepositoryContentConsumer} objects that are
     * available and present in the classpath and as components in the IoC.
     *
     * @return the list of all available {@link InvalidRepositoryContentConsumer} present in the classpath
     * and as a component in the IoC.
     */
    public List<InvalidRepositoryContentConsumer> getAvailableInvalidConsumers()
    {
        return new ArrayList<>( applicationContext.getBeansOfType( InvalidRepositoryContentConsumer.class ).values() );
    }

    /**
     * A convienence method to execute all of the active selected consumers for a
     * particular arbitrary file.
     * NOTE: Make sure that there is no repository scanning task executing before invoking this so as to prevent
     * the index writer/reader of the current index-content consumer executing from getting closed. For an example,
     * see ArchivaDavResource#executeConsumers( File ).
     *
     * @param repository             the repository configuration to use.
     * @param localFile              the local file to execute the consumers against.
     * @param updateRelatedArtifacts TODO
     */
    public void executeConsumers( ManagedRepository repository, File localFile, boolean updateRelatedArtifacts )
        throws RepositoryAdminException
    {
        List<KnownRepositoryContentConsumer> selectedKnownConsumers = null;
        // Run the repository consumers
        try
        {
            Closure triggerBeginScan = new TriggerBeginScanClosure( repository, getStartTime(), false );

            selectedKnownConsumers = getSelectedKnownConsumers();

            // MRM-1212/MRM-1197 
            // - do not create missing/fix invalid checksums and update metadata when deploying from webdav since these are uploaded by maven
            if ( !updateRelatedArtifacts )
            {
                List<KnownRepositoryContentConsumer> clone = new ArrayList<>();
                clone.addAll( selectedKnownConsumers );

                for ( KnownRepositoryContentConsumer consumer : clone )
                {
                    if ( consumer.getId().equals( "create-missing-checksums" ) || consumer.getId().equals(
                        "metadata-updater" ) )
                    {
                        selectedKnownConsumers.remove( consumer );
                    }
                }
            }

            List<InvalidRepositoryContentConsumer> selectedInvalidConsumers = getSelectedInvalidConsumers();
            CollectionUtils.forAllDo( selectedKnownConsumers, triggerBeginScan );
            CollectionUtils.forAllDo( selectedInvalidConsumers, triggerBeginScan );

            // yuck. In case you can't read this, it says
            // "process the file if the consumer has it in the includes list, and not in the excludes list"
            BaseFile baseFile = new BaseFile( repository.getLocation(), localFile );
            ConsumerWantsFilePredicate predicate = new ConsumerWantsFilePredicate( repository );
            predicate.setBasefile( baseFile );
            predicate.setCaseSensitive( false );

            ConsumerProcessFileClosure closure = new ConsumerProcessFileClosure();
            closure.setBasefile( baseFile );
            closure.setExecuteOnEntireRepo( false );

            Closure processIfWanted = IfClosure.getInstance( predicate, closure );

            CollectionUtils.forAllDo( selectedKnownConsumers, processIfWanted );

            if ( predicate.getWantedFileCount() <= 0 )
            {
                // Nothing known processed this file.  It is invalid!
                CollectionUtils.forAllDo( selectedInvalidConsumers, closure );
            }

            TriggerScanCompletedClosure scanCompletedClosure = new TriggerScanCompletedClosure( repository, false );

            CollectionUtils.forAllDo( selectedKnownConsumers, scanCompletedClosure );
        }
        finally
        {
            /* TODO: This is never called by the repository scanner instance, so not calling here either - but it probably should be?
                        CollectionUtils.forAllDo( availableKnownConsumers, triggerCompleteScan );
                        CollectionUtils.forAllDo( availableInvalidConsumers, triggerCompleteScan );
            */
            releaseSelectedKnownConsumers( selectedKnownConsumers );
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

    public void setArchivaAdministration( ArchivaAdministration archivaAdministration )
    {
        this.archivaAdministration = archivaAdministration;
    }
}
