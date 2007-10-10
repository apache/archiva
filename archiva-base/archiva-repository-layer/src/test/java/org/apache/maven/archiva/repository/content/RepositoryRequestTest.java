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
import org.apache.maven.archiva.repository.layout.LayoutException;

/**
 * RepositoryRequestTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryRequestTest
    extends AbstractRepositoryLayerTestCase
{
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
        assertValid( "commons-lang/jars/commons-lang-2.1-javadoc.jar", "commons-lang", "commons-lang", "2.1-javadoc",
                     null, "javadoc" );
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
        assertValid(
                     "com/company/department/com.company.department.project/0.2/com.company.department.project-0.2.pom",
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

    public void testIsArtifact()
    {
        assertTrue( repoRequest.isArtifact( "test.maven-arch/poms/test-arch-2.0.3-SNAPSHOT.pom" ) );
        assertTrue( repoRequest.isArtifact( "test/maven-arch/test-arch/2.0.3-SNAPSHOT/test-arch-2.0.3-SNAPSHOT.jar" ) );
        assertTrue( repoRequest.isArtifact( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz" ) );
        
        assertFalse( repoRequest.isArtifact( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.sha1" ));
        assertFalse( repoRequest.isArtifact( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.md5" ));
        assertFalse( repoRequest.isArtifact( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.asc" ));
        assertFalse( repoRequest.isArtifact( "org/apache/derby/derby/10.2.2.0/derby-10.2.2.0-bin.tar.gz.pgp" ));
        assertFalse( repoRequest.isArtifact( "org/apache/derby/derby/10.2.2.0/maven-metadata.xml" ));
        assertFalse( repoRequest.isArtifact( "org/apache/derby/derby/maven-metadata.xml" ));
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
