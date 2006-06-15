package org.apache.maven.repository;

import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.repository.converter.RepositoryConversionException;
import org.apache.maven.repository.converter.RepositoryConverter;
import org.apache.maven.repository.discovery.ArtifactDiscoverer;
import org.apache.maven.repository.reporting.ArtifactReporter;

import java.io.File;
import java.util.List;
import java.net.MalformedURLException;

/**
 * @author Jason van Zyl
 * @plexus.component
 */
public class DefaultRepositoryManager
    implements RepositoryManager
{
    /**
     * @plexus.requirement role="org.apache.maven.artifact.repository.discovery.ArtifactDiscoverer" role-hint="legacy"
     */
    private ArtifactDiscoverer artifactDiscoverer;

    /**
     * @plexus.requirement role="org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout" role-hint="legacy"
     */
    private ArtifactRepositoryLayout legacyLayout;

    /**
     * @plexus.requirement role="org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout" role-hint="default"
     */
    private ArtifactRepositoryLayout defaultLayout;

    /**
     * @plexus.requirement role="org.apache.maven.artifact.repository.ArtifactRepositoryFactory"
     */
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    /**
     * @plexus.requirement role="org.apache.maven.repository.converter.ArtifactRepositoryFactory"
     */
    private RepositoryConverter repositoryConverter;

    /**
     * @plexus.requirement role="org.apache.maven.artifact.repository.reporter.ArtifactReporter" role-hint="default"
     */
    private ArtifactReporter reporter;

    public void convertLegacyRepository( File legacyRepositoryDirectory,
                                         File repositoryDirectory,
                                         boolean includeSnapshots )
        throws RepositoryConversionException
    {
        ArtifactRepository legacyRepository;

        ArtifactRepository repository;

        try
        {
            legacyRepository = artifactRepositoryFactory.createArtifactRepository( "legacy",
                                                                                   legacyRepositoryDirectory.toURL().toString(),
                                                                                   legacyLayout, null, null );

            repository = artifactRepositoryFactory.createArtifactRepository( "default",
                                                                             legacyRepositoryDirectory.toURL().toString(),
                                                                             defaultLayout, null, null );
        }
        catch ( MalformedURLException e )
        {
            throw new RepositoryConversionException( "Error convering legacy repository.", e );
        }

        List legacyArtifacts = artifactDiscoverer.discoverArtifacts( legacyRepository, null, includeSnapshots );

        repositoryConverter.convert( legacyArtifacts, repository, reporter );
    }
}
