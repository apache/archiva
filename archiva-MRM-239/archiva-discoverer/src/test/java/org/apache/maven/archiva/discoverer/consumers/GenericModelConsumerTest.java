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
import org.apache.maven.model.Model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * GenericModelConsumerTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class GenericModelConsumerTest
    extends AbstractConsumerTestCase
{
    private MockModelConsumer getMockModelConsumer() throws Exception
    {
        return (MockModelConsumer) lookup(DiscovererConsumer.ROLE, "mock-model");
    }
    
    public void testScanLegacy()
        throws Exception
    {
        ArtifactRepository repository = getLegacyRepository();
        List consumers = new ArrayList();

        MockModelConsumer mockConsumer = getMockModelConsumer();

        consumers.add( mockConsumer );

        DiscovererStatistics stats = discoverer.scanRepository( repository, consumers, true );

        assertNotNull( stats );

        assertNotNull( consumers );

        Iterator it = mockConsumer.getModelMap().entrySet().iterator();
        while ( it.hasNext() )
        {
            Map.Entry entry = (Entry) it.next();
            String path = (String) entry.getKey();
            Model model = (Model) entry.getValue();
            System.out.println( "Model: " + path + " -> " + model );
        }

        // TODO: Add some poms to legacy repository!
        assertEquals( 0, mockConsumer.getModelMap().size() );
    }

    public void testScanDefault()
        throws Exception
    {
        ArtifactRepository repository = getDefaultRepository();
        List consumers = new ArrayList();

        MockModelConsumer mockConsumer = getMockModelConsumer();

        consumers.add( mockConsumer );

        DiscovererStatistics stats = discoverer.scanRepository( repository, consumers, true );

        // Test Statistics

        assertNotNull( stats );

        assertEquals( 10, stats.getFilesConsumed() );
        assertEquals( 0, stats.getFilesSkipped() );
        assertEquals( 10, stats.getFilesIncluded() );
        assertTrue( stats.getElapsedMilliseconds() > 0 );
        assertTrue( stats.getTimestampFinished() >= stats.getTimestampStarted() );
        assertTrue( stats.getTimestampStarted() > 0 );

        // Test gathered information from Mock consumer.

        Iterator it;

        it = mockConsumer.getModelMap().entrySet().iterator();
        while ( it.hasNext() )
        {
            Map.Entry entry = (Entry) it.next();
            String path = (String) entry.getKey();
            Model model = (Model) entry.getValue();
            System.out.println( "Model: " + path + " -> " + model );
        }

        assertEquals( 10, mockConsumer.getModelMap().size() );

        // Test for known include metadata

        // Test for known excluded files and dirs to validate exclusions.

        it = mockConsumer.getModelMap().keySet().iterator();
        while ( it.hasNext() )
        {
            String path = (String) it.next();
            assertFalse( "Check not CVS", path.indexOf( "CVS" ) >= 0 );
            assertFalse( "Check not .svn", path.indexOf( ".svn" ) >= 0 );
        }
    }
}
