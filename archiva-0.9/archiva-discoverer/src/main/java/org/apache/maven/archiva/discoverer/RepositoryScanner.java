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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.lang.SystemUtils;
import org.apache.maven.archiva.common.consumers.Consumer;
import org.apache.maven.archiva.common.utils.BaseFile;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.DirectoryWalkListener;
import org.codehaus.plexus.util.SelectorUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * RepositoryScanner - this is an instance of a scan against a repository.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryScanner
    implements DirectoryWalkListener
{
    public static final String ROLE = RepositoryScanner.class.getName();

    private List consumers;

    private ArtifactRepository repository;

    private Logger logger;

    private boolean isCaseSensitive = true;

    private DiscovererStatistics stats;

    private long onlyModifiedAfterTimestamp = 0;

    public RepositoryScanner( ArtifactRepository repository, List consumerList )
    {
        this.repository = repository;
        this.consumers = consumerList;
        stats = new DiscovererStatistics( repository );

        Iterator it = this.consumers.iterator();
        while ( it.hasNext() )
        {
            Consumer consumer = (Consumer) it.next();

            if ( !consumer.init( this.repository ) )
            {
                throw new IllegalStateException( "Consumer [" + consumer.getName() +
                    "] is reporting that it is incompatible with the [" + repository.getId() + "] repository." );
            }
        }

        if ( SystemUtils.IS_OS_WINDOWS )
        {
            isCaseSensitive = false;
        }
    }

    public DiscovererStatistics getStatistics()
    {
        return stats;
    }

    public void directoryWalkStarting( File basedir )
    {
        getLogger().info( "Walk Started: [" + this.repository.getId() + "] " + this.repository.getBasedir() );
        stats.reset();
        stats.timestampStarted = System.currentTimeMillis();
    }

    public void directoryWalkStep( int percentage, File file )
    {
        getLogger().debug( "Walk Step: " + percentage + ", " + file );

        // Timestamp finished points to the last successful scan, not this current one.
        if ( file.lastModified() < onlyModifiedAfterTimestamp )
        {
            // Skip file as no change has occured.
            getLogger().debug( "Skipping, No Change: " + file.getAbsolutePath() );
            stats.filesSkipped++;
            return;
        }

        synchronized ( consumers )
        {
            stats.filesIncluded++;

            BaseFile basefile = new BaseFile( repository.getBasedir(), file );

            Iterator itConsumers = this.consumers.iterator();
            while ( itConsumers.hasNext() )
            {
                Consumer consumer = (Consumer) itConsumers.next();

                if ( wantsFile( consumer, StringUtils.replace( basefile.getRelativePath(), "\\", "/" ) ) )
                {
                    try
                    {
                        getLogger().debug( "Sending to consumer: " + consumer.getName() );
                        stats.filesConsumed++;
                        consumer.processFile( basefile );
                    }
                    catch ( Exception e )
                    {
                        /* Intentionally Catch all exceptions.
                         * So that the discoverer processing can continue.
                         */
                        getLogger().error( "Consumer [" + consumer.getName() + "] had an error when processing file [" +
                            basefile.getAbsolutePath() + "]: " + e.getMessage(), e );
                    }
                }
                else
                {
                    getLogger().debug(
                        "Skipping consumer " + consumer.getName() + " for file " + basefile.getRelativePath() );
                }
            }
        }
    }

    public void directoryWalkFinished()
    {
        getLogger().info( "Walk Finished: [" + this.repository.getId() + "] " + this.repository.getBasedir() );
        stats.timestampFinished = System.currentTimeMillis();
    }

    private boolean wantsFile( Consumer consumer, String relativePath )
    {
        Iterator it;

        // Test excludes first.
        it = consumer.getExcludePatterns().iterator();
        while ( it.hasNext() )
        {
            String pattern = (String) it.next();
            if ( SelectorUtils.matchPath( pattern, relativePath, isCaseSensitive ) )
            {
                // Definately does NOT WANT FILE.
                return false;
            }
        }

        // Now test includes.
        it = consumer.getIncludePatterns().iterator();
        while ( it.hasNext() )
        {
            String pattern = (String) it.next();
            if ( SelectorUtils.matchPath( pattern, relativePath, isCaseSensitive ) )
            {
                // Specifically WANTS FILE.
                return true;
            }
        }

        // Not included, and Not excluded?  Default to EXCLUDE.
        return false;
    }

    public long getOnlyModifiedAfterTimestamp()
    {
        return onlyModifiedAfterTimestamp;
    }

    public void setOnlyModifiedAfterTimestamp( long onlyModifiedAfterTimestamp )
    {
        this.onlyModifiedAfterTimestamp = onlyModifiedAfterTimestamp;
    }

    /**
     * Debug method from DirectoryWalker.
     */
    public void debug( String message )
    {
        getLogger().debug( "Repository Scanner: " + message );
    }

    public Logger getLogger()
    {
        return logger;
    }

    public void setLogger( Logger logger )
    {
        this.logger = logger;
    }
}
