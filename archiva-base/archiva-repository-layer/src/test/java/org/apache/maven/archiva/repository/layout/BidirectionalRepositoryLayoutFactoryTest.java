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

/**
 * BidirectionalRepositoryLayoutFactoryTest
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class BidirectionalRepositoryLayoutFactoryTest
    extends AbstractBidirectionalRepositoryLayoutTestCase
{
    private BidirectionalRepositoryLayoutFactory factory;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        factory = (BidirectionalRepositoryLayoutFactory) lookup( BidirectionalRepositoryLayoutFactory.class.getName() );
    }

    public void testLayoutDefault()
        throws LayoutException
    {
        BidirectionalRepositoryLayout layout = factory.getLayout( "default" );
        assertNotNull( "Layout should not be null", layout );

        ArchivaArtifact artifact = createArtifact( "com.foo", "foo-tool", "1.0", "", "jar" );

        assertEquals( "com/foo/foo-tool/1.0/foo-tool-1.0.jar", layout.toPath( artifact ) );
    }

    public void testLayoutLegacy()
        throws LayoutException
    {
        BidirectionalRepositoryLayout layout = factory.getLayout( "legacy" );
        assertNotNull( "Layout should not be null", layout );

        ArchivaArtifact artifact = createArtifact( "com.foo", "foo-tool", "1.0", "", "jar" );

        assertEquals( "com.foo/jars/foo-tool-1.0.jar", layout.toPath( artifact ) );
    }

    public void testLayoutInvalid()
    {
        try
        {
            factory.getLayout( "-invalid-" );
            fail( "Should have thrown a LayoutException due to missing layout type." );
        }
        catch ( LayoutException e )
        {
            /* expected path */
        }
    }

    public void testFindLayoutForPath()
        throws LayoutException
    {
        BidirectionalRepositoryLayout layout =
            factory.getLayoutForPath( "javax/servlet/servlet-api/2.3/servlet-api-2.3.jar" );
        assertEquals( "default", layout.getId() );

        layout = factory.getLayoutForPath( "javax.servlet/jars/servlet-api-2.3.jar" );
        assertEquals( "legacy", layout.getId() );
    }
}
