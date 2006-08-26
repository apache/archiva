package org.apache.maven.archiva.indexer.record;

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
import java.util.List;

/**
 * Test the minimal artifact index record.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class StandardArtifactIndexRecordFactoryTest
    extends PlexusTestCase
{
    private RepositoryIndexRecordFactory factory;

    private ArtifactRepository repository;

    private ArtifactFactory artifactFactory;

    private static final String TEST_GROUP_ID = "org.apache.maven.archiva.record";

    private static final List JAR_CLASS_LIST = Arrays.asList( new String[]{"A", "b.B", "b.c.C"} );

    private static final List JAR_FILE_LIST =
        Arrays.asList( new String[]{"META-INF/MANIFEST.MF", "A.class", "b/B.class", "b/c/C.class"} );

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
        throws RepositoryIndexException
    {
        Artifact artifact = createArtifact( "test-jar" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        StandardArtifactIndexRecord expectedRecord = new StandardArtifactIndexRecord();
        expectedRecord.setMd5Checksum( "3a0adc365f849366cd8b633cad155cb7" );
        expectedRecord.setFilename( repository.pathOf( artifact ) );
        expectedRecord.setLastModified( artifact.getFile().lastModified() );
        expectedRecord.setSize( artifact.getFile().length() );
        expectedRecord.setClasses( JAR_CLASS_LIST );
        expectedRecord.setArtifactId( "test-jar" );
        expectedRecord.setGroupId( TEST_GROUP_ID );
        expectedRecord.setBaseVersion( "1.0" );
        expectedRecord.setVersion( "1.0" );
        expectedRecord.setFiles( JAR_FILE_LIST );
        expectedRecord.setSha1Checksum( "c66f18bf192cb613fc2febb4da541a34133eedc2" );
        expectedRecord.setType( "jar" );
        expectedRecord.setRepository( "test" );

        assertEquals( "check record", expectedRecord, record );
    }

    public void testIndexedJarWithClassifier()
        throws RepositoryIndexException
    {
        Artifact artifact = createArtifact( "test-jar", "1.0", "jar", "jdk14" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        StandardArtifactIndexRecord expectedRecord = new StandardArtifactIndexRecord();
        expectedRecord.setMd5Checksum( "3a0adc365f849366cd8b633cad155cb7" );
        expectedRecord.setFilename( repository.pathOf( artifact ) );
        expectedRecord.setLastModified( artifact.getFile().lastModified() );
        expectedRecord.setSize( artifact.getFile().length() );
        expectedRecord.setClasses( JAR_CLASS_LIST );
        expectedRecord.setArtifactId( "test-jar" );
        expectedRecord.setGroupId( TEST_GROUP_ID );
        expectedRecord.setBaseVersion( "1.0" );
        expectedRecord.setVersion( "1.0" );
        expectedRecord.setFiles( JAR_FILE_LIST );
        expectedRecord.setSha1Checksum( "c66f18bf192cb613fc2febb4da541a34133eedc2" );
        expectedRecord.setType( "jar" );
        expectedRecord.setRepository( "test" );
        expectedRecord.setClassifier( "jdk14" );

        assertEquals( "check record", expectedRecord, record );
    }

    public void testIndexedJarAndPom()
        throws RepositoryIndexException
    {
        Artifact artifact = createArtifact( "test-jar-and-pom", "1.0-alpha-1", "jar" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        StandardArtifactIndexRecord expectedRecord = new StandardArtifactIndexRecord();
        expectedRecord.setMd5Checksum( "3a0adc365f849366cd8b633cad155cb7" );
        expectedRecord.setFilename( repository.pathOf( artifact ) );
        expectedRecord.setLastModified( artifact.getFile().lastModified() );
        expectedRecord.setSize( artifact.getFile().length() );
        expectedRecord.setClasses( JAR_CLASS_LIST );
        expectedRecord.setArtifactId( "test-jar-and-pom" );
        expectedRecord.setGroupId( TEST_GROUP_ID );
        expectedRecord.setBaseVersion( "1.0-alpha-1" );
        expectedRecord.setVersion( "1.0-alpha-1" );
        expectedRecord.setFiles( JAR_FILE_LIST );
        expectedRecord.setSha1Checksum( "c66f18bf192cb613fc2febb4da541a34133eedc2" );
        expectedRecord.setType( "jar" );
        expectedRecord.setRepository( "test" );
        expectedRecord.setPackaging( "jar" );
        expectedRecord.setProjectName( "Test JAR and POM" );

        assertEquals( "check record", expectedRecord, record );
    }

    public void testIndexedJarAndPomWithClassifier()
        throws RepositoryIndexException
    {
        Artifact artifact = createArtifact( "test-jar-and-pom", "1.0-alpha-1", "jar", "jdk14" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        StandardArtifactIndexRecord expectedRecord = new StandardArtifactIndexRecord();
        expectedRecord.setMd5Checksum( "3a0adc365f849366cd8b633cad155cb7" );
        expectedRecord.setFilename( repository.pathOf( artifact ) );
        expectedRecord.setLastModified( artifact.getFile().lastModified() );
        expectedRecord.setSize( artifact.getFile().length() );
        expectedRecord.setClasses( JAR_CLASS_LIST );
        expectedRecord.setArtifactId( "test-jar-and-pom" );
        expectedRecord.setGroupId( TEST_GROUP_ID );
        expectedRecord.setBaseVersion( "1.0-alpha-1" );
        expectedRecord.setVersion( "1.0-alpha-1" );
        expectedRecord.setFiles( JAR_FILE_LIST );
        expectedRecord.setSha1Checksum( "c66f18bf192cb613fc2febb4da541a34133eedc2" );
        expectedRecord.setType( "jar" );
        expectedRecord.setRepository( "test" );
        expectedRecord.setPackaging( "jar" );
        expectedRecord.setProjectName( "Test JAR and POM" );
        expectedRecord.setClassifier( "jdk14" );

        assertEquals( "check record", expectedRecord, record );
    }

    public void testIndexedJarWithParentPom()
        throws RepositoryIndexException
    {
        Artifact artifact = createArtifact( "test-child-pom", "1.0-20060728.121314-1", "jar" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        StandardArtifactIndexRecord expectedRecord = new StandardArtifactIndexRecord();
        expectedRecord.setMd5Checksum( "3a0adc365f849366cd8b633cad155cb7" );
        expectedRecord.setFilename( repository.pathOf( artifact ) );
        expectedRecord.setLastModified( artifact.getFile().lastModified() );
        expectedRecord.setSize( artifact.getFile().length() );
        expectedRecord.setClasses( JAR_CLASS_LIST );
        expectedRecord.setArtifactId( "test-child-pom" );
        expectedRecord.setGroupId( TEST_GROUP_ID );
        expectedRecord.setBaseVersion( "1.0-SNAPSHOT" );
        expectedRecord.setVersion( "1.0-20060728.121314-1" );
        expectedRecord.setFiles( JAR_FILE_LIST );
        expectedRecord.setSha1Checksum( "c66f18bf192cb613fc2febb4da541a34133eedc2" );
        expectedRecord.setType( "jar" );
        expectedRecord.setRepository( "test" );
        expectedRecord.setPackaging( "jar" );
        expectedRecord.setProjectName( "Child Project" );
        expectedRecord.setProjectDescription( "Description" );
        expectedRecord.setInceptionYear( "2005" );

        assertEquals( "check record", expectedRecord, record );
    }

    public void testIndexedPom()
        throws RepositoryIndexException
    {
        Artifact artifact = createArtifact( "test-pom", "1.0", "pom" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        StandardArtifactIndexRecord expectedRecord = new StandardArtifactIndexRecord();
        expectedRecord.setMd5Checksum( "98b4a1b708a90a8637aaf541bef5094f" );
        expectedRecord.setFilename( repository.pathOf( artifact ) );
        expectedRecord.setLastModified( artifact.getFile().lastModified() );
        expectedRecord.setSize( artifact.getFile().length() );
        expectedRecord.setArtifactId( "test-pom" );
        expectedRecord.setGroupId( TEST_GROUP_ID );
        expectedRecord.setBaseVersion( "1.0" );
        expectedRecord.setVersion( "1.0" );
        expectedRecord.setSha1Checksum( "d95348bee1666a46511260696292bfa0519b61c1" );
        expectedRecord.setType( "pom" );
        expectedRecord.setRepository( "test" );
        expectedRecord.setPackaging( "pom" );
        expectedRecord.setInceptionYear( "2005" );
        expectedRecord.setProjectName( "Maven Repository Manager Test POM" );
        expectedRecord.setProjectDescription( "Description" );

        assertEquals( "check record", expectedRecord, record );
    }

    public void testNonIndexedPom()
        throws RepositoryIndexException
    {
        // If we pass in only the POM that belongs to a JAR, then expect null not the POM
        Artifact artifact = createArtifact( "test-jar-and-pom", "1.0", "pom" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        assertNull( "Check no record", record );

        artifact = createArtifact( "test-plugin", "1.0", "pom" );

        record = factory.createRecord( artifact );

        assertNull( "Check no record", record );

        artifact = createArtifact( "test-archetype", "1.0", "pom" );

        record = factory.createRecord( artifact );

        assertNull( "Check no record", record );
    }

    public void testIndexedPlugin()
        throws RepositoryIndexException, IOException, XmlPullParserException
    {
        Artifact artifact = createArtifact( "test-plugin" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        StandardArtifactIndexRecord expectedRecord = new StandardArtifactIndexRecord();
        expectedRecord.setMd5Checksum( "3530896791670ebb45e17708e5d52c40" );
        expectedRecord.setFilename( repository.pathOf( artifact ) );
        expectedRecord.setLastModified( artifact.getFile().lastModified() );
        expectedRecord.setSize( artifact.getFile().length() );
        expectedRecord.setArtifactId( "test-plugin" );
        expectedRecord.setGroupId( TEST_GROUP_ID );
        expectedRecord.setBaseVersion( "1.0" );
        expectedRecord.setVersion( "1.0" );
        expectedRecord.setSha1Checksum( "2cd2619d59a684e82e97471d2c2e004144c8f24e" );
        expectedRecord.setType( "maven-plugin" );
        expectedRecord.setRepository( "test" );
        expectedRecord.setClasses( Arrays.asList( new String[]{"org.apache.maven.archiva.record.MyMojo"} ) );
        expectedRecord.setFiles( Arrays.asList( new String[]{"META-INF/MANIFEST.MF",
            "META-INF/maven/org.apache.maven.archiva.record/test-plugin/pom.properties",
            "META-INF/maven/org.apache.maven.archiva.record/test-plugin/pom.xml", "META-INF/maven/plugin.xml",
            "org/apache/maven/archiva/record/MyMojo.class"} ) );
        expectedRecord.setPackaging( "maven-plugin" );
        expectedRecord.setProjectName( "Maven Mojo Archetype" );
        expectedRecord.setPluginPrefix( "test" );

        assertEquals( "check record", expectedRecord, record );
    }

    public void testIndexedArchetype()
        throws RepositoryIndexException, IOException, XmlPullParserException
    {
        Artifact artifact = createArtifact( "test-archetype" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        StandardArtifactIndexRecord expectedRecord = new StandardArtifactIndexRecord();
        expectedRecord.setMd5Checksum( "52b7ea4b53818b8a5f4c329d88fd60d9" );
        expectedRecord.setFilename( repository.pathOf( artifact ) );
        expectedRecord.setLastModified( artifact.getFile().lastModified() );
        expectedRecord.setSize( artifact.getFile().length() );
        expectedRecord.setArtifactId( "test-archetype" );
        expectedRecord.setGroupId( TEST_GROUP_ID );
        expectedRecord.setBaseVersion( "1.0" );
        expectedRecord.setVersion( "1.0" );
        expectedRecord.setSha1Checksum( "05841f5e51c124f1729d86c1687438c36b9255d9" );
        expectedRecord.setType( "maven-archetype" );
        expectedRecord.setRepository( "test" );
        expectedRecord.setFiles( Arrays.asList( new String[]{"META-INF/MANIFEST.MF", "META-INF/maven/archetype.xml",
            "META-INF/maven/org.apache.maven.archiva.record/test-archetype/pom.properties",
            "META-INF/maven/org.apache.maven.archiva.record/test-archetype/pom.xml", "archetype-resources/pom.xml",
            "archetype-resources/src/main/java/App.java", "archetype-resources/src/test/java/AppTest.java"} ) );
        expectedRecord.setPackaging( "jar" );
        expectedRecord.setProjectName( "Archetype - test-archetype" );

        assertEquals( "check record", expectedRecord, record );
    }

    public void testCorruptJar()
        throws RepositoryIndexException
    {
        Artifact artifact = createArtifact( "test-corrupt-jar" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        assertNull( "Confirm no record is returned", record );
    }

    public void testDll()
        throws RepositoryIndexException
    {
        Artifact artifact = createArtifact( "test-dll", "1.0.1.34", "dll" );

        RepositoryIndexRecord record = factory.createRecord( artifact );

        StandardArtifactIndexRecord expectedRecord = new StandardArtifactIndexRecord();
        expectedRecord.setMd5Checksum( "d41d8cd98f00b204e9800998ecf8427e" );
        expectedRecord.setFilename( repository.pathOf( artifact ) );
        expectedRecord.setLastModified( artifact.getFile().lastModified() );
        expectedRecord.setSize( artifact.getFile().length() );
        expectedRecord.setArtifactId( "test-dll" );
        expectedRecord.setGroupId( TEST_GROUP_ID );
        expectedRecord.setBaseVersion( "1.0.1.34" );
        expectedRecord.setVersion( "1.0.1.34" );
        expectedRecord.setSha1Checksum( "da39a3ee5e6b4b0d3255bfef95601890afd80709" );
        expectedRecord.setType( "dll" );
        expectedRecord.setRepository( "test" );

        assertEquals( "check record", expectedRecord, record );
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
