package org.apache.maven.archiva.repository.project.resolvers;

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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.configuration.functors.LocalRepositoryPredicate;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.repository.ArchivaConfigurationAdaptor;
import org.apache.maven.archiva.repository.RepositoryException;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayoutFactory;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.archiva.repository.project.ProjectModelReader;
import org.apache.maven.archiva.repository.project.ProjectModelResolver;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Factory for ProjectModelResolver objects 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.repository.project.resolvers.RepositoryProjectModelResolverFactory"
 */
public class RepositoryProjectModelResolverFactory
    extends AbstractLogEnabled
    implements RegistryListener, Initializable
{
    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @plexus.requirement
     */
    private BidirectionalRepositoryLayoutFactory layoutFactory;

    /**
     * @plexus.requirement role-hint="model400"
     */
    private ProjectModelReader project400Reader;

    /**
     * @plexus.requirement role-hint="model300"
     */
    private ProjectModelReader project300Reader;

    /**
     * Get the {@link ProjectModelResolver} for the specific archiva repository.
     * 
     * @param repo the repository to base resolver on.
     * @return return the resolver for the archiva repository provided.
     * @throws RepositoryException if unable to create a resolver for the provided {@link ArchivaRepository}
     */
    public ProjectModelResolver getResolver( ArchivaRepository repo )
        throws RepositoryException
    {
        if ( resolverMap.containsKey( repo.getId() ) )
        {
            return (ProjectModelResolver) this.resolverMap.get( repo.getId() );
        }

        ProjectModelResolver resolver = toResolver( repo );
        resolverMap.put( repo.getId(), resolver );

        return resolver;
    }

    /**
     * Get the {@link ProjectModelResolver} for the specific archiva repository based on repository id.
     * 
     * @param repoid the repository id to get the resolver for.
     * @return the {@link ProjectModelResolver} if found, or null if repository is not found.
     */
    public ProjectModelResolver getResolver( String repoid )
    {
        return (ProjectModelResolver) this.resolverMap.get( repoid );
    }

    /**
     * Get the {@link List} of {@link ProjectModelResolver} for 
     * the {@link List} of {@link ArchivaRepository} objects provided.
     * 
     * @param repositoryList the {@link List} of {@link ArchivaRepository} objects to 
     *             get {@link ProjectModelResolver} for.
     * @return the {@link List} of {@link ProjectModelResolver} objects.
     * @throws RepositoryException if unable to convert any of the provided {@link ArchivaRepository} objects into
     *             a {@link ProjectModelResolver} object.
     */
    public List getResolverList( List repositoryList )
        throws RepositoryException
    {
        List ret = new ArrayList();

        if ( CollectionUtils.isEmpty( repositoryList ) )
        {
            return ret;
        }

        Iterator it = repositoryList.iterator();
        while ( it.hasNext() )
        {
            ArchivaRepository repo = (ArchivaRepository) it.next();
            ret.add( getResolver( repo ) );
        }

        return ret;
    }

    /**
     * Get the entire {@link List} of {@link ProjectModelResolver} that the factory is tracking.
     *  
     * @return the entire list of {@link ProjectModelResolver} that is being tracked.
     */
    public List getAllResolvers()
    {
        List ret = new ArrayList();

        ret.addAll( this.resolverMap.values() );

        return ret;
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isRepositories( propertyName ) )
        {
            update();
        }
    }

    private Map resolverMap = new HashMap();

    private void update()
    {
        synchronized ( resolverMap )
        {
            resolverMap.clear();

            List configRepos = archivaConfiguration.getConfiguration().getRepositories();
            Collection configLocalRepos = CollectionUtils.select( configRepos, LocalRepositoryPredicate.getInstance() );

            Iterator it = configLocalRepos.iterator();
            while ( it.hasNext() )
            {
                RepositoryConfiguration repoconfig = (RepositoryConfiguration) it.next();
                ArchivaRepository repo = ArchivaConfigurationAdaptor.toArchivaRepository( repoconfig );
                try
                {
                    RepositoryProjectResolver resolver = toResolver( repo );
                    resolverMap.put( repo.getId(), resolver );
                }
                catch ( RepositoryException e )
                {
                    getLogger().warn( e.getMessage(), e );
                }
            }
        }
    }

    private RepositoryProjectResolver toResolver( ArchivaRepository repo )
        throws RepositoryException
    {
        if ( !repo.isManaged() )
        {
            throw new RepositoryException( "Unable to create RepositoryProjectResolver from non-managed repository: "
                + repo );
        }

        try
        {
            BidirectionalRepositoryLayout layout = layoutFactory.getLayout( repo.getLayoutType() );
            ProjectModelReader reader = project400Reader;

            if ( StringUtils.equals( "legacy", repo.getLayoutType() ) )
            {
                reader = project300Reader;
            }

            RepositoryProjectResolver resolver = new RepositoryProjectResolver( repo, reader, layout );
            return resolver;
        }
        catch ( LayoutException e )
        {
            throw new RepositoryException( "Unable to create RepositoryProjectResolver due to invalid layout spec: "
                + repo );
        }
    }

    public void initialize()
        throws InitializationException
    {
        update();
        archivaConfiguration.addChangeListener( this );
    }
}
