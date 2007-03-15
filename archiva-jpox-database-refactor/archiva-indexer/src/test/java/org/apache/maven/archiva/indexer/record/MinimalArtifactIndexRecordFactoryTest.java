package org.apache.maven.archiva.indexer.record;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.versioning.VersionRange;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    private static final String TEST_GROUP_ID = "org.apache.maven.archiva.record";

    private static final List JAR_CLASS_LIST = Arrays.asList( new String[]{"A", "b.B", "b.c.C"} );

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
        throws RepositoryIndexException
    {
        Artifact artifact = createArtifact( "test-jar" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        MinimalArtifactIndexRecord expectedRecord = new MinimalArtifactIndexRecord();
        expectedRecord.setMd5Checksum( "3a0adc365f849366cd8b633cad155cb7" );
        expectedRecord.setFilename( repository.pathOf( artifact ) );
        expectedRecord.setLastModified( artifact.getFile().lastModified() );
        expectedRecord.setSize( artifact.getFile().length() );
        expectedRecord.setClasses( JAR_CLASS_LIST );

        assertEquals( "check record", expectedRecord, record );
    }

    public void testIndexedJarWithClassifier()
        throws RepositoryIndexException
    {
        Artifact artifact = createArtifact( "test-jar", "1.0", "jar", "jdk14" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        MinimalArtifactIndexRecord expectedRecord = new MinimalArtifactIndexRecord();
        expectedRecord.setMd5Checksum( "3a0adc365f849366cd8b633cad155cb7" );
        expectedRecord.setFilename( repository.pathOf( artifact ) );
        expectedRecord.setLastModified( artifact.getFile().lastModified() );
        expectedRecord.setSize( artifact.getFile().length() );
        expectedRecord.setClasses( JAR_CLASS_LIST );

        assertEquals( "check record", expectedRecord, record );
    }

    public void testIndexedJarAndPom()
        throws RepositoryIndexException
    {
        Artifact artifact = createArtifact( "test-jar-and-pom", "1.0-alpha-1", "jar" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        MinimalArtifactIndexRecord expectedRecord = new MinimalArtifactIndexRecord();
        expectedRecord.setMd5Checksum( "3a0adc365f849366cd8b633cad155cb7" );
        expectedRecord.setFilename( repository.pathOf( artifact ) );
        expectedRecord.setLastModified( artifact.getFile().lastModified() );
        expectedRecord.setSize( artifact.getFile().length() );
        expectedRecord.setClasses( JAR_CLASS_LIST );

        assertEquals( "check record", expectedRecord, record );
    }

    public void testIndexedJarAndPomWithClassifier()
        throws RepositoryIndexException
    {
        Artifact artifact = createArtifact( "test-jar-and-pom", "1.0-alpha-1", "jar", "jdk14" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        MinimalArtifactIndexRecord expectedRecord = new MinimalArtifactIndexRecord();
        expectedRecord.setMd5Checksum( "3a0adc365f849366cd8b633cad155cb7" );
        expectedRecord.setFilename( repository.pathOf( artifact ) );
        expectedRecord.setLastModified( artifact.getFile().lastModified() );
        expectedRecord.setSize( artifact.getFile().length() );
        expectedRecord.setClasses( JAR_CLASS_LIST );

        assertEquals( "check record", expectedRecord, record );
    }

    public void testIndexedPom()
        throws RepositoryIndexException
    {
        Artifact artifact = createArtifact( "test-pom", "1.0", "pom" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        assertNull( "Check no record", record );
    }

    public void testNonIndexedPom()
        throws RepositoryIndexException
    {
        // If we pass in only the POM that belongs to a JAR, then expect null not the POM
        Artifact artifact = createArtifact( "test-jar-and-pom", "1.0-alpha-1", "pom" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        assertNull( "Check no record", record );

        artifact = createArtifact( "test-plugin", "1.0", "pom" );

        record = factory.createRecord( artifact );

        assertNull( "Check no record", record );

        artifact = createArtifact( "test-archetype", "1.0", "pom" );

        record = factory.createRecord( artifact );

        assertNull( "Check no record", record );

        artifact = createArtifact( "test-skin", "1.0", "pom" );

        record = factory.createRecord( artifact );

        assertNull( "Check no record", record );
    }

    public void testIndexedPlugin()
        throws RepositoryIndexException, IOException, XmlPullParserException
    {
        Artifact artifact = createArtifact( "test-plugin" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        MinimalArtifactIndexRecord expectedRecord = new MinimalArtifactIndexRecord();
        expectedRecord.setMd5Checksum( "3530896791670ebb45e17708e5d52c40" );
        expectedRecord.setFilename( repository.pathOf( artifact ) );
        expectedRecord.setLastModified( artifact.getFile().lastModified() );
        expectedRecord.setSize( artifact.getFile().length() );
        expectedRecord.setClasses( Collections.singletonList( "org.apache.maven.archiva.record.MyMojo" ) );

        assertEquals( "check record", expectedRecord, record );
    }

    public void testCorruptJar()
        throws RepositoryIndexException
    {
        Artifact artifact = createArtifact( "test-corrupt-jar" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        assertNull( "Confirm no record is returned", record );
    }

    public void testNonJar()
        throws RepositoryIndexException
    {
        Artifact artifact = createArtifact( "test-dll", "1.0.1.34", "dll" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        assertNull( "Confirm no record is returned", record );
    }

    public void testMissingFile()
        throws RepositoryIndexException
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
        return createArtifact( artifactId, version, type, null );
    }

    private Artifact createArtifact( String artifactId, String version, String type, String classifier )
    {
        Artifact artifact = artifactFactory.createDependencyArtifact( TEST_GROUP_ID, artifactId,
                                                                      VersionRange.createFromVersion( version ), type,
                                                                      classifier, Artifact.SCOPE_RUNTIME );
        artifact.isSnapshot();
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        artifact.setRepository( repository );
        return artifact;
    }
}
