package org.apache.archiva.repository.base.managed;
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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.components.registry.Registry;
import org.apache.archiva.repository.base.ConfigurationHandler;
import org.apache.archiva.repository.validation.AbstractRepositoryValidator;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.validation.RepositoryValidator;
import org.apache.archiva.repository.validation.ValidationError;
import org.apache.archiva.repository.validation.ValidationResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.apache.archiva.components.scheduler.CronExpressionValidator;


import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.apache.archiva.repository.validation.ErrorKeys.*;

/**
 * Validator for managed repository data.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service( "repositoryValidator#common#managed" )
public class BasicManagedRepositoryValidator extends AbstractRepositoryValidator<ManagedRepository> implements RepositoryValidator<ManagedRepository>
{
    RepositoryRegistry repositoryRegistry;
    private static final String CATEGORY = "managed_repository";
    private static final Pattern REPOSITORY_ID_VALID_EXPRESSION_PATTERN = Pattern.compile( REPOSITORY_ID_VALID_EXPRESSION );
    private static final Pattern REPOSITORY_NAME_VALID_EXPRESSION_PATTERN = Pattern.compile( REPOSITORY_NAME_VALID_EXPRESSION );
    private static final Pattern REPOSITORY_LOCATION_VALID_EXPRESSION_PATTERN = Pattern.compile( REPOSITORY_LOCATION_VALID_EXPRESSION );

    private final ConfigurationHandler configurationHandler;


    public BasicManagedRepositoryValidator( ConfigurationHandler configurationHandler)
    {
        super( CATEGORY );
        this.configurationHandler = configurationHandler;
    }

    @Override
    public ValidationResponse<ManagedRepository> apply( ManagedRepository managedRepository, boolean update )
    {
        Map<String, List<ValidationError>> errors = null;
        if (managedRepository==null) {
            errors = appendError( errors, "object", ISNULL );
            return new ValidationResponse<>( managedRepository, errors );
        }
        final String repoId = managedRepository.getId( );
        if ( StringUtils.isBlank( repoId ) ) {
            errors = appendError( errors, "id", ISEMPTY );
        }

        if (!update)
        {
            if ( repositoryRegistry.hasManagedRepository( managedRepository.getId( ) ) )
            {
                errors = appendError( errors, "id", MANAGED_REPOSITORY_EXISTS, repoId );
            }
            else if ( repositoryRegistry.hasRemoteRepository( repoId ) )
            {
                errors = appendError( errors, "id", REMOTE_REPOSITORY_EXISTS, repoId );
            }
            else if ( repositoryRegistry.hasRepositoryGroup( repoId ) )
            {
                errors = appendError( errors, "id", REPOSITORY_GROUP_EXISTS, repoId );
            }
        }

        if ( !REPOSITORY_ID_VALID_EXPRESSION_PATTERN.matcher( repoId ).matches( ) )
        {
            errors = appendError( errors, "id", INVALID_CHARS, repoId, REPOSITORY_ID_ALLOWED );
        }
        if ( StringUtils.isBlank( managedRepository.getName() ) )
        {
            errors = appendError( errors, "name", ISEMPTY );
        } else if ( !REPOSITORY_NAME_VALID_EXPRESSION_PATTERN.matcher( managedRepository.getName( ) ).matches( ) )
        {
            errors = appendError( errors, "name", INVALID_CHARS, managedRepository.getName( ), REPOSITORY_NAME_ALLOWED );
        }

        String cronExpression = managedRepository.getSchedulingDefinition( );
        if ( StringUtils.isNotBlank( cronExpression ) )
        {
            CronExpressionValidator validator = new CronExpressionValidator( );

            if ( !validator.validate( cronExpression ) )
            {
                errors = appendError( errors, "scheduling_definition", INVALID_SCHEDULING_EXPRESSION, cronExpression );
            }
        }
        // Cron expression may be empty

        String repoLocation = interpolateVars( managedRepository.getLocation( ).toString() );

        if ( !REPOSITORY_LOCATION_VALID_EXPRESSION_PATTERN.matcher( repoLocation ).matches() )
        {
            errors = appendError( errors, "location", INVALID_LOCATION, repoLocation, new String[]{"alphanumeric", "=", "?", "!", "&", "/", "\\", "_", ".", ":", "~", "-"} );
        }

        return new ValidationResponse<>( managedRepository, errors );
    }

    public String interpolateVars( String directory )
    {
        Registry registry = configurationHandler.getArchivaConfiguration( ).getRegistry( );
        String value = StringUtils.replace( directory, "${appserver.base}",
            registry.getString( "appserver.base", "${appserver.base}" ) );
        value = StringUtils.replace( value, "${appserver.home}",
            registry.getString( "appserver.home", "${appserver.home}" ) );
        return value;
    }


    @Override
    public void setRepositoryRegistry( RepositoryRegistry repositoryRegistry )
    {
        this.repositoryRegistry = repositoryRegistry;
    }

    @Override
    public Class<ManagedRepository> getFlavour( )
    {
        return ManagedRepository.class;
    }

}
