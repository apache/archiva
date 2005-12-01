/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.repository.reporting;

import org.codehaus.plexus.PlexusTestCase;

/**
 * Test the artifact reporter.
 *
 * @author <a href="mailto:jtolentino@mergere.com">John Tolentino</a>
 */
public class DefaultRepositoryQueryLayerTest
    extends PlexusTestCase
{
    private RepositoryQueryLayer queryLayer;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        queryLayer = (RepositoryQueryLayer) lookup( RepositoryQueryLayer.ROLE, "default" );
    }

    public void testNonExistingArtifact()
    {
        assertTrue( queryLayer.containsArtifact( null ) );
    }

    protected void tearDown()
        throws Exception
    {
        queryLayer = null;
        super.tearDown();
    }
}
/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.repository.reporting;

import org.codehaus.plexus.PlexusTestCase;

/**
 * Test the artifact reporter.
 *
 * @author <a href="mailto:jtolentino@mergere.com">John Tolentino</a>
 */
public class DefaultRepositoryQueryLayerTest
    extends PlexusTestCase
{
    private RepositoryQueryLayer queryLayer;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        queryLayer = (RepositoryQueryLayer) lookup( RepositoryQueryLayer.ROLE, "default" );
    }

    public void testNonExistingArtifact()
    {
        assertTrue( queryLayer.containsArtifact( null ) );
    }

    protected void tearDown()
        throws Exception
    {
        queryLayer = null;
        super.tearDown();
    }
}
/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.repository.reporting;

import org.codehaus.plexus.PlexusTestCase;

/**
 * Test the artifact reporter.
 *
 * @author <a href="mailto:jtolentino@mergere.com">John Tolentino</a>
 */
public class DefaultRepositoryQueryLayerTest
    extends PlexusTestCase
{
    private RepositoryQueryLayer queryLayer;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        queryLayer = (RepositoryQueryLayer) lookup( RepositoryQueryLayer.ROLE, "default" );
    }

    public void testNonExistingArtifact()
    {
        assertTrue( queryLayer.containsArtifact( null ) );
    }

    protected void tearDown()
        throws Exception
    {
        queryLayer = null;
        super.tearDown();
    }
}
