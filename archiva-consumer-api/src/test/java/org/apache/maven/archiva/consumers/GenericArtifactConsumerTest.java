package org.apache.maven.archiva.consumers;

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
import org.apache.maven.archiva.repository.ArchivaRepository;
import org.apache.maven.archiva.repository.consumer.ConsumerException;
import org.apache.maven.artifact.Artifact;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * GenericArtifactConsumerTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class GenericArtifactConsumerTest
    extends AbstractGenericConsumerTestCase
{
    private MockArtifactConsumer getMockArtifactConsumer()
        throws Exception
    {
        return (MockArtifactConsumer) consumerFactory.createConsumer( "mock-artifact" );
    }

    public void testScanLegacy()
        throws Exception
    {
        ArchivaRepository repository = getLegacyRepository();
        List consumers = new ArrayList();

        MockArtifactConsumer mockConsumer = getMockArtifactConsumer();
        mockConsumer.init( repository );

        consumers.add( mockConsumer );

        List files = getLegacyLayoutArtifactPaths();
        for ( Iterator it = files.iterator(); it.hasNext(); )
        {
            String path = (String) it.next();
            try
            {
                mockConsumer.processFile( new BaseFile( repository.getRepositoryURL().getPath(), path ) );
            }
            catch ( ConsumerException e )
            {
                mockConsumer.getProblemsTracker().addProblem( e );
            }
        }

        assertNotNull( consumers );

        FileProblemsTracker tracker = mockConsumer.getProblemsTracker();

        assertTracker( tracker, 16 );

        assertHasFailureMessage( "Path does not match a legacy repository path for an artifact",
                                 "invalid/invalid-1.0.jar", tracker );
        assertHasFailureMessage( "Path filename version is empty", "invalid/jars/invalid.jar", tracker );
        assertHasFailureMessage( "Path does not match a legacy repository path for an artifact",
                                 "invalid/jars/1.0/invalid-1.0.jar", tracker );

        assertEquals( 10, mockConsumer.getArtifactMap().size() );
    }

    public void testScanDefault()
        throws Exception
    {
        ArchivaRepository repository = getDefaultRepository();
        List consumers = new ArrayList();

        MockArtifactConsumer mockConsumer = getMockArtifactConsumer();
        mockConsumer.init( repository );

        consumers.add( mockConsumer );

        List files = getDefaultLayoutArtifactPaths();
        for ( Iterator it = files.iterator(); it.hasNext(); )
        {
            String path = (String) it.next();
            try
            {
                mockConsumer.processFile( new BaseFile( repository.getRepositoryURL().getPath(), path ) );
            }
            catch ( ConsumerException e )
            {
                mockConsumer.getProblemsTracker().addProblem( e );
            }
        }

        // Test gathered information from Mock consumer.

        assertNotNull( consumers );

        FileProblemsTracker tracker = mockConsumer.getProblemsTracker();

        assertTracker( tracker, 21 );

        assertHasFailureMessage( "Failed to create a snapshot artifact: invalid:invalid:jar:1.0:runtime",
                                 "invalid/invalid/1.0-SNAPSHOT/invalid-1.0.jar", tracker );
        assertHasFailureMessage( "Path is too short to build an artifact from.", "invalid/invalid-1.0.jar", tracker );
        assertHasFailureMessage( "Built artifact version does not match path version",
                                 "invalid/invalid/1.0/invalid-2.0.jar", tracker );

        assertEquals( 25, mockConsumer.getArtifactMap().size() );

        // Test for known include artifacts

        Collection artifacts = mockConsumer.getArtifactMap().values();
        assertHasArtifact( "org.apache.maven", "testing", "1.0", "jar", null, artifacts );
        assertHasArtifact( "org.apache.maven", "some-ejb", "1.0", "jar", "client", artifacts );
        assertHasArtifact( "org.apache.maven", "testing", "1.0", "java-source", "sources", artifacts );
        assertHasArtifact( "org.apache.maven", "testing", "1.0", "java-source", "test-sources", artifacts );
        assertHasArtifact( "org.apache.maven", "testing", "1.0", "distribution-zip", null, artifacts );
        assertHasArtifact( "org.apache.maven", "testing", "1.0", "distribution-tgz", null, artifacts );
        assertHasArtifact( "javax.sql", "jdbc", "2.0", "jar", null, artifacts );
        assertHasArtifact( "org.apache.maven", "test", "1.0-20050611.112233-1", "jar", null, artifacts );
        assertHasArtifact( "org.apache.maven", "test", "1.0-20050611.112233-1", "jar", "javadoc", artifacts );

        // Test for known excluded files and dirs to validate exclusions.

        Iterator it = mockConsumer.getArtifactMap().values().iterator();
        while ( it.hasNext() )
        {
            Artifact a = (Artifact) it.next();
            assertTrue( "Artifact " + a + " should have it's .getFile() set.", a.getFile() != null );
            assertTrue( "Artifact " + a + " should have it's .getRepository() set.", a.getRepository() != null );
            assertTrue( "Artifact " + a + " should have non-null repository url.", a.getRepository().getUrl() != null );
            assertFalse( "Check not CVS", a.getFile().getPath().indexOf( "CVS" ) >= 0 );
            assertFalse( "Check not .svn", a.getFile().getPath().indexOf( ".svn" ) >= 0 );
        }
    }

    private void dumpProblems( FileProblemsTracker tracker )
    {
        int problemNum = 0;
        System.out.println( "-- ProblemTracker dump -------------------------" );
        for ( Iterator itPaths = tracker.getPaths().iterator(); itPaths.hasNext(); )
        {
            String path = (String) itPaths.next();
            System.out.println( " [" + problemNum + "]: " + path );

            int messageNum = 0;
            for ( Iterator itProblems = tracker.getProblems( path ).iterator(); itProblems.hasNext(); )
            {
                String message = (String) itProblems.next();
                System.out.println( "    [" + messageNum + "]: " + message );
                messageNum++;
            }

            problemNum++;
        }
    }

    private void assertTracker( FileProblemsTracker tracker, int expectedProblemCount )
    {
        assertNotNull( "ProblemsTracker should not be null.", tracker );

        int actualProblemCount = tracker.getProblemCount();
        if ( expectedProblemCount != actualProblemCount )
        {
            dumpProblems( tracker );
            fail( "Problem count (across all paths) expected:<" + expectedProblemCount + ">, actual:<"
                + actualProblemCount + ">" );
        }
    }

    private void assertHasFailureMessage( String message, String path, FileProblemsTracker tracker )
    {
        if ( !tracker.hasProblems( path ) )
        {
            fail( "There are no messages for expected path [" + path + "]" );
        }

        assertTrue( "Unable to find message [" + message + "] in path [" + path + "]", tracker.getProblems( path )
            .contains( message ) );
    }

    private void assertHasArtifact( String groupId, String artifactId, String version, String type, String classifier,
                                    Collection collection )
    {
        for ( Iterator it = collection.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            if ( StringUtils.equals( groupId, artifact.getGroupId() )
                && StringUtils.equals( artifactId, artifact.getArtifactId() )
                && StringUtils.equals( version, artifact.getVersion() )
                && StringUtils.equals( type, artifact.getType() )
                && StringUtils.equals( classifier, artifact.getClassifier() ) )
            {
                // Found it!
                return;
            }
        }

        fail( "Was unable to find artifact " + groupId + ":" + artifactId + ":" + version + ":" + type + ":"
            + classifier );
    }
}
