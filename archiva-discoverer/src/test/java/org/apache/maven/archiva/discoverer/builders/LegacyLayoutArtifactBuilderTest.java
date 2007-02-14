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
import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * LegacyLayoutArtifactBuilderTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class LegacyLayoutArtifactBuilderTest
    extends AbstractLayoutArtifactBuilderTestCase
{
    LayoutArtifactBuilder builder;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        builder = (LayoutArtifactBuilder) lookup( LayoutArtifactBuilder.class.getName(), "legacy" );
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

    public void testPathNormal()
        throws BuilderException, DiscovererException
    {
        Artifact artifact = builder.build( "javax.sql/jars/jdbc-2.0.jar" );

        assertArtifact( "javax.sql", "jdbc", "2.0", "jar", null, artifact );
    }

    public void testPathFinal()
        throws BuilderException, DiscovererException
    {
        Artifact artifact = builder.build( "org.apache.maven.test/jars/maven-model-1.0-final-20060606.jar" );

        assertArtifact( "org.apache.maven.test", "maven-model", "1.0-final-20060606", "jar", null, artifact );
    }

    public void testPathSnapshot()
        throws BuilderException, DiscovererException
    {
        Artifact artifact = builder.build( "org.apache.maven.test/jars/maven-model-1.0-SNAPSHOT.jar" );

        assertArtifact( "org.apache.maven.test", "maven-model", "1.0-SNAPSHOT", "jar", null, artifact );
    }

    public void testPathJavadoc()
        throws BuilderException, DiscovererException
    {
        Artifact artifact = builder.build( "javax.sql/javadoc.jars/jdbc-2.0-javadoc.jar" );

        assertArtifact( "javax.sql", "jdbc", "2.0", "javadoc.jar", "javadoc", artifact );
    }

    public void testPathSources()
        throws BuilderException, DiscovererException
    {
        Artifact artifact = builder.build( "javax.sql/java-sources/jdbc-2.0-sources.jar" );

        assertArtifact( "javax.sql", "jdbc", "2.0", "java-source", "sources", artifact );
    }

    public void testPathPlugin()
        throws BuilderException, DiscovererException
    {
        Artifact artifact = builder.build( "maven/plugins/maven-test-plugin-1.8.jar" );

        assertArtifact( "maven", "maven-test-plugin", "1.8", "plugin", null, artifact );
    }

    public void testProblemNoType()
    {
        try
        {
            builder.build( "invalid/invalid/1/invalid-1" );

            fail( "Should have detected no type." );
        }
        catch ( DiscovererException e )
        {
            /* expected path */
            assertEquals( "Path does not match a legacy repository path for an artifact", e.getMessage() );
        }
    }

    public void testProblemWrongArtifactPackaging()
        throws ComponentLookupException, DiscovererException
    {
        try
        {
            builder.build( "org.apache.maven.test/jars/artifactId-1.0.jar.md5" );

            fail( "Should have detected wrong package extension." );
        }
        catch ( DiscovererException e )
        {
            /* expected path */
            assertEquals( "Path type does not match the extension", e.getMessage() );
        }
    }

    public void testProblemNoArtifactId()
        throws DiscovererException
    {
        try
        {
            builder.build( "groupId/jars/-1.0.jar" );

            fail( "Should have detected artifactId is missing" );
        }
        catch ( DiscovererException e )
        {
            /* expected path */
            assertEquals( "Path filename artifactId is empty", e.getMessage() );
        }

        try
        {
            builder.build( "groupId/jars/1.0.jar" );

            fail( "Should have detected artifactId is missing" );
        }
        catch ( DiscovererException e )
        {
            /* expected path */
            assertEquals( "Path filename artifactId is empty", e.getMessage() );
        }
    }
}
