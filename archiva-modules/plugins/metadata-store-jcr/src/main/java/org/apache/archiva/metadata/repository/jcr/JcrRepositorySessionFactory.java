package org.apache.archiva.metadata.repository.jcr;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.repository.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.jackrabbit.oak.segment.file.InvalidFileStoreVersionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@Service( "repositorySessionFactory#jcr" )
public class JcrRepositorySessionFactory extends AbstractRepositorySessionFactory
    implements RepositorySessionFactory
{

    private Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private ApplicationContext applicationContext;

    private Map<String, MetadataFacetFactory> metadataFacetFactories;

    private Repository repository;

    // Lazy evaluation to avoid problems with circular dependencies during initialization
    private MetadataResolver metadataResolver;

    @Inject
    private RepositorySessionFactoryBean repositorySessionFactoryBean;

    private RepositoryFactory repositoryFactory;

    private JcrMetadataRepository jcrMetadataRepository;

    @Override
    public RepositorySession createSession() throws MetadataRepositoryException
    {
        try
        {
            return new JcrSession( jcrMetadataRepository, getMetadataResolver() );
        }
        catch ( RepositoryException e )
        {
            // FIXME: a custom exception requires refactoring for callers to handle it
            throw new RuntimeException( e );
        }
    }

    // Lazy evaluation to avoid problems with circular dependencies during initialization
    private MetadataResolver getMetadataResolver()
    {
        if ( this.metadataResolver == null )
        {
            this.metadataResolver = applicationContext.getBean( MetadataResolver.class );
        }
        return this.metadataResolver;
    }

    protected void initialize()
    {

        // skip initialisation if not jcr
        if ( repositorySessionFactoryBean!=null && !StringUtils.equals( repositorySessionFactoryBean.getId(), "jcr" ) )
        {
            return;
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        if (applicationContext!=null) {
            metadataFacetFactories = applicationContext.getBeansOfType(MetadataFacetFactory.class);
        }
        // olamy with spring the "id" is now "metadataFacetFactory#hint"
        // whereas was only hint with plexus so let remove  metadataFacetFactory#
        Map<String, MetadataFacetFactory> cleanedMetadataFacetFactories =
            new HashMap<>( metadataFacetFactories.size() );

        for ( Map.Entry<String, MetadataFacetFactory> entry : metadataFacetFactories.entrySet() )
        {
            if (entry.getKey().contains("#")) {
                cleanedMetadataFacetFactories.put( StringUtils.substringAfterLast( entry.getKey(), "#" ),
                        entry.getValue() );

            } else {
                cleanedMetadataFacetFactories.put(entry.getKey(), entry.getValue());
            }
        }

        metadataFacetFactories = cleanedMetadataFacetFactories;

        try
        {

            repositoryFactory = new RepositoryFactory();
            // FIXME this need to be configurable
            Path directoryPath = Paths.get( System.getProperty( "appserver.base" ), "data/jcr" );
            repositoryFactory.setRepositoryPath( directoryPath );
            try {
                repository = repositoryFactory.createRepository();
            } catch (InvalidFileStoreVersionException | IOException e) {
                logger.error("Repository creation failed {}", e.getMessage());
                throw new RuntimeException("Fatal error. Could not create metadata repository.");
            }
            jcrMetadataRepository = new JcrMetadataRepository( metadataFacetFactories, repository );
            try (JcrSession session = new JcrSession( jcrMetadataRepository, metadataResolver )) {
                JcrMetadataRepository.initializeNodeTypes( session.getJcrSession() );
                // Saves automatically with close
            }
        }
        catch ( RepositoryException e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }

        stopWatch.stop();
        logger.info( "time to initialize JcrRepositorySessionFactory: {}", stopWatch.getTime() );
    }

    @Override
    protected void shutdown() {
        repositoryFactory.close();
    }

    @PreDestroy
    public void close()
    {
        super.close();
    }

    public void setMetadataResolver(MetadataResolver metadataResolver) {
        this.metadataResolver = metadataResolver;
    }

    public JcrMetadataRepository getMetadataRepository() {
        return jcrMetadataRepository;
    }

    public void setMetadataFacetFactories(Map<String, MetadataFacetFactory> metadataFacetFactories) {
        this.metadataFacetFactories = metadataFacetFactories;
    }

}
