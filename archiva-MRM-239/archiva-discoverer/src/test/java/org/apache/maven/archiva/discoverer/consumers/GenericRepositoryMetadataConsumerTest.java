package org.apache.maven.archiva.discoverer.consumers;

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

import org.apache.maven.archiva.discoverer.DiscovererConsumer;
import org.apache.maven.archiva.discoverer.DiscovererStatistics;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * GenericRepositoryMetadataConsumerTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class GenericRepositoryMetadataConsumerTest
    extends AbstractConsumerTestCase
{
    private MockRepositoryMetadataConsumer getMockRepositoryMetadataConsumer() throws Exception
    {
        return (MockRepositoryMetadataConsumer) lookup(DiscovererConsumer.ROLE, "mock-metadata");
    }
    
    public void testScanLegacy()
        throws Exception
    {
        ArtifactRepository repository = getLegacyRepository();
        List consumers = new ArrayList();

        MockRepositoryMetadataConsumer mockConsumer = getMockRepositoryMetadataConsumer();

        consumers.add( mockConsumer );

        try
        {
            discoverer.scanRepository( repository, consumers, true );
            fail( "Should not have worked on a legacy repository." );
        }
        catch ( IllegalStateException e )
        {
            /* expected path */
        }
    }

    public void testScanDefault()
        throws Exception
    {
        ArtifactRepository repository = getDefaultRepository();
        List consumers = new ArrayList();

        MockRepositoryMetadataConsumer mockConsumer = getMockRepositoryMetadataConsumer();

        consumers.add( mockConsumer );

        DiscovererStatistics stats = discoverer.scanRepository( repository, consumers, true );

        // Test Statistics

        assertNotNull( stats );

        assertEquals( 7, stats.getFilesConsumed() );
        assertEquals( 0, stats.getFilesSkipped() );
        assertEquals( 7, stats.getFilesIncluded() );
        assertTrue( stats.getElapsedMilliseconds() > 0 );
        assertTrue( stats.getTimestampFinished() >= stats.getTimestampStarted() );
        assertTrue( stats.getTimestampStarted() > 0 );

        // Test gathered information from Mock consumer.

        Iterator it;

        it = mockConsumer.getRepositoryMetadataMap().entrySet().iterator();
        while ( it.hasNext() )
        {
            Map.Entry entry = (Entry) it.next();
            String path = (String) entry.getKey();
            RepositoryMetadata repometa = (RepositoryMetadata) entry.getValue();
            System.out.println( "Metadata: " + path + " -> " + repometa );
        }

        assertEquals( 5, mockConsumer.getRepositoryMetadataMap().size() );

        // Test for known include metadata

        // Test for known excluded files and dirs to validate exclusions.

        it = mockConsumer.getRepositoryMetadataMap().keySet().iterator();
        while ( it.hasNext() )
        {
            String path = (String) it.next();
            assertFalse( "Check not CVS", path.indexOf( "CVS" ) >= 0 );
            assertFalse( "Check not .svn", path.indexOf( ".svn" ) >= 0 );
        }
    }
}
