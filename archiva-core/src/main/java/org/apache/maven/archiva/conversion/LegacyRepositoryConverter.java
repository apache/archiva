package org.apache.maven.archiva.conversion;

import org.apache.maven.archiva.converter.RepositoryConversionException;
import org.apache.maven.archiva.discoverer.DiscovererException;

import java.io.File;
import java.util.List;

/**
 * @author Jason van Zyl
 */
public interface LegacyRepositoryConverter
{
    String ROLE = LegacyRepositoryConverter.class.getName();

    /**
     * Convert a legacy repository to a modern repository. This means a Maven 1.x repository
     * using v3 POMs to a Maven 2.x repository using v4.0.0 POMs.
     *
     * @param legacyRepositoryDirectory
     * @param repositoryDirectory
     * @throws org.apache.maven.archiva.converter.RepositoryConversionException
     *
     */
    void convertLegacyRepository( File legacyRepositoryDirectory,
                                  File repositoryDirectory,
                                  List blacklistedPatterns,
                                  boolean includeSnapshots )
        throws RepositoryConversionException, DiscovererException;
}
