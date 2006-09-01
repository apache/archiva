package org.apache.maven.archiva;

import org.apache.maven.archiva.converter.RepositoryConversionException;
import org.apache.maven.archiva.discoverer.DiscovererException;

import java.io.File;

/**
 * @author Jason van Zyl
 */
public interface RepositoryManager
{
    /**
     * Role of the Repository Manager
     */
    String ROLE = RepositoryManager.class.getName();

    /**
     * Convert a legacy repository to a modern repository. This means a Maven 1.x repository
     * using v3 POMs to a Maven 2.x repository using v4.0.0 POMs.
     *
     * @param legacyRepositoryDirectory
     * @param repositoryDirectory
     * @throws RepositoryConversionException
     */
    void convertLegacyRepository( File legacyRepositoryDirectory, File repositoryDirectory, boolean includeSnapshots )
        throws RepositoryConversionException, DiscovererException;
}
