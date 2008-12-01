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
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.archiva.rss.RssFeedEntry;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.model.ArchivaArtifact;

import com.sun.syndication.feed.synd.SyndFeed;

/**
 * @version
 */
public abstract class AbstractArtifactsRssFeedProcessor
    implements RssFeedProcessor
{
    public abstract SyndFeed process( Map<String, String> reqParams ) throws ArchivaDatabaseException;

    protected List<RssFeedEntry> processData( List<ArchivaArtifact> artifacts, boolean isRepoLevel )
    {
        long tmp = 0;
        RssFeedEntry entry = null;
        List<RssFeedEntry> entries = new ArrayList<RssFeedEntry>();
        String description = "";
        int idx = 0;
        for ( ArchivaArtifact artifact : artifacts )
        {
            long whenGathered = artifact.getModel().getWhenGathered().getTime();
            
            if ( tmp != whenGathered )
            {
                if ( entry != null )
                {                    
                    entry.setDescription( description );
                    entries.add( entry );
                    entry = null;
                }
                
                if ( !isRepoLevel )
                {
                    entry =
                        new RssFeedEntry( getTitle() + "\'" + artifact.getGroupId() + ":" + artifact.getArtifactId() +
                            "\'" + " as of " + new Date( whenGathered ) );
                    entry.setPublishedDate( artifact.getModel().getWhenGathered() );
                    description = getDescription() + "\'" + artifact.getGroupId() + ":" + artifact.getArtifactId() +
                        "\'" + ": \n" + artifact.toString() + " | ";
                }
                else
                {
                    String repoId = artifact.getModel().getRepositoryId();
                    entry = new RssFeedEntry( getTitle() + "\'" + repoId + "\'" + " as of " + new Date( whenGathered ) );
                    entry.setPublishedDate( artifact.getModel().getWhenGathered() );
                    description = getDescription() + "\'" + repoId + "\'" + ": \n" + artifact.toString() + " | ";
                }
            }
            else
            {
                description = description + artifact.toString() + " | ";
            }

            if ( idx == ( artifacts.size() - 1 ) )
            {                
                entry.setDescription( description );
                entries.add( entry );
            }

            tmp = whenGathered;
            idx++;
        }

        return entries;
    }

    protected abstract String getTitle();

    protected abstract String getDescription();

}
