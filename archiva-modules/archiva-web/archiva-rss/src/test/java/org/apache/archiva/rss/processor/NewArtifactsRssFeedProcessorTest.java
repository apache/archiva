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

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.rss.RssFeedGenerator;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;
import org.easymock.MockControl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class NewArtifactsRssFeedProcessorTest
    extends PlexusInSpringTestCase
{
    private static final String TEST_REPO = "test-repo";

    private NewArtifactsRssFeedProcessor newArtifactsProcessor;

    private MetadataRepository metadataRepository;

    private MockControl metadataRepositoryControl;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        newArtifactsProcessor = new NewArtifactsRssFeedProcessor();
        newArtifactsProcessor.setGenerator( new RssFeedGenerator() );

        metadataRepositoryControl = MockControl.createControl( MetadataRepository.class );
        metadataRepository = (MetadataRepository) metadataRepositoryControl.getMock();
        newArtifactsProcessor.setMetadataRepository( metadataRepository );
    }

    @SuppressWarnings("unchecked")
    public void testProcess()
        throws Exception
    {
        List<ArtifactMetadata> newArtifacts = new ArrayList<ArtifactMetadata>();
        Date whenGathered = Calendar.getInstance().getTime();

        newArtifacts.add( createArtifact( "artifact-one", "1.0", whenGathered ) );
        newArtifacts.add( createArtifact( "artifact-one", "1.1", whenGathered ) );
        newArtifacts.add( createArtifact( "artifact-one", "2.0", whenGathered ) );
        newArtifacts.add( createArtifact( "artifact-two", "1.0.1", whenGathered ) );
        newArtifacts.add( createArtifact( "artifact-two", "1.0.2", whenGathered ) );
        newArtifacts.add( createArtifact( "artifact-two", "1.0.3-SNAPSHOT", whenGathered ) );
        newArtifacts.add( createArtifact( "artifact-three", "2.0-SNAPSHOT", whenGathered ) );
        newArtifacts.add( createArtifact( "artifact-four", "1.1-beta-2", whenGathered ) );

        Calendar cal = Calendar.getInstance( TimeZone.getTimeZone( "GMT" ) );
        cal.add( Calendar.DATE, -30 );
        cal.clear( Calendar.MILLISECOND );
        metadataRepositoryControl.expectAndReturn(
            metadataRepository.getArtifactsByDateRange( TEST_REPO, cal.getTime(), null ), newArtifacts );
        metadataRepositoryControl.replay();

        Map<String, String> reqParams = new HashMap<String, String>();
        reqParams.put( RssFeedProcessor.KEY_REPO_ID, TEST_REPO );

        SyndFeed feed = newArtifactsProcessor.process( reqParams );

        assertTrue( feed.getTitle().equals( "New Artifacts in Repository 'test-repo'" ) );
        assertTrue(
            feed.getDescription().equals( "New artifacts found in repository 'test-repo' during repository scan." ) );
        assertTrue( feed.getLanguage().equals( "en-us" ) );
        assertTrue( feed.getPublishedDate().equals( whenGathered ) );

        List<SyndEntry> entries = feed.getEntries();
        assertEquals( entries.size(), 1 );
        assertTrue(
            entries.get( 0 ).getTitle().equals( "New Artifacts in Repository 'test-repo' as of " + whenGathered ) );
        assertTrue( entries.get( 0 ).getPublishedDate().equals( whenGathered ) );

        metadataRepositoryControl.verify();
    }

    private ArtifactMetadata createArtifact( String artifactId, String version, Date whenGathered )
    {
        ArtifactMetadata artifact = new ArtifactMetadata();
        artifact.setNamespace( "org.apache.archiva" );
        artifact.setId( artifactId + "-" + version + ".jar" );
        artifact.setRepositoryId( TEST_REPO );
        artifact.setWhenGathered( whenGathered );
        artifact.setProject( artifactId );
        artifact.setProjectVersion( version );
        artifact.setVersion( version );
        return artifact;
    }
}
