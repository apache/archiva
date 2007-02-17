package org.apache.maven.archiva.repositories;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.common.artifact.managed.ManagedArtifact;
import org.apache.maven.archiva.common.artifact.managed.ManagedArtifactTypes;
import org.apache.maven.archiva.common.artifact.managed.ManagedEjbArtifact;
import org.apache.maven.archiva.common.artifact.managed.ManagedJavaArtifact;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ConfiguredRepositoryFactory;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.discoverer.DiscovererStatistics;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.cache.Cache;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * DefaultActiveManagedRepositories
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.repositories.ActiveManagedRepositories"
 */
public class DefaultActiveManagedRepositories
    extends AbstractLogEnabled
    implements ActiveManagedRepositories, Initializable, RegistryListener
{
    /**
     * @plexus.requirement role-hint="artifactCache"
     */
    private Cache artifactCache;

    /**
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @plexus.requirement
     */
    private MavenProjectBuilder projectBuilder;

    /**
     * @plexus.requirement role-hint="projectCache"
     */
    private Cache projectCache;

    /**
     * @plexus.requirement
     */
    private ConfiguredRepositoryFactory repositoryFactory;

    private Configuration configuration;

    private ArtifactRepository localRepository;

    private List repositories;

    public Artifact createRelatedArtifact( Artifact artifact, String classifier, String type )
    {
        String groupId = artifact.getGroupId();
        String artifactId = artifact.getArtifactId();
        String version = artifact.getVersion();
        String reltype = StringUtils.defaultIfEmpty( type, artifact.getType() );
        return artifactFactory.createArtifactWithClassifier( groupId, artifactId, version, reltype, classifier );
    }

    public ManagedArtifact findArtifact( Artifact artifact )
    {
        ManagedArtifact managedArtifact = (ManagedArtifact) artifactCache.get( toKey( artifact ) );

        if ( managedArtifact != null )
        {
            return managedArtifact;
        }

        boolean snapshot = artifact.isSnapshot();

        for ( Iterator i = repositories.iterator(); i.hasNext(); )
        {
            ArtifactRepository repository = (ArtifactRepository) i.next();
            if ( snapshot && !repository.getSnapshots().isEnabled() )
            {
                // skip repo.
                continue;
            }

            String path = repository.pathOf( artifact );
            File f = new File( repository.getBasedir(), path );
            if ( f.exists() )
            {
                // Found it.
                managedArtifact = createManagedArtifact( repository, artifact, f );

                artifactCache.put( toKey( artifact ), managedArtifact );

                return managedArtifact;
            }
        }

        return null;
    }

    public ManagedArtifact findArtifact( String groupId, String artifactId, String version )
        throws ProjectBuildingException
    {
        MavenProject project = findProject( groupId, artifactId, version );
        Model model = project.getModel();

        return findArtifact( model.getGroupId(), model.getArtifactId(), model.getVersion(), model.getPackaging() );
    }

    public ManagedArtifact findArtifact( String groupId, String artifactId, String version, String type )
    {
        Artifact artifact = artifactFactory.createBuildArtifact( groupId, artifactId, version, type );
        return findArtifact( artifact );
    }

    public MavenProject findProject( String groupId, String artifactId, String version )
        throws ProjectBuildingException
    {
        MavenProject project = (MavenProject) projectCache.get( toKey( groupId, artifactId, version ) );

        if ( project != null )
        {
            return project;
        }

        Artifact projectArtifact = artifactFactory.createProjectArtifact( groupId, artifactId, version );

        project = projectBuilder.buildFromRepository( projectArtifact, repositories, localRepository );

        projectCache.put( toKey( groupId, artifactId, version ), project );

        return project;
    }

    public ArtifactRepository getArtifactRepository( String id )
    {
        RepositoryConfiguration repoConfig = getRepositoryConfiguration( id );
        if ( repoConfig == null )
        {
            return null;
        }

        return repositoryFactory.createRepository( repoConfig );
    }

    public List getAllArtifactRepositories()
    {
        return repositoryFactory.createRepositories( configuration );
    }

    public RepositoryConfiguration getRepositoryConfiguration( String id )
    {
        return this.configuration.getRepositoryById( id );
    }

    public void initialize()
        throws InitializationException
    {
        Configuration config = archivaConfiguration.getConfiguration();
        archivaConfiguration.addChangeListener( this );
        configureSelf( config );
    }

    private String toKey( Artifact artifact )
    {
        if ( artifact == null )
        {
            return null;
        }

        return toKey( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );
    }

    private String toKey( String groupId, String artifactId, String version )
    {
        return groupId + ":" + artifactId + ":" + version;
    }

    private void configureSelf( Configuration config )
    {
        this.configuration = config;
        this.artifactCache.clear();
        this.projectCache.clear();

        repositories = repositoryFactory.createRepositories( this.configuration );
        localRepository = repositoryFactory.createLocalRepository( this.configuration );

    }

    private ManagedArtifact createManagedArtifact( ArtifactRepository repository, Artifact artifact, File f )
    {
        artifact.isSnapshot();
        String path = repository.pathOf( artifact );

        switch ( ManagedArtifactTypes.whichType( artifact.getType() ) )
        {
            case ManagedArtifactTypes.EJB:
                ManagedEjbArtifact managedEjbArtifact = new ManagedEjbArtifact( repository.getId(), artifact, path );

                managedEjbArtifact.setJavadocPath( pathToRelatedArtifact( repository, artifact, "javadoc", "jar" ) );
                managedEjbArtifact.setSourcesPath( pathToRelatedArtifact( repository, artifact, "sources", "jar" ) );
                managedEjbArtifact.setClientPath( pathToRelatedArtifact( repository, artifact, "client", "jar" ) );

                return managedEjbArtifact;

            case ManagedArtifactTypes.JAVA:
                ManagedJavaArtifact managedJavaArtifact = new ManagedJavaArtifact( repository.getId(), artifact, path );

                managedJavaArtifact.setJavadocPath( pathToRelatedArtifact( repository, artifact, "javadoc", "jar" ) );
                managedJavaArtifact.setSourcesPath( pathToRelatedArtifact( repository, artifact, "sources", "jar" ) );

                return managedJavaArtifact;

            case ManagedArtifactTypes.GENERIC:
            default:
                return new ManagedArtifact( repository.getId(), artifact, path );
        }
    }

    private String pathToRelatedArtifact( ArtifactRepository repository, Artifact artifact, String classifier,
                                          String type )
    {
        Artifact relatedArtifact = createRelatedArtifact( artifact, classifier, type );

        relatedArtifact.isSnapshot();
        String path = repository.pathOf( relatedArtifact );

        File relatedFile = new File( repository.getBasedir(), path );
        if ( !relatedFile.exists() )
        {
            // Return null to set the ManagedArtifact related path to empty.
            return null;
        }

        return path;
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        // nothing to do
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( propertyName.startsWith( "repositories" ) || propertyName.startsWith( "localRepository" ) )
        {
            getLogger().debug(
                               "Triggering managed repository configuration change with " + propertyName + " set to "
                                   + propertyValue );
            configureSelf( archivaConfiguration.getConfiguration() );
        }
        else
        {
            getLogger().debug( "Not triggering managed repository configuration change with " + propertyName );
        }
    }

    public long getLastDataRefreshTime()
    {
        long lastDataRefreshTime = 0;

        for ( Iterator i = getAllArtifactRepositories().iterator(); i.hasNext(); )
        {
            ArtifactRepository repository = (ArtifactRepository) i.next();

            DiscovererStatistics stats = new DiscovererStatistics( repository );
            if ( stats.getTimestampFinished() > lastDataRefreshTime )
            {
                lastDataRefreshTime = stats.getTimestampFinished();
            }
        }

        return lastDataRefreshTime;
    }

    public boolean needsDataRefresh()
    {
        for ( Iterator i = getAllArtifactRepositories().iterator(); i.hasNext(); )
        {
            ArtifactRepository repository = (ArtifactRepository) i.next();

            DiscovererStatistics stats = new DiscovererStatistics( repository );
            if ( stats.getTimestampFinished() <= 0 )
            {
                // Found a repository that has NEVER had it's data walked.
                return true;
            }
        }

        return false;
    }
}
