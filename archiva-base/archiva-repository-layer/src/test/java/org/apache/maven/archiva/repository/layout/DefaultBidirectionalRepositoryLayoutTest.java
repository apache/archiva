package org.apache.maven.archiva.repository.layout;

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

import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArtifactReference;

/**
 * DefaultBidirectionalRepositoryLayoutTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DefaultBidirectionalRepositoryLayoutTest
    extends AbstractBidirectionalRepositoryLayoutTestCase
{
    private BidirectionalRepositoryLayout layout;

    public void testBadPathMissingType()
    {
        assertBadPath( "invalid/invalid/1/invalid-1", "missing type" );
    }

    public void testBadPathReleaseInSnapshotDir()
    {
        assertBadPath( "invalid/invalid/1.0-SNAPSHOT/invalid-1.0.jar", "non snapshot artifact inside of a snapshot dir" );
    }

    public void testBadPathTimestampedSnapshotNotInSnapshotDir()
    {
        assertBadPath( "invalid/invalid/1.0-20050611.123456-1/invalid-1.0-20050611.123456-1.jar",
                       "Timestamped Snapshot artifact not inside of an Snapshot dir" );
    }

    public void testBadPathTooShort()
    {
        assertBadPath( "invalid/invalid-1.0.jar", "path is too short" );
    }

    public void testBadPathVersionMismatchA()
    {
        assertBadPath( "invalid/invalid/1.0/invalid-2.0.jar", "version mismatch between path and artifact" );
    }

    public void testBadPathVersionMismatchB()
    {
        assertBadPath( "invalid/invalid/1.0/invalid-1.0b.jar", "version mismatch between path and artifact" );
    }

    public void testBadPathWrongArtifactId()
    {
        assertBadPath( "org/apache/maven/test/1.0-SNAPSHOT/wrong-artifactId-1.0-20050611.112233-1.jar",
                       "wrong artifact id" );
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
        String classifier = null;
        String type = "jar";
        String path = "ch/ethz/ganymed/ganymed-ssh2/build210/ganymed-ssh2-build210.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
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
        String classifier = null;
        String type = "jar";
        String path = "javax/comm/3.0-u1/comm-3.0-u1.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
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
        String classifier = null;
        String type = "pom";
        String path = "maven/maven-test-plugin/1.8.2/maven-test-plugin-1.8.2.pom";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    /**
     * [MRM-486] Can not deploy artifact test.maven-arch:test-arch due to "No ArtifactID Detected"
     */
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
    public void testGoodDotNotationSameGroupIdAndArtifactId()
        throws LayoutException
    {
        String groupId = "com.company.department";
        String artifactId = "com.company.department.project";
        String version = "0.3";
        String classifier = null;
        String type = "pom";
        String path = "com/company/department/com.company.department.project/0.3/com.company.department.project-0.3.pom";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

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
        String classifier = null;
        String type = "ejb-client"; // oddball type-spec (should result in jar extension)
        String path = "com/foo/foo-client/1.0/foo-client-1.0.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }
    */

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
        String classifier = "sources";
        String type = "java-source"; // oddball type-spec (should result in jar extension)
        String path = "com/foo/lib/foo-lib/2.1-alpha-1/foo-lib-2.1-alpha-1-sources.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
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
        String classifier = null;
        String type = "jar";
        String path = "org/apache/archiva/test/redonkulous/3.1-beta-1-SNAPSHOT/redonkulous-3.1-beta-1-20050831.101112-42.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    public void testToArtifactOnEmptyPath()
    {
        try
        {
            layout.toArtifact( "" );
            fail( "Should have failed due to empty path." );
        }
        catch ( LayoutException e )
        {
            /* expected path */
        }
    }

    public void testToArtifactOnNullPath()
    {
        try
        {
            layout.toArtifact( null );
            fail( "Should have failed due to null path." );
        }
        catch ( LayoutException e )
        {
            /* expected path */
        }
    }

    public void testToArtifactReferenceOnEmptyPath()
    {
        try
        {
            layout.toArtifactReference( "" );
            fail( "Should have failed due to empty path." );
        }
        catch ( LayoutException e )
        {
            /* expected path */
        }
    }

    public void testToArtifactReferenceOnNullPath()
    {
        try
        {
            layout.toArtifactReference( null );
            fail( "Should have failed due to null path." );
        }
        catch ( LayoutException e )
        {
            /* expected path */
        }
    }

    public void testToPathOnNullArtifactReference()
    {
        try
        {
            ArtifactReference reference = null;
            layout.toPath( reference );
            fail( "Should have failed due to null artifact reference." );
        }
        catch ( IllegalArgumentException e )
        {
            /* expected path */
        }
    }

    public void testToPathOnNullArtifact()
    {
        try
        {
            ArchivaArtifact artifact = null;
            layout.toPath( artifact );
            fail( "Should have failed due to null artifact." );
        }
        catch ( IllegalArgumentException e )
        {
            /* expected path */
        }
    }

    protected void assertBadPath( String path, String reason )
    {
        try
        {
            layout.toArtifact( path );
            fail( "Should have thrown a LayoutException on the invalid path [" + path + "] because of [" + reason + "]" );
        }
        catch ( LayoutException e )
        {
            /* expected path */
        }
    }

    /**
     * Perform a roundtrip through the layout routines to determine success.
     */
    private void assertLayout( String path, String groupId, String artifactId, String version, String classifier,
                               String type )
        throws LayoutException
    {
        ArchivaArtifact expectedArtifact = createArtifact( groupId, artifactId, version, classifier, type );

        // --- Artifact Tests.
        // Artifact to Path 
        assertEquals( "Artifact <" + expectedArtifact + "> to path:", path, layout.toPath( expectedArtifact ) );

        // Path to Artifact.
        ArchivaArtifact testArtifact = layout.toArtifact( path );
        assertArtifact( testArtifact, groupId, artifactId, version, classifier, type );

        // And back again, using test Artifact from previous step.
        assertEquals( "Artifact <" + expectedArtifact + "> to path:", path, layout.toPath( testArtifact ) );

        // --- Artifact Reference Tests

        // Path to Artifact Reference.
        ArtifactReference testReference = layout.toArtifactReference( path );
        assertArtifactReference( testReference, groupId, artifactId, version, classifier, type );

        // And back again, using test Reference from previous step.
        assertEquals( "Artifact <" + expectedArtifact + "> to path:", path, layout.toPath( testReference ) );
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        layout = (BidirectionalRepositoryLayout) lookup( BidirectionalRepositoryLayout.class.getName(), "default" );
    }
}
