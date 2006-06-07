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
public class ArtifactUtilsLegacyTest
    extends PlexusTestCase
{
    private ArtifactFactory factory;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        factory = (ArtifactFactory) container.lookup( ArtifactFactory.ROLE );
    }

    protected void tearDown()
        throws Exception
    {
        container.release( factory );

        super.tearDown();
    }

    public void testWrongArtifactPackaging()
        throws ComponentLookupException
    {
        String testPath = "org.apache.maven.test/jars/artifactId-1.0.jar.md5";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNull( "Artifact should be null for wrong package extension", artifact );
    }

    public void testNoArtifactid()
    {
        String testPath = "groupId/jars/-1.0.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNull( "Artifact should be null when artifactId is missing", artifact );

        testPath = "groupId/jars/1.0.jar";

        artifact = getArtifactFromPath( testPath );

        assertNull( "Artifact should be null when artifactId is missing", artifact );
    }

    public void testNoType()
        throws ComponentLookupException
    {
        String testPath = "invalid/invalid/1/invalid-1";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNull( "Artifact should be null for no type", artifact );
    }

    public void testSnapshot()
        throws ComponentLookupException
    {
        String testPath = "org.apache.maven.test/jars/maven-model-1.0-SNAPSHOT.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNotNull( "Artifact path with invalid snapshot error", artifact );

        assertEquals( createArtifact( "org.apache.maven.test", "maven-model", "1.0-SNAPSHOT" ), artifact );
    }

    public void testFinal()
        throws ComponentLookupException
    {
        String testPath = "org.apache.maven.test/jars/maven-model-1.0-final-20060606.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNotNull( "Artifact path with invalid snapshot error", artifact );

        assertEquals( createArtifact( "org.apache.maven.test", "maven-model", "1.0-final-20060606" ), artifact );
    }

    public void testNormal()
        throws ComponentLookupException
    {
        String testPath = "javax.sql/jars/jdbc-2.0.jar";

        Artifact artifact = getArtifactFromPath( testPath );

        assertNotNull( "Normal artifact path error", artifact );

        assertEquals( createArtifact( "javax.sql", "jdbc", "2.0" ), artifact );
    }

    private Artifact getArtifactFromPath( String path )
    {
        return ArtifactUtils.buildArtifactFromLegacyPath( path, factory );
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
