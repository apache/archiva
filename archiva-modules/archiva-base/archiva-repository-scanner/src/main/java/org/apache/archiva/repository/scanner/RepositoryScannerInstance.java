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

import org.apache.archiva.common.utils.BaseFile;
import org.apache.archiva.common.utils.PathUtil;
import org.apache.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.consumers.RepositoryContentConsumer;
import org.apache.archiva.consumers.functors.ConsumerWantsFilePredicate;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.scanner.functors.ConsumerProcessFileClosure;
import org.apache.archiva.repository.scanner.functors.TriggerBeginScanClosure;
import org.apache.archiva.repository.scanner.functors.TriggerScanCompletedClosure;
import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.functors.IfClosure;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RepositoryScannerInstance
 */
public class RepositoryScannerInstance
    implements FileVisitor<Path>
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

    private ManagedRepository repository;

    private RepositoryScanStatistics stats;

    private long changesSince = 0;

    private ConsumerProcessFileClosure consumerProcessFile;

    private ConsumerWantsFilePredicate consumerWantsFile;

    private Map<String, Long> consumerTimings;

    private Map<String, Long> consumerCounts;


    private List<String> fileNameIncludePattern = new ArrayList<>();
    private List<String> fileNameExcludePattern = new ArrayList<>();

    private List<PathMatcher> includeMatcher = new ArrayList<>();
    private List<PathMatcher> excludeMatcher = new ArrayList<>();

    private boolean isRunning = false;

    Path basePath = null;

    public RepositoryScannerInstance( ManagedRepository repository,
                                      List<KnownRepositoryContentConsumer> knownConsumerList,
                                      List<InvalidRepositoryContentConsumer> invalidConsumerList )
    {
        this.repository = repository;
        this.knownConsumers = knownConsumerList;
        this.invalidConsumers = invalidConsumerList;

        addFileNameIncludePattern("**/*");

        consumerTimings = new HashMap<>();
        consumerCounts = new HashMap<>();

        this.consumerProcessFile = new ConsumerProcessFileClosure();
        consumerProcessFile.setExecuteOnEntireRepo( true );
        consumerProcessFile.setConsumerTimings( consumerTimings );
        consumerProcessFile.setConsumerCounts( consumerCounts );

        this.consumerWantsFile = new ConsumerWantsFilePredicate( repository );

        stats = new RepositoryScanStatistics();
        stats.setRepositoryId( repository.getId() );

        Closure<RepositoryContentConsumer> triggerBeginScan =
            new TriggerBeginScanClosure( repository, new Date( System.currentTimeMillis() ), true );

        IterableUtils.forEach( knownConsumerList, triggerBeginScan );
        IterableUtils.forEach( invalidConsumerList, triggerBeginScan );

        if ( SystemUtils.IS_OS_WINDOWS )
        {
            consumerWantsFile.setCaseSensitive( false );
        }
    }

    public RepositoryScannerInstance( ManagedRepository repository,
                                      List<KnownRepositoryContentConsumer> knownContentConsumers,
                                      List<InvalidRepositoryContentConsumer> invalidContentConsumers,
                                      long changesSince )
    {
        this( repository, knownContentConsumers, invalidContentConsumers );

        consumerWantsFile.setChangesSince( changesSince );

        this.changesSince = changesSince;
    }

    public RepositoryScanStatistics getStatistics()
    {
        return stats;
    }

    public Map<String, Long> getConsumerTimings()
    {
        return consumerTimings;
    }

    public Map<String, Long> getConsumerCounts()
    {
        return consumerCounts;
    }

    public ManagedRepository getRepository()
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

    public List<String> getFileNameIncludePattern() {
        return fileNameIncludePattern;
    }

    public void setFileNameIncludePattern(List<String> fileNamePattern) {
        this.fileNameIncludePattern = fileNamePattern;
        FileSystem sys = FileSystems.getDefault();
        this.includeMatcher = fileNamePattern.stream().map(ts ->sys
                .getPathMatcher("glob:" + ts)).collect(Collectors.toList());
    }

    public void addFileNameIncludePattern(String fileNamePattern) {
        if (! this.fileNameIncludePattern.contains(fileNamePattern)) {
            this.fileNameIncludePattern.add(fileNamePattern);
            this.includeMatcher.add(FileSystems.getDefault().getPathMatcher("glob:" + fileNamePattern));
        }
    }

    public List<String> getFileNameExcludePattern() {
        return fileNameExcludePattern;
    }

    public void setFileNameExcludePattern(List<String> fileNamePattern) {
        this.fileNameExcludePattern = fileNamePattern;
        FileSystem sys = FileSystems.getDefault();
        this.excludeMatcher = fileNamePattern.stream().map(ts ->sys
                .getPathMatcher("glob:" + ts)).collect(Collectors.toList());
    }

    public void addFileNameExcludePattern(String fileNamePattern) {
        if (! this.fileNameExcludePattern.contains(fileNamePattern)) {
            this.fileNameExcludePattern.add(fileNamePattern);
            this.excludeMatcher.add(FileSystems.getDefault().getPathMatcher("glob:" + fileNamePattern));
        }
    }


    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (!isRunning) {
            isRunning = true;
            this.basePath = dir;
            log.info( "Walk Started: [{}] {}", this.repository.getId(), this.repository.getLocation() );
            stats.triggerStart();
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        final Path relativeFile = basePath.relativize( file );
        if (excludeMatcher.stream().noneMatch(m -> m.matches(relativeFile)) && includeMatcher.stream().allMatch(m -> m.matches(relativeFile))) {
            log.debug( "Walk Step: {}, {}", file );

            stats.increaseFileCount();

            // consume files regardless - the predicate will check the timestamp
            Path repoPath = PathUtil.getPathFromUri( repository.getLocation() );
            BaseFile basefile = new BaseFile( repoPath.toString(), file.toFile() );

            // Timestamp finished points to the last successful scan, not this current one.
            if ( Files.getLastModifiedTime(file).toMillis() >= changesSince )
            {
                stats.increaseNewFileCount();
            }

            consumerProcessFile.setBasefile( basefile );
            consumerWantsFile.setBasefile( basefile );

            Closure<RepositoryContentConsumer> processIfWanted = IfClosure.ifClosure( consumerWantsFile, consumerProcessFile );
            IterableUtils.forEach( this.knownConsumers, processIfWanted );

            if ( consumerWantsFile.getWantedFileCount() <= 0 )
            {
                // Nothing known processed this file.  It is invalid!
                IterableUtils.forEach( this.invalidConsumers, consumerProcessFile );
            }

        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        log.error("Error occured at {}: {}", file, exc.getMessage(), exc);
        try
        {
            if ( basePath != null && Files.isSameFile( file, basePath ) )
            {
                log.debug( "Finishing walk from visitFileFailed" );
                finishWalk( );
            }
        } catch (Throwable e) {
            log.error( "Error during visitFileFailed handling: {}", e.getMessage( ), e );
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (Files.isSameFile(dir, basePath)) {
            finishWalk();
        }
        return FileVisitResult.CONTINUE;
    }

    private void finishWalk() {
        this.isRunning = false;
        TriggerScanCompletedClosure scanCompletedClosure = new TriggerScanCompletedClosure( repository, true );
        IterableUtils.forEach( knownConsumers, scanCompletedClosure );
        IterableUtils.forEach( invalidConsumers, scanCompletedClosure );

        stats.setConsumerTimings( consumerTimings );
        stats.setConsumerCounts( consumerCounts );

        log.info( "Walk Finished: [{}] {}", this.repository.getId(), this.repository.getLocation() );
        stats.triggerFinished();
        this.basePath = null;
    }
}
