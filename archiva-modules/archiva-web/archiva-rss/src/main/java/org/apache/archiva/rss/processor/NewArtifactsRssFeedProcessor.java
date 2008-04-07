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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.archiva.rss.RssFeedEntry;
import org.apache.archiva.rss.RssFeedGenerator;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process new artifacts in the repository and generate RSS feeds.
 * 
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version
 * @plexus.component role="org.apache.archiva.rss.processor.RssFeedProcessor" role-hint="new-artifacts"
 */
public class NewArtifactsRssFeedProcessor
    implements RssFeedProcessor
{
    public static final String NEW_ARTIFACTS_IN_REPO = "New Artifacts in Repository ";

    public static final String NEW_VERSIONS_OF_ARTIFACT = "New Versions of Artifact ";

    /**
     * @plexus.requirement
     */
    private RssFeedGenerator generator;

    private Logger log = LoggerFactory.getLogger( NewArtifactsRssFeedProcessor.class );

    /**
     * Process the newly discovered artifacts in the repository. Generate feeds for new artifacts in the repository and
     * new versions of artifact.
     */
    public void process( List<ArchivaArtifact> data )
    {
        log.debug( "Process new artifacts into rss feeds." );
        
        processNewArtifactsInRepo( data );
        processNewVersionsOfArtifact( data );
    }

    private void processNewArtifactsInRepo( List<ArchivaArtifact> data )
    {
        List<RssFeedEntry> entries = new ArrayList<RssFeedEntry>();
        String repoId = getRepoId( data );

        RssFeedEntry entry =
            new RssFeedEntry( NEW_ARTIFACTS_IN_REPO + "\'" + repoId + "\'" + " as of " +
                Calendar.getInstance().getTime(), "http://localhost:8080/archiva/rss/new_artifacts_" + repoId + ".xml" );
        String description = "These are the new artifacts found in repository " + "\'" + repoId + "\'" + ": \n";

        for ( ArchivaArtifact artifact : data )
        {
            description = description + artifact.toString() + " | ";
        }
        entry.setDescription( description );
        entries.add( entry );

        generateFeed( "new_artifacts_" + repoId + ".xml", NEW_ARTIFACTS_IN_REPO + "\'" + repoId + "\'",
                      "http://localhost:8080/archiva/repository/rss/new_artifacts_" + repoId + ".xml",
                      "New artifacts found in repository " + "\'" + repoId + "\'" + " during repository scan.", entries );
    }

    private void processNewVersionsOfArtifact( List<ArchivaArtifact> data )
    {
        String repoId = getRepoId( data );

        List<String> artifacts = new ArrayList<String>();

        for ( ArchivaArtifact artifact : data )
        {
            artifacts.add( artifact.toString() );
        }

        Collections.sort( artifacts );

        Map<String, String> artifactsMap = toMap( artifacts );

        for ( String key : artifactsMap.keySet() )
        {
            List<RssFeedEntry> entries = new ArrayList<RssFeedEntry>();
            RssFeedEntry entry =
                new RssFeedEntry( NEW_VERSIONS_OF_ARTIFACT + "\'" + key + "\'" + " as of " +
                    Calendar.getInstance().getTime(), "http://localhost:8080/archiva/rss/new_versions_" + key + ".xml" );

            String description =
                "These are the new versions of artifact " + "\'" + key + "\'" + " in the repository: \n" +
                    ( (String) artifactsMap.get( key ) );

            entry.setDescription( description );
            entries.add( entry );

            generateFeed( "new_versions_" + key + ".xml", NEW_VERSIONS_OF_ARTIFACT + "\'" + key + "\'",
                          "http://localhost:8080/archiva/rss/new_versions_" + key + ".xml",
                          "New versions of artifact " + "\'" + key + "\' found in repository " + "\'" + repoId + "\'" +
                              " during repository scan.", entries );
        }
    }

    private String getRepoId( List<ArchivaArtifact> data )
    {
        String repoId = "";
        if ( !data.isEmpty() )
        {
            repoId = ( (ArchivaArtifact) data.get( 0 ) ).getModel().getRepositoryId();
        }

        return repoId;
    }

    private void generateFeed( String filename, String title, String link, String description,
                               List<RssFeedEntry> dataEntries )
    {
        generator.generateFeed( title, link, description, dataEntries, filename );
    }

    private Map<String, String> toMap( List<String> artifacts )
    {
        Map<String, String> artifactsMap = new HashMap<String, String>();
        for ( String id : artifacts )
        {
            String key = StringUtils.substringBefore( id, ":" );
            key = key + ":" + StringUtils.substringBefore( StringUtils.substringAfter( id, ":" ), ":" );

            String value = (String) artifactsMap.get( key );
            if ( value != null )
            {
                value = value + " | " + id;
            }
            else
            {
                value = id;
            }
            artifactsMap.put( key, value );
        }

        return artifactsMap;
    }

    public RssFeedGenerator getGenerator()
    {
        return generator;
    }

    public void setGenerator( RssFeedGenerator generator )
    {
        this.generator = generator;
    }

}
