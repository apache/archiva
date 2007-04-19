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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.VersionedReference;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

        public String pathArtifact;

        public String pathVersiond;

        public String pathProjectd;

        public LayoutExample( String groupId, String artifactId, String version, String classifier, String type )
        {
            super();
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.classifier = classifier;
            this.type = type;
        }

        public void setDelimitedPath( String delimPath )
        {
            // Silly Test Writer! Don't end the path with a slash!
            if ( delimPath.endsWith( "/" ) )
            {
                delimPath = delimPath.substring( 0, delimPath.length() - 1 );
            }

            String parts[] = StringUtils.split( delimPath, '|' );
            switch ( parts.length )
            {
                case 3:
                    this.pathArtifact = parts[0] + "/" + parts[1] + "/" + parts[2];
                case 2:
                    this.pathVersiond = parts[0] + "/" + parts[1] + "/maven-metadata.xml";
                case 1:
                    this.pathProjectd = parts[0] + "/maven-metadata.xml";
                    break;
                default:
                    fail( "Unknown number of path pieces, expected between 1 and 3, got <" + parts.length + "> on <"
                        + delimPath + ">" );
            }
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
        example = new LayoutExample( "com.foo", "foo-tool", "1.0", null, "jar" );
        example.setDelimitedPath( "com/foo/foo-tool|1.0|foo-tool-1.0.jar" );
        ret.add( example );

        example = new LayoutExample( "com.foo", "foo-client", "1.0", null, "ejb-client" );
        example.setDelimitedPath( "com/foo/foo-client|1.0|foo-client-1.0.jar" );
        ret.add( example );

        example = new LayoutExample( "com.foo.lib", "foo-lib", "2.1-alpha-1", "sources", "java-source" );
        example.setDelimitedPath( "com/foo/lib/foo-lib|2.1-alpha-1|foo-lib-2.1-alpha-1-sources.jar" );
        ret.add( example );

        example = new LayoutExample( "com.foo", "foo-connector", "2.1-20060822.123456-35", null, "jar" );
        example.setDelimitedPath( "com/foo/foo-connector/2.1-SNAPSHOT/foo-connector-2.1-20060822.123456-35.jar" );
        ret.add( example );

        example = new LayoutExample( "org.apache.maven.test", "get-metadata-snapshot", "1.0-20050831.101112-1", null,
                                     "jar" );
        example
            .setDelimitedPath( "org/apache/maven/test/get-metadata-snapshot|1.0-SNAPSHOT|get-metadata-snapshot-1.0-20050831.101112-1.jar" );
        ret.add( example );

        example = new LayoutExample( "commons-lang", "commons-lang", "2.1", null, "jar" );
        example.setDelimitedPath( "commons-lang/commons-lang|2.1|commons-lang-2.1.jar" );
        ret.add( example );

        example = new LayoutExample( "com.foo", "foo-tool", "1.0", null, "jar" );
        example.setDelimitedPath( "com/foo/foo-tool|1.0|foo-tool-1.0.jar" );
        ret.add( example );

        // Versioned References (done here by setting classifier and type to null)
        example = new LayoutExample( "com.foo", "foo-tool", "1.0", null, null );
        example.setDelimitedPath( "com/foo/foo-tool|1.0" );
        ret.add( example );

        example = new LayoutExample( "net.i.have.a.really.long.path.just.for.the.hell.of.it", "a", "1.1-alpha-1", null,
                                     null );
        example.setDelimitedPath( "net/i/have/a/really/long/path/just/for/the/hell/of/it/a|1.1-alpha-1" );
        ret.add( example );

        example = new LayoutExample( "com.foo", "foo-connector", "2.1-20060822.123456-35", null, null );
        example.setDelimitedPath( "com/foo/foo-connector|2.1-SNAPSHOT" );
        ret.add( example );

        example = new LayoutExample( "com.foo", "foo-connector", "2.1-SNAPSHOT", null, null );
        example.setDelimitedPath( "com/foo/foo-connector|2.1-SNAPSHOT" );
        ret.add( example );

        // Project References (done here by setting version, classifier, and type to null)
        example = new LayoutExample( "com.foo", "foo-tool", null, null, null );
        example.setDelimitedPath( "com/foo/foo-tool/" );
        ret.add( example );

        example = new LayoutExample( "net.i.have.a.really.long.path.just.for.the.hell.of.it", "a", null, null, null );
        example.setDelimitedPath( "net/i/have/a/really/long/path/just/for/the/hell/of/it/a/" );
        ret.add( example );

        example = new LayoutExample( "com.foo", "foo-connector", null, null, null );
        example.setDelimitedPath( "com/foo/foo-connector" );
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
                assertEquals( "Artifact <" + artifact + "> to path:", example.pathArtifact, layout.toPath( artifact ) );
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

                assertEquals( "ArtifactReference <" + reference + "> to path:", example.pathArtifact, layout
                    .toPath( reference ) );
            }
        }
    }

    public void testVersionedReferenceToPath()
    {
        Iterator it = getGoodExamples().iterator();
        while ( it.hasNext() )
        {
            LayoutExample example = (LayoutExample) it.next();
            if ( example.isSuitableForVersionedTest() || example.isSuitableForArtifactTest() )
            {
                VersionedReference reference = new VersionedReference();
                reference.setGroupId( example.groupId );
                reference.setArtifactId( example.artifactId );
                reference.setVersion( example.version );

                assertEquals( "VersionedReference <" + reference + "> to path:", example.pathVersiond, layout
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
            if ( example.isSuitableForProjectTest() || example.isSuitableForVersionedTest()
                || example.isSuitableForArtifactTest() )
            {
                ProjectReference reference = new ProjectReference();
                reference.setGroupId( example.groupId );
                reference.setArtifactId( example.artifactId );

                assertEquals( "ProjectReference <" + reference + "> to path:", example.pathProjectd, layout
                    .toPath( reference ) );
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
                ArchivaArtifact artifact = layout.toArtifact( example.pathArtifact );
                assertArtifact( artifact, example.groupId, example.artifactId, example.version, example.classifier,
                                example.type );
            }
        }
    }

    public void testPathToArtifactReference()
        throws LayoutException
    {
        Iterator it = getGoodExamples().iterator();
        while ( it.hasNext() )
        {
            LayoutExample example = (LayoutExample) it.next();
            if ( example.isSuitableForArtifactTest() )
            {
                ArtifactReference reference = layout.toArtifactReference( example.pathArtifact );
                assertArtifactReference( reference, example.groupId, example.artifactId, example.version,
                                         example.classifier, example.type );
            }
        }
    }

    public void testPathToVersionedReference()
        throws LayoutException
    {
        Iterator it = getGoodExamples().iterator();
        while ( it.hasNext() )
        {
            LayoutExample example = (LayoutExample) it.next();
            if ( example.isSuitableForVersionedTest() )
            {
                VersionedReference reference = layout.toVersionedReference( example.pathVersiond );
                
                String baseVersion = reference.getVersion();

                assertVersionedReference( reference, example.groupId, example.artifactId, baseVersion );
            }
        }
    }

    public void testPathToProjectReference()
        throws LayoutException
    {
        Iterator it = getGoodExamples().iterator();
        while ( it.hasNext() )
        {
            LayoutExample example = (LayoutExample) it.next();
            if ( example.isSuitableForProjectTest() )
            {
                ProjectReference reference = layout.toProjectReference( example.pathProjectd );

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
                assertEquals( "Artifact <" + artifact + "> to path:", example.pathArtifact, testPath );
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
                ArchivaArtifact artifact = layout.toArtifact( example.pathArtifact );
                assertArtifact( artifact, example.groupId, example.artifactId, example.version, example.classifier,
                                example.type );
                String testPath = layout.toPath( artifact );
                assertEquals( "Artifact <" + artifact + "> to path:", example.pathArtifact, testPath );
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
