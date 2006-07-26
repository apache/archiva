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
 * @todo test packaging!
 * @todo test pluginPrefix!
 */
public class StandardArtifactIndexRecordFactoryTest
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

        factory = (RepositoryIndexRecordFactory) lookup( RepositoryIndexRecordFactory.ROLE, "standard" );

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

        StandardArtifactIndexRecord expectedRecord = new StandardArtifactIndexRecord();
        expectedRecord.setMd5Checksum( "3a0adc365f849366cd8b633cad155cb7" );
        expectedRecord.setFilename( "test-jar-1.0.jar" );
        expectedRecord.setLastModified( artifact.getFile().lastModified() );
        expectedRecord.setSize( artifact.getFile().length() );
        expectedRecord.setClasses( "A\nB\nC\n" );
        expectedRecord.setArtifactId( "test-jar" );
        expectedRecord.setGroupId( TEST_GROUP_ID );
        expectedRecord.setVersion( "1.0" );
        expectedRecord.setFiles( "META-INF/MANIFEST.MF\nA.class\nb/B.class\nb/c/C.class\n" );
        expectedRecord.setPackages( "b\nb.c\n" );
        expectedRecord.setSha1Checksum( "c66f18bf192cb613fc2febb4da541a34133eedc2" );
        expectedRecord.setType( "jar" );
        expectedRecord.setRepository( "test" );

        assertEquals( "check record", expectedRecord, record );
    }

    public void testCorruptJar()
    {
        Artifact artifact = createArtifact( "test-corrupt-jar" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        assertNull( "Confirm no record is returned", record );
    }

    public void testDll()
    {
        Artifact artifact = createArtifact( "test-dll", "1.0.1.34", "dll" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        StandardArtifactIndexRecord expectedRecord = new StandardArtifactIndexRecord();
        expectedRecord.setMd5Checksum( "d41d8cd98f00b204e9800998ecf8427e" );
        expectedRecord.setFilename( "test-dll-1.0.1.34.dll" );
        expectedRecord.setLastModified( artifact.getFile().lastModified() );
        expectedRecord.setSize( artifact.getFile().length() );
        expectedRecord.setArtifactId( "test-dll" );
        expectedRecord.setGroupId( TEST_GROUP_ID );
        expectedRecord.setVersion( "1.0.1.34" );
        expectedRecord.setSha1Checksum( "da39a3ee5e6b4b0d3255bfef95601890afd80709" );
        expectedRecord.setType( "dll" );
        expectedRecord.setRepository( "test" );
        expectedRecord.setClasses( "" );
        expectedRecord.setPackages( "" );
        expectedRecord.setFiles( "" );

        assertEquals( "check record", expectedRecord, record );
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
        artifact.setRepository( repository );
        return artifact;
    }
}
