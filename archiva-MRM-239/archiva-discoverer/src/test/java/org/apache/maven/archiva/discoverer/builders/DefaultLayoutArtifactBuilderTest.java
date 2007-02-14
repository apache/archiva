package org.apache.maven.archiva.discoverer.builders;

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

import org.apache.maven.archiva.discoverer.DiscovererException;

/**
 * DefaultLayoutArtifactBuilderTest 
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DefaultLayoutArtifactBuilderTest
    extends AbstractLayoutArtifactBuilderTestCase
{
    LayoutArtifactBuilder builder;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        builder = (LayoutArtifactBuilder) lookup( LayoutArtifactBuilder.class.getName(), "default" );
        assertNotNull( builder );
    }

    protected void tearDown()
        throws Exception
    {
        if ( builder != null )
        {
            release( builder );
        }
        super.tearDown();
    }

    public void testPathDistributionArtifacts()
        throws BuilderException, DiscovererException
    {
        assertArtifact( "org.apache.maven", "testing", "1.0", "distribution-tgz", null, builder
            .build( "org/apache/maven/testing/1.0/testing-1.0.tar.gz" ) );

        assertArtifact( "org.apache.maven", "testing", "1.0", "distribution-zip", null, builder
            .build( "org/apache/maven/testing/1.0/testing-1.0.zip" ) );
    }

    public void testPathNormal()
        throws BuilderException, DiscovererException
    {
        assertArtifact( "org.apache.maven.wagon", "wagon", "1.0", "jar", null, builder
            .build( "/org/apache/maven/wagon/wagon/1.0/wagon-1.0.jar" ) );

        assertArtifact( "org.apache.maven.wagon", "wagon", "1.0", "jar", null, builder
            .build( "org/apache/maven/wagon/wagon/1.0/wagon-1.0.jar" ) );

        assertArtifact( "javax.sql", "jdbc", "2.0", "jar", null, builder.build( "javax/sql/jdbc/2.0/jdbc-2.0.jar" ) );

    }

    public void testPathSnapshots()
        throws BuilderException, DiscovererException
    {
        assertArtifact( "org.apache.maven", "test", "1.0-SNAPSHOT", "jar", null, builder
            .build( "org/apache/maven/test/1.0-SNAPSHOT/test-1.0-SNAPSHOT.jar" ) );

        assertArtifact( "org.apache.maven", "test", "1.0-20050611.112233-1", "jar", null, builder
            .build( "org/apache/maven/test/1.0-SNAPSHOT/test-1.0-20050611.112233-1.jar" ) );
    }

    public void testPathSnapshotWithClassifier()
        throws BuilderException, DiscovererException
    {
        assertArtifact( "org.apache.maven", "test", "1.0-20050611.112233-1", "jar", "javadoc", builder
            .build( "org/apache/maven/test/1.0-SNAPSHOT/test-1.0-20050611.112233-1-javadoc.jar" ) );
    }

    public void testPathWithClassifier()
        throws BuilderException, DiscovererException
    {
        assertArtifact( "org.apache.maven", "some-ejb", "1.0", "jar", "client", builder
            .build( "org/apache/maven/some-ejb/1.0/some-ejb-1.0-client.jar" ) );
    }

    public void testPathWithJavaSourceInclusion()
        throws BuilderException, DiscovererException
    {
        assertArtifact( "org.apache.maven", "testing", "1.0", "java-source", "sources", builder
            .build( "org/apache/maven/testing/1.0/testing-1.0-sources.jar" ) );
    }

    public void testProblemMissingType()
        throws DiscovererException
    {
        try
        {
            builder.build( "invalid/invalid/1/invalid-1" );
            fail( "Should have detected missing type." );
        }
        catch ( BuilderException e )
        {
            /* expected path */
            assertEquals( "Path filename does not have an extension.", e.getMessage() );
        }
    }

    public void testProblemNonSnapshotInSnapshotDir()
        throws DiscovererException
    {
        try
        {
            builder.build( "invalid/invalid/1.0-SNAPSHOT/invalid-1.0.jar" );
            fail( "Non Snapshot artifact inside of an Snapshot dir is invalid." );
        }
        catch ( BuilderException e )
        {
            /* expected path */
            assertEquals( "Failed to create a snapshot artifact: invalid:invalid:jar:1.0:runtime", e.getMessage() );
        }
    }

    public void testProblemPathTooShort()
        throws DiscovererException
    {
        try
        {
            builder.build( "invalid/invalid-1.0.jar" );
            fail( "Should have detected that path is too short." );
        }
        catch ( BuilderException e )
        {
            /* expected path */
            assertEquals( "Path is too short to build an artifact from.", e.getMessage() );
        }
    }

    public void testProblemTimestampSnapshotNotInSnapshotDir()
        throws DiscovererException
    {
        try
        {
            builder.build( "invalid/invalid/1.0-20050611.123456-1/invalid-1.0-20050611.123456-1.jar" );
            fail( "Timestamped Snapshot artifact not inside of an Snapshot dir is invalid." );
        }
        catch ( BuilderException e )
        {
            /* expected path */
            // TODO: Is this really the right thing to do for this kind of artifact??
            assertEquals( "Built snapshot artifact base version does not match path version: 1.0-SNAPSHOT; "
                + "should have been version: 1.0-20050611.123456-1", e.getMessage() );
        }
    }

    public void testProblemVersionPathMismatch()
        throws DiscovererException
    {
        try
        {
            builder.build( "invalid/invalid/1.0/invalid-2.0.jar" );
            fail( "Should have detected version mismatch between path and artifact." );
        }
        catch ( BuilderException e )
        {
            /* expected path */
            assertEquals( "Built artifact version does not match path version", e.getMessage() );
        }
    }

    public void testProblemVersionPathMismatchAlt()
        throws DiscovererException
    {
        try
        {
            builder.build( "invalid/invalid/1.0/invalid-1.0b.jar" );
            fail( "Should have version mismatch between directory and artifact." );
        }
        catch ( BuilderException e )
        {
            /* expected path */
            assertEquals( "Path version does not corresspond to an artifact version", e.getMessage() );
        }
    }

    public void testProblemWrongArtifactId()
        throws DiscovererException
    {
        try
        {
            builder.build( "org/apache/maven/test/1.0-SNAPSHOT/wrong-artifactId-1.0-20050611.112233-1.jar" );
            fail( "Should have detected wrong artifact Id." );
        }
        catch ( BuilderException e )
        {
            /* expected path */
            assertEquals( "Path filename does not correspond to an artifact.", e.getMessage() );
        }
    }

}
