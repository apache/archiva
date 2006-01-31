package org.apache.maven.repository.proxy.configuration;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.repository.proxy.repository.ProxyRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * @plexus.component role="org.apache.maven.repository.proxy.configuration.ProxyConfiguration"
 *
 * @author Edwin Punzalan
 */
public class ProxyConfiguration
{
    public static final String ROLE = ProxyConfiguration.class.getName();

    /** @plexus.requirement */
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    private boolean browsable;
    private ArtifactRepository repoCache;
    private ArrayList repositories = new ArrayList();

    public void setBrowsable( boolean browsable )
    {
        this.browsable = browsable;
    }

    public boolean isBrowsable()
    {
        return browsable;
    }

    public void setRepositoryCachePath( String repoCachePath )
    {
        ArtifactRepositoryPolicy standardPolicy;
        standardPolicy = new ArtifactRepositoryPolicy( true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
                                                       ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE );

        ArtifactRepositoryLayout layout = new DefaultRepositoryLayout();

        repoCache = artifactRepositoryFactory.createArtifactRepository( "localCache", repoCachePath, layout,
                                                                        standardPolicy, standardPolicy );
    }

    public ArtifactRepository getRepositoryCache( )
    {
        return repoCache;
    }

    public String getRepositoryCachePath()
    {
        return repoCache.getBasedir();
    }

    public void addRepository( ProxyRepository repository )
    {
        repositories.add( repository );
    }

    public List getRepositories()
    {
        return Collections.unmodifiableList( repositories );
    }

    public void setRepositories( ArrayList repositories )
    {
        this.repositories = repositories;
    }
}
