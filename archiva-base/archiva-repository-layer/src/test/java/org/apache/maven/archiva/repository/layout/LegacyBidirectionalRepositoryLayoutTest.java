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
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;

/**
 * LegacyBidirectionalRepositoryLayoutTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class LegacyBidirectionalRepositoryLayoutTest extends AbstractBidirectionalRepositoryLayoutTestCase
{
    private BidirectionalRepositoryLayout layout;

    protected void setUp() throws Exception
    {
        super.setUp();

        layout = (BidirectionalRepositoryLayout) lookup( BidirectionalRepositoryLayout.class.getName(), "legacy" );
    }

    public void testToPathBasic()
    {
        ArchivaArtifact artifact = createArtifact( "com.foo", "foo-tool", "1.0", "", "jar" );

        assertEquals( "com.foo/jars/foo-tool-1.0.jar", layout.toPath( artifact ) );
    }

    public void testToPathEjbClient()
    {
        ArchivaArtifact artifact = createArtifact( "com.foo", "foo-client", "1.0", "", "ejb-client" );

        assertEquals( "com.foo/ejbs/foo-client-1.0.jar", layout.toPath( artifact ) );
    }

    public void testToPathWithClassifier()
    {
        ArchivaArtifact artifact = createArtifact( "com.foo.lib", "foo-lib", "2.1-alpha-1", "sources", "jar" );

        assertEquals( "com.foo.lib/javadoc.jars/foo-lib-2.1-alpha-1-sources.jar", layout.toPath( artifact ) );
    }

    public void testToPathUsingUniqueSnapshot()
    {
        ArchivaArtifact artifact = createArtifact( "com.foo", "foo-connector", "2.1-20060822.123456-35", "", "jar" );

        assertEquals( "com.foo/jars/foo-connector-2.1-20060822.123456-35.jar", layout.toPath( artifact ) );
    }

    public void testToArtifactBasicSimpleGroupId() throws LayoutException
    {
        ArchivaArtifact artifact = layout.toArtifact( "commons-lang/jars/commons-lang-2.1.jar" );
        assertArtifact( artifact, "commons-lang", "commons-lang", "2.1", "", "jar" );
    }

    public void testToArtifactBasicLongGroupId() throws LayoutException
    {
        ArchivaArtifact artifact = layout.toArtifact( "org.apache.derby/jars/derby-10.2.2.0.jar" );
        assertArtifact( artifact, "org.apache.derby", "derby", "10.2.2.0", "", "jar" );
    }

    public void testToArtifactLongGroupId() throws LayoutException
    {
        ArchivaArtifact artifact = layout.toArtifact( "org.apache.geronimo.specs/jars/geronimo-ejb_2.1_spec-1.0.1.jar" );
        assertArtifact( artifact, "org.apache.geronimo.specs", "geronimo-ejb_2.1_spec", "1.0.1", "", "jar" );
    }

    public void testToArtifactEjbClient() throws LayoutException
    {
        ArchivaArtifact artifact = layout.toArtifact( "org.apache.beehive/jars/beehive-ejb-control-1.0.1.jar" );
        // The type is correct. as we cannot possibly know this is an ejb client without parsing the pom
        assertArtifact( artifact, "org.apache.beehive", "beehive-ejb-control", "1.0.1", "", "jar" );
    }

    public void testToArtifactWithClassifier() throws LayoutException
    {
        ArchivaArtifact artifact = layout.toArtifact( "commons-lang/jars/commons-lang-2.3-sources.jar" );
        // The 'java-source' type is correct.  You might be thinking of extension, which we are not testing here.
        assertArtifact( artifact, "commons-lang", "commons-lang", "2.3", "sources", "java-source" );
    }

    public void testToArtifactSnapshot() throws LayoutException
    {
        ArchivaArtifact artifact = layout.toArtifact( "directory-clients/poms/ldap-clients-0.9.1-SNAPSHOT.pom" );
        assertSnapshotArtifact( artifact, "directory-clients", "ldap-clients", "0.9.1-SNAPSHOT", "", "pom" );
    }
    
    public void testInvalidNoType()
    {
        try
        {
            layout.toArtifact( "invalid/invalid/1/invalid-1" );
            fail( "Should have detected no type." );
        }
        catch ( LayoutException e )
        {
            /* expected path */
        }
    }
    
    public void testInvalidArtifactPackaging()
    {
        try
        {
            layout.toArtifact( "org.apache.maven.test/jars/artifactId-1.0.jar.md5" );
            fail( "Should have detected wrong package extension." );
        }
        catch ( LayoutException e )
        {
            /* expected path */
        }
    }
    
    public void testInvalidNoArtifactId()
    {
        try
        {
            layout.toArtifact( "groupId/jars/-1.0.jar" );
            fail( "Should have detected artifactId is missing" );
        }
        catch ( LayoutException e )
        {
            /* expected path */
        }
        
        try
        {
            layout.toArtifact( "groupId/jars/1.0.jar" );
            fail( "Should have detected artifactId is missing" );
        }
        catch ( LayoutException e )
        {
            /* expected path */
        }
    }
}
