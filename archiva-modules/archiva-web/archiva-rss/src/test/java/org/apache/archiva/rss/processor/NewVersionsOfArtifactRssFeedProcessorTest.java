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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.archiva.rss.RssFeedGenerator;
import org.apache.archiva.rss.stubs.ArtifactDAOStub;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

public class NewVersionsOfArtifactRssFeedProcessorTest
    extends PlexusInSpringTestCase
{
    private NewVersionsOfArtifactRssFeedProcessor newVersionsProcessor;

    private ArtifactDAOStub artifactDAOStub;

    private RssFeedGenerator rssFeedGenerator;

    public void setUp()
        throws Exception
    {
        super.setUp();

        newVersionsProcessor = new NewVersionsOfArtifactRssFeedProcessor();
        artifactDAOStub = new ArtifactDAOStub();

        rssFeedGenerator = new RssFeedGenerator();

        newVersionsProcessor.setGenerator( rssFeedGenerator );
        newVersionsProcessor.setArtifactDAO( artifactDAOStub );
    }

    public void testProcess()
        throws Exception
    {
        List<ArchivaArtifact> artifacts = new ArrayList<ArchivaArtifact>();

        Date whenGathered = Calendar.getInstance().getTime();
        whenGathered.setTime( 123456789 );

        ArchivaArtifact artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-two", "1.0.1", "", "jar" );
        artifact.getModel().setRepositoryId( "test-repo" );
        artifact.getModel().setWhenGathered( whenGathered );
        artifacts.add( artifact );

        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-two", "1.0.2", "", "jar" );
        artifact.getModel().setRepositoryId( "test-repo" );
        artifact.getModel().setWhenGathered( whenGathered );
        artifacts.add( artifact );

        Date whenGatheredNext = Calendar.getInstance().getTime();
        whenGatheredNext.setTime( 345678912 );

        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-two", "1.0.3-SNAPSHOT", "", "jar" );
        artifact.getModel().setRepositoryId( "test-repo" );
        artifact.getModel().setWhenGathered( whenGatheredNext );
        artifacts.add( artifact );

        artifactDAOStub.setArtifacts( artifacts );

        Map<String, String> reqParams = new HashMap<String, String>();
        reqParams.put( RssFeedProcessor.KEY_REPO_ID, "test-repo" );
        reqParams.put( RssFeedProcessor.KEY_GROUP_ID, "org.apache.archiva" );
        reqParams.put( RssFeedProcessor.KEY_ARTIFACT_ID, "artifact-two" );

        SyndFeed feed = newVersionsProcessor.process( reqParams );

        assertEquals( "New Versions of Artifact 'org.apache.archiva:artifact-two'", feed.getTitle() );
        assertEquals( "http://localhost:8080/archiva/rss/rss_feeds?groupId=org.apache.archiva&artifactId=artifact-two",
                      feed.getLink() );
        assertEquals(
                      "New versions of artifact 'org.apache.archiva:artifact-two' found in repository 'test-repo' during repository scan.",
                      feed.getDescription() );
        assertEquals( "en-us", feed.getLanguage() );

        List<SyndEntry> entries = feed.getEntries();

        assertEquals( 2, entries.size() );
        assertEquals( "New Versions of Artifact 'org.apache.archiva:artifact-two' as of " + whenGathered,
                      entries.get( 0 ).getTitle() );
        assertEquals( "New Versions of Artifact 'org.apache.archiva:artifact-two' as of " + whenGatheredNext,
                      entries.get( 1 ).getTitle() );
    }

}
