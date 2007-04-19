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
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.LayoutException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * DefaultBidirectionalRepositoryLayoutTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DefaultBidirectionalRepositoryLayoutTest
    extends AbstractBidirectionalRepositoryLayoutTestCase
{
    class LayoutExample
    {
        public String groupId;

        public String artifactId;

        public String version;

        public String classifier;

        public String type;

        public String path;

        public LayoutExample( String groupId, String artifactId, String version, String classifier, String type )
        {
            super();
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.classifier = classifier;
            this.type = type;
        }

        public boolean isSuitableForArtifactTest()
        {
            return ( this.type != null ) && ( this.classifier != null ) && ( this.version != null );
        }

        public boolean isSuitableForVersionedTest()
        {
            return ( this.type == null ) && ( this.classifier == null ) && ( this.version != null );
        }

        public boolean isSuitableForProjectTest()
        {
            return ( this.type == null ) && ( this.classifier == null ) && ( this.version == null );
        }
    }

    class InvalidExample
    {
        public String path;

        public String reason;

        public boolean hasFilename;

        public InvalidExample( String path, boolean hasFilename, String reason )
        {
            super();
            this.path = path;
            this.hasFilename = hasFilename;
            this.reason = reason;
        }
    }

    private BidirectionalRepositoryLayout layout;

    public List /*<LayoutExample>*/getGoodExamples()
    {
        List ret = new ArrayList();

        LayoutExample example;

        // Artifact References
        example = new LayoutExample( "com.foo", "foo-tool", "1.0", "", "jar" );
        example.path = "com/foo/foo-tool/1.0/foo-tool-1.0.jar";
        ret.add( example );

        example = new LayoutExample( "com.foo", "foo-client", "1.0", "", "ejb-client" );
        example.path = "com/foo/foo-client/1.0/foo-client-1.0.jar";
        ret.add( example );

        example = new LayoutExample( "com.foo.lib", "foo-lib", "2.1-alpha-1", "sources", "java-source" );
        example.path = "com/foo/lib/foo-lib/2.1-alpha-1/foo-lib-2.1-alpha-1-sources.jar";
        ret.add( example );

        example = new LayoutExample( "com.foo", "foo-connector", "2.1-20060822.123456-35", "", "jar" );
        example.path = "com/foo/foo-connector/2.1-SNAPSHOT/foo-connector-2.1-20060822.123456-35.jar";
        ret.add( example );

        example = new LayoutExample( "org.apache.maven.test", "get-metadata-snapshot", "1.0-20050831.101112-1", "",
                                     "jar" );
        example.path = "org/apache/maven/test/get-metadata-snapshot/1.0-SNAPSHOT/get-metadata-snapshot-1.0-20050831.101112-1.jar";
        ret.add( example );

        example = new LayoutExample( "commons-lang", "commons-lang", "2.1", "", "jar" );
        example.path = "commons-lang/commons-lang/2.1/commons-lang-2.1.jar";
        ret.add( example );

        example = new LayoutExample( "com.foo", "foo-tool", "1.0", "", "jar" );
        example.path = "com/foo/foo-tool/1.0/foo-tool-1.0.jar";
        ret.add( example );

        // Versioned References (done here by setting classifier and type to null)
        example = new LayoutExample( "com.foo", "foo-tool", "1.0", null, null );
        example.path = "com/foo/foo-tool/1.0/foo-tool-1.0.jar";
        ret.add( example );

        example = new LayoutExample( "com.foo", "foo-tool", "1.0", null, null );
        example.path = "com/foo/foo-tool/1.0/";
        ret.add( example );

        example = new LayoutExample( "com.foo", "foo-tool", "1.0", null, null );
        example.path = "com/foo/foo-tool/1.0";
        ret.add( example );

        example = new LayoutExample( "com.foo", "foo-connector", "2.1-20060822.123456-35", null, null );
        example.path = "com/foo/foo-connector/2.1-SNAPSHOT/foo-connector-2.1-20060822.123456-35.jar";
        ret.add( example );

        example = new LayoutExample( "com.foo", "foo-connector", "2.1-20060822.123456-35", null, null );
        example.path = "com/foo/foo-connector/2.1-SNAPSHOT/";
        ret.add( example );

        example = new LayoutExample( "com.foo", "foo-connector", "2.1-20060822.123456-35", null, null );
        example.path = "com/foo/foo-connector/2.1-SNAPSHOT";
        ret.add( example );

        return ret;
    }

    public List /*<InvalidExample>*/getInvalidPaths()
    {
        List ret = new ArrayList();

        InvalidExample example;

        example = new InvalidExample( "invalid/invalid/1/invalid-1", false, "missing type" );
        ret.add( example );

        example = new InvalidExample( "invalid/invalid/1.0-SNAPSHOT/invalid-1.0.jar", true,
                                      "non snapshot artifact inside of a snapshot dir" );
        ret.add( example );

        example = new InvalidExample( "invalid/invalid-1.0.jar", true, "path is too short" );
        ret.add( example );

        example = new InvalidExample( "invalid/invalid/1.0-20050611.123456-1/invalid-1.0-20050611.123456-1.jar", true,
                                      "Timestamped Snapshot artifact not inside of an Snapshot dir" );
        ret.add( example );

        example = new InvalidExample( "invalid/invalid/1.0/invalid-2.0.jar", true,
                                      "version mismatch between path and artifact" );
        ret.add( example );

        example = new InvalidExample( "invalid/invalid/1.0/invalid-1.0b.jar", true,
                                      "version mismatch between path and artifact" );
        ret.add( example );

        example = new InvalidExample( "org/apache/maven/test/1.0-SNAPSHOT/wrong-artifactId-1.0-20050611.112233-1.jar",
                                      true, "wrong artifact id" );

        return ret;
    }

    public void testArtifactToPath()
    {
        Iterator it = getGoodExamples().iterator();
        while ( it.hasNext() )
        {
            LayoutExample example = (LayoutExample) it.next();
            if ( example.isSuitableForArtifactTest() )
            {
                ArchivaArtifact artifact = createArtifact( example.groupId, example.artifactId, example.version,
                                                           example.classifier, example.type );
                assertEquals( "Artifact <" + artifact + "> to path:", example.path, layout.toPath( artifact ) );
            }
        }
    }

    public void testArtifactReferenceToPath()
    {
        Iterator it = getGoodExamples().iterator();
        while ( it.hasNext() )
        {
            LayoutExample example = (LayoutExample) it.next();
            if ( example.isSuitableForArtifactTest() )
            {
                ArtifactReference reference = new ArtifactReference();
                reference.setGroupId( example.groupId );
                reference.setArtifactId( example.artifactId );
                reference.setVersion( example.version );
                reference.setClassifier( example.classifier );
                reference.setType( example.type );

                assertEquals( "ArtifactReference <" + reference + "> to path:", example.path, layout.toPath( reference ) );
            }
        }
    }

    public void testVersionedReferenceToPath()
    {
        Iterator it = getGoodExamples().iterator();
        while ( it.hasNext() )
        {
            LayoutExample example = (LayoutExample) it.next();
            if ( example.isSuitableForVersionedTest() && example.isSuitableForArtifactTest() )
            {
                VersionedReference reference = new VersionedReference();
                reference.setGroupId( example.groupId );
                reference.setArtifactId( example.artifactId );
                reference.setVersion( example.version );

                assertEquals( "VersionedReference <" + reference + "> to path:", example.path, layout
                    .toPath( reference ) );
            }
        }
    }

    public void testProjectReferenceToPath()
    {
        Iterator it = getGoodExamples().iterator();
        while ( it.hasNext() )
        {
            LayoutExample example = (LayoutExample) it.next();
            if ( example.isSuitableForProjectTest() && example.isSuitableForVersionedTest()
                && example.isSuitableForArtifactTest() )
            {
                ProjectReference reference = new ProjectReference();
                reference.setGroupId( example.groupId );
                reference.setArtifactId( example.artifactId );

                assertEquals( "ProjectReference <" + reference + "> to path:", example.path, layout.toPath( reference ) );
            }
        }
    }

    public void testInvalidPathToArtifact()
    {
        Iterator it = getInvalidPaths().iterator();
        while ( it.hasNext() )
        {
            InvalidExample example = (InvalidExample) it.next();

            try
            {
                layout.toArtifact( example.path );
                fail( "Should have thrown a LayoutException on the invalid path [" + example.path + "] because of ["
                    + example.reason + "]" );
            }
            catch ( LayoutException e )
            {
                /* expected path */
            }
        }
    }

    public void testInvalidPathToArtifactReference()
    {
        Iterator it = getInvalidPaths().iterator();
        while ( it.hasNext() )
        {
            InvalidExample example = (InvalidExample) it.next();

            try
            {
                layout.toArtifactReference( example.path );
                fail( "Should have thrown a LayoutException on the invalid path [" + example.path + "] because of ["
                    + example.reason + "]" );
            }
            catch ( LayoutException e )
            {
                /* expected path */
            }
        }
    }

    public void testInvalidPathToVersionedReference()
    {
        Iterator it = getInvalidPaths().iterator();
        while ( it.hasNext() )
        {
            InvalidExample example = (InvalidExample) it.next();

            try
            {
                layout.toVersionedReference( example.path );
                if ( example.hasFilename )
                {
                    fail( "Should have thrown a LayoutException on the invalid path [" + example.path
                        + "] because of [" + example.reason + "]" );
                }
            }
            catch ( LayoutException e )
            {
                /* expected path */
            }
        }
    }

    public void testInvalidPathToProjectReference()
    {
        Iterator it = getInvalidPaths().iterator();
        while ( it.hasNext() )
        {
            InvalidExample example = (InvalidExample) it.next();

            try
            {
                layout.toProjectReference( example.path );
                if ( example.hasFilename )
                {
                    fail( "Should have thrown a LayoutException on the invalid path [" + example.path
                        + "] because of [" + example.reason + "]" );
                }
            }
            catch ( LayoutException e )
            {
                /* expected path */
            }
        }
    }

    public void testPathToArtifact()
        throws LayoutException
    {
        Iterator it = getGoodExamples().iterator();
        while ( it.hasNext() )
        {
            LayoutExample example = (LayoutExample) it.next();
            if ( example.isSuitableForArtifactTest() )
            {
                ArchivaArtifact artifact = layout.toArtifact( example.path );
                assertArtifact( artifact, example.groupId, example.artifactId, example.version, example.classifier,
                                example.type );
            }
        }
    }

    /* TODO: Fix layout object to pass test.
    public void testPathToArtifactReference()
        throws LayoutException
    {
        Iterator it = getGoodExamples().iterator();
        while ( it.hasNext() )
        {
            LayoutExample example = (LayoutExample) it.next();
            if ( example.isSuitableForArtifactTest() )
            {
                ArtifactReference reference = layout.toArtifactReference( example.path );
                assertArtifactReference( reference, example.groupId, example.artifactId, example.version,
                                         example.classifier, example.type );
            }
        }
    }
    */

    /* TODO: Fix layout object to pass test.
    public void testPathToVersionedReference()
        throws LayoutException
    {
        Iterator it = getGoodExamples().iterator();
        while ( it.hasNext() )
        {
            LayoutExample example = (LayoutExample) it.next();
            if ( example.isSuitableForVersionedTest() )
            {
                VersionedReference reference = layout.toVersionedReference( example.path );

                assertVersionedReference( reference, example.groupId, example.artifactId, example.version );
            }
        }
    }
    */

    public void testPathToProjectReference()
        throws LayoutException
    {
        Iterator it = getGoodExamples().iterator();
        while ( it.hasNext() )
        {
            LayoutExample example = (LayoutExample) it.next();
            if ( example.isSuitableForProjectTest() )
            {
                ProjectReference reference = layout.toProjectReference( example.path );

                assertProjectReference( reference, example.groupId, example.artifactId );
            }
        }
    }

    public void testRoundtripArtifactToPathToArtifact()
        throws LayoutException
    {
        Iterator it = getGoodExamples().iterator();
        while ( it.hasNext() )
        {
            LayoutExample example = (LayoutExample) it.next();
            if ( example.isSuitableForArtifactTest() )
            {
                ArchivaArtifact artifact = createArtifact( example.groupId, example.artifactId, example.version,
                                                           example.classifier, example.type );
                String testPath = layout.toPath( artifact );
                assertEquals( "Artifact <" + artifact + "> to path:", example.path, testPath );
                ArchivaArtifact testArtifact = layout.toArtifact( testPath );
                assertArtifact( testArtifact, artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                                artifact.getClassifier(), artifact.getType() );
            }
        }
    }

    public void testRoundtripPathToArtifactToPath()
        throws LayoutException
    {
        Iterator it = getGoodExamples().iterator();
        while ( it.hasNext() )
        {
            LayoutExample example = (LayoutExample) it.next();
            if ( example.isSuitableForArtifactTest() )
            {
                ArchivaArtifact artifact = layout.toArtifact( example.path );
                assertArtifact( artifact, example.groupId, example.artifactId, example.version, example.classifier,
                                example.type );
                String testPath = layout.toPath( artifact );
                assertEquals( "Artifact <" + artifact + "> to path:", example.path, testPath );
            }
        }
    }

    public void testTimestampedSnapshotRoundtrip()
        throws LayoutException
    {
        String originalPath = "org/apache/maven/test/get-metadata-snapshot/1.0-SNAPSHOT/get-metadata-snapshot-1.0-20050831.101112-1.jar";
        ArchivaArtifact artifact = layout.toArtifact( originalPath );
        assertArtifact( artifact, "org.apache.maven.test", "get-metadata-snapshot", "1.0-20050831.101112-1", "", "jar" );

        assertEquals( originalPath, layout.toPath( artifact ) );

        ArtifactReference aref = new ArtifactReference();
        aref.setGroupId( artifact.getGroupId() );
        aref.setArtifactId( artifact.getArtifactId() );
        aref.setVersion( artifact.getVersion() );
        aref.setClassifier( artifact.getClassifier() );
        aref.setType( artifact.getType() );

        assertEquals( originalPath, layout.toPath( aref ) );
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();

        layout = (BidirectionalRepositoryLayout) lookup( BidirectionalRepositoryLayout.class.getName(), "default" );
    }
}
