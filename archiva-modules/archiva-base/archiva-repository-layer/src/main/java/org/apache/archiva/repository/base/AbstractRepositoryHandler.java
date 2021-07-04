package org.apache.archiva.repository.base;
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
import org.apache.archiva.repository.RepositoryHandler;
import org.apache.archiva.repository.validation.CombinedValidator;
import org.apache.archiva.repository.validation.RepositoryValidator;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base abstract class for repository handlers.
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public abstract class AbstractRepositoryHandler<R extends Repository, C> implements RepositoryHandler<R, C>
{
    protected List<RepositoryValidator<R>> initValidators( Class<R> clazz, List<RepositoryValidator<? extends Repository>> repositoryGroupValidatorList) {
        if (repositoryGroupValidatorList!=null && repositoryGroupValidatorList.size()>0) {
            return repositoryGroupValidatorList.stream( ).filter(
                v -> v.isFlavour( clazz )
            ).map( v -> v.narrowTo( clazz ) ).collect( Collectors.toList( ) );
        } else {
            return Collections.emptyList( );
        }
    }

    protected CombinedValidator<R> getCombinedValidatdor(Class<R> clazz, List<RepositoryValidator<? extends Repository>> repositoryGroupValidatorList) {
        return new CombinedValidator<>( clazz, initValidators( clazz, repositoryGroupValidatorList ) );
    }

}
