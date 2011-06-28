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

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.RepositoryContentConsumer;
import org.codehaus.plexus.util.DirectoryWalker;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

/**
 * DefaultRepositoryScanner
 *
 * @version $Id$
 * plexus.component role="org.apache.archiva.repository.scanner.RepositoryScanner"
 */
@Service("repositoryScanner#default")
public class DefaultRepositoryScanner
    implements RepositoryScanner
{
    /**
     * plexus.requirement
     */
    @Inject
    private FileTypes filetypes;

    /**
     * plexus.requirement
     */
    @Inject
    private RepositoryContentConsumers consumerUtil;

    private Set<RepositoryScannerInstance> inProgressScans = new LinkedHashSet<RepositoryScannerInstance>();

    public RepositoryScanStatistics scan( ManagedRepositoryConfiguration repository, long changesSince )
        throws RepositoryScannerException
    {
        List<KnownRepositoryContentConsumer> knownContentConsumers = consumerUtil.getSelectedKnownConsumers();
        List<InvalidRepositoryContentConsumer> invalidContentConsumers = consumerUtil.getSelectedInvalidConsumers();
        List<String> ignoredPatterns = filetypes.getFileTypePatterns( FileTypes.IGNORED );

        return scan( repository, knownContentConsumers, invalidContentConsumers, ignoredPatterns, changesSince );
    }

    public RepositoryScanStatistics scan( ManagedRepositoryConfiguration repository,
                                          List<KnownRepositoryContentConsumer> knownContentConsumers,
                                          List<InvalidRepositoryContentConsumer> invalidContentConsumers,
                                          List<String> ignoredContentPatterns, long changesSince )
        throws RepositoryScannerException
    {
        if ( repository == null )
        {
            throw new IllegalArgumentException( "Unable to operate on a null repository." );
        }

        File repositoryBase = new File( repository.getLocation() );

        //MRM-1342 Repository statistics report doesn't appear to be working correctly
        //create the repo if not existing to have an empty stats
        if ( !repositoryBase.exists() && !repositoryBase.mkdirs() )
        {
            throw new UnsupportedOperationException( "Unable to scan a repository, directory "
                + repositoryBase.getPath() + " does not exist." );
        }

        if ( !repositoryBase.isDirectory() )
        {
            throw new UnsupportedOperationException( "Unable to scan a repository, path "
                + repositoryBase.getPath() + " is not a directory." );
        }

        // Setup Includes / Excludes.

        List<String> allExcludes = new ArrayList<String>();
        List<String> allIncludes = new ArrayList<String>();

        if ( CollectionUtils.isNotEmpty( ignoredContentPatterns ) )
        {
            allExcludes.addAll( ignoredContentPatterns );
        }

        // Scan All Content. (intentional)
        allIncludes.add( "**/*" );

        // Setup Directory Walker
        DirectoryWalker dirWalker = new DirectoryWalker();

        dirWalker.setBaseDir( repositoryBase );

        dirWalker.setIncludes( allIncludes );
        dirWalker.setExcludes( allExcludes );

        // Setup the Scan Instance
        RepositoryScannerInstance scannerInstance = new RepositoryScannerInstance( repository, knownContentConsumers,
                                                                                   invalidContentConsumers, changesSince );

        inProgressScans.add( scannerInstance );

        dirWalker.addDirectoryWalkListener( scannerInstance );

        // Execute scan.
        dirWalker.scan();

        RepositoryScanStatistics stats = scannerInstance.getStatistics();

        stats.setKnownConsumers( gatherIds( knownContentConsumers ) );
        stats.setInvalidConsumers( gatherIds( invalidContentConsumers ) );

        inProgressScans.remove( scannerInstance );

        return stats;
    }

    private List<String> gatherIds( List<? extends RepositoryContentConsumer> consumers )
    {
        List<String> ids = new ArrayList<String>();
        for ( RepositoryContentConsumer consumer : consumers )
        {
            ids.add( consumer.getId() );
        }
        return ids;
    }

    public Set<RepositoryScannerInstance> getInProgressScans()
    {
        return inProgressScans;
    }
}
