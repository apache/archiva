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

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version
 */
public class NewArtifactsRssFeedProcessorTest
    extends PlexusInSpringTestCase
{
    private NewArtifactsRssFeedProcessor newArtifactsProcessor;

    private ArtifactDAOStub artifactDAOStub;

    private RssFeedGenerator rssFeedGenerator;

    public void setUp()
        throws Exception
    {
        super.setUp();

        newArtifactsProcessor = new NewArtifactsRssFeedProcessor();
        artifactDAOStub = new ArtifactDAOStub();

        rssFeedGenerator = new RssFeedGenerator();

        newArtifactsProcessor.setGenerator( rssFeedGenerator );
        newArtifactsProcessor.setArtifactDAO( artifactDAOStub );
    }

    public void testProcess()
        throws Exception
    {
        List<ArchivaArtifact> newArtifacts = new ArrayList<ArchivaArtifact>();
        Date whenGathered = Calendar.getInstance().getTime();

        ArchivaArtifact artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-one", "1.0", "", "jar" );
        artifact.getModel().setRepositoryId( "test-repo" );
        artifact.getModel().setWhenGathered( whenGathered );
        newArtifacts.add( artifact );

        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-one", "1.1", "", "jar" );
        artifact.getModel().setRepositoryId( "test-repo" );
        artifact.getModel().setWhenGathered( whenGathered );
        newArtifacts.add( artifact );

        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-one", "2.0", "", "jar" );
        artifact.getModel().setRepositoryId( "test-repo" );
        artifact.getModel().setWhenGathered( whenGathered );
        newArtifacts.add( artifact );

        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-two", "1.0.1", "", "jar" );
        artifact.getModel().setRepositoryId( "test-repo" );
        artifact.getModel().setWhenGathered( whenGathered );
        newArtifacts.add( artifact );

        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-two", "1.0.2", "", "jar" );
        artifact.getModel().setRepositoryId( "test-repo" );
        artifact.getModel().setWhenGathered( whenGathered );
        newArtifacts.add( artifact );

        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-two", "1.0.3-SNAPSHOT", "", "jar" );
        artifact.getModel().setRepositoryId( "test-repo" );
        artifact.getModel().setWhenGathered( whenGathered );
        newArtifacts.add( artifact );

        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-three", "2.0-SNAPSHOT", "", "jar" );
        artifact.getModel().setRepositoryId( "test-repo" );
        artifact.getModel().setWhenGathered( whenGathered );
        newArtifacts.add( artifact );

        artifact = new ArchivaArtifact( "org.apache.archiva", "artifact-four", "1.1-beta-2", "", "jar" );
        artifact.getModel().setRepositoryId( "test-repo" );
        artifact.getModel().setWhenGathered( whenGathered );
        newArtifacts.add( artifact );

        artifactDAOStub.setArtifacts( newArtifacts );

        Map<String, String> reqParams = new HashMap<String, String>();
        reqParams.put( RssFeedProcessor.KEY_REPO_ID, "test-repo" );

        SyndFeed feed = newArtifactsProcessor.process( reqParams );

        assertTrue( feed.getTitle().equals( "New Artifacts in Repository 'test-repo'" ) );
        assertTrue( feed.getLink().equals( "http://localhost:8080/archiva/rss/rss_feeds?repoId=test-repo" ) );
        assertTrue( feed.getDescription().equals(
                                                  "New artifacts found in repository 'test-repo' during repository scan." ) );
        assertTrue( feed.getLanguage().equals( "en-us" ) );
        assertTrue( feed.getPublishedDate().equals( whenGathered ) );

        List<SyndEntry> entries = feed.getEntries();
        assertEquals( entries.size(), 1 );
        assertTrue( entries.get( 0 ).getTitle().equals( "New Artifacts in Repository 'test-repo' as of " + whenGathered ) );
        assertTrue( entries.get( 0 ).getPublishedDate().equals( whenGathered ) );
    }
}
