package org.apache.maven.archiva.discovery;

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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author Edwin Punzalan
 */
public abstract class AbstractArtifactDiscovererTest
    extends PlexusTestCase
{
    protected ArtifactDiscoverer discoverer;

    private ArtifactFactory factory;

    protected ArtifactRepository repository;

    protected static final String TEST_OPERATION = "test";

    protected abstract String getLayout();

    protected abstract File getRepositoryFile();

    protected void setUp()
        throws Exception
    {
        super.setUp();

        discoverer = (ArtifactDiscoverer) lookup( ArtifactDiscoverer.ROLE, getLayout() );

        factory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );

        repository = getRepository();

        removeTimestampMetadata();
    }

    protected ArtifactRepository getRepository()
        throws Exception
    {
        File basedir = getRepositoryFile();

        ArtifactRepositoryFactory factory = (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );

        ArtifactRepositoryLayout layout =
            (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, getLayout() );

        return factory.createArtifactRepository( "discoveryRepo", "file://" + basedir, layout, null, null );
    }

    protected Artifact createArtifact( String groupId, String artifactId, String version )
    {
        Artifact artifact = factory.createArtifact( groupId, artifactId, version, null, "jar" );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        artifact.setRepository( repository );
        return artifact;
    }

    protected Artifact createArtifact( String groupId, String artifactId, String version, String type )
    {
        return factory.createArtifact( groupId, artifactId, version, null, type );
    }

    protected Artifact createArtifact( String groupId, String artifactId, String version, String type,
                                       String classifier )
    {
        return factory.createArtifactWithClassifier( groupId, artifactId, version, type, classifier );
    }

    public void testUpdatedInRepository()
        throws ComponentLookupException, DiscovererException, ParseException, IOException
    {
        // Set repository time to 1-1-2000, a time in the distant past so definitely updated
        discoverer.setLastCheckedTime( repository, "update",
                                       new SimpleDateFormat( "yyyy-MM-dd", Locale.US ).parse( "2000-01-01" ) );

        List artifacts = discoverer.discoverArtifacts( repository, "update", null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check included",
                    artifacts.contains( createArtifact( "org.apache.maven.update", "test-updated", "1.0" ) ) );

        // try again with the updated timestamp
        artifacts = discoverer.discoverArtifacts( repository, "update", null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertFalse( "Check not included",
                     artifacts.contains( createArtifact( "org.apache.maven.update", "test-updated", "1.0" ) ) );
    }

    public void testNotUpdatedInRepository()
        throws ComponentLookupException, DiscovererException, IOException
    {
        // Set repository time to now, which is after any artifacts, so definitely not updated
        discoverer.setLastCheckedTime( repository, "update", new Date() );

        List artifacts = discoverer.discoverArtifacts( repository, "update", null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertFalse( "Check not included",
                     artifacts.contains( createArtifact( "org.apache.maven.update", "test-not-updated", "1.0" ) ) );
    }

    public void testNotUpdatedInRepositoryForcedDiscovery()
        throws ComponentLookupException, DiscovererException, IOException
    {
        discoverer.resetLastCheckedTime( repository, "update" );

        List artifacts = discoverer.discoverArtifacts( repository, "update", null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check included",
                    artifacts.contains( createArtifact( "org.apache.maven.update", "test-not-updated", "1.0" ) ) );

        // try again with the updated timestamp
        artifacts = discoverer.discoverArtifacts( repository, "update", null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertFalse( "Check not included",
                     artifacts.contains( createArtifact( "org.apache.maven.update", "test-not-updated", "1.0" ) ) );
    }

    public void testUpdatedInRepositoryBlackout()
        throws ComponentLookupException, DiscovererException, IOException
    {
        discoverer.resetLastCheckedTime( repository, "update" );

        Artifact artifact = createArtifact( "org.apache.maven.update", "test-not-updated", "1.0" );
        artifact.getFile().setLastModified( System.currentTimeMillis() );

        List artifacts = discoverer.discoverArtifacts( repository, "update", null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertFalse( "Check not included", artifacts.contains( artifact ) );

        // try again with the updated timestamp
        artifacts = discoverer.discoverArtifacts( repository, "update", null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertFalse( "Check not included", artifacts.contains( artifact ) );
    }

    public void testUpdatedInRepositoryNotBlackout()
        throws ComponentLookupException, DiscovererException, IOException
    {
        discoverer.resetLastCheckedTime( repository, "update" );

        Artifact artifact = createArtifact( "org.apache.maven.update", "test-not-updated", "1.0" );
        artifact.getFile().setLastModified( System.currentTimeMillis() - 61000 );

        List artifacts = discoverer.discoverArtifacts( repository, "update", null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check included", artifacts.contains( artifact ) );

        // try again with the updated timestamp
        artifacts = discoverer.discoverArtifacts( repository, "update", null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertFalse( "Check not included", artifacts.contains( artifact ) );
    }

    public void testNotUpdatedInRepositoryForcedDiscoveryMetadataAlreadyExists()
        throws ComponentLookupException, DiscovererException, IOException
    {
        discoverer.setLastCheckedTime( repository, "update", new Date() );

        discoverer.resetLastCheckedTime( repository, "update" );

        List artifacts = discoverer.discoverArtifacts( repository, "update", null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check included",
                    artifacts.contains( createArtifact( "org.apache.maven.update", "test-not-updated", "1.0" ) ) );

        // try again with the updated timestamp
        artifacts = discoverer.discoverArtifacts( repository, "update", null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertFalse( "Check not included",
                     artifacts.contains( createArtifact( "org.apache.maven.update", "test-not-updated", "1.0" ) ) );
    }

    public void testNotUpdatedInRepositoryForcedDiscoveryOtherMetadataAlreadyExists()
        throws ComponentLookupException, DiscovererException, IOException
    {
        discoverer.setLastCheckedTime( repository, "test", new Date() );

        discoverer.resetLastCheckedTime( repository, "update" );

        List artifacts = discoverer.discoverArtifacts( repository, "update", null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check included",
                    artifacts.contains( createArtifact( "org.apache.maven.update", "test-not-updated", "1.0" ) ) );

        // try again with the updated timestamp
        artifacts = discoverer.discoverArtifacts( repository, "update", null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertFalse( "Check not included",
                     artifacts.contains( createArtifact( "org.apache.maven.update", "test-not-updated", "1.0" ) ) );
    }

    public void testNoRepositoryMetadata()
        throws ComponentLookupException, DiscovererException, ParseException, IOException
    {
        removeTimestampMetadata();

        // should find all
        List artifacts = discoverer.discoverArtifacts( repository, TEST_OPERATION, null, true );
        assertNotNull( "Check artifacts not null", artifacts );

        assertTrue( "Check included",
                    artifacts.contains( createArtifact( "org.apache.maven.update", "test-updated", "1.0" ) ) );
    }

    private void removeTimestampMetadata()
    {
        // remove the metadata that tracks time
        File file = new File( repository.getBasedir(), "maven-metadata.xml" );
        file.delete();
        assertFalse( file.exists() );
    }
}
