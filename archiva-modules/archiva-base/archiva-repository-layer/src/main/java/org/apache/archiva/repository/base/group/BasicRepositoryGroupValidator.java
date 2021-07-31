package org.apache.archiva.repository.base.group;
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

import org.apache.archiva.repository.RepositoryGroup;
import org.apache.archiva.repository.RepositoryRegistry;
import org.apache.archiva.repository.base.ConfigurationHandler;
import org.apache.archiva.repository.validation.AbstractRepositoryValidator;
import org.apache.archiva.repository.validation.RepositoryValidator;
import org.apache.archiva.repository.validation.ValidationError;
import org.apache.archiva.repository.validation.ValidationResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.archiva.repository.validation.ErrorKeys.*;

/**
 *
 * A validator for repository groups. All validation errors are prefixed with category 'repository_group'.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service( "repositoryValidator#common#group" )
public class BasicRepositoryGroupValidator extends AbstractRepositoryValidator<RepositoryGroup> implements RepositoryValidator<RepositoryGroup>
{

    private static final String CATEGORY = "repository_group";
    private static final Pattern REPOSITORY_ID_VALID_EXPRESSION_PATTERN = Pattern.compile( REPOSITORY_ID_VALID_EXPRESSION );
    private static final Pattern REPOSITORY_NAME_VALID_EXPRESSION_PATTERN = Pattern.compile( REPOSITORY_NAME_VALID_EXPRESSION );

    private final ConfigurationHandler configurationHandler;

    private RepositoryRegistry repositoryRegistry;

    public BasicRepositoryGroupValidator( ConfigurationHandler configurationHandler )
    {
        super( CATEGORY );
        this.configurationHandler = configurationHandler;
    }


    @Override
    public ValidationResponse<RepositoryGroup> apply( RepositoryGroup repositoryGroup, boolean updateMode ) throws IllegalArgumentException
    {
        Map<String, List<ValidationError>> errors = null;
        if (repositoryGroup==null) {
            errors = appendError( null, "object", ISNULL );
            return new ValidationResponse<>( repositoryGroup, errors );
        }
        final String repoGroupId = repositoryGroup.getId( );
        if ( StringUtils.isBlank( repoGroupId ) )
        {
            errors = appendError( null, "id", ISEMPTY );
        } else
        {

            if ( repoGroupId.length( ) > 100 )
            {
                errors = appendError( errors, "id", MAX_LENGTH_EXCEEDED, repoGroupId, Integer.toString( 100 ) );

            }

            Matcher matcher = REPOSITORY_ID_VALID_EXPRESSION_PATTERN.matcher( repoGroupId );
            if ( !matcher.matches( ) )
            {
                errors = appendError( errors, "id", INVALID_CHARS, repoGroupId, REPOSITORY_ID_ALLOWED );
            }
        }

        if ( repositoryGroup.getMergedIndexTTL( ) <= 0 )
        {
            errors = appendError( errors, "merged_index_ttl",BELOW_MIN, "0" );
        }

        if (StringUtils.isBlank( repositoryGroup.getName() )) {
            errors = appendError( errors, "name", ISEMPTY );
        } else if ( !REPOSITORY_NAME_VALID_EXPRESSION_PATTERN.matcher( repositoryGroup.getName( ) ).matches( ) )
        {
            errors = appendError( errors, "name", INVALID_CHARS, repoGroupId, REPOSITORY_NAME_ALLOWED );
        }



        if ( repositoryRegistry != null && !updateMode )
        {
            if ( repositoryRegistry.hasRepositoryGroup( repoGroupId ) )
            {
                errors = appendError( errors, "id", REPOSITORY_GROUP_EXISTS, repoGroupId );
            }
            else if ( repositoryRegistry.hasManagedRepository( repoGroupId ) )
            {
                errors = appendError( errors, "id", MANAGED_REPOSITORY_EXISTS );
            }
            else if ( repositoryRegistry.hasRemoteRepository( repoGroupId ) )
            {
                errors = appendError( errors, "id", REMOTE_REPOSITORY_EXISTS );
            }
        }
        return new ValidationResponse<>(repositoryGroup, errors );
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

}
