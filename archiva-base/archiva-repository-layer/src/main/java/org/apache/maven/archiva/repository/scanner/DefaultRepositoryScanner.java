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
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.consumers.RepositoryContentConsumer;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.RepositoryContentStatistics;
import org.apache.maven.archiva.repository.RepositoryException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.DirectoryWalker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * DefaultRepositoryScanner
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.repository.scanner.RepositoryScanner"
 */
public class DefaultRepositoryScanner
    extends AbstractLogEnabled
    implements RepositoryScanner
{
    /**
     * @plexus.requirement
     */
    private FileTypes filetypes;

    /**
     * @plexus.requirement
     */
    private RepositoryContentConsumers consumerUtil;

    public RepositoryContentStatistics scan( ArchivaRepository repository, long changesSince )
        throws RepositoryException
    {
        List knownContentConsumers = consumerUtil.getSelectedKnownConsumers();
        List invalidContentConsumers = consumerUtil.getSelectedInvalidConsumers();
        List ignoredPatterns = filetypes.getFileTypePatterns( FileTypes.IGNORED );

        return scan( repository, knownContentConsumers, invalidContentConsumers, ignoredPatterns, changesSince );
    }

    public RepositoryContentStatistics scan( ArchivaRepository repository, List knownContentConsumers,
                                             List invalidContentConsumers, List ignoredContentPatterns,
                                             long changesSince )
        throws RepositoryException
    {
        if ( repository == null )
        {
            throw new IllegalArgumentException( "Unable to operate on a null repository." );
        }

        if ( !"file".equals( repository.getUrl().getProtocol() ) )
        {
            throw new UnsupportedOperationException( "Only filesystem repositories are supported." );
        }

        File repositoryBase = new File( repository.getUrl().getPath() );

        if ( !repositoryBase.exists() )
        {
            throw new UnsupportedOperationException(
                "Unable to scan a repository, directory " + repositoryBase.getAbsolutePath() + " does not exist." );
        }

        if ( !repositoryBase.isDirectory() )
        {
            throw new UnsupportedOperationException(
                "Unable to scan a repository, path " + repositoryBase.getAbsolutePath() + " is not a directory." );
        }

        // Setup Includes / Excludes.

        List allExcludes = new ArrayList();
        List allIncludes = new ArrayList();

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
        RepositoryScannerInstance scannerInstance =
            new RepositoryScannerInstance( repository, knownContentConsumers, invalidContentConsumers, getLogger() );
        scannerInstance.setOnlyModifiedAfterTimestamp( changesSince );

        dirWalker.addDirectoryWalkListener( scannerInstance );

        // Execute scan.
        dirWalker.scan();

        RepositoryContentStatistics stats = scannerInstance.getStatistics();

        ConsumerIdClosure consumerIdList;

        consumerIdList = new ConsumerIdClosure();
        CollectionUtils.forAllDo( knownContentConsumers, consumerIdList );
        stats.setKnownConsumers( consumerIdList.getList() );

        consumerIdList = new ConsumerIdClosure();
        CollectionUtils.forAllDo( invalidContentConsumers, consumerIdList );
        stats.setInvalidConsumers( consumerIdList.getList() );

        return stats;
    }

    class ConsumerIdClosure
        implements Closure
    {
        private List list = new ArrayList();

        public void execute( Object input )
        {
            if ( input instanceof RepositoryContentConsumer )
            {
                RepositoryContentConsumer consumer = (RepositoryContentConsumer) input;
                list.add( consumer.getId() );
            }
        }

        public List getList()
        {
            return list;
        }
    }
}
