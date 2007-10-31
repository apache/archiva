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
import org.apache.commons.collections.functors.IfClosure;
import org.apache.commons.lang.SystemUtils;
import org.apache.maven.archiva.common.utils.BaseFile;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.repository.scanner.functors.ConsumerProcessFileClosure;
import org.apache.maven.archiva.repository.scanner.functors.ConsumerWantsFilePredicate;
import org.apache.maven.archiva.repository.scanner.functors.TriggerBeginScanClosure;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.DirectoryWalkListener;

import java.io.File;
import java.util.List;

/**
 * RepositoryScannerInstance 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryScannerInstance
    implements DirectoryWalkListener
{
    /**
     * Consumers that process known content.
     */
    private List<KnownRepositoryContentConsumer> knownConsumers;

    /**
     * Consumers that process unknown/invalid content.
     */
    private List<InvalidRepositoryContentConsumer> invalidConsumers;

    private ManagedRepositoryConfiguration repository;

    private RepositoryScanStatistics stats;

    private long changesSince = 0;

    private ConsumerProcessFileClosure consumerProcessFile;

    private ConsumerWantsFilePredicate consumerWantsFile;

    private Logger logger;

    public RepositoryScannerInstance( ManagedRepositoryConfiguration repository,
                                      List<KnownRepositoryContentConsumer> knownConsumerList,
                                      List<InvalidRepositoryContentConsumer> invalidConsumerList, Logger logger )
    {
        this.repository = repository;
        this.knownConsumers = knownConsumerList;
        this.invalidConsumers = invalidConsumerList;
        this.logger = logger;

        this.consumerProcessFile = new ConsumerProcessFileClosure( logger );
        this.consumerWantsFile = new ConsumerWantsFilePredicate();

        stats = new RepositoryScanStatistics();
        stats.setRepositoryId( repository.getId() );

        Closure triggerBeginScan = new TriggerBeginScanClosure( repository, logger );

        CollectionUtils.forAllDo( knownConsumerList, triggerBeginScan );
        CollectionUtils.forAllDo( invalidConsumerList, triggerBeginScan );

        if ( SystemUtils.IS_OS_WINDOWS )
        {
            consumerWantsFile.setCaseSensitive( false );
        }
    }

    public RepositoryScannerInstance( ManagedRepositoryConfiguration repository,
                                      List<KnownRepositoryContentConsumer> knownContentConsumers,
                                      List<InvalidRepositoryContentConsumer> invalidContentConsumers, Logger logger,
                                      long changesSince )
    {
        this( repository, knownContentConsumers, invalidContentConsumers, logger );

        consumerWantsFile.setChangesSince( changesSince );

        this.changesSince = changesSince;
    }

    public RepositoryScanStatistics getStatistics()
    {
        return stats;
    }

    public void directoryWalkStarting( File basedir )
    {
        logger.info( "Walk Started: [" + this.repository.getId() + "] " + this.repository.getLocation() );
        stats.triggerStart();
    }

    public void directoryWalkStep( int percentage, File file )
    {
        logger.debug( "Walk Step: " + percentage + ", " + file );

        stats.increaseFileCount();

        // Timestamp finished points to the last successful scan, not this current one.
        if ( file.lastModified() >= changesSince )
        {
            stats.increaseNewFileCount();
        }

        // consume files regardless - the predicate will check the timestamp
        BaseFile basefile = new BaseFile( repository.getLocation(), file );
        
        consumerProcessFile.setBasefile( basefile );
        consumerWantsFile.setBasefile( basefile );
        
        Closure processIfWanted = IfClosure.getInstance( consumerWantsFile, consumerProcessFile );
        CollectionUtils.forAllDo( this.knownConsumers, processIfWanted );
        
        if ( consumerWantsFile.getWantedFileCount() <= 0 )
        {
            // Nothing known processed this file.  It is invalid!
            CollectionUtils.forAllDo( this.invalidConsumers, consumerProcessFile );
        }
    }

    public void directoryWalkFinished()
    {
        logger.info( "Walk Finished: [" + this.repository.getId() + "] " + this.repository.getLocation() );
        stats.triggerFinished();
    }

    /**
     * Debug method from DirectoryWalker.
     */
    public void debug( String message )
    {
        logger.debug( "Repository Scanner: " + message );
    }
}
