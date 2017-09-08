package org.apache.archiva.admin.repository.group;
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

import org.apache.archiva.admin.model.AuditInformation;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.beans.RepositoryGroup;
import org.apache.archiva.admin.model.group.RepositoryGroupAdmin;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.admin.repository.AbstractRepositoryAdmin;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.scheduler.MergedRemoteIndexesScheduler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Olivier Lamy
 */
@Service("repositoryGroupAdmin#default")
public class DefaultRepositoryGroupAdmin
    extends AbstractRepositoryAdmin
    implements RepositoryGroupAdmin
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    private static final Pattern REPO_GROUP_ID_PATTERN = Pattern.compile( "[A-Za-z0-9\\._\\-]+" );

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    private MergedRemoteIndexesScheduler mergedRemoteIndexesScheduler;

    private Path groupsDirectory;

    @PostConstruct
    public void initialize()
    {
        String appServerBase = getRegistry().getString( "appserver.base" );
        groupsDirectory = Paths.get( appServerBase, "groups" );
        if ( !Files.exists(groupsDirectory) )
        {
            Files.exists(groupsDirectory);
        }

        try
        {
            for ( RepositoryGroup repositoryGroup : getRepositoriesGroups() )
            {
                mergedRemoteIndexesScheduler.schedule( repositoryGroup,
                                                       getMergedIndexDirectory( repositoryGroup.getId() ));
                // create the directory for each group if not exists
                Path groupPath = groupsDirectory.resolve(repositoryGroup.getId() );
                if ( !Files.exists(groupPath) )
                {
                    try {
                        Files.createDirectories(groupPath);
                    } catch (IOException e) {
                        log.error("Could not create directory {}", groupPath);
                    }
                }
            }
        }
        catch ( RepositoryAdminException e )
        {
            log.warn( "fail to getRepositoriesGroups {}", e.getMessage(), e );
        }

    }


    @Override
    public Path getMergedIndexDirectory( String repositoryGroupId )
    {
        return groupsDirectory.resolve( repositoryGroupId );
    }

    @Override
    public List<RepositoryGroup> getRepositoriesGroups()
        throws RepositoryAdminException
    {
        List<RepositoryGroup> repositoriesGroups =
            new ArrayList<>( getArchivaConfiguration().getConfiguration().getRepositoryGroups().size() );

        for ( RepositoryGroupConfiguration repositoryGroupConfiguration : getArchivaConfiguration().getConfiguration().getRepositoryGroups() )
        {
            repositoriesGroups.add( new RepositoryGroup( repositoryGroupConfiguration.getId(), new ArrayList<String>(
                repositoryGroupConfiguration.getRepositories() ) ).mergedIndexPath(
                repositoryGroupConfiguration.getMergedIndexPath() ).mergedIndexTtl(
                repositoryGroupConfiguration.getMergedIndexTtl() ).cronExpression(
                repositoryGroupConfiguration.getCronExpression() ) );
        }

        return repositoriesGroups;
    }

    @Override
    public RepositoryGroup getRepositoryGroup( String repositoryGroupId )
        throws RepositoryAdminException
    {
        List<RepositoryGroup> repositoriesGroups = getRepositoriesGroups();
        for ( RepositoryGroup repositoryGroup : repositoriesGroups )
        {
            if ( StringUtils.equals( repositoryGroupId, repositoryGroup.getId() ) )
            {
                return repositoryGroup;
            }
        }
        return null;
    }

    @Override
    public Boolean addRepositoryGroup( RepositoryGroup repositoryGroup, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        validateRepositoryGroup( repositoryGroup, false );
        validateManagedRepositoriesExists( repositoryGroup.getRepositories() );

        RepositoryGroupConfiguration repositoryGroupConfiguration = new RepositoryGroupConfiguration();
        repositoryGroupConfiguration.setId( repositoryGroup.getId() );
        repositoryGroupConfiguration.setRepositories( repositoryGroup.getRepositories() );
        repositoryGroupConfiguration.setMergedIndexPath( repositoryGroup.getMergedIndexPath() );
        repositoryGroupConfiguration.setMergedIndexTtl( repositoryGroup.getMergedIndexTtl() );
        repositoryGroupConfiguration.setCronExpression( repositoryGroup.getCronExpression() );
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        configuration.addRepositoryGroup( repositoryGroupConfiguration );
        saveConfiguration( configuration );
        triggerAuditEvent( repositoryGroup.getId(), null, AuditEvent.ADD_REPO_GROUP, auditInformation );
        mergedRemoteIndexesScheduler.schedule( repositoryGroup, getMergedIndexDirectory( repositoryGroup.getId() ) );
        return Boolean.TRUE;
    }

    @Override
    public Boolean deleteRepositoryGroup( String repositoryGroupId, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        Configuration configuration = getArchivaConfiguration().getConfiguration();
        RepositoryGroupConfiguration repositoryGroupConfiguration =
            configuration.getRepositoryGroupsAsMap().get( repositoryGroupId );
        mergedRemoteIndexesScheduler.unschedule(
            new RepositoryGroup( repositoryGroupId, Collections.<String>emptyList() ) );
        if ( repositoryGroupConfiguration == null )
        {
            throw new RepositoryAdminException(
                "repositoryGroup with id " + repositoryGroupId + " doesn't not exists so cannot remove" );
        }
        configuration.removeRepositoryGroup( repositoryGroupConfiguration );
        triggerAuditEvent( repositoryGroupId, null, AuditEvent.DELETE_REPO_GROUP, auditInformation );

        return Boolean.TRUE;
    }

    @Override
    public Boolean updateRepositoryGroup( RepositoryGroup repositoryGroup, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        return updateRepositoryGroup( repositoryGroup, auditInformation, true );
    }

    private Boolean updateRepositoryGroup( RepositoryGroup repositoryGroup, AuditInformation auditInformation,
                                           boolean triggerAuditEvent )
        throws RepositoryAdminException
    {
        validateRepositoryGroup( repositoryGroup, true );
        validateManagedRepositoriesExists( repositoryGroup.getRepositories() );
        Configuration configuration = getArchivaConfiguration().getConfiguration();

        RepositoryGroupConfiguration repositoryGroupConfiguration =
            configuration.getRepositoryGroupsAsMap().get( repositoryGroup.getId() );

        configuration.removeRepositoryGroup( repositoryGroupConfiguration );

        repositoryGroupConfiguration.setRepositories( repositoryGroup.getRepositories() );
        repositoryGroupConfiguration.setMergedIndexPath( repositoryGroup.getMergedIndexPath() );
        repositoryGroupConfiguration.setMergedIndexTtl( repositoryGroup.getMergedIndexTtl() );
        repositoryGroupConfiguration.setCronExpression( repositoryGroup.getCronExpression() );
        configuration.addRepositoryGroup( repositoryGroupConfiguration );

        saveConfiguration( configuration );
        if ( triggerAuditEvent )
        {
            triggerAuditEvent( repositoryGroup.getId(), null, AuditEvent.MODIFY_REPO_GROUP, auditInformation );
        }
        mergedRemoteIndexesScheduler.unschedule( repositoryGroup );
        mergedRemoteIndexesScheduler.schedule( repositoryGroup, getMergedIndexDirectory( repositoryGroup.getId() ) );
        return Boolean.TRUE;
    }


    @Override
    public Boolean addRepositoryToGroup( String repositoryGroupId, String repositoryId,
                                         AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        RepositoryGroup repositoryGroup = getRepositoryGroup( repositoryGroupId );
        if ( repositoryGroup == null )
        {
            throw new RepositoryAdminException(
                "repositoryGroup with id " + repositoryGroupId + " doesn't not exists so cannot add repository to it" );
        }

        if ( repositoryGroup.getRepositories().contains( repositoryId ) )
        {
            throw new RepositoryAdminException(
                "repositoryGroup with id " + repositoryGroupId + " already contain repository with id" + repositoryId );
        }
        validateManagedRepositoriesExists( Arrays.asList( repositoryId ) );

        repositoryGroup.addRepository( repositoryId );
        updateRepositoryGroup( repositoryGroup, auditInformation, false );
        triggerAuditEvent( repositoryGroup.getId(), null, AuditEvent.ADD_REPO_TO_GROUP, auditInformation );
        return Boolean.TRUE;
    }

    @Override
    public Boolean deleteRepositoryFromGroup( String repositoryGroupId, String repositoryId,
                                              AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        RepositoryGroup repositoryGroup = getRepositoryGroup( repositoryGroupId );
        if ( repositoryGroup == null )
        {
            throw new RepositoryAdminException( "repositoryGroup with id " + repositoryGroupId
                                                    + " doesn't not exists so cannot remove repository from it" );
        }

        if ( !repositoryGroup.getRepositories().contains( repositoryId ) )
        {
            throw new RepositoryAdminException(
                "repositoryGroup with id " + repositoryGroupId + " doesn't not contains repository with id"
                    + repositoryId
            );
        }

        repositoryGroup.removeRepository( repositoryId );
        updateRepositoryGroup( repositoryGroup, auditInformation, false );
        triggerAuditEvent( repositoryGroup.getId(), null, AuditEvent.DELETE_REPO_FROM_GROUP, auditInformation );
        return Boolean.TRUE;
    }

    @Override
    public Map<String, RepositoryGroup> getRepositoryGroupsAsMap()
        throws RepositoryAdminException
    {
        List<RepositoryGroup> repositoriesGroups = getRepositoriesGroups();
        Map<String, RepositoryGroup> map = new HashMap<>( repositoriesGroups.size() );
        for ( RepositoryGroup repositoryGroup : repositoriesGroups )
        {
            map.put( repositoryGroup.getId(), repositoryGroup );
        }
        return map;
    }

    @Override
    public Map<String, List<String>> getGroupToRepositoryMap()
        throws RepositoryAdminException
    {

        Map<String, List<String>> map = new HashMap<>();

        for ( ManagedRepository repo : getManagedRepositoryAdmin().getManagedRepositories() )
        {
            for ( RepositoryGroup group : getRepositoriesGroups() )
            {
                if ( !group.getRepositories().contains( repo.getId() ) )
                {
                    String groupId = group.getId();
                    List<String> repos = map.get( groupId );
                    if ( repos == null )
                    {
                        repos = new ArrayList<>();
                        map.put( groupId, repos );
                    }
                    repos.add( repo.getId() );
                }
            }
        }
        return map;
    }

    @Override
    public Map<String, List<String>> getRepositoryToGroupMap()
        throws RepositoryAdminException
    {
        Map<String, List<String>> map = new HashMap<>();

        for ( RepositoryGroup group : getRepositoriesGroups() )
        {
            for ( String repositoryId : group.getRepositories() )
            {
                List<String> groups = map.get( repositoryId );
                if ( groups == null )
                {
                    groups = new ArrayList<>();
                    map.put( repositoryId, groups );
                }
                groups.add( group.getId() );
            }
        }
        return map;
    }

    public Boolean validateRepositoryGroup( RepositoryGroup repositoryGroup, boolean updateMode )
        throws RepositoryAdminException
    {
        String repoGroupId = repositoryGroup.getId();
        if ( StringUtils.isBlank( repoGroupId ) )
        {
            throw new RepositoryAdminException( "repositoryGroup id cannot be empty" );
        }

        if ( repoGroupId.length() > 100 )
        {
            throw new RepositoryAdminException(
                "Identifier [" + repoGroupId + "] is over the maximum limit of 100 characters" );

        }

        Matcher matcher = REPO_GROUP_ID_PATTERN.matcher( repoGroupId );
        if ( !matcher.matches() )
        {
            throw new RepositoryAdminException(
                "Invalid character(s) found in identifier. Only the following characters are allowed: alphanumeric, '.', '-' and '_'" );
        }

        if ( repositoryGroup.getMergedIndexTtl() <= 0 )
        {
            throw new RepositoryAdminException( "Merged Index TTL must be greater than 0." );
        }

        Configuration configuration = getArchivaConfiguration().getConfiguration();

        if ( configuration.getRepositoryGroupsAsMap().containsKey( repoGroupId ) )
        {
            if ( !updateMode )
            {
                throw new RepositoryAdminException( "Unable to add new repository group with id [" + repoGroupId
                                                        + "], that id already exists as a repository group." );
            }
        }
        else if ( configuration.getManagedRepositoriesAsMap().containsKey( repoGroupId ) )
        {
            throw new RepositoryAdminException( "Unable to add new repository group with id [" + repoGroupId
                                                    + "], that id already exists as a managed repository." );
        }
        else if ( configuration.getRemoteRepositoriesAsMap().containsKey( repoGroupId ) )
        {
            throw new RepositoryAdminException( "Unable to add new repository group with id [" + repoGroupId
                                                    + "], that id already exists as a remote repository." );
        }

        return Boolean.TRUE;
    }

    private void validateManagedRepositoriesExists( List<String> managedRepositoriesIds )
        throws RepositoryAdminException
    {
        for ( String id : managedRepositoriesIds )
        {
            if ( getManagedRepositoryAdmin().getManagedRepository( id ) == null )
            {
                throw new RepositoryAdminException(
                    "managedRepository with id " + id + " not exists so cannot be used in a repositoryGroup" );
            }
        }
    }

    public ManagedRepositoryAdmin getManagedRepositoryAdmin()
    {
        return managedRepositoryAdmin;
    }

    public void setManagedRepositoryAdmin( ManagedRepositoryAdmin managedRepositoryAdmin )
    {
        this.managedRepositoryAdmin = managedRepositoryAdmin;
    }
}
