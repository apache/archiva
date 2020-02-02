package org.apache.archiva.admin.repository;
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
import org.apache.archiva.admin.model.beans.AbstractRepository;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.components.registry.Registry;
import org.apache.archiva.components.scheduler.CronExpressionValidator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.regex.Pattern;

/**
 * apply basic repository validation : id and name.
 * Check if already exists.
 *
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@Service
public class DefaultRepositoryCommonValidator
    implements RepositoryCommonValidator
{

    private static final Pattern REPOSITORY_ID_VALID_EXPRESSION_PATTERN = Pattern.compile( REPOSITORY_ID_VALID_EXPRESSION );
    private static final Pattern REPOSITORY_NAME_VALID_EXPRESSION_PATTERN = Pattern.compile( REPOSITORY_NAME_VALID_EXPRESSION );
    private static final Pattern REPOSITORY_LOCATION_VALID_EXPRESSION_PATTERN = Pattern.compile( ManagedRepositoryAdmin.REPOSITORY_LOCATION_VALID_EXPRESSION );


    @Inject
    private ArchivaConfiguration archivaConfiguration;

    @Inject
    @Named( value = "commons-configuration" )
    private org.apache.archiva.components.registry.Registry registry;

    /**
     * @param abstractRepository
     * @param update             in update mode if yes already exists won't be check
     * @throws RepositoryAdminException
     */
    @Override
    public void basicValidation( AbstractRepository abstractRepository, boolean update )
        throws RepositoryAdminException
    {
        Configuration config = archivaConfiguration.getConfiguration( );

        String repoId = abstractRepository.getId( );

        if ( !update )
        {

            if ( config.getManagedRepositoriesAsMap( ).containsKey( repoId ) )
            {
                throw new RepositoryAdminException( "Unable to add new repository with id [" + repoId
                    + "], that id already exists as a managed repository." );
            }
            else if ( config.getRepositoryGroupsAsMap( ).containsKey( repoId ) )
            {
                throw new RepositoryAdminException( "Unable to add new repository with id [" + repoId
                    + "], that id already exists as a repository group." );
            }
            else if ( config.getRemoteRepositoriesAsMap( ).containsKey( repoId ) )
            {
                throw new RepositoryAdminException( "Unable to add new repository with id [" + repoId
                    + "], that id already exists as a remote repository." );
            }
        }

        if ( StringUtils.isBlank( repoId ) )
        {
            throw new RepositoryAdminException( "Repository ID cannot be empty." );
        }

        if ( !REPOSITORY_ID_VALID_EXPRESSION_PATTERN.matcher( repoId ).matches( ) )
        {
            throw new RepositoryAdminException(
                "Invalid repository ID. Identifier must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        }

        String name = abstractRepository.getName( );

        if ( StringUtils.isBlank( name ) )
        {
            throw new RepositoryAdminException( "repository name cannot be empty" );
        }

        if ( !REPOSITORY_NAME_VALID_EXPRESSION_PATTERN.matcher( name ).matches( ) )
        {
            throw new RepositoryAdminException(
                "Invalid repository name. Repository Name must only contain alphanumeric characters, white-spaces(' '), "
                    + "forward-slashes(/), open-parenthesis('('), close-parenthesis(')'),  underscores(_), dots(.), and dashes(-)." );
        }

    }

    /**
     * validate cronExpression and location format
     *
     * @param managedRepository
     * @since 1.4-M2
     */
    @Override
    public void validateManagedRepository( ManagedRepository managedRepository )
        throws RepositoryAdminException
    {
        String cronExpression = managedRepository.getCronExpression( );
        // FIXME : olamy can be empty to avoid scheduled scan ?
        if ( StringUtils.isNotBlank( cronExpression ) )
        {
            CronExpressionValidator validator = new CronExpressionValidator( );

            if ( !validator.validate( cronExpression ) )
            {
                throw new RepositoryAdminException( "Invalid cron expression.", "cronExpression" );
            }
        }
        else
        {
            throw new RepositoryAdminException( "Cron expression cannot be empty." );
        }

        String repoLocation = removeExpressions( managedRepository.getLocation( ) );

        if ( !REPOSITORY_LOCATION_VALID_EXPRESSION_PATTERN.matcher( repoLocation ).matches() )
        {
            throw new RepositoryAdminException(
                "Invalid repository location. Directory must only contain alphanumeric characters, equals(=), question-marks(?), "
                    + "exclamation-points(!), ampersands(&amp;), forward-slashes(/), back-slashes(\\), underscores(_), dots(.), colons(:), tildes(~), and dashes(-).",
                "location" );
        }
    }

    /**
     * replace some interpolations ${appserver.base} with correct values
     *
     * @param directory
     * @return
     */
    @Override
    public String removeExpressions( String directory )
    {
        String value = StringUtils.replace( directory, "${appserver.base}",
            getRegistry( ).getString( "appserver.base", "${appserver.base}" ) );
        value = StringUtils.replace( value, "${appserver.home}",
            getRegistry( ).getString( "appserver.home", "${appserver.home}" ) );
        return value;
    }

    public ArchivaConfiguration getArchivaConfiguration( )
    {
        return archivaConfiguration;
    }

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }

    public Registry getRegistry( )
    {
        return registry;
    }

    public void setRegistry( org.apache.archiva.components.registry.Registry registry )
    {
        this.registry = registry;
    }
}
