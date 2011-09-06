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
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@Service( "repositorySessionFactory#jcr" )
public class JcrRepositorySessionFactory
    implements RepositorySessionFactory
{

    @Inject
    private ApplicationContext applicationContext;

    /**
     * plexus.requirement role="org.apache.archiva.metadata.model.MetadataFacetFactory"
     */
    private Map<String, MetadataFacetFactory> metadataFacetFactories;

    /**
     * plexus.requirement
     */
    @Inject
    private Repository repository;

    /**
     * plexus.requirement
     */
    @Inject
    private MetadataResolver metadataResolver;

    public RepositorySession createSession()
    {
        try
        {
            // FIXME: is this the right separation? or should a JCR session object contain the JCR session information?
            //  such a change might allow us to avoid creating two objects for each request. It would also clear up
            //  the ambiguities in the API where session & repository are the inverse of JCR; and the resolver is
            //  retrieved from the session but must have it passed in. These should be reviewed before finalising the
            //  API.
            MetadataRepository metadataRepository = new JcrMetadataRepository( metadataFacetFactories, repository );

            return new RepositorySession( metadataRepository, metadataResolver );
        }
        catch ( RepositoryException e )
        {
            // FIXME: a custom exception requires refactoring for callers to handle it
            throw new RuntimeException( e );
        }
    }

    @PostConstruct
    public void initialize()
    {
        metadataFacetFactories = applicationContext.getBeansOfType( MetadataFacetFactory.class );
        // olamy with spring the "id" is now "metadataFacetFactory#hint"
        // whereas was only hint with plexus so let remove  metadataFacetFactory#
        Map<String, MetadataFacetFactory> cleanedMetadataFacetFactories =
            new HashMap<String, MetadataFacetFactory>( metadataFacetFactories.size() );

        for ( Map.Entry<String, MetadataFacetFactory> entry : metadataFacetFactories.entrySet() )
        {
            cleanedMetadataFacetFactories.put( StringUtils.substringAfterLast( entry.getKey(), "#" ),
                                               entry.getValue() );
        }

        metadataFacetFactories = cleanedMetadataFacetFactories;

        JcrMetadataRepository metadataRepository = null;
        try
        {
            metadataRepository = new JcrMetadataRepository( metadataFacetFactories, repository );
            JcrMetadataRepository.initialize( metadataRepository.getJcrSession() );
        }
        catch ( RepositoryException e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }
        finally
        {
            if ( metadataRepository != null )
            {
                metadataRepository.close();
            }
        }
    }
}
