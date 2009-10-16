package org.apache.maven.archiva.repository.content;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.AbstractRepositoryLayerTestCase;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.layout.LayoutException;

import java.io.File;

/**
 * RepositoryRequestTest
 *
 * @version $Id$
 */
public class RepositoryRequestTest
    extends AbstractRepositoryLayerTestCase
{
    public void testInvalidRequestEmptyPath()
    {
        assertInvalidRequest( "" );
    }
    
    public void testInvalidRequestSlashOnly()
    {
        assertInvalidRequest( "//" );
    }
    
    public void testInvalidRequestNoArtifactId()
    {
        assertInvalidRequest( "groupId/jars/-1.0.jar" );
    }

    public void testInvalidLegacyRequestBadLocation()
    {
        assertInvalidRequest( "org.apache.maven.test/jars/artifactId-1.0.war" );
    }

    public void testInvalidRequestTooShort()
    {
        assertInvalidRequest( "org.apache.maven.test/artifactId-2.0.jar" );
    }

    public void testInvalidDefaultRequestBadLocation()
    {
        assertInvalidRequest( "invalid/invalid/1.0-20050611.123456-1/invalid-1.0-20050611.123456-1.jar" );
    }

    public void testValidLegacyGanymed()
        throws Exception
    {
        assertValid( "ch.ethz.ganymed/jars/ganymed-ssh2-build210.jar", "ch.ethz.ganymed", "ganymed-ssh2", "build210",
                     null, "jar" );
    }

    public void testValidDefaultGanymed()
        throws Exception
    {
        assertValid( "ch/ethz/ganymed/ganymed-ssh2/build210/ganymed-ssh2-build210.jar", "ch.ethz.ganymed",
                     "ganymed-ssh2", "build210", null, "jar" );
    }

    public void testValidLegacyJavaxComm()
        throws Exception
    {
        assertValid( "javax/jars/comm-3.0-u1.jar", "javax", "comm", "3.0-u1", null, "jar" );
    }

    public void testValidDefaultJavaxComm()
        throws Exception
    {
        assertValid( "javax/comm/3.0-u1/comm-3.0-u1.jar", "javax", "comm", "3.0-u1", null, "jar" );
    }

    public void testValidLegacyJavaxPersistence()
        throws Exception
    {
        assertValid( "javax.persistence/jars/ejb-3.0-public_review.jar", "javax.persistence", "ejb",
                     "3.0-public_review", null, "jar" );
    }

    public void testValidDefaultJavaxPersistence()
        throws Exception
    {
        assertValid( "javax/persistence/ejb/3.0-public_review/ejb-3.0-public_review.jar", "javax.persistence", "ejb",
                     "3.0-public_review", null, "jar" );
    }

    public void testValidLegacyMavenTestPlugin()
        throws Exception
    {
        assertValid( "maven/jars/maven-test-plugin-1.8.2.jar", "maven", "maven-test-plugin", "1.8.2", null, "jar" );
    }

    public void testValidDefaultMavenTestPlugin()
        throws Exception
    {
        assertValid( "maven/maven-test-plugin/1.8.2/maven-test-plugin-1.8.2.pom", "maven", "maven-test-plugin",
                     "1.8.2", null, "pom" );
    }

    public void testValidLegacyCommonsLangJavadoc()
        throws Exception
    {
        assertValid( "commons-lang/javadoc.jars/commons-lang-2.1-javadoc.jar", "commons-lang", "commons-lang", "2.1",
                     "javadoc", "javadoc" );
    }

    public void testValidDefaultCommonsLangJavadoc()
        throws Exception
    {
        assertValid( "commons-lang/commons-lang/2.1/commons-lang-2.1-javadoc.jar", "commons-lang", "commons-lang",
                     "2.1", "javadoc", "javadoc" );
    }

    public void testValidLegacyDerbyPom()
        throws Exception
    {
        assertValid( "org.apache.derby/poms/derby-10.2.2.0.pom", "org.apache.derby", "derby", "10.2.2.0", null, "pom" );
        // Starting slash should not prevent detection.
        assertValid( "/org.apache.derby/poms/derby-10.2.2.0.pom", "org.apache.derby", "derby", "10.2.2.0", null, "pom" );
    }

    public void testValidDefaultDerbyPom()
        throws Exception
    {
        assertValid( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0.pom", "org.apache.derby", "derby", "10.2.2.0",
                     null, "pom" );
    }

    public void testValidLegacyGeronimoEjbSpec()
        throws Exception
    {
        assertValid( "org.apache.geronimo.specs/jars/geronimo-ejb_2.1_spec-1.0.1.jar", "org.apache.geronimo.specs",
                     "geronimo-ejb_2.1_spec", "1.0.1", null, "jar" );
    }

    public void testValidDefaultGeronimoEjbSpec()
        throws Exception
    {
        assertValid( "org/apache/geronimo/specs/geronimo-ejb_2.1_spec/1.0.1/geronimo-ejb_2.1_spec-1.0.1.jar",
                     "org.apache.geronimo.specs", "geronimo-ejb_2.1_spec", "1.0.1", null, "jar" );
    }

    public void testValidLegacyLdapSnapshot()
        throws Exception
    {
        assertValid( "directory-clients/poms/ldap-clients-0.9.1-SNAPSHOT.pom", "directory-clients", "ldap-clients",
                     "0.9.1-SNAPSHOT", null, "pom" );
    }

    public void testValidDefaultLdapSnapshot()
        throws Exception
    {
        assertValid( "directory-clients/ldap-clients/0.9.1-SNAPSHOT/ldap-clients-0.9.1-SNAPSHOT.pom",
                     "directory-clients", "ldap-clients", "0.9.1-SNAPSHOT", null, "pom" );
    }

    public void testValidLegacyTestArchSnapshot()
        throws Exception
    {
        assertValid( "test.maven-arch/poms/test-arch-2.0.3-SNAPSHOT.pom", "test.maven-arch", "test-arch",
                     "2.0.3-SNAPSHOT", null, "pom" );
    }

    public void testValidDefaultTestArchSnapshot()
        throws Exception
    {
        assertValid( "test/maven-arch/test-arch/2.0.3-SNAPSHOT/test-arch-2.0.3-SNAPSHOT.pom", "test.maven-arch",
                     "test-arch", "2.0.3-SNAPSHOT", null, "pom" );
    }

    public void testValidLegacyOddDottedArtifactId()
        throws Exception
    {
        assertValid( "com.company.department/poms/com.company.department.project-0.2.pom", "com.company.department",
                     "com.company.department.project", "0.2", null, "pom" );
    }

    public void testValidDefaultOddDottedArtifactId()
        throws Exception
    {
        assertValid( "com/company/department/com.company.department.project/0.2/com.company.department.project-0.2.pom",
                     "com.company.department", "com.company.department.project", "0.2", null, "pom" );
    }

    public void testValidLegacyTimestampedSnapshot()
        throws Exception
    {
        assertValid( "org.apache.archiva.test/jars/redonkulous-3.1-beta-1-20050831.101112-42.jar",
                     "org.apache.archiva.test", "redonkulous", "3.1-beta-1-20050831.101112-42", null, "jar" );
    }

    public void testValidDefaultTimestampedSnapshot()
        throws Exception
    {
        assertValid(
                     "org/apache/archiva/test/redonkulous/3.1-beta-1-SNAPSHOT/redonkulous-3.1-beta-1-20050831.101112-42.jar",
                     "org.apache.archiva.test", "redonkulous", "3.1-beta-1-20050831.101112-42", null, "jar" );
    }

    public void testIsSupportFile()
    {
        assertTrue( repoRequest.isSupportFile( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.sha1" ) );
        assertTrue( repoRequest.isSupportFile( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.md5" ) );
        assertTrue( repoRequest.isSupportFile( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.asc" ) );
        assertTrue( repoRequest.isSupportFile( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.pgp" ) );
        assertTrue( repoRequest.isSupportFile( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml.sha1" ) );
        assertTrue( repoRequest.isSupportFile( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml.md5" ) );

        assertFalse( repoRequest.isSupportFile( "test.maven-arch/poms/test-arch-2.0.3-SNAPSHOT.pom" ) );
        assertFalse( repoRequest.isSupportFile( "test/maven-arch/test-arch/2.0.3-SNAPSHOT/test-arch-2.0.3-SNAPSHOT.jar" ) );
        assertFalse( repoRequest.isSupportFile( "org/apache/archiva/archiva-api/1.0/archiva-api-1.0.xml.zip" ) );
        assertFalse( repoRequest.isSupportFile( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz" ) );
        assertFalse( repoRequest.isSupportFile( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml" ) );
        assertFalse( repoRequest.isSupportFile( "org/apache/derby/derby/maven-metadata.xml" ) );
    }

    public void testIsMetadata()
    {
        assertTrue( repoRequest.isMetadata( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml" ));
        assertTrue( repoRequest.isMetadata( "org/apache/derby/derby/maven-metadata.xml" ));

        assertFalse( repoRequest.isMetadata( "test.maven-arch/poms/test-arch-2.0.3-SNAPSHOT.pom" ) );
        assertFalse( repoRequest.isMetadata( "test/maven-arch/test-arch/2.0.3-SNAPSHOT/test-arch-2.0.3-SNAPSHOT.jar" ) );
        assertFalse( repoRequest.isMetadata( "org/apache/archiva/archiva-api/1.0/archiva-api-1.0.xml.zip" ) );
        assertFalse( repoRequest.isMetadata( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz" ) );
        assertFalse( repoRequest.isMetadata( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.pgp" ) );
        assertFalse( repoRequest.isMetadata( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml.sha1" ) );
    }
    
    public void testIsMetadataSupportFile()
    {
        assertFalse( repoRequest.isMetadataSupportFile( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml" ));
        assertFalse( repoRequest.isMetadataSupportFile( "org/apache/derby/derby/maven-metadata.xml" ));
        assertTrue( repoRequest.isMetadataSupportFile( "org/apache/derby/derby/maven-metadata.xml.sha1" ));
        assertTrue( repoRequest.isMetadataSupportFile( "org/apache/derby/derby/maven-metadata.xml.md5" ));

        assertFalse( repoRequest.isMetadataSupportFile( "test.maven-arch/poms/test-arch-2.0.3-SNAPSHOT.pom" ) );
        assertFalse( repoRequest.isMetadataSupportFile( "test/maven-arch/test-arch/2.0.3-SNAPSHOT/test-arch-2.0.3-SNAPSHOT.jar" ) );
        assertFalse( repoRequest.isMetadataSupportFile( "org/apache/archiva/archiva-api/1.0/archiva-api-1.0.xml.zip" ) );
        assertFalse( repoRequest.isMetadataSupportFile( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz" ) );
        assertFalse( repoRequest.isMetadataSupportFile( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.pgp" ) );
        assertTrue( repoRequest.isMetadataSupportFile( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml.sha1" ) );
        assertTrue( repoRequest.isMetadataSupportFile( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml.md5" ) );        
    }

    public void testIsDefault()
    {
        assertFalse( repoRequest.isDefault( "test.maven-arch/poms/test-arch-2.0.3-SNAPSHOT.pom" ) );
        assertFalse( repoRequest.isDefault( "directory-clients/poms/ldap-clients-0.9.1-SNAPSHOT.pom" ) );
        assertFalse( repoRequest.isDefault( "commons-lang/jars/commons-lang-2.1-javadoc.jar" ) );

        assertTrue( repoRequest.isDefault( "test/maven-arch/test-arch/2.0.3-SNAPSHOT/test-arch-2.0.3-SNAPSHOT.jar" ) );
        assertTrue( repoRequest.isDefault( "org/apache/archiva/archiva-api/1.0/archiva-api-1.0.xml.zip" ) );
        assertTrue( repoRequest.isDefault( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz" ) );
        assertTrue( repoRequest.isDefault( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.pgp" ) );
        assertTrue( repoRequest.isDefault( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml.sha1" ) );
        assertTrue( repoRequest.isDefault( "eclipse/jdtcore/maven-metadata.xml" ) );
        assertTrue( repoRequest.isDefault( "eclipse/jdtcore/maven-metadata.xml.sha1" ) );
        assertTrue( repoRequest.isDefault( "eclipse/jdtcore/maven-metadata.xml.md5" ) );

        assertFalse( repoRequest.isDefault( null ) );
        assertFalse( repoRequest.isDefault( "" ) );
        assertFalse( repoRequest.isDefault( "foo" ) );
        assertFalse( repoRequest.isDefault( "some.short/path" ) );
    }

    public void testIsLegacy()
    {
        assertTrue( repoRequest.isLegacy( "test.maven-arch/poms/test-arch-2.0.3-SNAPSHOT.pom" ) );
        assertTrue( repoRequest.isLegacy( "directory-clients/poms/ldap-clients-0.9.1-SNAPSHOT.pom" ) );
        assertTrue( repoRequest.isLegacy( "commons-lang/jars/commons-lang-2.1-javadoc.jar" ) );

        assertFalse( repoRequest.isLegacy( "test/maven-arch/test-arch/2.0.3-SNAPSHOT/test-arch-2.0.3-SNAPSHOT.jar" ) );
        assertFalse( repoRequest.isLegacy( "org/apache/archiva/archiva-api/1.0/archiva-api-1.0.xml.zip" ) );
        assertFalse( repoRequest.isLegacy( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz" ) );
        assertFalse( repoRequest.isLegacy( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.pgp" ) );
        assertFalse( repoRequest.isLegacy( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml.sha1" ) );

        assertFalse( repoRequest.isLegacy( null ) );
        assertFalse( repoRequest.isLegacy( "" ) );
        assertFalse( repoRequest.isLegacy( "some.short/path" ) );
    }

    private ManagedRepositoryContent createManagedRepo( String layout )
        throws Exception
    {
        File repoRoot = getTestFile( "target/test-repo" );
        return createManagedRepositoryContent( "test-internal", "Internal Test Repo", repoRoot, layout );
    }

    /**
     * [MRM-481] Artifact requests with a .xml.zip extension fail with a 404 Error
     */
    public void testToNativePathArtifactDefaultToDefaultDualExtension()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "default" );

        // Test (artifact) default to default - dual extension
        assertEquals( "org/project/example-presentation/3.2/example-presentation-3.2.xml.zip", repoRequest
            .toNativePath( "org/project/example-presentation/3.2/example-presentation-3.2.xml.zip", repository ) );
    }

    /**
     * [MRM-481] Artifact requests with a .xml.zip extension fail with a 404 Error
     */
    public void testToNativePathArtifactLegacyToDefaultDualExtension()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "default" );

        // Test (artifact) legacy to default - dual extension
        // NOTE: The detection of a dual extension is flawed.
        assertEquals( "org/project/example-presentation/3.2.xml/example-presentation-3.2.xml.zip", repoRequest
            .toNativePath( "org.project/zips/example-presentation-3.2.xml.zip", repository ) );
    }

    public void testToNativePathMetadataDefaultToDefault()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "default" );

        // Test (metadata) default to default
        assertEquals( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml.sha1", repoRequest
            .toNativePath( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml.sha1", repository ) );
    }

    public void testNativePathPomLegacyToDefault()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "default" );

        // Test (pom) legacy to default
        assertEquals( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0.pom",
                      repoRequest.toNativePath( "org.apache.derby/poms/derby-10.2.2.0.pom", repository ) );
    }

    public void testNativePathPomLegacyToLegacy()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "legacy" );

        // Test (pom) legacy to default
        assertEquals( "org.apache.derby/poms/derby-10.2.2.0.pom",
                      repoRequest.toNativePath( "org.apache.derby/poms/derby-10.2.2.0.pom", repository ) );
    }

    public void testNativePathPomLegacyToDefaultEjb()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "default" );

        // Test (pom) legacy to default
        assertEquals( "mygroup/myejb/1.0/myejb-1.0.jar",
                      repoRequest.toNativePath( "mygroup/ejbs/myejb-1.0.jar", repository ) );
    }

    public void testNativePathPomLegacyToLegacyEjb()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "legacy" );

        // Test (pom) legacy to default
        assertEquals( "mygroup/ejbs/myejb-1.0.jar",
                      repoRequest.toNativePath( "mygroup/ejbs/myejb-1.0.jar", repository ) );
    }

    public void testNativePathPomLegacyToLegacyStrutsModule()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "legacy" );

        // Test (pom) legacy to default
        assertEquals( "WebPortal/struts-modules/eventsDB-1.2.3.struts-module",
                      repoRequest.toNativePath( "WebPortal/struts-modules/eventsDB-1.2.3.struts-module", repository ) );
    }

    public void testNativePathSupportFileLegacyToDefault()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "default" );

        // Test (supportfile) legacy to default
        assertEquals( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0.jar.sha1", repoRequest
            .toNativePath( "org.apache.derby/jars/derby-10.2.2.0.jar.sha1", repository ) );
    }

    public void testNativePathBadRequestTooShort()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "default" );

        // Test bad request path (too short)
        try
        {
            repoRequest.toNativePath( "org.apache.derby/license.txt", repository );
            fail( "Should have thrown an exception about a too short path." );
        }
        catch ( LayoutException e )
        {
            // expected path.
        }
    }

    public void testNativePathBadRequestBlank()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "default" );

        // Test bad request path (too short)
        try
        {
            repoRequest.toNativePath( "", repository );
            fail( "Should have thrown an exception about an blank request." );
        }
        catch ( LayoutException e )
        {
            // expected path.
        }
    }

    public void testNativePathBadRequestNull()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "default" );

        // Test bad request path (too short)
        try
        {
            repoRequest.toNativePath( null, repository );
            fail( "Should have thrown an exception about an null request." );
        }
        catch ( LayoutException e )
        {
            // expected path.
        }
    }

    public void testNativePathBadRequestUnknownType()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "default" );

        // Test bad request path (too short)
        try
        {
            repoRequest.toNativePath( "org/apache/derby/derby/10.2.2.0/license.txt", repository );
            fail( "Should have thrown an exception about an invalid type." );
        }
        catch ( LayoutException e )
        {
            // expected path.
        }
    }

    public void testToNativePathLegacyMetadataDefaultToLegacy()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "legacy" );

        // Test (metadata) default to legacy

        // Special Case: This direction is not supported, should throw a LayoutException.
        try
        {
            repoRequest.toNativePath( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml", repository );
            fail("Should have thrown a LayoutException, can't translate a maven-metadata.xml to a legacy layout.");
        }
        catch(LayoutException e)
        {
            // expected path.
        }
    }

    public void testNativePathPomDefaultToLegacy()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "legacy" );

        // Test (pom) default to legacy
        assertEquals( "org.apache.derby/poms/derby-10.2.2.0.pom", repoRequest
            .toNativePath( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0.pom", repository ) );
    }

    public void testNativePathSupportFileDefaultToLegacy()
        throws Exception
    {
        ManagedRepositoryContent repository = createManagedRepo( "legacy" );

        // Test (supportfile) default to legacy
        assertEquals( "org.apache.derby/jars/derby-10.2.2.0.jar.sha1", repoRequest
            .toNativePath( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0.jar.sha1", repository ) );
    }

    private void assertValid( String path, String groupId, String artifactId, String version, String classifier,
                              String type )
        throws Exception
    {
        String expectedId = "ArtifactReference - " + groupId + ":" + artifactId + ":" + version + ":"
            + ( classifier != null ? classifier + ":" : "" ) + type;

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

    private RepositoryRequest repoRequest;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        repoRequest = (RepositoryRequest) lookup( RepositoryRequest.class );
    }
}
