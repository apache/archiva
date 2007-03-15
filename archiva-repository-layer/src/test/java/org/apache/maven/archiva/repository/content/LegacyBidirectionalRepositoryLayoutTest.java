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

        assertEquals( "com.foo/jars/foo-tool-1.0.jar", layout.pathOf( artifact ) );
    }

    public void testToPathEjbClient()
    {
        ArchivaArtifact artifact = createArtifact( "com.foo", "foo-client", "1.0", "", "ejb-client" );

        assertEquals( "com.foo/ejbs/foo-client-1.0.jar", layout.pathOf( artifact ) );
    }

    public void testToPathWithClassifier()
    {
        ArchivaArtifact artifact = createArtifact( "com.foo.lib", "foo-lib", "2.1-alpha-1", "sources", "jar" );

        assertEquals( "com.foo.lib/javadoc.jars/foo-lib-2.1-alpha-1-sources.jar", layout.pathOf( artifact ) );
    }

    public void testToPathUsingUniqueSnapshot()
    {
        ArchivaArtifact artifact = createArtifact( "com.foo", "foo-connector", "2.1-20060822.123456-35", "", "jar" );

        assertEquals( "com.foo/jars/foo-connector-2.1-20060822.123456-35.jar",
                      layout.pathOf( artifact ) );
    }
}
