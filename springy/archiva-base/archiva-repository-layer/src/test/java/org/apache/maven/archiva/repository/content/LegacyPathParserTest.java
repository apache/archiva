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


import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.DefaultArchivaConfiguration;
import org.apache.maven.archiva.configuration.LegacyArtifactPath;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.AbstractRepositoryLayerTestCase;
import org.apache.maven.archiva.repository.layout.LayoutException;

/**
 * LegacyPathParserTest
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class LegacyPathParserTest
    extends AbstractRepositoryLayerTestCase
{
    private LegacyPathParser parser = new LegacyPathParser();

    /**
     * Configure the ArchivaConfiguration
     * {@inheritDoc}
     * @see org.codehaus.plexus.PlexusTestCase#setUp()
     */
    protected void setUp()
        throws Exception
    {
        super.setUp();
        ArchivaConfiguration config = (ArchivaConfiguration) lookup( ArchivaConfiguration.class );
        LegacyArtifactPath jaxen = new LegacyArtifactPath();
        jaxen.setPath( "jaxen/jars/jaxen-1.0-FCS-full.jar" );
        jaxen.setArtifact( "jaxen:jaxen:1.0-FCS:full:jar" );
        config.getConfiguration().addLegacyArtifactPath( jaxen );
        parser.configuration = config;
    }


    public void testBadPathArtifactIdMissingA()
    {
        assertBadPath( "groupId/jars/-1.0.jar", "artifactId is missing" );
    }

    public void testBadPathArtifactIdMissingB()
    {
        assertBadPath( "groupId/jars/1.0.jar", "artifactId is missing" );
    }

    public void testBadPathMissingType()
    {
        assertBadPath( "invalid/invalid/1/invalid-1", "missing type" );
    }

    public void testBadPathTooShort()
    {
        // NEW
        assertBadPath( "invalid/invalid-1.0.jar", "path is too short" );
    }

    public void testBadPathWrongPackageExtension()
    {
        assertBadPath( "org.apache.maven.test/jars/artifactId-1.0.war", "wrong package extension" );
    }

    /**
     * [MRM-481] Artifact requests with a .xml.zip extension fail with a 404 Error
     */
    public void testGoodButDualExtensions()
        throws LayoutException
    {
        String groupId = "org.project";
        String artifactId = "example-presentation";
        String version = "3.2.xml";
        String type = "distribution-zip";
        String path = "org.project/zips/example-presentation-3.2.xml.zip";

        assertLayout( path, groupId, artifactId, version, null, type );
    }

    /**
     * [MRM-432] Oddball version spec.
     * Example of an oddball / unusual version spec.
     * @throws LayoutException
     */
    public void testGoodButOddVersionSpecGanymedSsh2()
        throws LayoutException
    {
        String groupId = "ch.ethz.ganymed";
        String artifactId = "ganymed-ssh2";
        String version = "build210";
        String type = "jar";
        String path = "ch.ethz.ganymed/jars/ganymed-ssh2-build210.jar";

        assertLayout( path, groupId, artifactId, version, null, type );
    }

    /**
     * [MRM-432] Oddball version spec.
     * Example of an oddball / unusual version spec.
     * @throws LayoutException
     */
    public void testGoodButOddVersionSpecJavaxComm()
        throws LayoutException
    {
        String groupId = "javax";
        String artifactId = "comm";
        String version = "3.0-u1";
        String type = "jar";
        String path = "javax/jars/comm-3.0-u1.jar";

        assertLayout( path, groupId, artifactId, version, null, type );
    }

    /**
     * [MRM-432] Oddball version spec.
     * Example of an oddball / unusual version spec.
     * @throws LayoutException
     */
    public void testGoodButOddVersionSpecJavaxPersistence()
        throws LayoutException
    {
        String groupId = "javax.persistence";
        String artifactId = "ejb";
        String version = "3.0-public_review";
        String type = "jar";
        String path = "javax.persistence/jars/ejb-3.0-public_review.jar";

        /*
         * The version id of "public_review" can cause problems. is it part of
         * the version spec? or the classifier?
         */

        assertLayout( path, groupId, artifactId, version, null, type );
    }

    public void testGoodCommonsLang()
        throws LayoutException
    {
        String groupId = "commons-lang";
        String artifactId = "commons-lang";
        String version = "2.1";
        String type = "jar";
        String path = "commons-lang/jars/commons-lang-2.1.jar";

        assertLayout( path, groupId, artifactId, version, null, type );
    }

    public void testGoodDerby()
        throws LayoutException
    {
        String groupId = "org.apache.derby";
        String artifactId = "derby";
        String version = "10.2.2.0";
        String type = "jar";
        String path = "org.apache.derby/jars/derby-10.2.2.0.jar";

        assertLayout( path, groupId, artifactId, version, null, type );
    }

    /**
     * Test the ejb-client type spec.
     * Type specs are not a 1 to 1 map to the extension.
     * This tests that effect.
     * @throws LayoutException
     */
    /* TODO: Re-enabled in the future.
    public void testGoodFooEjbClient()
        throws LayoutException
    {
        String groupId = "com.foo";
        String artifactId = "foo-client";
        String version = "1.0";
        String type = "ejb"; // oddball type-spec (should result in jar extension)
        String path = "com.foo/ejbs/foo-client-1.0.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }
    */

    /**
     * Test the classifier.
     * @throws LayoutException
     */
    public void testGoodFooLibJavadoc()
        throws LayoutException
    {
        String groupId = "com.foo.lib";
        String artifactId = "foo-lib";
        String version = "2.1-alpha-1";
        String type = "javadoc";
        String classifier = "javadoc";
        String path = "com.foo.lib/javadoc.jars/foo-lib-2.1-alpha-1-javadoc.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    /**
     * Test the classifier, and java-source type spec.
     * @throws LayoutException
     */
    public void testGoodFooLibSources()
        throws LayoutException
    {
        String groupId = "com.foo.lib";
        String artifactId = "foo-lib";
        String version = "2.1-alpha-1";
        String type = "java-source"; // oddball type-spec (should result in jar extension)
        String classifier= "sources";
        String path = "com.foo.lib/java-sources/foo-lib-2.1-alpha-1-sources.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    public void testGoodFooTool()
        throws LayoutException
    {
        String groupId = "com.foo";
        String artifactId = "foo-tool";
        String version = "1.0";
        String type = "jar";
        String path = "com.foo/jars/foo-tool-1.0.jar";

        assertLayout( path, groupId, artifactId, version, null, type );
    }

    public void testGoodGeronimoEjbSpec()
        throws LayoutException
    {
        String groupId = "org.apache.geronimo.specs";
        String artifactId = "geronimo-ejb_2.1_spec";
        String version = "1.0.1";
        String type = "jar";
        String path = "org.apache.geronimo.specs/jars/geronimo-ejb_2.1_spec-1.0.1.jar";

        assertLayout( path, groupId, artifactId, version, null, type );
    }

    public void testGoodLdapClientsPom()
        throws LayoutException
    {
        String groupId = "directory-clients";
        String artifactId = "ldap-clients";
        String version = "0.9.1-SNAPSHOT";
        String type = "pom";
        String path = "directory-clients/poms/ldap-clients-0.9.1-SNAPSHOT.pom";

        assertLayout( path, groupId, artifactId, version, null, type );
    }

    /**
     * A timestamped versioned artifact, should reside in a SNAPSHOT baseversion directory.
     * @throws LayoutException
     */
    public void testGoodSnapshotMavenTest()
        throws LayoutException
    {
        String groupId = "org.apache.archiva.test";
        String artifactId = "redonkulous";
        String version = "3.1-beta-1-20050831.101112-42";
        String type = "jar";
        String path = "org.apache.archiva.test/jars/redonkulous-3.1-beta-1-20050831.101112-42.jar";

        assertLayout( path, groupId, artifactId, version, null, type );
    }

    /**
     * [MRM-519] version identifiers within filename cause misidentification of version.
     * Example uses "test" in artifact Id, which is also part of the versionKeyword list.
     */
    public void testGoodVersionKeywordInArtifactId()
        throws LayoutException
    {
        String groupId = "maven";
        String artifactId = "maven-test-plugin";
        String version = "1.8.2";
        String type = "pom";

        String path = "maven/poms/maven-test-plugin-1.8.2.pom";

        assertLayout( path, groupId, artifactId, version, null, type );
    }

    /**
     * [MRM-562] Artifact type "maven-plugin" is not detected correctly in .toArtifactReference() methods.
     * Example uses "test" in artifact Id, which is also part of the versionKeyword list.
     */
    public void testGoodDetectPluginMavenTest()
        throws LayoutException
    {
        String groupId = "maven";
        String artifactId = "maven-test-plugin";
        String version = "1.8.2";
        String type = "maven-plugin";
        String path = "maven/plugins/maven-test-plugin-1.8.2.jar";

        assertLayout( path, groupId, artifactId, version, null, type );
    }

    /**
     * [MRM-562] Artifact type "maven-plugin" is not detected correctly in .toArtifactReference() methods.
     */
    public void testGoodDetectPluginAvalonMeta()
        throws LayoutException
    {
        String groupId = "avalon-meta";
        String artifactId = "avalon-meta-plugin";
        String version = "1.1";
        String type = "maven-plugin";
        String path = "avalon-meta/plugins/avalon-meta-plugin-1.1.jar";

        assertLayout( path, groupId, artifactId, version, null, type );
    }

    /**
     * [MRM-562] Artifact type "maven-plugin" is not detected correctly in .toArtifactReference() methods.
     */
    public void testGoodDetectPluginCactusMaven()
        throws LayoutException
    {
        String groupId = "cactus";
        String artifactId = "cactus-maven";
        String version = "1.7dev-20040815";
        String type = "maven-plugin";
        String path = "cactus/plugins/cactus-maven-1.7dev-20040815.jar";

        assertLayout( path, groupId, artifactId, version, null, type );
    }

    /**
     * [MRM-562] Artifact type "maven-plugin" is not detected correctly in .toArtifactReference() methods.
     */
    public void testGoodDetectPluginGeronimoPackaging()
        throws LayoutException
    {
        String groupId = "geronimo";
        String artifactId = "geronimo-packaging-plugin";
        String version = "1.0.1";
        String type = "maven-plugin";
        String path = "geronimo/plugins/geronimo-packaging-plugin-1.0.1.jar";

        assertLayout( path, groupId, artifactId, version, null, type );
    }

    /**
     * [MRM-594] add some hook in LegacyPathParser to allow exceptions in artifact resolution
	 * @since 1.1
     */
    public void testCustomExceptionsInArtifactResolution()
        throws LayoutException
    {
        String groupId = "jaxen";
        String artifactId = "jaxen";
        String version = "1.0-FCS";
        String type = "jar";
        String classifier = "full";
        String path = "jaxen/jars/jaxen-1.0-FCS-full.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    /**
     * Perform a path to artifact reference lookup, and verify the results.
     */
    private void assertLayout( String path, String groupId, String artifactId, String version, String classifier, String type )
        throws LayoutException
    {
        // Path to Artifact Reference.
        ArtifactReference testReference = parser.toArtifactReference( path );
        assertArtifactReference( testReference, groupId, artifactId, version, classifier, type );
    }

    private void assertArtifactReference( ArtifactReference actualReference, String groupId, String artifactId,
                                          String version, String classifier, String type )
    {
        String expectedId = "ArtifactReference - " + groupId + ":" + artifactId + ":" + version + ":" + classifier + ":" + type;

        assertNotNull( expectedId + " - Should not be null.", actualReference );

        assertEquals( expectedId + " - Group ID", groupId, actualReference.getGroupId() );
        assertEquals( expectedId + " - Artifact ID", artifactId, actualReference.getArtifactId() );
        assertEquals( expectedId + " - Version ID", version, actualReference.getVersion() );
        assertEquals( expectedId + " - classifier", classifier, actualReference.getClassifier() );
        assertEquals( expectedId + " - Type", type, actualReference.getType() );
    }

    protected void assertBadPath( String path, String reason )
    {
        try
        {
            parser.toArtifactReference( path );
            fail( "Should have thrown a LayoutException on the invalid path [" + path + "] because of [" + reason + "]" );
        }
        catch ( LayoutException e )
        {
            /* expected path */
        }
    }
}
