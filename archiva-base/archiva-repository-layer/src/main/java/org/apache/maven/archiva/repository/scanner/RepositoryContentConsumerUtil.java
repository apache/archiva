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
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.RepositoryScanningConfiguration;
import org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.RepositoryContentConsumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RepositoryContentConsumerUtil 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.repository.scanner.RepositoryContentConsumerUtil"
 */
public class RepositoryContentConsumerUtil
{
    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer"
     */
    private List availableGoodConsumers;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer"
     */
    private List availableBadConsumers;

    class SelectedKnownRepoConsumersPredicate
        implements Predicate
    {
        public boolean evaluate( Object object )
        {
            boolean satisfies = false;

            if ( object instanceof KnownRepositoryContentConsumer )
            {
                KnownRepositoryContentConsumer known = (KnownRepositoryContentConsumer) object;
                Configuration config = archivaConfiguration.getConfiguration();

                return config.getRepositoryScanning().getGoodConsumers().contains( known.getId() );
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

            if ( object instanceof KnownRepositoryContentConsumer )
            {
                InvalidRepositoryContentConsumer invalid = (InvalidRepositoryContentConsumer) object;
                Configuration config = archivaConfiguration.getConfiguration();

                return config.getRepositoryScanning().getBadConsumers().contains( invalid.getId() );
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

    public Predicate getKnownSelectionPredicate()
    {
        return new SelectedKnownRepoConsumersPredicate();
    }

    public Predicate getInvalidSelectionPredicate()
    {
        return new SelectedInvalidRepoConsumersPredicate();
    }

    public Map getSelectedKnownConsumersMap()
    {
        RepositoryScanningConfiguration scanning = archivaConfiguration.getConfiguration().getRepositoryScanning();

        RepoConsumerToMapClosure consumerMapClosure = new RepoConsumerToMapClosure();
        Closure ifclosure = IfClosure.getInstance( getKnownSelectionPredicate(), consumerMapClosure );
        CollectionUtils.forAllDo( scanning.getGoodConsumers(), ifclosure );

        return consumerMapClosure.getMap();
    }

    public Map getSelectedInvalidConsumersMap()
    {
        RepositoryScanningConfiguration scanning = archivaConfiguration.getConfiguration().getRepositoryScanning();

        RepoConsumerToMapClosure consumerMapClosure = new RepoConsumerToMapClosure();
        Closure ifclosure = IfClosure.getInstance( getInvalidSelectionPredicate(), consumerMapClosure );
        CollectionUtils.forAllDo( scanning.getGoodConsumers(), ifclosure );

        return consumerMapClosure.getMap();
    }
    
    public List getSelectedKnownConsumers()
    {
        RepositoryScanningConfiguration scanning = archivaConfiguration.getConfiguration().getRepositoryScanning();

        List ret = new ArrayList();
        ret.addAll( CollectionUtils.select( scanning.getGoodConsumers(), getKnownSelectionPredicate() ));

        return ret;
    }

    public List getSelectedInvalidConsumers()
    {
        RepositoryScanningConfiguration scanning = archivaConfiguration.getConfiguration().getRepositoryScanning();

        List ret = new ArrayList();
        ret.addAll( CollectionUtils.select( scanning.getBadConsumers(), getInvalidSelectionPredicate() ));

        return ret;
    }

    public List getAvailableKnownConsumers()
    {
        return availableGoodConsumers;
    }

    public List getAvailableInvalidConsumers()
    {
        return availableBadConsumers;
    }
}
