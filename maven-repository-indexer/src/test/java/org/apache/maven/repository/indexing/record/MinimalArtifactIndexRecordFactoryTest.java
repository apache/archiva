package org.apache.maven.repository.indexing.record;

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

import java.io.File;

/**
 * Test the minimal artifact index record.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class MinimalArtifactIndexRecordFactoryTest
    extends PlexusTestCase
{
    private RepositoryIndexRecordFactory factory;

    private ArtifactRepository repository;

    private ArtifactFactory artifactFactory;

    private static final String TEST_GROUP_ID = "org.apache.maven.repository.record";

    protected void setUp()
        throws Exception
    {
        super.setUp();

        factory = (RepositoryIndexRecordFactory) lookup( RepositoryIndexRecordFactory.ROLE, "minimal" );

        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );

        ArtifactRepositoryFactory repositoryFactory =
            (ArtifactRepositoryFactory) lookup( ArtifactRepositoryFactory.ROLE );

        ArtifactRepositoryLayout layout = (ArtifactRepositoryLayout) lookup( ArtifactRepositoryLayout.ROLE, "default" );

        File file = getTestFile( "src/test/managed-repository" );
        repository =
            repositoryFactory.createArtifactRepository( "test", file.toURI().toURL().toString(), layout, null, null );
    }

    public void testIndexedJar()
    {
        Artifact artifact = createArtifact( "test-jar" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        MinimalArtifactIndexRecord expectedRecord = new MinimalArtifactIndexRecord();
        expectedRecord.setMd5Checksum( "3a0adc365f849366cd8b633cad155cb7" );
        expectedRecord.setFilename( "test-jar-1.0.jar" );
        expectedRecord.setLastModified( artifact.getFile().lastModified() );
        expectedRecord.setSize( artifact.getFile().length() );
        expectedRecord.setClasses( "A\nB\nC\n" );

        assertEquals( "check record", expectedRecord, record );
    }

    public void testCorruptJar()
    {
        Artifact artifact = createArtifact( "test-corrupt-jar" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        assertNull( "Confirm no record is returned", record );
    }

    public void testNonJar()
    {
        Artifact artifact = createArtifact( "test-dll", "1.0.1.34", "dll" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        assertNull( "Confirm no record is returned", record );
    }

    public void testMissingFile()
    {
        Artifact artifact = createArtifact( "test-foo" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        assertNull( "Confirm no record is returned", record );
    }

    private Artifact createArtifact( String artifactId )
    {
        return createArtifact( artifactId, "1.0", "jar" );
    }

    private Artifact createArtifact( String artifactId, String version, String type )
    {
        Artifact artifact = artifactFactory.createBuildArtifact( TEST_GROUP_ID, artifactId, version, type );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        return artifact;
    }
}
