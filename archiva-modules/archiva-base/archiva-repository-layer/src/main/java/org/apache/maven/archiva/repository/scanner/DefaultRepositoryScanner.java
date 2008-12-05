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
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.consumers.RepositoryContentConsumer;
import org.apache.maven.archiva.repository.RepositoryException;
import org.codehaus.plexus.util.DirectoryWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DefaultRepositoryScanner
 *
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.repository.scanner.RepositoryScanner"
 */
public class DefaultRepositoryScanner
    implements RepositoryScanner
{
    private Logger log = LoggerFactory.getLogger( DefaultRepositoryScanner.class );
    
    /**
     * @plexus.requirement
     */
    private FileTypes filetypes;

    /**
     * @plexus.requirement
     */
    private RepositoryContentConsumers consumerUtil;
    
    public RepositoryScanStatistics scan( ManagedRepositoryConfiguration repository, long changesSince )
        throws RepositoryException
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
        throws RepositoryException
    {
        if ( repository == null )
        {
            throw new IllegalArgumentException( "Unable to operate on a null repository." );
        }

        File repositoryBase = new File( repository.getLocation() );

        if ( !repositoryBase.exists() )
        {
            throw new UnsupportedOperationException( "Unable to scan a repository, directory "
                + repositoryBase.getAbsolutePath() + " does not exist." );
        }

        if ( !repositoryBase.isDirectory() )
        {
            throw new UnsupportedOperationException( "Unable to scan a repository, path "
                + repositoryBase.getAbsolutePath() + " is not a directory." );
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

        dirWalker.addDirectoryWalkListener( scannerInstance );

        // Execute scan.
        dirWalker.scan();

        RepositoryScanStatistics stats = scannerInstance.getStatistics();

        stats.setKnownConsumers( gatherIds( knownContentConsumers ) );
        stats.setInvalidConsumers( gatherIds( invalidContentConsumers ) );
        
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
}
