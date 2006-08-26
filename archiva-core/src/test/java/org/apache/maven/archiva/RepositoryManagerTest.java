package org.apache.maven.archiva;

import org.codehaus.plexus.PlexusTestCase;

import java.io.File;

/**
 * @author Jason van Zyl
 */
public class RepositoryManagerTest
    extends PlexusTestCase
{
    public void testLegacyRepositoryConversion()
        throws Exception
    {
        File legacyRepositoryDirectory = getTestFile( "src/test/maven-1.x-repository" );

        File repositoryDirectory = getTestFile( "target/maven-2.x-repository" );

        RepositoryManager rm = (RepositoryManager) lookup( RepositoryManager.ROLE );

        rm.convertLegacyRepository( legacyRepositoryDirectory, repositoryDirectory, true );
    }
}
