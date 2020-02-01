package org.apache.archiva.consumers.functors;

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
import org.apache.archiva.consumers.RepositoryContentConsumer;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.List;

/**
 * ConsumerWantsFilePredicate
 */
public class ConsumerWantsFilePredicate
    implements Predicate<RepositoryContentConsumer>
{
    private BaseFile basefile;

    private boolean isCaseSensitive = true;

    private int wantedFileCount = 0;

    private long changesSince = 0;

    private ManagedRepository managedRepository;

    private Logger logger = LoggerFactory.getLogger( getClass( ) );

    /**
     * @deprecated use constructor with ManagedRepository
     */
    public ConsumerWantsFilePredicate( )
    {
        // no-op
    }

    public ConsumerWantsFilePredicate( ManagedRepository managedRepository )
    {
        this.managedRepository = managedRepository;
    }

    @Override
    public boolean evaluate( RepositoryContentConsumer object )
    {
        boolean satisfies = false;

        RepositoryContentConsumer consumer = (RepositoryContentConsumer) object;
        if ( wantsFile( consumer, FilenameUtils.separatorsToUnix( basefile.getRelativePath( ) ) ) )
        {
            satisfies = true;

            // regardless of the timestamp, we record that it was wanted so it doesn't get counted as invalid
            wantedFileCount++;

            if ( !consumer.isProcessUnmodified( ) )
            {
                // Timestamp finished points to the last successful scan, not this current one.
                if ( basefile.lastModified( ) < changesSince )
                {
                    // Skip file as no change has occurred.
                    satisfies = false;
                }
            }
        }

        return satisfies;
    }

    public BaseFile getBasefile( )
    {
        return basefile;
    }

    public int getWantedFileCount( )
    {
        return wantedFileCount;
    }

    public boolean isCaseSensitive( )
    {
        return isCaseSensitive;
    }

    public void setBasefile( BaseFile basefile )
    {
        this.basefile = basefile;
        this.wantedFileCount = 0;
    }

    public void setCaseSensitive( boolean isCaseSensitive )
    {
        this.isCaseSensitive = isCaseSensitive;
    }

    private boolean wantsFile( RepositoryContentConsumer consumer, String relativePath )
    {
        // Test excludes first.
        List<String> excludes = consumer.getExcludes( );
        if ( excludes != null )
        {
            for ( String pattern : excludes )
            {
                if ( PathUtil.matchPath( pattern, relativePath, isCaseSensitive ) )
                {
                    // Definately does NOT WANT FILE.
                    return false;
                }
            }
        }

        if ( managedRepository != null )
        {
            String indexDirectory;
            if ( managedRepository.supportsFeature( IndexCreationFeature.class ) )
            {
                IndexCreationFeature icf = managedRepository.getFeature( IndexCreationFeature.class ).get( );
                if ( icf.getIndexPath( ) == null )
                {
                    indexDirectory = ".index";
                }
                else
                {
                    indexDirectory = ( icf.getIndexPath( ).getScheme( ) == null ? Paths.get( icf.getIndexPath( ).getPath( ) ) : Paths.get( icf.getIndexPath( ) ) ).toString( );
                }
            }
            else
            {
                indexDirectory = ".index";
            }
            if ( StringUtils.isEmpty( indexDirectory ) )
            {
                indexDirectory = ".index";
            }
            if ( StringUtils.startsWith( relativePath, indexDirectory ) )
            {
                logger.debug( "ignore file {} part of the index directory {}", relativePath, indexDirectory );
                return false;
            }
        }

        // Now test includes.
        for ( String pattern : consumer.getIncludes( ) )
        {
            if ( PathUtil.matchPath( pattern, relativePath, isCaseSensitive ) )
            {
                // Specifically WANTS FILE.
                return true;
            }
        }

        // Not included, and Not excluded?  Default to EXCLUDE.
        return false;
    }

    public void setChangesSince( long changesSince )
    {
        this.changesSince = changesSince;
    }
}
