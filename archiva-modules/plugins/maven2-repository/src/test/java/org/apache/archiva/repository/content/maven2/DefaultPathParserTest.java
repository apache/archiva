package org.apache.archiva.repository.content.maven2;

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

import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.repository.content.PathParser;
import org.apache.archiva.repository.layout.LayoutException;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.Assert.*;

/**
 * DefaultPathParserTest
 *
 * TODO: move to path translator tests
 *
 *
 */
@RunWith ( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration ( { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public class DefaultPathParserTest
{
    private PathParser parser = new DefaultPathParser();

    @Test
    public void testBadPathMissingType()
    {
        // TODO: should we allow this instead?
        assertBadPath( "invalid/invalid/1/invalid-1", "missing type" );
    }

    @Test
    public void testBadPathReleaseInSnapshotDir()
    {
        assertBadPath( "invalid/invalid/1.0-SNAPSHOT/invalid-1.0.jar",
                       "non snapshot artifact inside of a snapshot dir" );
    }

    @Test
    public void testBadPathTimestampedSnapshotNotInSnapshotDir()
    {
        assertBadPath( "invalid/invalid/1.0-20050611.123456-1/invalid-1.0-20050611.123456-1.jar",
                       "Timestamped Snapshot artifact not inside of an Snapshot dir" );
    }

    @Test
    public void testBadPathTooShort()
    {
        assertBadPath( "invalid/invalid-1.0.jar", "path is too short" );
    }

    @Test
    public void testBadPathVersionMismatchA()
    {
        assertBadPath( "invalid/invalid/1.0/invalid-2.0.jar", "version mismatch between path and artifact" );
    }

    @Test
    public void testBadPathVersionMismatchB()
    {
        assertBadPath( "invalid/invalid/1.0/invalid-1.0b.jar", "version mismatch between path and artifact" );
    }

    @Test
    public void testBadPathWrongArtifactId()
    {
        assertBadPath( "org/apache/maven/test/1.0-SNAPSHOT/wrong-artifactId-1.0-20050611.112233-1.jar",
                       "wrong artifact id" );
    }

    /**
     * [MRM-481] Artifact requests with a .xml.zip extension fail with a 404 Error
     */
    @Test
    public void testGoodButDualExtensions()
        throws LayoutException
    {
        String groupId = "org.project";
        String artifactId = "example-presentation";
        String version = "3.2";
        String classifier = null;
        String type = "xml.zip";
        String path = "org/project/example-presentation/3.2/example-presentation-3.2.xml.zip";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    @Test
    public void testGoodButDualExtensionsWithClassifier()
        throws LayoutException
    {
        String groupId = "org.project";
        String artifactId = "example-presentation";
        String version = "3.2";
        String classifier = "extras";
        String type = "xml.zip";
        String path = "org/project/example-presentation/3.2/example-presentation-3.2-extras.xml.zip";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    @Test
    public void testGoodButDualExtensionsTarGz()
        throws LayoutException
    {
        String groupId = "org.project";
        String artifactId = "example-distribution";
        String version = "1.3";
        String classifier = null;
        String type = "tar.gz"; // no longer using distribution-tgz / distribution-zip in maven 2
        String path = "org/project/example-distribution/1.3/example-distribution-1.3.tar.gz";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    @Test
    public void testGoodButDualExtensionsTarGzAndClassifier()
        throws LayoutException
    {
        String groupId = "org.project";
        String artifactId = "example-distribution";
        String version = "1.3";
        String classifier = "bin";
        String type = "tar.gz"; // no longer using distribution-tgz / distribution-zip in maven 2
        String path = "org/project/example-distribution/1.3/example-distribution-1.3-bin.tar.gz";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    /**
     * [MRM-432] Oddball version spec.
     * Example of an oddball / unusual version spec.
     *
     * @throws org.apache.archiva.repository.layout.LayoutException
     */
    @Test
    public void testGoodButOddVersionSpecGanymedSsh2()
        throws LayoutException
    {
        String groupId = "ch.ethz.ganymed";
        String artifactId = "ganymed-ssh2";
        String version = "build210";
        String classifier = null;
        String type = "jar";
        String path = "ch/ethz/ganymed/ganymed-ssh2/build210/ganymed-ssh2-build210.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    /**
     * [MRM-432] Oddball version spec.
     * Example of an oddball / unusual version spec.
     *
     * @throws org.apache.archiva.repository.layout.LayoutException
     */
    @Test
    public void testGoodButOddVersionSpecJavaxComm()
        throws LayoutException
    {
        String groupId = "javax";
        String artifactId = "comm";
        String version = "3.0-u1";
        String classifier = null;
        String type = "jar";
        String path = "javax/comm/3.0-u1/comm-3.0-u1.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    /**
     * Test the ejb-client type spec.
     * Type specs are not a 1 to 1 map to the extension.
     * This tests that effect.
     * @throws org.apache.archiva.repository.layout.LayoutException
     */
    /* TODO: Re-enabled in the future.
    public void testGoodFooEjbClient()
        throws LayoutException
    {
        String groupId = "com.foo";
        String artifactId = "foo-client";
        String version = "1.0";
        String classifier = null;
        String type = "ejb-client"; // oddball type-spec (should result in jar extension)
        String path = "com/foo/foo-client/1.0/foo-client-1.0.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }
    */

    /**
     * [MRM-432] Oddball version spec.
     * Example of an oddball / unusual version spec.
     *
     * @throws org.apache.archiva.repository.layout.LayoutException
     */
    @Test
    public void testGoodButOddVersionSpecJavaxPersistence()
        throws LayoutException
    {
        String groupId = "javax.persistence";
        String artifactId = "ejb";
        String version = "3.0-public_review";
        String classifier = null;
        String type = "jar";
        String path = "javax/persistence/ejb/3.0-public_review/ejb-3.0-public_review.jar";

        /*
         * The version id of "public_review" can cause problems. is it part of
         * the version spec? or the classifier?
         * Since the path spec below shows it in the path, then it is really
         * part of the version spec.
         */

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    @Test
    public void testGoodComFooTool()
        throws LayoutException
    {
        String groupId = "com.foo";
        String artifactId = "foo-tool";
        String version = "1.0";
        String classifier = null;
        String type = "jar";
        String path = "com/foo/foo-tool/1.0/foo-tool-1.0.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    @Test
    public void testGoodCommonsLang()
        throws LayoutException
    {
        String groupId = "commons-lang";
        String artifactId = "commons-lang";
        String version = "2.1";
        String classifier = null;
        String type = "jar";
        String path = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    @Test
    public void testWindowsPathSeparator()
        throws LayoutException
    {
        String groupId = "commons-lang";
        String artifactId = "commons-lang";
        String version = "2.1";
        String classifier = null;
        String type = "jar";
        String path = "commons-lang\\commons-lang/2.1\\commons-lang-2.1.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    /**
     * [MRM-486] Can not deploy artifact test.maven-arch:test-arch due to "No ArtifactID Detected"
     */
    @Test
    public void testGoodDashedArtifactId()
        throws LayoutException
    {
        String groupId = "test.maven-arch";
        String artifactId = "test-arch";
        String version = "2.0.3-SNAPSHOT";
        String classifier = null;
        String type = "pom";
        String path = "test/maven-arch/test-arch/2.0.3-SNAPSHOT/test-arch-2.0.3-SNAPSHOT.pom";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    /**
     * It may seem odd, but this is a valid artifact.
     */
    @Test
    public void testGoodDotNotationArtifactId()
        throws LayoutException
    {
        String groupId = "com.company.department";
        String artifactId = "com.company.department";
        String version = "0.2";
        String classifier = null;
        String type = "pom";
        String path = "com/company/department/com.company.department/0.2/com.company.department-0.2.pom";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    /**
     * It may seem odd, but this is a valid artifact.
     */
    @Test
    public void testGoodDotNotationSameGroupIdAndArtifactId()
        throws LayoutException
    {
        String groupId = "com.company.department";
        String artifactId = "com.company.department.project";
        String version = "0.3";
        String classifier = null;
        String type = "pom";
        String path =
            "com/company/department/com.company.department.project/0.3/com.company.department.project-0.3.pom";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    /**
     * Test the classifier, and java-source type spec.
     *
     * @throws org.apache.archiva.repository.layout.LayoutException
     */
    @Test
    public void testGoodFooLibSources()
        throws LayoutException
    {
        String groupId = "com.foo.lib";
        String artifactId = "foo-lib";
        String version = "2.1-alpha-1";
        String classifier = "sources";
        String type = "java-source"; // oddball type-spec (should result in jar extension)
        String path = "com/foo/lib/foo-lib/2.1-alpha-1/foo-lib-2.1-alpha-1-sources.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    /**
     * A timestamped versioned artifact, should reside in a SNAPSHOT baseversion directory.
     *
     * @throws org.apache.archiva.repository.layout.LayoutException
     */
    @Test
    public void testGoodSnapshotMavenTest()
        throws LayoutException
    {
        String groupId = "org.apache.archiva.test";
        String artifactId = "redonkulous";
        String version = "3.1-beta-1-20050831.101112-42";
        String classifier = null;
        String type = "jar";
        String path =
            "org/apache/archiva/test/redonkulous/3.1-beta-1-SNAPSHOT/redonkulous-3.1-beta-1-20050831.101112-42.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    /**
     * A timestamped versioned artifact, should reside in a SNAPSHOT baseversion directory.
     *
     * @throws org.apache.archiva.repository.layout.LayoutException
     */
    @Test
    public void testGoodLongSnapshotMavenTest()
        throws LayoutException
    {
        String groupId = "a.group.id";
        String artifactId = "artifact-id";
        String version = "1.0-abc-1.1-20080221.062205-9";
        String classifier = null;
        String type = "pom";
        String path = "a/group/id/artifact-id/1.0-abc-1.1-SNAPSHOT/artifact-id-1.0-abc-1.1-20080221.062205-9.pom";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    /**
     * A timestamped versioned artifact but without release version part. Like on axiom trunk.
     */
    @Test
    public void testBadSnapshotWithoutReleasePart()
    {
        assertBadPath( "org/apache/ws/commons/axiom/axiom/SNAPSHOT/axiom-20070912.093446-2.pom",
                       "snapshot version without release part" );
    }

    /**
     * A timestamped versioned artifact, should reside in a SNAPSHOT baseversion directory.
     *
     * @throws org.apache.archiva.repository.layout.LayoutException
     */
    @Test
    public void testClassifiedSnapshotMavenTest()
        throws LayoutException
    {
        String groupId = "a.group.id";
        String artifactId = "artifact-id";
        String version = "1.0-20070219.171202-34";
        String classifier = "test-sources";
        String type = "jar";
        String path = "a/group/id/artifact-id/1.0-SNAPSHOT/artifact-id-1.0-20070219.171202-34-test-sources.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    /**
     * [MRM-519] version identifiers within filename cause misidentification of version.
     * Example uses "test" in artifact Id, which is also part of the versionKeyword list.
     */
    @Test
    public void testGoodVersionKeywordInArtifactId()
        throws LayoutException
    {
        String groupId = "maven";
        String artifactId = "maven-test-plugin";
        String version = "1.8.2";
        String classifier = null;
        String type = "pom";
        String path = "maven/maven-test-plugin/1.8.2/maven-test-plugin-1.8.2.pom";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    /**
     * [MRM-562] Artifact type "maven-plugin" is not detected correctly in .toArtifactReference() methods.
     * Example uses "test" in artifact Id, which is also part of the versionKeyword list.
     */
    @Test
    public void testGoodDetectMavenTestPlugin()
        throws LayoutException
    {
        String groupId = "maven";
        String artifactId = "maven-test-plugin";
        String version = "1.8.2";
        String classifier = null;
        String type = "maven-plugin";
        String path = "maven/maven-test-plugin/1.8.2/maven-test-plugin-1.8.2.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    /**
     * [MRM-562] Artifact type "maven-plugin" is not detected correctly in .toArtifactReference() methods.
     */
    @Test
    public void testGoodDetectCoberturaMavenPlugin()
        throws LayoutException
    {
        String groupId = "org.codehaus.mojo";
        String artifactId = "cobertura-maven-plugin";
        String version = "2.1";
        String classifier = null;
        String type = "maven-plugin";
        String path = "org/codehaus/mojo/cobertura-maven-plugin/2.1/cobertura-maven-plugin-2.1.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    @Test
    public void testToArtifactOnEmptyPath()
    {
        try
        {
            parser.toArtifactReference( "" );
            fail( "Should have failed due to empty path." );
        }
        catch ( LayoutException e )
        {
            /* expected path */
        }
    }

    @Test
    public void testToArtifactOnNullPath()
    {
        try
        {
            parser.toArtifactReference( null );
            fail( "Should have failed due to null path." );
        }
        catch ( LayoutException e )
        {
            /* expected path */
        }
    }

    @Test
    public void testToArtifactReferenceOnEmptyPath()
    {
        try
        {
            parser.toArtifactReference( "" );
            fail( "Should have failed due to empty path." );
        }
        catch ( LayoutException e )
        {
            /* expected path */
        }
    }

    @Test
    public void testToArtifactReferenceOnNullPath()
    {
        try
        {
            parser.toArtifactReference( null );
            fail( "Should have failed due to null path." );
        }
        catch ( LayoutException e )
        {
            /* expected path */
        }
    }

    /**
     * Perform a path to artifact reference lookup, and verify the results.
     */
    private void assertLayout( String path, String groupId, String artifactId, String version, String classifier,
                               String type )
        throws LayoutException
    {
        // Path to Artifact Reference.
        ArtifactReference testReference = parser.toArtifactReference( path );
        assertArtifactReference( testReference, groupId, artifactId, version, classifier, type );
    }

    private void assertArtifactReference( ArtifactReference actualReference, String groupId, String artifactId,
                                          String version, String classifier, String type )
    {
        String expectedId =
            "ArtifactReference - " + groupId + ":" + artifactId + ":" + version + ":" + classifier + ":" + type;

        assertNotNull( expectedId + " - Should not be null.", actualReference );

        assertEquals( expectedId + " - Group ID", groupId, actualReference.getGroupId() );
        assertEquals( expectedId + " - Artifact ID", artifactId, actualReference.getArtifactId() );
        if ( StringUtils.isNotBlank( classifier ) )
        {
            assertEquals( expectedId + " - Classifier", classifier, actualReference.getClassifier() );
        }
        assertEquals( expectedId + " - Version ID", version, actualReference.getVersion() );
        assertEquals( expectedId + " - Type", type, actualReference.getType() );
    }

    private void assertBadPath( String path, String reason )
    {
        try
        {
            parser.toArtifactReference( path );
            fail(
                "Should have thrown a LayoutException on the invalid path [" + path + "] because of [" + reason + "]" );
        }
        catch ( LayoutException e )
        {
            /* expected path */
        }
    }
}
