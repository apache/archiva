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
 * LegacyBidirectionalRepositoryLayoutTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class LegacyBidirectionalRepositoryLayoutTest
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
                    this.pathVersiond = parts[0] + "/jars/maven-metadata.xml";
                case 1:
                    this.pathProjectd = parts[0] + "/jars/maven-metadata.xml";
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

        public boolean suitableForArtifactTests = true;
        public boolean suitableForVersionedTests = false;
        public boolean suitableForProjectTests = false;

        public InvalidExample( String path, String reason )
        {
            super();
            this.path = path;
            this.reason = reason;
        }
    }

    private BidirectionalRepositoryLayout layout;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        layout = (BidirectionalRepositoryLayout) lookup( BidirectionalRepositoryLayout.class.getName(), "legacy" );
    }

    public List /*<LayoutExample>*/getGoodExamples()
    {
        List ret = new ArrayList();

        LayoutExample example;

        // Artifact References
        example = new LayoutExample( "com.foo", "foo-tool", "1.0", null, "jar" );
        example.setDelimitedPath( "com.foo|jars|foo-tool-1.0.jar" );
        ret.add( example );

        example = new LayoutExample( "com.foo", "foo-client", "1.0", null, "ejb-client" );
        example.setDelimitedPath( "com.foo|ejbs|foo-client-1.0.jar" );
        ret.add( example );

        example = new LayoutExample( "com.foo.lib", "foo-lib", "2.1-alpha-1", "sources", "jar" );
        example.setDelimitedPath( "com.foo.lib|source.jars|foo-lib-2.1-alpha-1-sources.jar" );
        ret.add( example );

        example = new LayoutExample( "com.foo.lib", "foo-lib", "2.1-alpha-1", "javadoc", "jar" );
        example.setDelimitedPath( "com.foo.lib|javadoc.jars|foo-lib-2.1-alpha-1-javadoc.jar" );
        ret.add( example );
        
        example = new LayoutExample( "com.foo", "foo-connector", "2.1-20060822.123456-35", null, "jar" );
        example.setDelimitedPath( "com.foo|jars|foo-connector-2.1-20060822.123456-35.jar" );
        ret.add( example );

        example = new LayoutExample( "org.apache.maven.test", "get-metadata-snapshot", "1.0-20050831.101112-1", null,
                                     "jar" );
        example.setDelimitedPath( "org.apache.maven.test|jars|get-metadata-snapshot-1.0-20050831.101112-1.jar" );
        ret.add( example );

        example = new LayoutExample( "commons-lang", "commons-lang", "2.1", null, "jar" );
        example.setDelimitedPath( "commons-lang|jars|commons-lang-2.1.jar" );
        ret.add( example );

        example = new LayoutExample( "org.apache.derby", "derby", "10.2.2.0", null, "jar" );
        example.setDelimitedPath( "org.apache.derby|jars|derby-10.2.2.0.jar" );
        ret.add( example );

        example = new LayoutExample( "org.apache.geronimo.specs", "geronimo-ejb_2.1_spec", "1.0.1", null, "jar" );
        example.setDelimitedPath( "org.apache.geronimo.specs|jars|geronimo-ejb_2.1_spec-1.0.1.jar" );
        ret.add( example );

        example = new LayoutExample( "org.apache.beehive", "beehive-ejb-control", "1.0.1", null, "jar" );
        example.setDelimitedPath( "org.apache.beehive|jars|beehive-ejb-control-1.0.1.jar" );
        ret.add( example );

        example = new LayoutExample( "commons-lang", "commons-lang", "2.3", "sources", "jar" );
        example.setDelimitedPath( "commons-lang|source.jars|commons-lang-2.3-sources.jar" );
        ret.add( example );

        example = new LayoutExample( "directory-clients", "ldap-clients", "0.9.1-SNAPSHOT", null, "pom" );
        example.setDelimitedPath( "directory-clients|poms|ldap-clients-0.9.1-SNAPSHOT.pom" );
        ret.add( example );

        // Versioned References (done here by setting classifier and type to null)
        
        // TODO: Not sure how to represent a VersionedReference as a legacy path.

        // Project References (done here by setting version, classifier, and type to null)
        
        // TODO: Not sure how to represent a ProjectReference as a legacy path.

        return ret;
    }
    
    public List /*<InvalidExample>*/getInvalidPaths()
    {
        List ret = new ArrayList();

        InvalidExample example;

        example = new InvalidExample( "invalid/invalid/1/invalid-1", "missing type" );
        example.suitableForArtifactTests = true;
        example.suitableForVersionedTests = false;
        example.suitableForProjectTests = true;
        ret.add( example );

        example = new InvalidExample( "org.apache.maven.test/jars/artifactId-1.0.jar.md5", "wrong package extension" );
        example.suitableForArtifactTests = true;
        example.suitableForVersionedTests = false;
        example.suitableForProjectTests = false;
        ret.add( example );

        example = new InvalidExample( "groupId/jars/-1.0.jar", "artifactId is missing" );
        example.suitableForArtifactTests = true;
        example.suitableForVersionedTests = false;
        example.suitableForProjectTests = true;
        ret.add( example );

        example = new InvalidExample( "groupId/jars/1.0.jar", "artifactId is missing" );
        example.suitableForArtifactTests = true;
        example.suitableForVersionedTests = false;
        example.suitableForProjectTests = true;
        ret.add( example );

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
            
            if( !example.suitableForArtifactTests )
            {
                continue;
            }

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

            if( !example.suitableForVersionedTests )
            {
                continue;
            }

            try
            {
                layout.toVersionedReference( example.path );
                fail( "Should have thrown a LayoutException on the invalid path [" + example.path
                    + "] because of [" + example.reason + "]" );
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

            if( !example.suitableForProjectTests )
            {
                continue;
            }

            try
            {
                layout.toProjectReference( example.path );
                fail( "Should have thrown a LayoutException on the invalid path [" + example.path
                    + "] because of [" + example.reason + "]" );
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


}
