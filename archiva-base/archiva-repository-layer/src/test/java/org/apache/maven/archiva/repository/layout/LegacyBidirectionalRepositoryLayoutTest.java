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
 * LegacyBidirectionalRepositoryLayoutTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class LegacyBidirectionalRepositoryLayoutTest
    extends AbstractBidirectionalRepositoryLayoutTestCase
{
    private BidirectionalRepositoryLayout layout;

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
     * [MRM-432] Oddball version spec.
     * Example of an oddball / unusual version spec.
     * @throws LayoutException 
     */
    /* TODO: Re-enabled in the future. 
    public void testGoodButOddVersionSpecGanymedSsh2()
        throws LayoutException
    {
        String groupId = "ch.ethz.ganymed";
        String artifactId = "ganymed-ssh2";
        String version = "build210";
        String classifier = null;
        String type = "jar";
        String path = "ch.ethz.ganymed/jars/ganymed-ssh2-build210.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }
    */

    /** 
     * [MRM-432] Oddball version spec.
     * Example of an oddball / unusual version spec.
     * @throws LayoutException 
     */
    /* TODO: Re-enabled in the future. 
    public void testGoodButOddVersionSpecJavaxComm()
        throws LayoutException
    {
        String groupId = "javax";
        String artifactId = "comm";
        String version = "3.0-u1";
        String classifier = null;
        String type = "jar";
        String path = "javax/jars/comm-3.0-u1.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }
    */

    /** 
     * [MRM-432] Oddball version spec.
     * Example of an oddball / unusual version spec.
     * @throws LayoutException 
     */
    /* TODO: Re-enabled in the future. 
    public void testGoodButOddVersionSpecJavaxPersistence()
        throws LayoutException
    {
        String groupId = "javax.persistence";
        String artifactId = "ejb";
        String version = "3.0-public_review";
        String classifier = null;
        String type = "jar";
        String path = "javax.persistence/jars/ejb-3.0-public_review.jar";

        /* 
         * The version id of "public_review" can cause problems. is it part of
         * the version spec? or the classifier?
         * /

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }
    */

    public void testGoodCommonsLang()
        throws LayoutException
    {
        String groupId = "commons-lang";
        String artifactId = "commons-lang";
        String version = "2.1";
        String classifier = null;
        String type = "jar";
        String path = "commons-lang/jars/commons-lang-2.1.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    public void testGoodDerby()
        throws LayoutException
    {
        String groupId = "org.apache.derby";
        String artifactId = "derby";
        String version = "10.2.2.0";
        String classifier = null;
        String type = "jar";
        String path = "org.apache.derby/jars/derby-10.2.2.0.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    /**
     * Test the ejb-client type spec.
     * Type specs are not a 1 to 1 map to the extension. 
     * This tests that effect.
     * @throws LayoutException 
     */
    public void testGoodFooEjbClient()
        throws LayoutException
    {
        String groupId = "com.foo";
        String artifactId = "foo-client";
        String version = "1.0";
        String classifier = null;
        String type = "ejb"; // oddball type-spec (should result in jar extension)
        String path = "com.foo/ejbs/foo-client-1.0.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

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
        String classifier = "javadoc";
        String type = "javadoc.jar";
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
        String classifier = "sources";
        String type = "java-source"; // oddball type-spec (should result in jar extension)
        String path = "com.foo.lib/java-sources/foo-lib-2.1-alpha-1-sources.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    public void testGoodFooTool()
        throws LayoutException
    {
        String groupId = "com.foo";
        String artifactId = "foo-tool";
        String version = "1.0";
        String classifier = null;
        String type = "jar";
        String path = "com.foo/jars/foo-tool-1.0.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    public void testGoodGeronimoEjbSpec()
        throws LayoutException
    {
        String groupId = "org.apache.geronimo.specs";
        String artifactId = "geronimo-ejb_2.1_spec";
        String version = "1.0.1";
        String classifier = null;
        String type = "jar";
        String path = "org.apache.geronimo.specs/jars/geronimo-ejb_2.1_spec-1.0.1.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
    }

    public void testGoodLdapClientsPom()
        throws LayoutException
    {
        String groupId = "directory-clients";
        String artifactId = "ldap-clients";
        String version = "0.9.1-SNAPSHOT";
        String classifier = null;
        String type = "pom";
        String path = "directory-clients/poms/ldap-clients-0.9.1-SNAPSHOT.pom";

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
        String path = "org.apache.archiva.test/jars/redonkulous-3.1-beta-1-20050831.101112-42.jar";

        assertLayout( path, groupId, artifactId, version, classifier, type );
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

    protected void setUp()
        throws Exception
    {
        super.setUp();

        layout = (BidirectionalRepositoryLayout) lookup( BidirectionalRepositoryLayout.class.getName(), "legacy" );
    }

}
