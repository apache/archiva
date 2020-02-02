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

import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.consumers.ConsumerException;
import org.apache.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.archiva.consumers.RepositoryContentConsumer;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.util.*;

/**
 * DefaultRepositoryScanner
 *
 *
 */
@Service( "repositoryScanner#default" )
public class DefaultRepositoryScanner
    implements RepositoryScanner
{

    private static final Logger log  = LoggerFactory.getLogger(DefaultRepositoryScanner.class);

    @Inject
    private FileTypes filetypes;

    @Inject
    private RepositoryContentConsumers repositoryContentConsumers;

    private Set<RepositoryScannerInstance> inProgressScans = new LinkedHashSet<>();

    @Override
    public RepositoryScanStatistics scan( ManagedRepository repository, long changesSince )
        throws RepositoryScannerException
    {
        List<KnownRepositoryContentConsumer> knownContentConsumers = null;
        try
        {
            knownContentConsumers = repositoryContentConsumers.getSelectedKnownConsumers();
            List<InvalidRepositoryContentConsumer> invalidContentConsumers = repositoryContentConsumers.getSelectedInvalidConsumers();
            List<String> ignoredPatterns = filetypes.getFileTypePatterns( FileTypes.IGNORED );

            return scan( repository, knownContentConsumers, invalidContentConsumers, ignoredPatterns, changesSince );
        }
        catch ( ConsumerException e )
        {
            throw new RepositoryScannerException( e.getMessage( ), e );
        }
        finally
        {
            repositoryContentConsumers.releaseSelectedKnownConsumers( knownContentConsumers );
        }
    }

    @Override
    public RepositoryScanStatistics scan( ManagedRepository repository,
                                          List<KnownRepositoryContentConsumer> knownContentConsumers,
                                          List<InvalidRepositoryContentConsumer> invalidContentConsumers,
                                          List<String> ignoredContentPatterns, long changesSince )
        throws RepositoryScannerException
    {
        if ( repository == null )
        {
            throw new IllegalArgumentException( "Unable to operate on a null repository." );
        }

        StorageAsset repositoryBase = repository.getAsset("");

        //MRM-1342 Repository statistics report doesn't appear to be working correctly
        //create the repo if not existing to have an empty stats
        if ( !repositoryBase.exists())
        {
            try {
                repositoryBase.create();
            } catch (IOException e) {
                throw new UnsupportedOperationException("Unable to scan a repository, directory " + repositoryBase + " does not exist." );
            }
        }

        if ( !repositoryBase.isContainer())
        {
            throw new UnsupportedOperationException(
                "Unable to scan a repository, path " + repositoryBase+ " is not a directory." );
        }

        // Setup Includes / Excludes.

        List<String> allExcludes = new ArrayList<>();
        List<String> allIncludes = new ArrayList<>();

        if ( CollectionUtils.isNotEmpty( ignoredContentPatterns ) )
        {
            allExcludes.addAll( ignoredContentPatterns );
        }

        // Scan All Content. (intentional)
        allIncludes.add( "**/*" );

        // Setup the Scan Instance
        RepositoryScannerInstance scannerInstance =
            new RepositoryScannerInstance( repository, knownContentConsumers, invalidContentConsumers, changesSince );

        scannerInstance.setFileNameIncludePattern(allIncludes);
        scannerInstance.setFileNameExcludePattern(allExcludes);
        inProgressScans.add( scannerInstance );

        RepositoryScanStatistics stats = null;
        try
        {
            Files.walkFileTree(repositoryBase.getFilePath(), EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, scannerInstance);

            stats = scannerInstance.getStatistics();

            stats.setKnownConsumers( gatherIds( knownContentConsumers ) );
            stats.setInvalidConsumers( gatherIds( invalidContentConsumers ) );
        } catch (IOException e) {
            log.error("Could not scan directory {}: {}", repositoryBase, e.getMessage(), e);
        } finally
        {
            inProgressScans.remove( scannerInstance );
        }

        return stats;
    }

    private List<String> gatherIds( List<? extends RepositoryContentConsumer> consumers )
    {
        List<String> ids = new ArrayList<>();
        for ( RepositoryContentConsumer consumer : consumers )
        {
            ids.add( consumer.getId() );
        }
        return ids;
    }

    @Override
    public Set<RepositoryScannerInstance> getInProgressScans()
    {
        return inProgressScans;
    }
}
