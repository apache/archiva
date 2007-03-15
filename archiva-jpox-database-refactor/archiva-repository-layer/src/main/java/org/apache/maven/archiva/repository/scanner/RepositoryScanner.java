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

import org.apache.maven.archiva.consumers.Consumer;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.RepositoryContentStatistics;
import org.apache.maven.archiva.repository.RepositoryException;
import org.codehaus.plexus.util.DirectoryWalker;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * RepositoryScanner 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryScanner
{
    /**
     * Standard patterns to exclude from discovery as they are usually noise.
     */
    private static final String[] STANDARD_SCANNER_EXCLUDES = {
        "bin/**",
        "reports/**",
        ".index",
        ".reports/**",
        ".maven/**",
        "**/*snapshot-version",
        "*/website/**",
        "*/licences/**",
        "**/.htaccess",
        "**/*.html",
        "**/*.txt",
        "**/README*",
        "**/CHANGELOG*",
        "**/KEYS*" };

    /**
     * Walk the repository, and report to the consumers the files found.
     * 
     * Report changes to the appropriate Consumer.
     * 
     * This is just a convenience method to {@link #scan(ArtifactRepository, List, boolean, long, List, List)}
     * equivalent to calling <code>scan( repository, consumers, includeSnapshots, 0, null, null );</code>
     * 
     * @param repository the repository to change.
     * @param consumers use the provided list of consumers.
     * @param includeSnapshots true to include snapshots in the walking of this repository.
     * @return the statistics for this scan.
     * @throws RepositoryException if there was a fundamental problem with getting the discoverer started.
     */
    public RepositoryContentStatistics scan( ArchivaRepository repository, List consumers, boolean includeSnapshots )
        throws RepositoryException
    {
        return scan( repository, consumers, includeSnapshots, 0, null, null );
    }

    /**
     * Walk the repository, and report to the consumers the files found.
     * 
     * Report changes to the appropriate Consumer.
     * 
     * @param repository the repository to change.
     * @param consumers use the provided list of consumers.
     * @param includeSnapshots true to include snapshots in the scanning of this repository.
     * @param onlyModifiedAfterTimestamp Only report to the consumers, files that have a {@link File#lastModified()}) 
     *          after the provided timestamp.
     * @param extraFileExclusions an optional list of file exclusions on the walk.
     * @param extraFileInclusions an optional list of file inclusions on the walk.
     * @return the statistics for this scan.
     * @throws RepositoryException if there was a fundamental problem with getting the discoverer started. 
     */
    public RepositoryContentStatistics scan( ArchivaRepository repository, List consumers, boolean includeSnapshots,
                                long onlyModifiedAfterTimestamp, List extraFileExclusions, List extraFileInclusions )
        throws RepositoryException
    {
        if ( repository == null )
        {
            throw new IllegalArgumentException( "Unable to operate on a null repository." );
        }

        if ( !"file".equals( repository.getRepositoryURL().getProtocol() ) )
        {
            throw new UnsupportedOperationException( "Only filesystem repositories are supported." );
        }

        File repositoryBase = new File( repository.getRepositoryURL().getPath() );

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

        List allExcludes = new ArrayList();
        List allIncludes = new ArrayList();

        // Exclude all of the SCM patterns.
        allExcludes.addAll( FileUtils.getDefaultExcludesAsList() );

        // Exclude all of the archiva noise patterns.
        allExcludes.addAll( Arrays.asList( STANDARD_SCANNER_EXCLUDES ) );

        if ( !includeSnapshots )
        {
            allExcludes.add( "**/*-SNAPSHOT*" );
        }

        if ( extraFileExclusions != null )
        {
            allExcludes.addAll( extraFileExclusions );
        }

        Iterator it = consumers.iterator();
        while ( it.hasNext() )
        {
            Consumer consumer = (Consumer) it.next();

            /* NOTE: Do not insert the consumer exclusion patterns here.
             * Exclusion patterns are handled by RepositoryScanner.wantsFile(Consumer, String)
             * 
             * addUniqueElements( consumer.getExcludePatterns(), allExcludes );
             */
            addUniqueElements( consumer.getIncludePatterns(), allIncludes );
        }

        if ( extraFileInclusions != null )
        {
            allIncludes.addAll( extraFileInclusions );
        }

        // Setup Directory Walker

        DirectoryWalker dirWalker = new DirectoryWalker();

        dirWalker.setBaseDir( repositoryBase );

        dirWalker.setIncludes( allIncludes );
        dirWalker.setExcludes( allExcludes );

        // Setup the Scan Instance
        RepositoryScannerInstance scannerInstance = new RepositoryScannerInstance( repository, consumers );
        scannerInstance.setOnlyModifiedAfterTimestamp( onlyModifiedAfterTimestamp );

        dirWalker.addDirectoryWalkListener( scannerInstance );

        // Execute scan.
        dirWalker.scan();

        return scannerInstance.getStatistics();
    }
    
    private void addUniqueElements( List fromList, List toList )
    {
        Iterator itFrom = fromList.iterator();
        while ( itFrom.hasNext() )
        {
            Object o = itFrom.next();
            if ( !toList.contains( o ) )
            {
                toList.add( o );
            }
        }
    }
}
