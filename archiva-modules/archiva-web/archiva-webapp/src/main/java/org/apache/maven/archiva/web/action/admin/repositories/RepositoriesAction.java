package org.apache.maven.archiva.web.action.admin.repositories;

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

import com.opensymphony.xwork2.Preparable;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.stats.RepositoryStatistics;
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.maven.archiva.configuration.functors.RepositoryConfigurationComparator;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.archiva.web.action.AbstractActionSupport;
import org.apache.maven.archiva.web.util.ContextUtils;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.redback.integration.interceptor.SecureAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

/**
 * Shows the Repositories Tab for the administrator.
 *
 * @version $Id$
 *          plexus.component role="com.opensymphony.xwork2.Action" role-hint="repositoriesAction" instantiation-strategy="per-lookup"
 */
@Controller( "repositoriesAction" )
@Scope( "prototype" )
public class RepositoriesAction
    extends AbstractActionSupport
    implements SecureAction, ServletRequestAware, Preparable
{
    /**
     * plexus.requirement
     */
    @Inject
    private ArchivaConfiguration archivaConfiguration;

    private List<ManagedRepositoryConfiguration> managedRepositories;

    private List<RemoteRepositoryConfiguration> remoteRepositories;

    private Map<String, RepositoryStatistics> repositoryStatistics;

    private Map<String, List<String>> repositoryToGroupMap;

    /**
     * Used to construct the repository WebDAV URL in the repository action.
     */
    private String baseUrl;

    /**
     * plexus.requirement
     */
    @Inject
    private RepositoryStatisticsManager repositoryStatisticsManager;

    public void setServletRequest( HttpServletRequest request )
    {
        // TODO: is there a better way to do this?
        this.baseUrl = ContextUtils.getBaseURL( request, "repository" );
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }

    @SuppressWarnings( "unchecked" )
    public void prepare()
    {
        Configuration config = archivaConfiguration.getConfiguration();

        remoteRepositories = new ArrayList<RemoteRepositoryConfiguration>( config.getRemoteRepositories() );
        managedRepositories = new ArrayList<ManagedRepositoryConfiguration>( config.getManagedRepositories() );
        repositoryToGroupMap = config.getRepositoryToGroupMap();

        Collections.sort( managedRepositories, new RepositoryConfigurationComparator() );
        Collections.sort( remoteRepositories, new RepositoryConfigurationComparator() );

        repositoryStatistics = new HashMap<String, RepositoryStatistics>();
        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();
            for ( ManagedRepositoryConfiguration repo : managedRepositories )
            {
                RepositoryStatistics stats = null;
                try
                {
                    stats = repositoryStatisticsManager.getLastStatistics( metadataRepository, repo.getId() );
                }
                catch ( MetadataRepositoryException e )
                {
                    addActionError(
                        "Error retrieving statistics for repository " + repo.getId() + " - consult application logs" );
                    log.warn( "Error retrieving repository statistics: " + e.getMessage(), e );
                }
                if ( stats != null )
                {
                    repositoryStatistics.put( repo.getId(), stats );
                }
            }
        }
        finally
        {
            repositorySession.close();
        }
    }

    public List<ManagedRepositoryConfiguration> getManagedRepositories()
    {
        List<ManagedRepositoryConfiguration> managedRepositoriesList = new ArrayList<ManagedRepositoryConfiguration>();
        for ( ManagedRepositoryConfiguration repoConfig : managedRepositories )
        {
            if ( !repoConfig.getId().endsWith( "-stage" ) )
            {
                managedRepositoriesList.add( repoConfig );
            }
        }
        return managedRepositoriesList;
    }

    public List<RemoteRepositoryConfiguration> getRemoteRepositories()
    {
        return remoteRepositories;
    }

    public Map<String, RepositoryStatistics> getRepositoryStatistics()
    {
        return repositoryStatistics;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public Map<String, List<String>> getRepositoryToGroupMap()
    {
        return repositoryToGroupMap;
    }
}
