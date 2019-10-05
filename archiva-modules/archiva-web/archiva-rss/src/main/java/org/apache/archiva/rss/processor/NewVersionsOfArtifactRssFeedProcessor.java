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

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.*;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.rss.RssFeedEntry;
import org.apache.archiva.rss.RssFeedGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Retrieve and process new versions of an artifact from the database and
 * generate a rss feed. The versions will be grouped by the date when the artifact
 * was gathered. Each group will appear as one entry in the feed.
 *
 */
@Service("rssFeedProcessor#new-versions")
public class NewVersionsOfArtifactRssFeedProcessor
    extends AbstractArtifactsRssFeedProcessor
{
    private Logger log = LoggerFactory.getLogger( NewVersionsOfArtifactRssFeedProcessor.class );

    private static final String title = "New Versions of Artifact ";

    private static final String desc = "These are the new versions of artifact ";

    @Inject
    private RssFeedGenerator generator;

    @Inject
    private RepositoryRegistry repositoryRegistry;

    @Inject
    private RepositorySessionFactory repositorySessionFactory;

    /**
     * Process all versions of the artifact which had a rss feed request.
     */
    @Override
    public SyndFeed process( Map<String, String> reqParams )
        throws FeedException
    {
        String groupId = reqParams.get( RssFeedProcessor.KEY_GROUP_ID );
        String artifactId = reqParams.get( RssFeedProcessor.KEY_ARTIFACT_ID );

        if ( groupId != null && artifactId != null )
        {
            return processNewVersionsOfArtifact( groupId, artifactId );
        }

        return null;
    }

    private SyndFeed processNewVersionsOfArtifact( String groupId, String artifactId )
        throws FeedException
    {
        List<ArtifactMetadata> artifacts = new ArrayList<>();
        try(RepositorySession session = repositorySessionFactory.createSession())
        {
            final MetadataRepository metadataRepository = session.getRepository( );
            for ( Repository repo : repositoryRegistry.getRepositories() )
            {
                final String repoId = repo.getId();
                Collection<String> versions = metadataRepository.getProjectVersions( session, repoId, groupId, artifactId );
                for ( String version : versions )
                {
                    artifacts.addAll( metadataRepository.getArtifacts(session , repoId, groupId, artifactId, version ) );
                }
            }
        }
        catch ( MetadataRepositoryException e )
        {
            throw new FeedException( "Unable to construct feed, metadata could not be retrieved: " + e.getMessage(),
                                     e );
        }
        catch ( MetadataResolutionException e )
        {
            throw new FeedException( "Unable to construct feed, metadata could not be retrieved: " + e.getMessage(),
                                     e );
        }

        long tmp = 0;
        RssFeedEntry entry = null;
        List<RssFeedEntry> entries = new ArrayList<>();
        String description = "";
        int idx = 0;
        for ( ArtifactMetadata artifact : artifacts )
        {
            long whenGathered = artifact.getWhenGathered().toInstant().toEpochMilli();

            if ( tmp != whenGathered )
            {
                if ( entry != null )
                {
                    entry.setDescription( description );
                    entries.add( entry );
                    entry = null;
                }

                entry = new RssFeedEntry(
                    this.getTitle() + "\'" + groupId + ":" + artifactId + "\'" + " as of " + new Date( whenGathered ) );
                entry.setPublishedDate( Date.from(artifact.getWhenGathered().toInstant()) );
                description =
                    this.getDescription() + "\'" + groupId + ":" + artifactId + "\'" + ": \n" + artifact.getId() +
                        " | ";
            }
            else
            {
                description = description + artifact.getId() + " | ";
            }

            if ( idx == ( artifacts.size() - 1 ) )
            {
                entry.setDescription( description );
                entries.add( entry );
            }

            tmp = whenGathered;
            idx++;
        }

        String key = groupId + ":" + artifactId;

        return generator.generateFeed( getTitle() + "\'" + key + "\'",
                                       "New versions of artifact " + "\'" + key + "\' found during repository scan.",
                                       entries );
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    @Override
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

    public RepositorySessionFactory getRepositorySessionFactory( )
    {
        return repositorySessionFactory;
    }

    public void setRepositorySessionFactory( RepositorySessionFactory repositorySessionFactory )
    {
        this.repositorySessionFactory = repositorySessionFactory;
    }

    public RepositoryRegistry getRepositoryRegistry( )
    {
        return repositoryRegistry;
    }

    public void setRepositoryRegistry( RepositoryRegistry repositoryRegistry )
    {
        this.repositoryRegistry = repositoryRegistry;
    }
}
