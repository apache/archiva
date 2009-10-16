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

import java.util.List;
import java.util.Map;

import org.apache.archiva.rss.RssFeedEntry;
import org.apache.archiva.rss.RssFeedGenerator;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.constraints.ArtifactVersionsConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;

import com.sun.syndication.feed.synd.SyndFeed;

/**
 * Retrieve and process new versions of an artifact from the database and
 * generate a rss feed. The versions will be grouped by the date when the artifact 
 * was gathered. Each group will appear as one entry in the feed.
 * 
 * @version
 * @plexus.component role="org.apache.archiva.rss.processor.RssFeedProcessor" role-hint="new-versions"
 */
public class NewVersionsOfArtifactRssFeedProcessor
    extends AbstractArtifactsRssFeedProcessor
{
    private static final String title = "New Versions of Artifact ";

    private static final String desc = "These are the new versions of artifact ";

    /**
     * @plexus.requirement
     */
    private RssFeedGenerator generator;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArtifactDAO artifactDAO;

    /**
     * Process all versions of the artifact which had a rss feed request.
     */
    public SyndFeed process( Map<String, String> reqParams ) throws ArchivaDatabaseException
    {
        String repoId = reqParams.get( RssFeedProcessor.KEY_REPO_ID );
        String groupId = reqParams.get( RssFeedProcessor.KEY_GROUP_ID );
        String artifactId = reqParams.get( RssFeedProcessor.KEY_ARTIFACT_ID );
        
        if ( groupId != null && artifactId != null )
        {
            return processNewVersionsOfArtifact( repoId, groupId, artifactId );
        }

        return null;
    }

    private SyndFeed processNewVersionsOfArtifact( String repoId, String groupId, String artifactId )
        throws ArchivaDatabaseException
    {
                    
        Constraint artifactVersions = new ArtifactVersionsConstraint( repoId, groupId, artifactId, "whenGathered" );
        List<ArchivaArtifact> artifacts = artifactDAO.queryArtifacts( artifactVersions );
        
        List<RssFeedEntry> entries = processData( artifacts, false );
        String key = groupId + ":" + artifactId;
        
        return generator.generateFeed( getTitle() + "\'" + key + "\'", "New versions of artifact " + "\'" + key +
            "\' found in repository " + "\'" + repoId + "\'" + " during repository scan.", entries );
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

    public ArtifactDAO getArtifactDAO()
    {
        return artifactDAO;
    }

    public void setArtifactDAO( ArtifactDAO artifactDAO )
    {
        this.artifactDAO = artifactDAO;
    }

}
