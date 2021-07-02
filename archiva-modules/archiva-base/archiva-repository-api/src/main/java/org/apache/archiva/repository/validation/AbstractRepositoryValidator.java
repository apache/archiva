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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public abstract class AbstractRepositoryValidator<R extends Repository> implements RepositoryValidator<R>
{
    protected RepositoryRegistry repositoryRegistry;
    private final String category;

    public AbstractRepositoryValidator( String category )
    {
        this.category = category;
    }

    @Override
    public void setRepositoryRegistry( RepositoryRegistry repositoryRegistry )
    {
        this.repositoryRegistry = repositoryRegistry;
    }

    protected String getCategory() {
        return this.category;
    }



    protected Map<String, List<ValidationError>> appendError( Map<String, List<ValidationError>> errorMap, String attribute, String type, Object... parameter )
    {
        String errorKey = getCategory( ) + "." + attribute + "." + type;
        Map<String, List<ValidationError>> result;
        result = errorMap == null ? new HashMap<>( ) : errorMap;
        ValidationError error = ValidationError.ofKey( errorKey, parameter );
        List<ValidationError> errList = result.computeIfAbsent( error.getAttribute( ), k -> new ArrayList<>( ) );
        errList.add( error );
        return result;
    }

    protected abstract ValidationResponse<R> apply( R repo, boolean update );

    @Override
    public ValidationResponse<R> apply( R r )
    {
        return apply( r, false );
    }

    @Override
    public ValidationResponse<R> applyForUpdate( R repo )
    {
        return apply( repo, true );
    }
}
