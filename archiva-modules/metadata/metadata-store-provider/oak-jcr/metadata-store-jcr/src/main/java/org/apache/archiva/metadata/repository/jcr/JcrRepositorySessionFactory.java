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

import org.apache.archiva.metadata.model.MetadataFacet;
import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.repository.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
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

    private Repository repository;

    // Lazy evaluation to avoid problems with circular dependencies during initialization
    private MetadataResolver metadataResolver;

    @Inject
    private RepositorySessionFactoryBean repositorySessionFactoryBean;

    @Inject
    private MetadataService metadataService;

    private OakRepositoryFactory repositoryFactory;

    private JcrMetadataRepository jcrMetadataRepository;

    @Override
    public RepositorySession createSession() throws MetadataRepositoryException
    {
        try
        {
            return new JcrRepositorySession( jcrMetadataRepository, getMetadataResolver() );
        }
        catch ( RepositoryException e )
        {
            // FIXME: a custom exception requires refactoring for callers to handle it
            throw new RuntimeException( e );
        }
    }

    private MetadataResolver getMetadataResolver() {
        return metadataService.getMetadataResolver( );
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

        try
        {

            repositoryFactory = new OakRepositoryFactory();
            // FIXME this need to be configurable
            Path directoryPath = Paths.get( System.getProperty( "appserver.base" ), "data/jcr" );
            repositoryFactory.setRepositoryPath( directoryPath );
            try {
                repository = repositoryFactory.createRepository();
            } catch (InvalidFileStoreVersionException | IOException e) {
                logger.error("Repository creation failed {}", e.getMessage());
                throw new RuntimeException("Fatal error. Could not create metadata repository.");
            }
            jcrMetadataRepository = new JcrMetadataRepository( metadataService, repository );
            try ( JcrRepositorySession session = new JcrRepositorySession( jcrMetadataRepository, metadataResolver )) {
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
        logger.info( "Shutting down JcrRepositorySessionFactory" );
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


    public MetadataService getMetadataService( )
    {
        return metadataService;
    }

    public void setMetadataService( MetadataService metadataService )
    {
        this.metadataService = metadataService;
    }
}
