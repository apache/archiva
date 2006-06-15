package org.apache.maven.repository;

import org.codehaus.plexus.PlexusTestCase;

/**
 * @author Jason van Zyl
 */
public class RepositoryManagerTest
    extends PlexusTestCase
{
    public void testLegacyRepositoryConversion()
        throws Exception
    {
        RepositoryManager rm = (RepositoryManager) lookup( RepositoryManager.ROLE );
    }
}
