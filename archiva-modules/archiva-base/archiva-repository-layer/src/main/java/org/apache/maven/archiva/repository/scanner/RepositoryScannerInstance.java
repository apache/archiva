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
import java.util.Date;
import java.util.List;

import org.apache.archiva.repository.scanner.functors.TriggerScanCompletedClosure;
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
import org.codehaus.plexus.util.DirectoryWalkListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * RepositoryScannerInstance 
 *
 * @version $Id$
 */
public class RepositoryScannerInstance
    implements DirectoryWalkListener
{
    private Logger log = LoggerFactory.getLogger( RepositoryScannerInstance.class );
    
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

    public RepositoryScannerInstance( ManagedRepositoryConfiguration repository,
                                      List<KnownRepositoryContentConsumer> knownConsumerList,
                                      List<InvalidRepositoryContentConsumer> invalidConsumerList )
    {
        this.repository = repository;
        this.knownConsumers = knownConsumerList;
        this.invalidConsumers = invalidConsumerList;

        this.consumerProcessFile = new ConsumerProcessFileClosure();
        this.consumerWantsFile = new ConsumerWantsFilePredicate();

        stats = new RepositoryScanStatistics();
        stats.setRepositoryId( repository.getId() );

        Closure triggerBeginScan = new TriggerBeginScanClosure( repository, new Date( System.currentTimeMillis() ), true );

        CollectionUtils.forAllDo( knownConsumerList, triggerBeginScan );
        CollectionUtils.forAllDo( invalidConsumerList, triggerBeginScan );

        if ( SystemUtils.IS_OS_WINDOWS )
        {
            consumerWantsFile.setCaseSensitive( false );
        }
    }

    public RepositoryScannerInstance( ManagedRepositoryConfiguration repository,
                                      List<KnownRepositoryContentConsumer> knownContentConsumers,
                                      List<InvalidRepositoryContentConsumer> invalidContentConsumers, long changesSince )
    {
        this( repository, knownContentConsumers, invalidContentConsumers );

        consumerWantsFile.setChangesSince( changesSince );

        this.changesSince = changesSince;
    }

    public RepositoryScanStatistics getStatistics()
    {
        return stats;
    }

    public void directoryWalkStarting( File basedir )
    {
        log.info( "Walk Started: [" + this.repository.getId() + "] " + this.repository.getLocation() );
        stats.triggerStart();
    }

    public void directoryWalkStep( int percentage, File file )
    {
        log.debug( "Walk Step: " + percentage + ", " + file );

        stats.increaseFileCount();

        // consume files regardless - the predicate will check the timestamp
        BaseFile basefile = new BaseFile( repository.getLocation(), file );
        
        // Timestamp finished points to the last successful scan, not this current one.
        if ( file.lastModified() >= changesSince )
        {
            stats.increaseNewFileCount();             
        }
        
        consumerProcessFile.setBasefile( basefile );
        consumerProcessFile.setExecuteOnEntireRepo( true );
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
        TriggerScanCompletedClosure scanCompletedClosure = new TriggerScanCompletedClosure( repository, true );
        
        CollectionUtils.forAllDo( knownConsumers, scanCompletedClosure );
        CollectionUtils.forAllDo( invalidConsumers, scanCompletedClosure );
        
        log.info( "Walk Finished: [" + this.repository.getId() + "] " + this.repository.getLocation() );
        stats.triggerFinished();
    }

    /**
     * Debug method from DirectoryWalker.
     */
    public void debug( String message )
    {
        log.debug( "Repository Scanner: " + message );
    }
    
    public ManagedRepositoryConfiguration getRepository()
    {
        return repository;
    }

    public RepositoryScanStatistics getStats()
    {
        return stats;
    }

    public long getChangesSince()
    {
        return changesSince;
    }
}
