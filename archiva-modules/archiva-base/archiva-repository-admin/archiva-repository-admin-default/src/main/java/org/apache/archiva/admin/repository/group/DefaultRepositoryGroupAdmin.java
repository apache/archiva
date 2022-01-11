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
import org.apache.archiva.admin.model.EntityExistsException;
import org.apache.archiva.admin.model.EntityNotFoundException;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.beans.RepositoryGroup;
import org.apache.archiva.admin.model.group.RepositoryGroupAdmin;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.admin.repository.AbstractRepositoryAdmin;
import org.apache.archiva.configuration.model.Configuration;
import org.apache.archiva.configuration.model.RepositoryGroupConfiguration;
import org.apache.archiva.event.Event;
import org.apache.archiva.event.EventHandler;
import org.apache.archiva.event.EventType;
import org.apache.archiva.metadata.model.facets.AuditEvent;
import org.apache.archiva.repository.EditableRepositoryGroup;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.event.RepositoryRegistryEvent;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Olivier Lamy
 */
@Service("repositoryGroupAdmin#default")
public class DefaultRepositoryGroupAdmin
    extends AbstractRepositoryAdmin
    implements RepositoryGroupAdmin, EventHandler<Event>
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    private static final Pattern REPO_GROUP_ID_PATTERN = Pattern.compile( "[A-Za-z0-9\\._\\-]+" );

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    private RepositoryRegistry repositoryRegistry;

    @PostConstruct
    public void baseInit() {
        this.repositoryRegistry.registerEventHandler( EventType.ROOT, this );
    }

    public void initialize()
    {
    }


    @Override
    public StorageAsset getMergedIndexDirectory(String repositoryGroupId )
    {
        org.apache.archiva.repository.RepositoryGroup group = repositoryRegistry.getRepositoryGroup(repositoryGroupId);
        if (group!=null) {
            return group.getFeature( IndexCreationFeature.class ).getLocalIndexPath();
        } else {
            return null;
        }
    }

    @Override
    public List<RepositoryGroup> getRepositoriesGroups() {
        return repositoryRegistry.getRepositoryGroups().stream().map( r -> convertRepositoryGroupObject( r ) ).collect( Collectors.toList());
    }

    @Override
    public RepositoryGroup getRepositoryGroup( String repositoryGroupId ) throws EntityNotFoundException
    {
        org.apache.archiva.repository.RepositoryGroup group = repositoryRegistry.getRepositoryGroup( repositoryGroupId );
        if (group==null) {
            throw new EntityNotFoundException( "Repository group does not exist" );
        } else {
            return convertRepositoryGroupObject( group );
        }
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
        repositoryGroupConfiguration.setCronExpression( StringUtils.isEmpty(repositoryGroup.getCronExpression()) ? "0 0 03 ? * MON" : repositoryGroup.getCronExpression() );

        try {
            repositoryRegistry.putRepositoryGroup(repositoryGroupConfiguration);
        } catch (RepositoryException e) {
            log.error( "Could not add the repository group to the registry: {}", e.getMessage( ), e );
            throw RepositoryAdminException.ofKey( "repository_group.registry.add_error", e, repositoryGroup.getId(), e.getMessage() );
        }

        triggerAuditEvent( repositoryGroup.getId(), null, AuditEvent.ADD_REPO_GROUP, auditInformation );
        return Boolean.TRUE;
    }

    @Override
    public Boolean deleteRepositoryGroup( String repositoryGroupId, AuditInformation auditInformation )
        throws RepositoryAdminException
    {

        org.apache.archiva.repository.RepositoryGroup repositoryGroup = repositoryRegistry.getRepositoryGroup(repositoryGroupId);
        try {
            repositoryRegistry.removeRepositoryGroup(repositoryGroup);
        } catch (RepositoryException e) {
            log.error("Removal of repository group {} failed: {}", repositoryGroup.getId(), e.getMessage(), e);
            throw new RepositoryAdminException("Removal of repository failed: " + e.getMessage(), e);
        }
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

        repositoryGroupConfiguration.setRepositories( repositoryGroup.getRepositories() );
        repositoryGroupConfiguration.setMergedIndexPath( repositoryGroup.getMergedIndexPath() );
        repositoryGroupConfiguration.setMergedIndexTtl( repositoryGroup.getMergedIndexTtl() );
        repositoryGroupConfiguration.setCronExpression( repositoryGroup.getCronExpression() );
        try {
            repositoryRegistry.putRepositoryGroup(repositoryGroupConfiguration);
        } catch (RepositoryException e) {
            log.error( "Could not update the repository group in the registry: {}", e.getMessage( ), e );
            throw RepositoryAdminException.ofKey( "repository_group.registry.update_error", e, repositoryGroup.getId(), e.getMessage() );
        }

        org.apache.archiva.repository.RepositoryGroup rg = repositoryRegistry.getRepositoryGroup( repositoryGroup.getId( ) );
        triggerAuditEvent( repositoryGroup.getId(), null, AuditEvent.MODIFY_REPO_GROUP, auditInformation );
        return Boolean.TRUE;
    }


    @Override
    public Boolean addRepositoryToGroup( String repositoryGroupId, String repositoryId,
                                         AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        org.apache.archiva.repository.RepositoryGroup repositoryGroup = repositoryRegistry.getRepositoryGroup( repositoryGroupId );
        if ( repositoryGroup == null )
        {
            throw EntityNotFoundException.ofMessage(
                    "Repository group with id " + repositoryGroupId + " doesn't not exists so cannot add repository to it", repositoryGroupId );
        }

        if (!(repositoryGroup instanceof EditableRepositoryGroup)) {
            throw RepositoryAdminException.ofKey("repository_group.not_editable",repositoryGroupId);
        }
        EditableRepositoryGroup editableRepositoryGroup = (EditableRepositoryGroup) repositoryGroup;
        if ( editableRepositoryGroup.getRepositories().stream().anyMatch( repo -> repositoryId.equals(repo.getId())) )
        {
            throw new EntityExistsException(
                "Repository group with id " + repositoryGroupId + " already contain repository with id" + repositoryId );
        }
        org.apache.archiva.repository.ManagedRepository managedRepo = repositoryRegistry.getManagedRepository(repositoryId);
        if (managedRepo==null) {
            throw EntityNotFoundException.ofMessage("Repository with id "+repositoryId+" does not exist", repositoryId );
        }

        editableRepositoryGroup.addRepository( managedRepo );
        try {
            repositoryRegistry.putRepositoryGroup(editableRepositoryGroup);
        } catch (RepositoryException e) {
            throw new RepositoryAdminException("Could not store the repository group "+repositoryGroupId, e);
        }
        triggerAuditEvent( repositoryGroup.getId(), null, AuditEvent.ADD_REPO_TO_GROUP, auditInformation );
        return Boolean.TRUE;
    }

    @Override
    public Boolean deleteRepositoryFromGroup( String repositoryGroupId, String repositoryId,
                                              AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        org.apache.archiva.repository.RepositoryGroup repositoryGroup = repositoryRegistry.getRepositoryGroup( repositoryGroupId );
        if ( repositoryGroup == null )
        {
            throw EntityNotFoundException.ofMessage( "Repository group with id " + repositoryGroupId
                                                    + " doesn't not exists so cannot remove repository from it", repositoryGroupId );
        }

        if ( !repositoryGroup.getRepositories().stream().anyMatch( repo -> repositoryId.equals(repo.getId()) ) )
        {
            throw EntityNotFoundException.ofMessage(
                "repositoryGroup with id " + repositoryGroupId + " doesn't not contains repository with id"
                    + repositoryId, repositoryId
            );
        }

        if (!(repositoryGroup instanceof EditableRepositoryGroup)) {
            throw RepositoryAdminException.ofKey("repository_group.not_editable",repositoryGroupId);
        }
        EditableRepositoryGroup editableRepositoryGroup = (EditableRepositoryGroup) repositoryGroup;

        editableRepositoryGroup.removeRepository( repositoryId );
        try {
            repositoryRegistry.putRepositoryGroup(editableRepositoryGroup);
        } catch (RepositoryException e) {
            throw new RepositoryAdminException("Could not store repository group " + repositoryGroupId, e);
        }
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
            throw RepositoryAdminException.ofKey("repository_group.id.empty" );
        }

        if ( repoGroupId.length() > 100 )
        {
            throw RepositoryAdminException.ofKey("repository_group.id.max_length",repoGroupId, Integer.toString( 100 ));

        }

        Matcher matcher = REPO_GROUP_ID_PATTERN.matcher( repoGroupId );
        if ( !matcher.matches() )
        {
            throw RepositoryAdminException.ofKey("repository_group.id.invalid_chars","alphanumeric, '.', '-','_'" );
        }

        if ( repositoryGroup.getMergedIndexTtl() <= 0 )
        {
            throw RepositoryAdminException.ofKey("repository_group.merged_index_ttl.min","0" );
        }

        Configuration configuration = getArchivaConfiguration().getConfiguration();

        if ( configuration.getRepositoryGroupsAsMap().containsKey( repoGroupId ) )
        {
            if ( !updateMode )
            {
                throw new EntityExistsException( "Unable to add new repository group with id [" + repoGroupId
                                                        + "], that id already exists as a repository group." );
            }
        }
        else if ( configuration.getManagedRepositoriesAsMap().containsKey( repoGroupId ) )
        {
            throw new EntityExistsException( "Unable to add new repository group with id [" + repoGroupId
                                                    + "], that id already exists as a managed repository." );
        }
        else if ( configuration.getRemoteRepositoriesAsMap().containsKey( repoGroupId ) )
        {
            throw new EntityExistsException( "Unable to add new repository group with id [" + repoGroupId
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
                throw RepositoryAdminException.ofKey("repository_group.repository.not_found",id );
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

    private RepositoryGroup convertRepositoryGroupObject( org.apache.archiva.repository.RepositoryGroup group ) {
        RepositoryGroup rg = new RepositoryGroup( group.getId( ), group.getRepositories().stream().map(r -> r.getId()).collect( Collectors.toList()) );
        if (group.supportsFeature( IndexCreationFeature.class ))
        {
            IndexCreationFeature indexCreationFeature = group.getFeature( IndexCreationFeature.class );
            rg.setMergedIndexPath( indexCreationFeature.getIndexPath().getPath() );
        }
        rg.setCronExpression( group.getSchedulingDefinition() );
        rg.setMergedIndexTtl( group.getMergedIndexTTL() );
        rg.setLocation( group.getLocation().toString() );
        return rg;
    }

    @Override
    public void handle( Event event )
    {
        if ( EventType.isInstanceOf( event.getType( ), RepositoryRegistryEvent.INITIALIZED ) )
        {
            log.debug( "Initializing RepositoryGroupAdmin" );
            initialize();
        }
    }
}
