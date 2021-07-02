package org.apache.archiva.repository.validation;
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
import org.apache.archiva.repository.RepositoryRegistry;

import java.util.List;
import java.util.Map;

/**
 * A combined validator cumulates the validation results of multiple validators
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class CombinedValidator<R extends Repository>
implements RepositoryValidator<R> {

    private final List<RepositoryValidator<R>> validatorList;
    private final Class<R> flavourClazz;

    public CombinedValidator( Class<R> flavourClazz, List<RepositoryValidator<R>> validatorList )
    {
        if (flavourClazz==null) {
            throw new IllegalArgumentException( "The flavour class may not be null" );
        }
        this.flavourClazz = flavourClazz;
        if (validatorList==null) {
            throw new IllegalArgumentException( "The validator list may not be null" );
        }
        this.validatorList = validatorList;
    }

    @Override
    public CheckedResult<R, Map<String, List<ValidationError>>> apply( R r )
    {
        CombinedValidationResponse<R> response = new CombinedValidationResponse<>( r );
        validatorList.stream( ).forEach(
            v -> response.addResult( v.apply( r ) )
        );
        return response;
    }

    @Override
    public CheckedResult<R, Map<String, List<ValidationError>>> applyForUpdate( R repo )
    {
        CombinedValidationResponse<R> response = new CombinedValidationResponse<>( repo );
        validatorList.stream( ).forEach(
            v -> response.addResult( v.applyForUpdate( repo ) )
        );
        return response;

    }

    @Override
    public void setRepositoryRegistry( RepositoryRegistry repositoryRegistry )
    {
        // Not used
    }

    @Override
    public Class<R> getFlavour( )
    {
        return flavourClazz;
    }

    @Override
    public boolean isFlavour( Class<?> clazz )
    {
        return flavourClazz.isAssignableFrom( clazz );
    }
}
