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
import junit.framework.TestCase;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.AbstractMetadataRepository;
import org.apache.archiva.rss.RssFeedGenerator;
import org.apache.archiva.test.utils.ArchivaBlockJUnit4ClassRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.*;

@RunWith (ArchivaBlockJUnit4ClassRunner.class)
public class NewArtifactsRssFeedProcessorTest
    extends TestCase
{
    private static final String TEST_REPO = "test-repo";

    private NewArtifactsRssFeedProcessor newArtifactsProcessor;

    private MetadataRepositoryMock metadataRepository;

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        newArtifactsProcessor = new NewArtifactsRssFeedProcessor();
        newArtifactsProcessor.setGenerator( new RssFeedGenerator() );

        metadataRepository = new MetadataRepositoryMock();
    }

    @SuppressWarnings ("unchecked")
    @Test
    public void testProcess()
        throws Exception
    {
        List<ArtifactMetadata> newArtifacts = new ArrayList<>();
        Date whenGathered = Calendar.getInstance().getTime();

        newArtifacts.add( createArtifact( "artifact-one", "1.0", whenGathered ) );
        newArtifacts.add( createArtifact( "artifact-one", "1.1", whenGathered ) );
        newArtifacts.add( createArtifact( "artifact-one", "2.0", whenGathered ) );
        newArtifacts.add( createArtifact( "artifact-two", "1.0.1", whenGathered ) );
        newArtifacts.add( createArtifact( "artifact-two", "1.0.2", whenGathered ) );
        newArtifacts.add( createArtifact( "artifact-two", "1.0.3-SNAPSHOT", whenGathered ) );
        newArtifacts.add( createArtifact( "artifact-three", "2.0-SNAPSHOT", whenGathered ) );
        newArtifacts.add( createArtifact( "artifact-four", "1.1-beta-2", whenGathered ) );

        metadataRepository.setArtifactsByDateRange( newArtifacts );

        Map<String, String> reqParams = new HashMap<>();
        reqParams.put( RssFeedProcessor.KEY_REPO_ID, TEST_REPO );

        SyndFeed feed = newArtifactsProcessor.process( reqParams, metadataRepository );

        // check that the date used in the call is close to the one passed (5 seconds difference at most)
        Calendar cal = Calendar.getInstance( TimeZone.getTimeZone( "GMT" ) );
        cal.add( Calendar.DATE, -30 );
        assertTrue( ( metadataRepository.getFrom().getTime() - cal.getTimeInMillis() ) < 1000 * 5 );
        assertEquals( null, metadataRepository.getTo() );
        assertEquals( TEST_REPO, metadataRepository.getRepoId() );

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

    // TODO: replace with mockito
    private class MetadataRepositoryMock
        extends AbstractMetadataRepository
    {
        private Date from, to;

        private String repoId;

        private List<ArtifactMetadata> artifactsByDateRange;

        @Override
        public List<ArtifactMetadata> getArtifactsByDateRange( String repoId, Date from, Date to )
        {
            setRepoId( repoId );
            setFrom( from );
            setTo( to );
            return artifactsByDateRange;
        }


        public void setFrom( Date from )
        {
            this.from = from;
        }

        public Date getFrom()
        {
            return from;
        }

        public void setTo( Date to )
        {
            this.to = to;
        }

        public Date getTo()
        {
            return to;
        }

        public void setRepoId( String repoId )
        {
            this.repoId = repoId;
        }

        public String getRepoId()
        {
            return repoId;
        }

        public void setArtifactsByDateRange( List<ArtifactMetadata> artifactsByDateRange )
        {
            this.artifactsByDateRange = artifactsByDateRange;
        }

        @Override
        public List<ArtifactMetadata> getArtifacts( String repositoryId )
        {
            return artifactsByDateRange;
        }
    }
}
