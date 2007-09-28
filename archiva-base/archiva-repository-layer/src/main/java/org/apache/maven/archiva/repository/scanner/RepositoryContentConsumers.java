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

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.functors.IfClosure;
import org.apache.commons.collections.functors.OrPredicate;
import org.apache.maven.archiva.common.utils.BaseFile;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.RepositoryScanningConfiguration;
import org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.RepositoryContentConsumer;
import org.apache.maven.archiva.consumers.functors.PermanentConsumerPredicate;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.repository.scanner.functors.ConsumerProcessFileClosure;
import org.apache.maven.archiva.repository.scanner.functors.ConsumerWantsFilePredicate;
import org.apache.maven.archiva.repository.scanner.functors.TriggerBeginScanClosure;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RepositoryContentConsumerUtil 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.repository.scanner.RepositoryContentConsumers"
 */
public class RepositoryContentConsumers
    extends AbstractLogEnabled
    implements Initializable
{
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

    private Predicate selectedKnownPredicate;

    private Predicate selectedInvalidPredicate;

    class SelectedKnownRepoConsumersPredicate
        implements Predicate
    {
        public boolean evaluate( Object object )
        {
            boolean satisfies = false;

            if ( object instanceof KnownRepositoryContentConsumer )
            {
                KnownRepositoryContentConsumer known = (KnownRepositoryContentConsumer) object;
                RepositoryScanningConfiguration scanning = archivaConfiguration.getConfiguration()
                    .getRepositoryScanning();

                return scanning.getKnownContentConsumers().contains( known.getId() );
            }

            return satisfies;
        }
    }

    class SelectedInvalidRepoConsumersPredicate
        implements Predicate
    {
        public boolean evaluate( Object object )
        {
            boolean satisfies = false;

            if ( object instanceof InvalidRepositoryContentConsumer )
            {
                InvalidRepositoryContentConsumer invalid = (InvalidRepositoryContentConsumer) object;
                RepositoryScanningConfiguration scanning = archivaConfiguration.getConfiguration()
                    .getRepositoryScanning();

                return scanning.getInvalidContentConsumers().contains( invalid.getId() );
            }

            return satisfies;
        }
    }

    class RepoConsumerToMapClosure
        implements Closure
    {
        private Map map = new HashMap();

        public void execute( Object input )
        {
            if ( input instanceof RepositoryContentConsumer )
            {
                RepositoryContentConsumer consumer = (RepositoryContentConsumer) input;
                map.put( consumer.getId(), consumer );
            }
        }

        public Map getMap()
        {
            return map;
        }
    }

    public void initialize()
        throws InitializationException
    {
        Predicate permanentConsumers = new PermanentConsumerPredicate();

        this.selectedKnownPredicate = new OrPredicate( permanentConsumers, new SelectedKnownRepoConsumersPredicate() );
        this.selectedInvalidPredicate = new OrPredicate( permanentConsumers,
                                                         new SelectedInvalidRepoConsumersPredicate() );
    }

    public List getSelectedKnownConsumerIds()
    {
        RepositoryScanningConfiguration scanning = archivaConfiguration.getConfiguration().getRepositoryScanning();
        return scanning.getKnownContentConsumers();
    }

    public List getSelectedInvalidConsumerIds()
    {
        RepositoryScanningConfiguration scanning = archivaConfiguration.getConfiguration().getRepositoryScanning();
        return scanning.getInvalidContentConsumers();
    }

    public Map getSelectedKnownConsumersMap()
    {
        RepoConsumerToMapClosure consumerMapClosure = new RepoConsumerToMapClosure();
        Closure ifclosure = IfClosure.getInstance( selectedKnownPredicate, consumerMapClosure );
        CollectionUtils.forAllDo( availableKnownConsumers, ifclosure );

        return consumerMapClosure.getMap();
    }

    public Map getSelectedInvalidConsumersMap()
    {
        RepoConsumerToMapClosure consumerMapClosure = new RepoConsumerToMapClosure();
        Closure ifclosure = IfClosure.getInstance( selectedInvalidPredicate, consumerMapClosure );
        CollectionUtils.forAllDo( availableInvalidConsumers, ifclosure );

        return consumerMapClosure.getMap();
    }

    public List getSelectedKnownConsumers()
    {
        List ret = new ArrayList();
        ret.addAll( CollectionUtils.select( availableKnownConsumers, selectedKnownPredicate ) );

        return ret;
    }

    public List getSelectedInvalidConsumers()
    {
        List ret = new ArrayList();
        ret.addAll( CollectionUtils.select( availableInvalidConsumers, selectedInvalidPredicate ) );

        return ret;
    }

    public List<KnownRepositoryContentConsumer> getAvailableKnownConsumers()
    {
        return availableKnownConsumers;
    }

    public List<InvalidRepositoryContentConsumer> getAvailableInvalidConsumers()
    {
        return availableInvalidConsumers;
    }

    public void setAvailableKnownConsumers( List<KnownRepositoryContentConsumer> availableKnownConsumers )
    {
        this.availableKnownConsumers = availableKnownConsumers;
    }

    public void setAvailableInvalidConsumers( List<InvalidRepositoryContentConsumer> availableInvalidConsumers )
    {
        this.availableInvalidConsumers = availableInvalidConsumers;
    }

    public void executeConsumers( ArchivaRepository repository, File localFile )
    {
        // Run the repository consumers
        try
        {
            Closure triggerBeginScan = new TriggerBeginScanClosure( repository, getLogger() );

            CollectionUtils.forAllDo( availableKnownConsumers, triggerBeginScan );
            CollectionUtils.forAllDo( availableInvalidConsumers, triggerBeginScan );

            // yuck. In case you can't read this, it says
            // "process the file if the consumer has it in the includes list, and not in the excludes list"
            BaseFile baseFile = new BaseFile( repository.getUrl().getPath(), localFile );
            ConsumerWantsFilePredicate predicate = new ConsumerWantsFilePredicate();
            predicate.setBasefile( baseFile );
            ConsumerProcessFileClosure closure = new ConsumerProcessFileClosure( getLogger() );
            closure.setBasefile( baseFile );
            predicate.setCaseSensitive( false );
            Closure processIfWanted = IfClosure.getInstance( predicate, closure );

            CollectionUtils.forAllDo( availableKnownConsumers, processIfWanted );

            if ( predicate.getWantedFileCount() <= 0 )
            {
                // Nothing known processed this file.  It is invalid!
                CollectionUtils.forAllDo( availableInvalidConsumers, closure );
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

}
