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

import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.commons.collections.CollectionUtils;

import javax.xml.bind.annotation.XmlRootElement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * RepositoryScanStatistics - extension to the RepositoryContentStatistics model.
 *
 *
 */
@XmlRootElement( name = "repositoryScanStatistics" )
public class RepositoryScanStatistics
{
    private transient List<String> knownConsumers;

    private transient List<String> invalidConsumers;

    private transient long startTimestamp;

    private SimpleDateFormat df = new SimpleDateFormat();

    /**
     * Field repositoryId
     */
    private String repositoryId;

    /**
     * Field whenGathered
     */
    private Date whenGathered;

    /**
     * Field duration
     */
    private long duration = 0;

    /**
     * Field totalFileCount
     */
    private long totalFileCount = 0;

    /**
     * Field newFileCount
     */
    private long newFileCount = 0;

    /**
     * Field totalSize
     */
    private long totalSize = 0;

    private Map<String, Long> consumerCounts;

    private Map<String, Long> consumerTimings;

    public void triggerStart()
    {
        startTimestamp = System.currentTimeMillis();
    }

    public java.util.Date getWhenGathered()
    {
        return whenGathered;
    }

    public void triggerFinished()
    {
        long finished = System.currentTimeMillis();
        this.duration = finished - startTimestamp;
        this.whenGathered = new java.util.Date( finished );
    }

    public void increaseFileCount()
    {
        this.totalFileCount += 1;
    }

    public void increaseNewFileCount()
    {
        this.newFileCount += 1;
    }

    public void setKnownConsumers( List<String> consumers )
    {
        knownConsumers = consumers;
    }

    public void setInvalidConsumers( List<String> consumers )
    {
        invalidConsumers = consumers;
    }

    public String toDump( ManagedRepository repo )
    {
        StringBuilder buf = new StringBuilder();

        buf.append( "\n.\\ Scan of " ).append( this.getRepositoryId() );
        buf.append( " \\.__________________________________________" );

        buf.append( "\n  Repository Dir    : " ).append( repo.getLocation() );
        buf.append( "\n  Repository Name   : " ).append( repo.getName() );
        buf.append( "\n  Repository Layout : " ).append( repo.getLayout() );

        buf.append( "\n  Known Consumers   : " );
        if ( CollectionUtils.isNotEmpty( knownConsumers ) )
        {
            buf.append( "(" ).append( knownConsumers.size() ).append( " configured)" );
            for ( String id : knownConsumers )
            {
                buf.append( "\n                      " ).append( id );
                if ( consumerTimings.containsKey( id ) )
                {
                    long time = consumerTimings.get( id );
                    buf.append( " (Total: " ).append( time ).append( "ms" );
                    if ( consumerCounts.containsKey( id ) )
                    {
                        long total = consumerCounts.get( id );
                        buf.append( "; Avg.: " + ( time / total ) + "; Count: " + total );
                    }
                    buf.append( ")" );
                }
            }
        }
        else
        {
            buf.append( "<none>" );
        }

        buf.append( "\n  Invalid Consumers : " );
        if ( CollectionUtils.isNotEmpty( invalidConsumers ) )
        {
            buf.append( "(" ).append( invalidConsumers.size() ).append( " configured)" );
            for ( String id : invalidConsumers )
            {
                buf.append( "\n                      " ).append( id );
                if ( consumerTimings.containsKey( id ) )
                {
                    long time = consumerTimings.get( id );
                    buf.append( " (Total: " ).append( time ).append( "ms" );
                    if ( consumerCounts.containsKey( id ) )
                    {
                        long total = consumerCounts.get( id );
                        buf.append( "; Avg.: " + ( time / total ) + "ms; Count: " + total );
                    }
                    buf.append( ")" );
                }
            }
        }
        else
        {
            buf.append( "<none>" );
        }

        buf.append( "\n  Duration          : " );
        buf.append( org.apache.archiva.common.utils.DateUtil.getDuration( this.getDuration() ) );
        buf.append( "\n  When Gathered     : " );
        if ( this.getWhenGathered() == null )
        {
            buf.append( "<null>" );
        }
        else
        {
            buf.append( df.format( this.getWhenGathered() ) );
        }

        buf.append( "\n  Total File Count  : " ).append( this.getTotalFileCount() );

        long averageMsPerFile = 0;

        if ( getTotalFileCount() != 0 )
        {
            averageMsPerFile = ( this.getDuration() / this.getTotalFileCount() );
        }

        buf.append( "\n  Avg Time Per File : " );
        buf.append( org.apache.archiva.common.utils.DateUtil.getDuration( averageMsPerFile ) );
        buf.append( "\n______________________________________________________________" );

        return buf.toString();
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public long getDuration()
    {
        return duration;
    }

    public long getTotalFileCount()
    {
        return totalFileCount;
    }

    public long getNewFileCount()
    {
        return newFileCount;
    }

    public long getTotalSize()
    {
        return totalSize;
    }

    public void setConsumerCounts( Map<String, Long> consumerCounts )
    {
        this.consumerCounts = consumerCounts;
    }

    public void setConsumerTimings( Map<String, Long> consumerTimings )
    {
        this.consumerTimings = consumerTimings;
    }
}
