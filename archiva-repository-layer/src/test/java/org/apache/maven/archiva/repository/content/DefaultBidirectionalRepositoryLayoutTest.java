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

import org.apache.maven.archiva.repository.ArchivaArtifact;

/**
 * DefaultBidirectionalRepositoryLayoutTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DefaultBidirectionalRepositoryLayoutTest extends AbstractBidirectionalRepositoryLayoutTestCase
{
    private BidirectionalRepositoryLayout layout;

    protected void setUp() throws Exception
    {
        super.setUp();

        layout = (BidirectionalRepositoryLayout) lookup( BidirectionalRepositoryLayout.class.getName(), "default" );
    }

    public void testToPathBasic()
    {
        ArchivaArtifact artifact = createArtifact( "com.foo", "foo-tool", "1.0", "", "jar" );

        assertEquals( "com/foo/foo-tool/1.0/foo-tool-1.0.jar", layout.pathOf( artifact ) );
    }

    public void testToPathEjbClient()
    {
        ArchivaArtifact artifact = createArtifact( "com.foo", "foo-client", "1.0", "", "ejb-client" );

        assertEquals( "com/foo/foo-client/1.0/foo-client-1.0.jar", layout.pathOf( artifact ) );
    }

    public void testToPathWithClassifier()
    {
        ArchivaArtifact artifact = createArtifact( "com.foo.lib", "foo-lib", "2.1-alpha-1", "sources", "jar" );

        assertEquals( "com/foo/lib/foo-lib/2.1-alpha-1/foo-lib-2.1-alpha-1-sources.jar", layout.pathOf( artifact ) );
    }

    public void testToPathUsingUniqueSnapshot()
    {
        ArchivaArtifact artifact = createArtifact( "com.foo", "foo-connector", "2.1-20060822.123456-35", "", "jar" );

        assertEquals( "com/foo/foo-connector/2.1-SNAPSHOT/foo-connector-2.1-20060822.123456-35.jar",
                      layout.pathOf( artifact ) );
    }

    public void testToArtifactBasic()
    {
        ArchivaArtifact artifact = layout.toArtifact( "com/foo/foo-tool/1.0/foo-tool-1.0.jar" );
        assertArtifact( artifact, "com.foo", "foo-tool", "1.0", "", "jar" );
    }

    public void testToArtifactEjbClient()
    {
        ArchivaArtifact artifact = layout.toArtifact( "com/foo/foo-client/1.0/foo-client-1.0.jar" );
        // The type is correct. as we cannot possibly know this is an ejb client without parsing the pom
        assertArtifact( artifact, "com.foo", "foo-client", "1.0", "", "jar" );
    }

    public void testToArtifactWithClassifier()
    {
        ArchivaArtifact artifact =
            layout.toArtifact( "com/foo/lib/foo-lib/2.1-alpha-1/foo-lib-2.1-alpha-1-sources.jar" );
        assertArtifact( artifact, "com.foo.lib", "foo-lib", "2.1-alpha-1", "sources", "jar" );
    }
    
    public void testToArtifactUsingUniqueSnapshot()
    {
        ArchivaArtifact artifact =
            layout.toArtifact( "com/foo/foo-connector/2.1-SNAPSHOT/foo-connector-2.1-20060822.123456-35.jar" );
        assertSnapshotArtifact( artifact, "com.foo", "foo-connector", "2.1-20060822.123456-35", "", "jar" );
    }
}
