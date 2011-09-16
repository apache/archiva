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

import java.util.List;

import org.apache.commons.collections.Predicate;
import org.apache.commons.io.FilenameUtils;
import org.apache.archiva.common.utils.BaseFile;
import org.apache.archiva.consumers.RepositoryContentConsumer;
import org.codehaus.plexus.util.SelectorUtils;

/**
 * ConsumerWantsFilePredicate 
 *
 * @version $Id$
 */
public class ConsumerWantsFilePredicate
    implements Predicate
{
    private BaseFile basefile;

    private boolean isCaseSensitive = true;

    private int wantedFileCount = 0;

    private long changesSince = 0;

    public boolean evaluate( Object object )
    {
        boolean satisfies = false;

        if ( object instanceof RepositoryContentConsumer )
        {
            RepositoryContentConsumer consumer = (RepositoryContentConsumer) object;
            if ( wantsFile( consumer, FilenameUtils.separatorsToUnix( basefile.getRelativePath() ) ) )
            {
                satisfies = true;
                
                // regardless of the timestamp, we record that it was wanted so it doesn't get counted as invalid
                wantedFileCount++;

                if ( !consumer.isProcessUnmodified() )
                {
                    // Timestamp finished points to the last successful scan, not this current one.
                    if ( basefile.lastModified() < changesSince )
                    {
                        // Skip file as no change has occurred.
                        satisfies = false;
                    }
                }
            }
        }

        return satisfies;
    }

    public BaseFile getBasefile()
    {
        return basefile;
    }

    public int getWantedFileCount()
    {
        return wantedFileCount;
    }

    public boolean isCaseSensitive()
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
        List<String> excludes = consumer.getExcludes();
        if ( excludes != null )
        {
            for ( String pattern : excludes )
            {
                if ( SelectorUtils.matchPath( pattern, relativePath, isCaseSensitive ) )
                {
                    // Definately does NOT WANT FILE.
                    return false;
                }
            }
        }

        // Now test includes.
        for ( String pattern : consumer.getIncludes() )
        {
            if ( SelectorUtils.matchPath( pattern, relativePath, isCaseSensitive ) )
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
