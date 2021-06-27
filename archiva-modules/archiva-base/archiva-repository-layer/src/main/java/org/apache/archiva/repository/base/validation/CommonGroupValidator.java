package org.apache.archiva.repository.base.validation;
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

import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryGroup;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.base.ConfigurationHandler;
import org.apache.archiva.repository.validation.AbstractRepositoryValidator;
import org.apache.archiva.repository.validation.RepositoryValidator;
import org.apache.archiva.repository.validation.ValidationError;
import org.apache.archiva.repository.validation.ValidationResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * A validator for repository groups. All validation errors are prefixed with category 'repository_group'.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service( "repositoryValidator#common#group" )
public class CommonGroupValidator extends AbstractRepositoryValidator<RepositoryGroup> implements RepositoryValidator<RepositoryGroup>
{

    private static final Pattern REPO_GROUP_ID_PATTERN = Pattern.compile( "[A-Za-z0-9\\._\\-]+" );
    private final ConfigurationHandler configurationHandler;

    private RepositoryRegistry repositoryRegistry;

    public CommonGroupValidator( ConfigurationHandler configurationHandler )
    {
        this.configurationHandler = configurationHandler;
    }


    private Map<String, List<ValidationError>> appendError( Map<String, List<ValidationError>> errorMap, String errorKey, Object... parameter )
    {
        Map<String, List<ValidationError>> result;
        result = errorMap == null ? new HashMap<>( ) : errorMap;
        ValidationError error = ValidationError.ofKey( errorKey, parameter );
        List<ValidationError> errList = result.computeIfAbsent( error.getAttribute( ), k -> new ArrayList<ValidationError>( ) );
        errList.add( error );
        return result;
    }

    public ValidationResponse apply( RepositoryGroup repositoryGroup, boolean updateMode ) throws IllegalArgumentException
    {
        final String repoGroupId = repositoryGroup.getId( );
        Map<String, List<ValidationError>> errors = null;
        if ( StringUtils.isBlank( repoGroupId ) )
        {
            errors = appendError( errors, "repository_group.id.empty" );
        }

        if ( repoGroupId.length( ) > 100 )
        {
            errors = appendError( errors, "repository_group.id.max_length", repoGroupId, Integer.toString( 100 ) );

        }

        Matcher matcher = REPO_GROUP_ID_PATTERN.matcher( repoGroupId );
        if ( !matcher.matches( ) )
        {
            errors = appendError( errors, "repository_group.id.invalid_chars", "alphanumeric, '.', '-','_'" );
        }

        if ( repositoryGroup.getMergedIndexTTL( ) <= 0 )
        {
            errors = appendError( errors, "repository_group.merged_index_ttl.min", "0" );
        }


        if ( repositoryRegistry != null && !updateMode )
        {
            if ( repositoryRegistry.hasRepositoryGroup( repoGroupId ) )
            {
                errors = appendError( errors, "repository_group.id.group_exists", repoGroupId );
            }
            else if ( repositoryRegistry.hasManagedRepository( repoGroupId ) )
            {
                errors = appendError( errors, "repository_group.id.managed_exists" );
            }
            else if ( repositoryRegistry.hasRemoteRepository( repoGroupId ) )
            {
                errors = appendError( errors, "repository_group.id.remote_exists" );
            }
        }
        return new ValidationResponse(repositoryGroup, errors );
    }




    public ConfigurationHandler getConfigurationHandler( )
    {
        return configurationHandler;
    }

    public RepositoryRegistry getRepositoryRegistry( )
    {
        return repositoryRegistry;
    }

    @Override
    public void setRepositoryRegistry( RepositoryRegistry repositoryRegistry )
    {
        this.repositoryRegistry = repositoryRegistry;
    }

    @Override
    public Class<RepositoryGroup> getFlavour( )
    {
        return RepositoryGroup.class;
    }

    @Override
    public boolean isFlavour( Class<?> clazz )
    {
        return RepositoryGroup.class.isAssignableFrom( clazz );
    }
}
