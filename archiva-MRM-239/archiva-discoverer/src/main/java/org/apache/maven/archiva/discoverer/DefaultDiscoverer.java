package org.apache.maven.archiva.discoverer;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiva.common.consumers.Consumer;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.DirectoryWalker;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Discoverer Implementation.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @plexus.component role="org.apache.maven.archiva.discoverer.Discoverer"
 */
public class DefaultDiscoverer
    extends AbstractLogEnabled
    implements Discoverer
{
    /**
     * Standard patterns to exclude from discovery as they are usually noise.
     */
    private static final String[] STANDARD_DISCOVERY_EXCLUDES = {
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

    public DefaultDiscoverer()
    {
    }

    public DiscovererStatistics walkRepository( ArtifactRepository repository, List consumers, boolean includeSnapshots )
        throws DiscovererException
    {
        return walkRepository( repository, consumers, includeSnapshots, 0, null, null );
    }

    public DiscovererStatistics walkRepository( ArtifactRepository repository, List consumers,
                                                boolean includeSnapshots, long onlyModifiedAfterTimestamp,
                                                List extraFileExclusions, List extraFileInclusions )
        throws DiscovererException
    {
        // Sanity Check

        if ( repository == null )
        {
            throw new IllegalArgumentException( "Unable to operate on a null repository." );
        }

        if ( !"file".equals( repository.getProtocol() ) )
        {
            throw new UnsupportedOperationException( "Only filesystem repositories are supported." );
        }

        File repositoryBase = new File( repository.getBasedir() );

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
        allExcludes.addAll( Arrays.asList( STANDARD_DISCOVERY_EXCLUDES ) );

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
        RepositoryScanner repoScanner = new RepositoryScanner( repository, consumers );
        repoScanner.setOnlyModifiedAfterTimestamp( onlyModifiedAfterTimestamp );

        repoScanner.setLogger( getLogger() );
        dirWalker.addDirectoryWalkListener( repoScanner );

        // Execute scan.
        dirWalker.scan();

        return repoScanner.getStatistics();
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
