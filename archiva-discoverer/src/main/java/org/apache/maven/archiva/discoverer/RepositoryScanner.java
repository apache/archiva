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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.DirectoryWalkListener;
import org.codehaus.plexus.util.SelectorUtils;

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

    private boolean checkLastModified = true;

    public RepositoryScanner( ArtifactRepository repository, List consumerList )
    {
        this.repository = repository;
        this.consumers = consumerList;
        stats = new DiscovererStatistics( repository );

        Iterator it = this.consumers.iterator();
        while ( it.hasNext() )
        {
            DiscovererConsumer consumer = (DiscovererConsumer) it.next();

            if ( !consumer.init( this.repository ) )
            {
                throw new IllegalStateException( "Consumer [" + consumer.getName()
                    + "] is reporting that it is incompatible with the [" + repository.getId() + "] repository." );
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

    public void directoryWalkFinished()
    {
        getLogger().info( "Walk Finished." );
        stats.timestampFinished = System.currentTimeMillis();
        
        if( isCheckLastModified() )
        {
            // Only save if dealing with 'last modified' concept.
            
            try
            {
                stats.save();
            }
            catch ( DiscovererException e )
            {
                getLogger().warn( "Unable to save Scan information.", e );
            }
        }
    }

    public void directoryWalkStarting( File basedir )
    {
        getLogger().info( "Walk Started." );
        stats.reset();
        stats.timestampStarted = System.currentTimeMillis();
    }

    public void directoryWalkStep( int percentage, File file )
    {
        getLogger().info( "Walk Step: " + percentage + ", " + file );

        // Timestamp finished points to the last successful scan, not this current one.
        if ( isCheckLastModified() && ( file.lastModified() <= stats.timestampFinished ) )
        {
            // Skip file as no change has occured.
            getLogger().debug( "Skipping, No Change: " + file.getAbsolutePath() );
            stats.filesSkipped++;
            return;
        }

        synchronized ( consumers )
        {
            stats.filesIncluded++;

            String relativePath = PathUtil.getRelative( repository.getBasedir(), file );

            Iterator itConsumers = this.consumers.iterator();
            while ( itConsumers.hasNext() )
            {
                DiscovererConsumer consumer = (DiscovererConsumer) itConsumers.next();

                if ( isConsumerOfFile( consumer, relativePath ) )
                {
                    try
                    {
                        getLogger().info( "Sending to consumer: " + consumer.getName() );
                        stats.filesConsumed++;
                        consumer.processFile( file );
                    }
                    catch ( Exception e )
                    {
                        /* Intentionally Catch all exceptions.
                         * So that the discoverer processing can continue.
                         */
                        getLogger()
                            .error( "Unable to process file [" + file.getAbsolutePath() + "]: " + e.getMessage(), e );
                    }
                }
                else
                {
                    getLogger().info( "Skipping consumer " + consumer.getName() + " for file " + relativePath );
                }
            }
        }
    }

    private boolean isConsumerOfFile( DiscovererConsumer consumer, String relativePath )
    {
        Iterator it = consumer.getIncludePatterns().iterator();
        // String name = file.getAbsolutePath();
        while ( it.hasNext() )
        {
            String pattern = (String) it.next();
            if ( SelectorUtils.matchPath( pattern, relativePath, isCaseSensitive ) )
            {
                return true;
            }
        }

        return false;
    }

    public boolean isCheckLastModified()
    {
        return checkLastModified;
    }

    public void setCheckLastModified( boolean checkLastModified )
    {
        this.checkLastModified = checkLastModified;
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
