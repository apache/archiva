package org.apache.maven.repository;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * @author Edwin Punzalan
 */
public class ArtifactUtilsTest
    extends PlexusTestCase
{
    private ArtifactFactory factory;

    protected void tearDown()
        throws Exception
    {
        container.release( factory );

        super.tearDown();
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        factory = (ArtifactFactory) container.lookup( ArtifactFactory.ROLE );
    }

    public void testShortPath()
        throws ComponentLookupException
    {
        String testPath = "invalid/invalid-1.0.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNull( "Artifact should be null for short paths", artifact );
    }

    public void testWrongArtifactId()
        throws ComponentLookupException
    {
        String testPath = "org/apache/maven/test/1.0-SNAPSHOT/wrong-artifactId-1.0-20050611.112233-1.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNull( "Artifact should be null for wrong ArtifactId", artifact );
    }

    public void testNoType()
        throws ComponentLookupException
    {
        String testPath = "invalid/invalid/1/invalid-1";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNull( "Artifact should be null for no type", artifact );
    }

    public void testWrongVersion()
        throws ComponentLookupException
    {
        String testPath = "invalid/invalid/1.0/invalid-2.0.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNull( "Artifact should be null for wrong version", artifact );
    }

    public void testLongVersion()
        throws ComponentLookupException
    {
        String testPath = "invalid/invalid/1.0/invalid-1.0b.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNull( "Artifact should be null for long version", artifact );
    }

    public void testWrongSnapshotVersion()
        throws ComponentLookupException
    {
        String testPath = "invalid/invalid/1.0-SNAPSHOT/invalid-1.0.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNull( "Artifact should be null for wrong snapshot version", artifact );
    }

    public void testSnapshotBaseVersion()
        throws ComponentLookupException
    {
        String testPath = "invalid/invalid/1.0-20050611.123456-1/invalid-1.0-20050611.123456-1.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNull( "Artifact should be null for snapshot base version", artifact );
    }

    public void testPathWithClassifier()
        throws ComponentLookupException
    {
        String testPath = "org/apache/maven/some-ejb/1.0/some-ejb-1.0-client.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNotNull( "Artifact path with classifier error", artifact );

        assertEquals( createArtifact( "org.apache.maven", "some-ejb", "1.0", "jar", "client" ), artifact );
    }

    public void testWithJavaSourceInclusion()
        throws ComponentLookupException
    {
        String testPath = "org/apache/maven/testing/1.0/testing-1.0-sources.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNotNull( "Artifact path with java source inclusion error", artifact );

        assertEquals( createArtifact( "org.apache.maven", "testing", "1.0", "java-source", "sources" ), artifact );
    }

    public void testDistributionArtifacts()
        throws ComponentLookupException
    {
        String testPath = "org/apache/maven/testing/1.0/testing-1.0.tar.gz";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNotNull( "tar.gz distribution artifact error", artifact );

        assertEquals( createArtifact( "org.apache.maven", "testing", "1.0", "distribution-tgz" ), artifact );

        testPath = "org/apache/maven/testing/1.0/testing-1.0.zip";

        artifact = getArtifactFromPath( testPath );

        assertNotNull( "zip distribution artifact error", artifact );

        assertEquals( createArtifact( "org.apache.maven", "testing", "1.0", "distribution-zip" ), artifact );
    }

    public void testSnapshot()
        throws ComponentLookupException
    {
        String testPath = "org/apache/maven/test/1.0-SNAPSHOT/test-1.0-SNAPSHOT.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNotNull( "Artifact path with invalid snapshot error", artifact );

        assertEquals( createArtifact( "org.apache.maven", "test", "1.0-SNAPSHOT" ), artifact );

        testPath = "org/apache/maven/test/1.0-SNAPSHOT/test-1.0-20050611.112233-1.jar";

        artifact = getArtifactFromPath( testPath );

        assertNotNull( "Artifact path with snapshot error", artifact );

        assertEquals( createArtifact( "org.apache.maven", "test", "1.0-20050611.112233-1" ), artifact );
    }

    public void testNormal()
        throws ComponentLookupException
    {
        String testPath = "javax/sql/jdbc/2.0/jdbc-2.0.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNotNull( "Normal artifact path error", artifact );

        assertEquals( createArtifact( "javax.sql", "jdbc", "2.0" ), artifact );
    }

    public void testSnapshotWithClassifier()
        throws ComponentLookupException
    {
        String testPath = "org/apache/maven/test/1.0-SNAPSHOT/test-1.0-20050611.112233-1-javadoc.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNotNull( "Artifact path with snapshot and classifier error", artifact );

        assertEquals( createArtifact( "org.apache.maven", "test", "1.0-20050611.112233-1", "jar", "javadoc" ),
                      artifact );
    }

    private Artifact getArtifactFromPath( String path )
    {
        return ArtifactUtils.buildArtifact( path, factory );
    }

    private Artifact createArtifact( String groupId, String artifactId, String version )
    {
        return factory.createArtifact( groupId, artifactId, version, null, "jar" );
    }

    private Artifact createArtifact( String groupId, String artifactId, String version, String type )
    {
        return factory.createArtifact( groupId, artifactId, version, null, type );
    }

    private Artifact createArtifact( String groupId, String artifactId, String version, String type, String classifier )
    {
        return factory.createArtifactWithClassifier( groupId, artifactId, version, type, classifier );
    }
}
