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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.RepositoryCommonValidator;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.stats.model.RepositoryStatistics;
import org.apache.archiva.metadata.repository.stats.model.RepositoryStatisticsManager;
import org.apache.archiva.rest.api.model.ArchivaRepositoryStatistics;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.ManagedRepositoriesService;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@Service( "managedRepositoriesService#rest" )
public class DefaultManagedRepositoriesService
    extends AbstractRestService
    implements ManagedRepositoriesService
{

    @Inject
    private RepositoryCommonValidator repositoryCommonValidator;

    @Inject
    private RepositoryStatisticsManager repositoryStatisticsManager;

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Override
    public List<ManagedRepository> getManagedRepositories()
        throws ArchivaRestServiceException
    {
        try
        {
            List<org.apache.archiva.admin.model.beans.ManagedRepository> repos =
                managedRepositoryAdmin.getManagedRepositories();
            return repos == null ? Collections.<ManagedRepository>emptyList() : repos;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e );
        }
    }

    @Override
    public ManagedRepository getManagedRepository( String repositoryId )
        throws ArchivaRestServiceException
    {
        List<ManagedRepository> repos = getManagedRepositories();
        for ( ManagedRepository repo : repos )
        {
            if ( StringUtils.equals( repo.getId(), repositoryId ) )
            {
                return repo;
            }
        }
        return null;
    }


    @Override
    public Boolean deleteManagedRepository( String repoId, boolean deleteContent )
        throws ArchivaRestServiceException
    {

        try
        {
            return managedRepositoryAdmin.deleteManagedRepository( repoId, getAuditInformation(), deleteContent );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage(), e.getFieldName(), e );
        }
    }

    @Override
    public ManagedRepository addManagedRepository( ManagedRepository managedRepository )
        throws ArchivaRestServiceException
    {

        try
        {
            boolean res =
                managedRepositoryAdmin.addManagedRepository( managedRepository, managedRepository.isStageRepoNeeded(),
                                                             getAuditInformation() );
            if ( res )
            {
                return getManagedRepository( managedRepository.getId() );
            }
            throw new ArchivaRestServiceException( "fail to created managed Repository", null );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e.getFieldName(), e );
        }
    }


    @Override
    public Boolean updateManagedRepository( ManagedRepository managedRepository )
        throws ArchivaRestServiceException
    {

        try
        {
            return managedRepositoryAdmin.updateManagedRepository( managedRepository,
                                                                   managedRepository.isStageRepoNeeded(),
                                                                   getAuditInformation(),
                                                                   managedRepository.isResetStats() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(), e.getFieldName(), e );
        }
    }

    @Override
    public Boolean fileLocationExists( String fileLocation )
        throws ArchivaRestServiceException
    {
        String location = repositoryCommonValidator.removeExpressions( fileLocation );
        return Files.exists( Paths.get( location ));
    }

    @Override
    public ArchivaRepositoryStatistics getManagedRepositoryStatistics( String repositoryId, String lang )
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
        SimpleDateFormat sdf = new SimpleDateFormat( "EEE, d MMM yyyy HH:mm:ss Z", new Locale( lang ) );
        try
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();

            RepositoryStatistics stats = null;
            try
            {
                stats = repositoryStatisticsManager.getLastStatistics( repositoryId );
            }
            catch ( MetadataRepositoryException e )
            {
                log.warn( "Error retrieving repository statistics: {}", e.getMessage(), e );
            }
            if ( stats != null )
            {
                ArchivaRepositoryStatistics archivaRepositoryStatistics =
                    getModelMapper().map( stats, ArchivaRepositoryStatistics.class );
                archivaRepositoryStatistics.setDuration( archivaRepositoryStatistics.getScanEndTime().getTime()
                                                             - archivaRepositoryStatistics.getScanStartTime().getTime() );
                archivaRepositoryStatistics.setLastScanDate(
                    sdf.format( archivaRepositoryStatistics.getScanEndTime() ) );
                return archivaRepositoryStatistics;
            }

        }
        finally
        {
            if ( repositorySession != null )
            {
                repositorySession.close();
            }
        }
        return null;
    }

    @Override
    public String getPomSnippet( String repositoryId )
        throws ArchivaRestServiceException
    {
        return createSnippet( getManagedRepository( repositoryId ) );
    }

    private String createSnippet( ManagedRepository repo )
        throws ArchivaRestServiceException
    {
        try
        {
            StringBuilder snippet = new StringBuilder();
            snippet.append( "<project>\n" );
            snippet.append( "  ...\n" );
            snippet.append( "  <distributionManagement>\n" );

            String distRepoName = "repository";
            if ( repo.isSnapshots() )
            {
                distRepoName = "snapshotRepository";
            }

            snippet.append( "    <" ).append( distRepoName ).append( ">\n" );
            snippet.append( "      <id>" ).append( repo.getId() ).append( "</id>\n" );
            snippet.append( "      <url>" );
            snippet.append( getBaseUrl(  ) + "/repository" );
            snippet.append( "/" ).append( repo.getId() ).append( "/" ).append( "</url>\n" );

            if ( !"default".equals( repo.getLayout() ) )
            {
                snippet.append( "      <layout>" ).append( repo.getLayout() ).append( "</layout>" );
            }

            snippet.append( "    </" ).append( distRepoName ).append( ">\n" );
            snippet.append( "  </distributionManagement>\n" );
            snippet.append( "\n" );

            snippet.append( "  <repositories>\n" );
            snippet.append( "    <repository>\n" );
            snippet.append( "      <id>" ).append( repo.getId() ).append( "</id>\n" );
            snippet.append( "      <name>" ).append( repo.getName() ).append( "</name>\n" );

            snippet.append( "      <url>" );
            snippet.append( getBaseUrl(  ) + "/repository" );
            snippet.append( "/" ).append( repo.getId() ).append( "/" );

            snippet.append( "</url>\n" );

            if ( !"default".equals( repo.getLayout() ) )
            {
                snippet.append( "      <layout>" ).append( repo.getLayout() ).append( "</layout>\n" );
            }

            snippet.append( "      <releases>\n" );
            snippet.append( "        <enabled>" ).append( Boolean.valueOf( repo.isReleases() ) ).append(
                "</enabled>\n" );
            snippet.append( "      </releases>\n" );
            snippet.append( "      <snapshots>\n" );
            snippet.append( "        <enabled>" ).append( Boolean.valueOf( repo.isSnapshots() ) ).append(
                "</enabled>\n" );
            snippet.append( "      </snapshots>\n" );
            snippet.append( "    </repository>\n" );
            snippet.append( "  </repositories>\n" );
            snippet.append( "  <pluginRepositories>\n" );
            snippet.append( "    <pluginRepository>\n" );
            snippet.append( "      <id>" ).append( repo.getId() ).append( "</id>\n" );
            snippet.append( "      <name>" ).append( repo.getName() ).append( "</name>\n" );

            snippet.append( "      <url>" );
            snippet.append( getBaseUrl(  ) + "/repository" );
            snippet.append( "/" ).append( repo.getId() ).append( "/" );

            snippet.append( "</url>\n" );

            if ( !"default".equals( repo.getLayout() ) )
            {
                snippet.append( "      <layout>" ).append( repo.getLayout() ).append( "</layout>\n" );
            }

            snippet.append( "      <releases>\n" );
            snippet.append( "        <enabled>" ).append( Boolean.valueOf( repo.isReleases() ) ).append(
                "</enabled>\n" );
            snippet.append( "      </releases>\n" );
            snippet.append( "      <snapshots>\n" );
            snippet.append( "        <enabled>" ).append( Boolean.valueOf( repo.isSnapshots() ) ).append(
                "</enabled>\n" );
            snippet.append( "      </snapshots>\n" );
            snippet.append( "    </pluginRepository>\n" );
            snippet.append( "  </pluginRepositories>\n" );

            snippet.append( "  ...\n" );
            snippet.append( "</project>\n" );

            return StringEscapeUtils.escapeXml( snippet.toString() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage(),
                                                   Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), e );
        }
    }
}
