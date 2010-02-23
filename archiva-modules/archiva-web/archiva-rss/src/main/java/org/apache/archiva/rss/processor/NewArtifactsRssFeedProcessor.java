package org.apache.archiva.rss.processor;

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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.sun.syndication.feed.synd.SyndFeed;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.rss.RssFeedEntry;
import org.apache.archiva.rss.RssFeedGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retrieve and process all artifacts of a repository from the database and generate a rss feed.
 * The artifacts will be grouped by the date when the artifacts were gathered. 
 * Each group will appear as one entry in the feed.
 * 
 * @plexus.component role="org.apache.archiva.rss.processor.RssFeedProcessor" role-hint="new-artifacts"
 */
public class NewArtifactsRssFeedProcessor
    extends AbstractArtifactsRssFeedProcessor
{
    private int numberOfDaysBeforeNow = 30;
    
    private static final String title = "New Artifacts in Repository ";

    private static final String desc = "These are the new artifacts found in the repository ";

    /**
     * @plexus.requirement
     */
    private RssFeedGenerator generator;

    private Logger log = LoggerFactory.getLogger( NewArtifactsRssFeedProcessor.class );

    private static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone( "GMT" );

    /**
     * Process the newly discovered artifacts in the repository. Generate feeds for new artifacts in the repository and
     * new versions of artifact.
     */
    public SyndFeed process( Map<String, String> reqParams )
    {
        log.debug( "Process new artifacts into rss feeds." );

        String repoId = reqParams.get( RssFeedProcessor.KEY_REPO_ID );
        if ( repoId != null )
        {
            return processNewArtifactsInRepo( repoId );
        }

        return null;
    }

    private SyndFeed processNewArtifactsInRepo( String repoId )
    {
        Calendar greaterThanThisDate = Calendar.getInstance( GMT_TIME_ZONE );
        greaterThanThisDate.add( Calendar.DATE, -( getNumberOfDaysBeforeNow() ) );
        greaterThanThisDate.clear( Calendar.MILLISECOND );

        List<ArtifactMetadata> artifacts =
            metadataRepository.getArtifactsByDateRange( repoId, greaterThanThisDate.getTime(), null );

        long tmp = 0;
        RssFeedEntry entry = null;
        List<RssFeedEntry> entries = new ArrayList<RssFeedEntry>();
        String description = "";
        int idx = 0;
        for ( ArtifactMetadata artifact : artifacts )
        {
            long whenGathered = artifact.getWhenGathered().getTime();

            String id = artifact.getNamespace() + "/" + artifact.getProject() + "/" + artifact.getId();
            if ( tmp != whenGathered )
            {
                if ( entry != null )
                {
                    entry.setDescription( description );
                    entries.add( entry );
                    entry = null;
                }

                String repoId1 = artifact.getRepositoryId();
                entry = new RssFeedEntry( this.getTitle() + "\'" + repoId1 + "\'" + " as of " + new Date( whenGathered ) );
                entry.setPublishedDate( artifact.getWhenGathered() );
                description = this.getDescription() + "\'" + repoId1 + "\'" + ": \n" + id + " | ";
            }
            else
            {
                description = description + id + " | ";
            }

            if ( idx == ( artifacts.size() - 1 ) )
            {
                entry.setDescription( description );
                entries.add( entry );
            }

            tmp = whenGathered;
            idx++;
        }

        return generator.generateFeed( getTitle() + "\'" + repoId + "\'", "New artifacts found in repository " +
            "\'" + repoId + "\'" + " during repository scan.", entries );
    }

    public String getTitle()
    {
        return title;
    }

    public String getDescription()
    {
        return desc;
    }

    public RssFeedGenerator getGenerator()
    {
        return generator;
    }

    public void setGenerator( RssFeedGenerator generator )
    {
        this.generator = generator;
    }

    public int getNumberOfDaysBeforeNow()
    {
        return numberOfDaysBeforeNow;
    }

    public void setNumberOfDaysBeforeNow( int numberOfDaysBeforeNow )
    {
        this.numberOfDaysBeforeNow = numberOfDaysBeforeNow;
    }
}
