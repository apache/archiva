package org.apache.archiva.rest.services;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.metadata.model.facets.RepositoryProblemFacet;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.stats.model.RepositoryStatistics;
import org.apache.archiva.metadata.repository.stats.model.RepositoryStatisticsManager;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.ReportRepositoriesService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * DefaultReportRepositoriesService
 *
 * @author Adrien Lecharpentier &lt;adrien.lecharpentier@zenika.com&gt;
 * @since 1.4-M3
 */
@Service( "reportRepositoriesService#rest" )
public class DefaultReportRepositoriesService
    extends AbstractRestService
    implements ReportRepositoriesService
{

    private static final String ALL_REPOSITORIES = "all";

    @Inject
    private RepositoryStatisticsManager repositoryStatisticsManager;

    @Override
    public List<RepositoryStatistics> getStatisticsReport( List<String> repositoriesId, int rowCount, Date startDate,
                                                           Date endDate )
        throws ArchivaRestServiceException
    {
        switch ( repositoriesId.size() )
        {
            case 0:
                throw new ArchivaRestServiceException( "report.statistics.report.missing-repositories", null );
            case 1:
                return getUniqueRepositoryReport( repositoriesId.get( 0 ), rowCount, startDate, endDate );
            default:
                return getMultipleRepositoriesReport( repositoriesId, rowCount );
        }
    }

    private List<RepositoryStatistics> getMultipleRepositoriesReport( List<String> repositoriesId, int rowCount )
    {
        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }
        try
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();
            List<RepositoryStatistics> stats = new ArrayList<>();
            for ( String repo : repositoriesId )
            {
                try
                {
                    stats.add( repositoryStatisticsManager.getLastStatistics( repo ) );
                }
                catch ( MetadataRepositoryException e )
                {
                    log.warn( "Unable to retrieve stats, assuming is empty: {}", e.getMessage(), e );
                }
            }

            return stats.subList( 0, stats.size() > rowCount ? rowCount : stats.size() );
        }
        finally
        {
            repositorySession.close();
        }
    }

    private List<RepositoryStatistics> getUniqueRepositoryReport( String repositoryId, int rowCount, Date startDate,
                                                                  Date endDate )
    {
        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }
        try
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();
            List<RepositoryStatistics> stats = null;
            try
            {
                stats = repositoryStatisticsManager.getStatisticsInRange( repositoryId, startDate,
                                                                          endDate );
            }
            catch ( MetadataRepositoryException e )
            {
                log.warn( "Unable to retrieve stats, assuming is empty: {}", e.getMessage(), e );
            }
            if ( stats == null || stats.isEmpty() )
            {
                return Collections.<RepositoryStatistics>emptyList();
            }

            return stats.subList( 0, stats.size() > rowCount ? rowCount : stats.size() );
        }
        finally
        {
            repositorySession.close();
        }
    }

    @Override
    public List<RepositoryProblemFacet> getHealthReport( String repository, String groupId, int rowCount )
        throws ArchivaRestServiceException
    {
        RepositorySession repositorySession = null;
        try
        {
            repositorySession = repositorySessionFactory.createSession();
        }
        catch ( MetadataRepositoryException e )
        {
            e.printStackTrace( );
        }
        try
        {
            List<String> observableRepositories = getObservableRepos();
            if ( !ALL_REPOSITORIES.equals( repository ) && !observableRepositories.contains( repository ) )
            {
                throw new ArchivaRestServiceException(
                    "${$.i18n.prop('report.repository.illegal-access', " + repository + ")}", "repositoryId",
                    new IllegalAccessException() );
            }

            if ( !ALL_REPOSITORIES.equals( repository ) )
            {
                observableRepositories = Collections.singletonList( repository );
            }

            List<RepositoryProblemFacet> problemArtifacts = new ArrayList<>();
            MetadataRepository metadataRepository = repositorySession.getRepository();
            for ( String repoId : observableRepositories )
            {
                for ( String name : metadataRepository.getMetadataFacets(repositorySession , repoId, RepositoryProblemFacet.FACET_ID ) )
                {
                    RepositoryProblemFacet metadataFacet =
                        (RepositoryProblemFacet) metadataRepository.getMetadataFacet(repositorySession ,
                            repoId,
                            RepositoryProblemFacet.FACET_ID, name );
                    if ( StringUtils.isEmpty( groupId ) || groupId.equals( metadataFacet.getNamespace() ) )
                    {
                        problemArtifacts.add( metadataFacet );
                    }
                }
            }

            return problemArtifacts;
        }
        catch ( MetadataRepositoryException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
        finally
        {
            repositorySession.close();
        }
    }
}
