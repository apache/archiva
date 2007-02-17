package org.apache.maven.archiva.discoverer;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.common.utils.BaseFile;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * DefaultDiscovererTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DefaultDiscovererTest
    extends AbstractDiscovererTestCase
{
    private MockConsumer createAndAddMockConsumer( List consumers, String includePattern, String excludePattern )
    {
        MockConsumer mockConsumer = new MockConsumer();
        mockConsumer.getIncludePatterns().add( includePattern );
        if ( StringUtils.isNotBlank( excludePattern ) )
        {
            mockConsumer.getExcludePatterns().add( excludePattern );
        }
        consumers.add( mockConsumer );
        return mockConsumer;
    }
    
    private void assertFilesProcessed( int expectedFileCount, DiscovererStatistics stats, MockConsumer mockConsumer )
    {
        assertNotNull( "Stats should not be null.", stats );
        assertNotNull( "MockConsumer should not be null.", mockConsumer );
        assertNotNull( "MockConsumer.filesProcessed should not be null.", mockConsumer.getFilesProcessed() );

        if ( stats.getFilesConsumed() != mockConsumer.getFilesProcessed().size() )
        {
            fail( "Somehow, the stats count of files consumed, and the count of actual files "
                + "processed by the consumer do not match." );
        }

        int actualFileCount = mockConsumer.getFilesProcessed().size();

        if ( expectedFileCount != actualFileCount )
        {
            stats.dump( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
            System.out.println( "Base Dir:" + stats.getRepository().getBasedir() );
            Iterator it = mockConsumer.getFilesProcessed().iterator();
            while ( it.hasNext() )
            {
                BaseFile file = (BaseFile) it.next();
                System.out.println( "  Processed File: " + file.getRelativePath() );
            }

            fail( "Files Processed mismatch: expected:<" + expectedFileCount + ">, actual:<" + actualFileCount + ">" );
        }
    }

    public void testLegacyLayoutRepositoryAll()
        throws Exception
    {
        ArtifactRepository repository = getLegacyRepository();
        List consumers = new ArrayList();
        MockConsumer mockConsumer = createAndAddMockConsumer( consumers, "**/*", null );

        DiscovererStatistics stats = discoverer.walkRepository( repository, consumers, true );

        assertNotNull( stats );

        assertFilesProcessed( 16, stats, mockConsumer );
    }

    public void testDefaultLayoutRepositoryAll()
        throws Exception
    {
        ArtifactRepository repository = getDefaultRepository();
        List consumers = new ArrayList();
        MockConsumer mockConsumer = createAndAddMockConsumer( consumers, "**/*", null );

        DiscovererStatistics stats = discoverer.walkRepository( repository, consumers, true );

        assertNotNull( stats );
        
        assertFilesProcessed( 43, stats, mockConsumer );
    }

    public void testDefaultLayoutRepositoryPomsOnly()
        throws Exception
    {
        ArtifactRepository repository = getDefaultRepository();
        List consumers = new ArrayList();
        MockConsumer mockConsumer = createAndAddMockConsumer( consumers, "**/*.pom", null );

        DiscovererStatistics stats = discoverer.walkRepository( repository, consumers, true );

        assertNotNull( stats );

        assertFilesProcessed( 10, stats, mockConsumer );
    }

    public void testDefaultLayoutRepositoryJarsOnly()
        throws Exception
    {
        ArtifactRepository repository = getDefaultRepository();
        List consumers = new ArrayList();
        MockConsumer mockConsumer = createAndAddMockConsumer( consumers, "**/*.jar", null );

        DiscovererStatistics stats = discoverer.walkRepository( repository, consumers, true );

        assertNotNull( stats );

        assertFilesProcessed( 17, stats, mockConsumer );
    }

    public void testDefaultLayoutRepositoryJarsNoSnapshots()
        throws Exception
    {
        ArtifactRepository repository = getDefaultRepository();
        List consumers = new ArrayList();
        MockConsumer mockConsumer = createAndAddMockConsumer( consumers, "**/*.jar", null );

        DiscovererStatistics stats = discoverer.walkRepository( repository, consumers, false );

        assertNotNull( stats );

        assertFilesProcessed( 13, stats, mockConsumer );
    }

    public void testDefaultLayoutRepositoryJarsNoSnapshotsWithExclusions()
        throws Exception
    {
        ArtifactRepository repository = getDefaultRepository();
        List consumers = new ArrayList();
        MockConsumer mockConsumer = createAndAddMockConsumer( consumers, "**/*.jar", null );

        List exclusions = new ArrayList();
        exclusions.add( "**/*-client.jar" );
        DiscovererStatistics stats = discoverer.walkRepository( repository, consumers, false, 0, exclusions, null );

        assertNotNull( stats );

        assertFilesProcessed( 12, stats, mockConsumer );
    }
}
