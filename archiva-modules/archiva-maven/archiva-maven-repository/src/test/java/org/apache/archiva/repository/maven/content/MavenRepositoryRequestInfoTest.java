package org.apache.archiva.repository.maven.content;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.common.filelock.FileLockManager;
import org.apache.archiva.common.utils.FileUtils;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.FileType;
import org.apache.archiva.configuration.FileTypes;
import org.apache.archiva.repository.maven.metadata.storage.ArtifactMappingProvider;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.repository.LayoutException;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RepositoryContentProvider;
import org.apache.archiva.repository.maven.MavenManagedRepository;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;

/**
 * RepositoryRequestTest
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( { "classpath*:/META-INF/spring-context.xml",
    "classpath:/spring-context-repo-request-test.xml" } )
public class MavenRepositoryRequestInfoTest
{

    @Inject
    protected ApplicationContext applicationContext;

    @Inject
    FileTypes fileTypes;

    @Inject
    @Named( "archivaConfiguration#repo-request-test" )
    private ArchivaConfiguration archivaConfiguration;

    @Inject
    List<? extends ArtifactMappingProvider> artifactMappingProviders;

    @Inject
    FileLockManager fileLockManager;

    private MavenRepositoryRequestInfo repoRequest;


    protected MavenManagedRepository createRepository( String id, String name, Path location ) throws IOException {
        MavenManagedRepository repo = MavenManagedRepository.newLocalInstance( id, name, location.getParent().toAbsolutePath());
        repo.setLocation( location.toAbsolutePath().toUri() );
        return repo;
    }

    private Path getRepositoryPath(String repoName) {
        try
        {
            return Paths.get( Thread.currentThread( ).getContextClassLoader( ).getResource( "repositories/" + repoName ).toURI( ) );
        }
        catch ( URISyntaxException e )
        {
            throw new RuntimeException( "Could not resolve repository path " + e.getMessage( ), e );
        }
    }

    @Before
    public void setUp()
        throws Exception
    {

        Path repoDir = getRepositoryPath( "default-repository" );
        MavenManagedRepository repository = createRepository( "testRepo", "Unit Test Repo", repoDir );

        FileType fileType = archivaConfiguration.getConfiguration().getRepositoryScanning().getFileTypes().get( 0 );
        fileType.addPattern( "**/*.xml" );
        assertEquals( FileTypes.ARTIFACTS, fileType.getId() );

        fileTypes.afterConfigurationChange( null, "fileType", null );

        ManagedDefaultRepositoryContent repoContent = new ManagedDefaultRepositoryContent(repository, artifactMappingProviders, fileTypes, fileLockManager);
        //repoContent = (ManagedRepositoryContent) lookup( ManagedRepositoryContent.class, "default" );
        repository.setContent(repoContent);
        repoRequest = new MavenRepositoryRequestInfo(repository);
    }

    @Test
    public void testInvalidRequestEmptyPath()
    {
        assertInvalidRequest( "" );
    }

    @Test
    public void testInvalidRequestSlashOnly()
    {
        assertInvalidRequest( "//" );
    }

    @Test
    public void testInvalidRequestNoArtifactId()
    {
        assertInvalidRequest( "groupId/jars/-1.0.jar" );
    }


    @Test
    public void testInvalidRequestTooShort()
    {
        assertInvalidRequest( "org.apache.maven.test/artifactId-2.0.jar" );
    }

    @Test
    public void testInvalidDefaultRequestBadLocation()
    {
        assertInvalidRequest( "invalid/invalid/1.0-20050611.123456-1/invalid-1.0-20050611.123456-1.jar" );
    }

    @Test( expected = LayoutException.class )
    public void testValidLegacyGanymed()
        throws Exception
    {
        assertValid( "ch.ethz.ganymed/jars/ganymed-ssh2-build210.jar", "ch.ethz.ganymed", "ganymed-ssh2", "build210",
                     null, "jar" );
    }

    @Test
    public void testValidDefaultGanymed()
        throws Exception
    {
        assertValid( "ch/ethz/ganymed/ganymed-ssh2/build210/ganymed-ssh2-build210.jar", "ch.ethz.ganymed",
                     "ganymed-ssh2", "build210", null, "jar" );
    }

    @Test( expected = LayoutException.class )
    public void testValidLegacyJavaxComm()
        throws Exception
    {
        assertValid( "javax/jars/comm-3.0-u1.jar", "javax", "comm", "3.0-u1", null, "jar" );
    }

    @Test
    public void testValidDefaultJavaxComm()
        throws Exception
    {
        assertValid( "javax/comm/3.0-u1/comm-3.0-u1.jar", "javax", "comm", "3.0-u1", null, "jar" );
    }

    @Test( expected = LayoutException.class )
    public void testValidLegacyJavaxPersistence()
        throws Exception
    {
        assertValid( "javax.persistence/jars/ejb-3.0-public_review.jar", "javax.persistence", "ejb",
                     "3.0-public_review", null, "jar" );
    }

    @Test
    public void testValidDefaultJavaxPersistence()
        throws Exception
    {
        assertValid( "javax/persistence/ejb/3.0-public_review/ejb-3.0-public_review.jar", "javax.persistence", "ejb",
                     "3.0-public_review", null, "jar" );
    }

    @Test( expected = LayoutException.class )
    public void testValidLegacyMavenTestPlugin()
        throws Exception
    {
        assertValid( "maven/jars/maven-test-plugin-1.8.2.jar", "maven", "maven-test-plugin", "1.8.2", null, "jar" );
    }

    @Test
    public void testValidDefaultMavenTestPlugin()
        throws Exception
    {
        assertValid( "maven/maven-test-plugin/1.8.2/maven-test-plugin-1.8.2.pom", "maven", "maven-test-plugin", "1.8.2",
                     null, "pom" );
    }

    @Test( expected = LayoutException.class )
    public void testValidLegacyCommonsLangJavadoc()
        throws Exception
    {
        assertValid( "commons-lang/javadoc.jars/commons-lang-2.1-javadoc.jar", "commons-lang", "commons-lang", "2.1",
                     "javadoc", "javadoc" );
    }

    @Test
    public void testValidDefaultCommonsLangJavadoc()
        throws Exception
    {
        assertValid( "commons-lang/commons-lang/2.1/commons-lang-2.1-javadoc.jar", "commons-lang", "commons-lang",
                     "2.1", "javadoc", "javadoc" );
    }

    @Test( expected = LayoutException.class )
    public void testValidLegacyDerbyPom()
        throws Exception
    {
        assertValid( "org.apache.derby/poms/derby-10.2.2.0.pom", "org.apache.derby", "derby", "10.2.2.0", null, "pom" );
        // Starting slash should not prevent detection.
        assertValid( "/org.apache.derby/poms/derby-10.2.2.0.pom", "org.apache.derby", "derby", "10.2.2.0", null,
                     "pom" );
    }

    @Test
    public void testValidDefaultDerbyPom()
        throws Exception
    {
        assertValid( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0.pom", "org.apache.derby", "derby", "10.2.2.0",
                     null, "pom" );
    }

    @Test( expected = LayoutException.class )
    public void testValidLegacyGeronimoEjbSpec()
        throws Exception
    {
        assertValid( "org.apache.geronimo.specs/jars/geronimo-ejb_2.1_spec-1.0.1.jar", "org.apache.geronimo.specs",
                     "geronimo-ejb_2.1_spec", "1.0.1", null, "jar" );
    }

    @Test
    public void testValidDefaultGeronimoEjbSpec()
        throws Exception
    {
        assertValid( "org/apache/geronimo/specs/geronimo-ejb_2.1_spec/1.0.1/geronimo-ejb_2.1_spec-1.0.1.jar",
                     "org.apache.geronimo.specs", "geronimo-ejb_2.1_spec", "1.0.1", null, "jar" );
    }

    @Test( expected = LayoutException.class )
    public void testValidLegacyLdapSnapshot()
        throws Exception
    {
        assertValid( "directory-clients/poms/ldap-clients-0.9.1-SNAPSHOT.pom", "directory-clients", "ldap-clients",
                     "0.9.1-SNAPSHOT", null, "pom" );
    }

    @Test
    public void testValidDefaultLdapSnapshot()
        throws Exception
    {
        assertValid( "directory-clients/ldap-clients/0.9.1-SNAPSHOT/ldap-clients-0.9.1-SNAPSHOT.pom",
                     "directory-clients", "ldap-clients", "0.9.1-SNAPSHOT", null, "pom" );
    }

    @Test( expected = LayoutException.class )
    public void testValidLegacyTestArchSnapshot()
        throws Exception
    {
        assertValid( "test.maven-arch/poms/test-arch-2.0.3-SNAPSHOT.pom", "test.maven-arch", "test-arch",
                     "2.0.3-SNAPSHOT", null, "pom" );
    }

    @Test
    public void testValidDefaultTestArchSnapshot()
        throws Exception
    {
        assertValid( "test/maven-arch/test-arch/2.0.3-SNAPSHOT/test-arch-2.0.3-SNAPSHOT.pom", "test.maven-arch",
                     "test-arch", "2.0.3-SNAPSHOT", null, "pom" );
    }

    @Test( expected = LayoutException.class )
    public void testValidLegacyOddDottedArtifactId()
        throws Exception
    {
        assertValid( "com.company.department/poms/com.company.department.project-0.2.pom", "com.company.department",
                     "com.company.department.project", "0.2", null, "pom" );
    }

    @Test
    public void testValidDefaultOddDottedArtifactId()
        throws Exception
    {
        assertValid( "com/company/department/com.company.department.project/0.2/com.company.department.project-0.2.pom",
                     "com.company.department", "com.company.department.project", "0.2", null, "pom" );
    }

    @Test( expected = LayoutException.class )
    public void testValidLegacyTimestampedSnapshot()
        throws Exception
    {
        assertValid( "org.apache.archiva.test/jars/redonkulous-3.1-beta-1-20050831.101112-42.jar",
                     "org.apache.archiva.test", "redonkulous", "3.1-beta-1-20050831.101112-42", null, "jar" );
    }

    @Test
    public void testValidDefaultTimestampedSnapshot()
        throws Exception
    {
        assertValid(
            "org/apache/archiva/test/redonkulous/3.1-beta-1-SNAPSHOT/redonkulous-3.1-beta-1-20050831.101112-42.jar",
            "org.apache.archiva.test", "redonkulous", "3.1-beta-1-20050831.101112-42", null, "jar" );
    }

    @Test
    public void testIsSupportFile()
    {
        assertTrue( repoRequest.isSupportFile( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.sha1" ) );
        assertTrue( repoRequest.isSupportFile( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.md5" ) );
        assertTrue( repoRequest.isSupportFile( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.asc" ) );
        assertTrue( repoRequest.isSupportFile( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.pgp" ) );
        assertTrue( repoRequest.isSupportFile( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml.sha1" ) );
        assertTrue( repoRequest.isSupportFile( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml.md5" ) );

        assertFalse( repoRequest.isSupportFile( "test.maven-arch/poms/test-arch-2.0.3-SNAPSHOT.pom" ) );
        assertFalse(
            repoRequest.isSupportFile( "test/maven-arch/test-arch/2.0.3-SNAPSHOT/test-arch-2.0.3-SNAPSHOT.jar" ) );
        assertFalse( repoRequest.isSupportFile( "org/apache/archiva/archiva-api/1.0/archiva-api-1.0.xml.zip" ) );
        assertFalse( repoRequest.isSupportFile( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz" ) );
        assertFalse( repoRequest.isSupportFile( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml" ) );
        assertFalse( repoRequest.isSupportFile( "org/apache/derby/derby/maven-metadata.xml" ) );
    }

    @Test
    public void testIsMetadata()
    {
        assertTrue( repoRequest.isMetadata( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml" ) );
        assertTrue( repoRequest.isMetadata( "org/apache/derby/derby/maven-metadata.xml" ) );

        assertFalse( repoRequest.isMetadata( "test.maven-arch/poms/test-arch-2.0.3-SNAPSHOT.pom" ) );
        assertFalse(
            repoRequest.isMetadata( "test/maven-arch/test-arch/2.0.3-SNAPSHOT/test-arch-2.0.3-SNAPSHOT.jar" ) );
        assertFalse( repoRequest.isMetadata( "org/apache/archiva/archiva-api/1.0/archiva-api-1.0.xml.zip" ) );
        assertFalse( repoRequest.isMetadata( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz" ) );
        assertFalse( repoRequest.isMetadata( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.pgp" ) );
        assertFalse( repoRequest.isMetadata( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml.sha1" ) );
    }

    @Test
    public void testIsMetadataSupportFile()
    {
        assertFalse( repoRequest.isMetadataSupportFile( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml" ) );
        assertFalse( repoRequest.isMetadataSupportFile( "org/apache/derby/derby/maven-metadata.xml" ) );
        assertTrue( repoRequest.isMetadataSupportFile( "org/apache/derby/derby/maven-metadata.xml.sha1" ) );
        assertTrue( repoRequest.isMetadataSupportFile( "org/apache/derby/derby/maven-metadata.xml.md5" ) );

        assertFalse( repoRequest.isMetadataSupportFile( "test.maven-arch/poms/test-arch-2.0.3-SNAPSHOT.pom" ) );
        assertFalse( repoRequest.isMetadataSupportFile(
            "test/maven-arch/test-arch/2.0.3-SNAPSHOT/test-arch-2.0.3-SNAPSHOT.jar" ) );
        assertFalse(
            repoRequest.isMetadataSupportFile( "org/apache/archiva/archiva-api/1.0/archiva-api-1.0.xml.zip" ) );
        assertFalse( repoRequest.isMetadataSupportFile( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz" ) );
        assertFalse(
            repoRequest.isMetadataSupportFile( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.pgp" ) );
        assertTrue( repoRequest.isMetadataSupportFile( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml.sha1" ) );
        assertTrue( repoRequest.isMetadataSupportFile( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml.md5" ) );
    }

    @Test
    public void testIsDefault()
    {
        assertNotEquals( "default", repoRequest.getLayout( "test.maven-arch/poms/test-arch-2.0.3-SNAPSHOT.pom" ) );
        assertNotEquals("default", repoRequest.getLayout( "directory-clients/poms/ldap-clients-0.9.1-SNAPSHOT.pom" ) );
        assertNotEquals("default", repoRequest.getLayout( "commons-lang/jars/commons-lang-2.1-javadoc.jar" ) );

        assertEquals("default", repoRequest.getLayout( "test/maven-arch/test-arch/2.0.3-SNAPSHOT/test-arch-2.0.3-SNAPSHOT.jar" ) );
        assertEquals("default", repoRequest.getLayout( "org/apache/archiva/archiva-api/1.0/archiva-api-1.0.xml.zip" ) );
        assertEquals("default", repoRequest.getLayout( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz" ) );
        assertEquals("default", repoRequest.getLayout( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.pgp" ) );
        assertEquals("default", repoRequest.getLayout( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml.sha1" ) );
        assertEquals("default", repoRequest.getLayout( "eclipse/jdtcore/maven-metadata.xml" ) );
        assertEquals("default", repoRequest.getLayout( "eclipse/jdtcore/maven-metadata.xml.sha1" ) );
        assertEquals("default", repoRequest.getLayout( "eclipse/jdtcore/maven-metadata.xml.md5" ) );

        assertNotEquals("default", repoRequest.getLayout( null ) );
        assertNotEquals("default", repoRequest.getLayout( "" ) );
        assertNotEquals("default", repoRequest.getLayout( "foo" ) );
        assertNotEquals("default", repoRequest.getLayout( "some.short/path" ) );
    }

    @Test
    public void testIsLegacy()
    {
        assertEquals("legacy", repoRequest.getLayout( "test.maven-arch/poms/test-arch-2.0.3-SNAPSHOT.pom" ) );
        assertEquals("legacy", repoRequest.getLayout( "directory-clients/poms/ldap-clients-0.9.1-SNAPSHOT.pom" ) );
        assertEquals("legacy", repoRequest.getLayout( "commons-lang/jars/commons-lang-2.1-javadoc.jar" ) );

        assertNotEquals("legacy", repoRequest.getLayout( "test/maven-arch/test-arch/2.0.3-SNAPSHOT/test-arch-2.0.3-SNAPSHOT.jar" ) );
        assertNotEquals("legacy", repoRequest.getLayout( "org/apache/archiva/archiva-api/1.0/archiva-api-1.0.xml.zip" ) );
        assertNotEquals("legacy", repoRequest.getLayout( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz" ) );
        assertNotEquals("legacy", repoRequest.getLayout( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.pgp" ) );
        assertNotEquals("legacy", repoRequest.getLayout( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml.sha1" ) );

        assertNotEquals("legacy", repoRequest.getLayout( null ) );
        assertNotEquals("legacy", repoRequest.getLayout( "" ) );
        assertNotEquals("legacy", repoRequest.getLayout( "some.short/path" ) );
    }

    private ManagedRepositoryContent createManagedRepo( String layout )
        throws Exception
    {
        Path repoRoot = Paths.get( FileUtils.getBasedir() + "/target/test-repo" );
        return createManagedRepositoryContent( "test-internal", "Internal Test Repo", repoRoot, layout );
    }

    /**
     * [MRM-481] Artifact requests with a .xml.zip extension fail with a 404 Error
     */
    @Test
    public void testToNativePathArtifactDefaultToDefaultDualExtension()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "default" );

        // Test (artifact) default to default - dual extension
        assertEquals( "org/project/example-presentation/3.2/example-presentation-3.2.xml.zip",
                      repoRequest.toNativePath( "org/project/example-presentation/3.2/example-presentation-3.2.xml.zip") );
    }


    @Test
    public void testToNativePathMetadataDefaultToDefault()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "default" );

        // Test (metadata) default to default
        assertEquals( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml.sha1",
                      repoRequest.toNativePath( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml.sha1") );
    }


    @Test
    public void testNativePathBadRequestTooShort()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "default" );

        // Test bad request path (too short)
        try
        {
            repoRequest.toNativePath( "org.apache.derby/license.txt");
            fail( "Should have thrown an exception about a too short path." );
        }
        catch ( LayoutException e )
        {
            // expected path.
        }
    }

    @Test
    public void testNativePathBadRequestBlank()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "default" );

        // Test bad request path (too short)
        try
        {
            repoRequest.toNativePath( "");
            fail( "Should have thrown an exception about an blank request." );
        }
        catch ( LayoutException e )
        {
            // expected path.
        }
    }

    @Test
    public void testNativePathBadRequestNull()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "default" );

        // Test bad request path (too short)
        try
        {
            repoRequest.toNativePath( null);
            fail( "Should have thrown an exception about an null request." );
        }
        catch ( LayoutException e )
        {
            // expected path.
        }
    }

    @Test
    public void testNativePathBadRequestUnknownType()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "default" );

        // Test bad request path (too short)
        try
        {
            repoRequest.toNativePath( "org/apache/derby/derby/10.2.2.0/license.txt");
            fail( "Should have thrown an exception about an invalid type." );
        }
        catch ( LayoutException e )
        {
            // expected path.
        }
    }


    private void assertValid( String path, String groupId, String artifactId, String version, String classifier,
                              String type )
        throws Exception
    {
        String expectedId =
            "ArtifactReference - " + groupId + ":" + artifactId + ":" + version + ":" + ( classifier != null ?
                classifier + ":" : "" ) + type;

        ArtifactReference reference = repoRequest.toArtifactReference( path );

        assertNotNull( expectedId + " - Should not be null.", reference );

        assertEquals( expectedId + " - Group ID", groupId, reference.getGroupId() );
        assertEquals( expectedId + " - Artifact ID", artifactId, reference.getArtifactId() );
        if ( StringUtils.isNotBlank( classifier ) )
        {
            assertEquals( expectedId + " - Classifier", classifier, reference.getClassifier() );
        }
        assertEquals( expectedId + " - Version ID", version, reference.getVersion() );
        assertEquals( expectedId + " - Type", type, reference.getType() );
    }

    private void assertInvalidRequest( String path )
    {
        try
        {
            repoRequest.toArtifactReference( path );
            fail( "Expected a LayoutException on an invalid path [" + path + "]" );
        }
        catch ( LayoutException e )
        {
            /* expected path */
        }
    }

    protected ManagedRepositoryContent createManagedRepositoryContent( String id, String name, Path location,
                                                                       String layout )
        throws Exception
    {
        MavenManagedRepository repo = MavenManagedRepository.newLocalInstance( id, name, archivaConfiguration.getRepositoryBaseDir());
        repo.setLocation( location.toAbsolutePath().toUri() );
        repo.setLayout( layout );

        RepositoryContentProvider provider = applicationContext.getBean( "repositoryContentProvider#maven", RepositoryContentProvider.class );

        ManagedRepositoryContent repoContent =
            provider.createManagedContent( repo );

        return repoContent;
    }

}
