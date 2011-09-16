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

import org.apache.archiva.admin.model.AbstractRepository;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.configuration.Configuration;
import org.codehaus.plexus.registry.Registry;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * apply basic repository validation : id and name.
 * Check if already exists.
 *
 * @author Olivier Lamy
 * @since 1.4
 */
@Service
public class RepositoryCommonValidator
{

    public static final String REPOSITORY_ID_VALID_EXPRESSION = "^[a-zA-Z0-9._-]+$";

    public static final String REPOSITORY_NAME_VALID_EXPRESSION = "^([a-zA-Z0-9.)/_(-]|\\s)+$";


    @Inject
    private ArchivaConfiguration archivaConfiguration;

    @Inject
    @Named( value = "commons-configuration" )
    private Registry registry;

    /**
     * @param abstractRepository
     * @param update             in update mode if yes already exists won't be check
     * @throws RepositoryAdminException
     */
    public void basicValidation( AbstractRepository abstractRepository, boolean update )
        throws RepositoryAdminException
    {
        Configuration config = archivaConfiguration.getConfiguration();

        String repoId = abstractRepository.getId();

        if ( !update )
        {

            if ( config.getManagedRepositoriesAsMap().containsKey( repoId ) )
            {
                throw new RepositoryAdminException( "Unable to add new repository with id [" + repoId
                                                        + "], that id already exists as a managed repository." );
            }
            else if ( config.getRepositoryGroupsAsMap().containsKey( repoId ) )
            {
                throw new RepositoryAdminException( "Unable to add new repository with id [" + repoId
                                                        + "], that id already exists as a repository group." );
            }
            else if ( config.getRemoteRepositoriesAsMap().containsKey( repoId ) )
            {
                throw new RepositoryAdminException( "Unable to add new repository with id [" + repoId
                                                        + "], that id already exists as a remote repository." );
            }
        }

        if ( StringUtils.isBlank( repoId ) )
        {
            throw new RepositoryAdminException( "Repository ID cannot be empty." );
        }

        if ( !GenericValidator.matchRegexp( repoId, REPOSITORY_ID_VALID_EXPRESSION ) )
        {
            throw new RepositoryAdminException(
                "Invalid repository ID. Identifier must only contain alphanumeric characters, underscores(_), dots(.), and dashes(-)." );
        }

        String name = abstractRepository.getName();

        if ( StringUtils.isBlank( name ) )
        {
            throw new RepositoryAdminException( "repository name cannot be empty" );
        }

        if ( !GenericValidator.matchRegexp( name, REPOSITORY_NAME_VALID_EXPRESSION ) )
        {
            throw new RepositoryAdminException(
                "Invalid repository name. Repository Name must only contain alphanumeric characters, white-spaces(' '), "
                    + "forward-slashes(/), open-parenthesis('('), close-parenthesis(')'),  underscores(_), dots(.), and dashes(-)." );
        }


    }

    /**
     * replace some interpolations ${appserver.base} with correct values
     *
     * @param directory
     * @return
     */
    public String removeExpressions( String directory )
    {
        String value = StringUtils.replace( directory, "${appserver.base}",
                                            getRegistry().getString( "appserver.base", "${appserver.base}" ) );
        value = StringUtils.replace( value, "${appserver.home}",
                                     getRegistry().getString( "appserver.home", "${appserver.home}" ) );
        return value;
    }

    public ArchivaConfiguration getArchivaConfiguration()
    {
        return archivaConfiguration;
    }

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }

    public Registry getRegistry()
    {
        return registry;
    }

    public void setRegistry( Registry registry )
    {
        this.registry = registry;
    }
}
