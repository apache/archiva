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
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class CombinedValidationResponse<R extends Repository> implements CheckedResult<R, Map<String, List<ValidationError>>>
{

    private final Map<String, List<ValidationError>> errorMap = new HashMap<>( );
    private final R repository;

    public CombinedValidationResponse( R repository )
    {
        this.repository = repository;
    }

    @Override
    public R getRepository( )
    {
        return repository;
    }

    @Override
    public boolean isValid( )
    {
        return errorMap.size()==0;
    }

    @Override
    public Map<String, List<ValidationError>> getResult( )
    {
        return errorMap;
    }

    public void addErrors(String key, List<ValidationError> errorList) {
        if ( StringUtils.isNotEmpty( key ) && errorList!=null && errorList.size()>0) {
            this.errorMap.put( key, errorList );
        }
    }

    public void addErrors(Map<String, List<ValidationError>> errorMap) {
        if (errorMap!=null) {
            errorMap.entrySet( ).stream( ).forEach( e -> addErrors( e.getKey( ), e.getValue( ) ) );
        }
    }

    public void addResult(CheckedResult<R, Map<String, List<ValidationError>>> result) {
        this.addErrors( result.getResult( ) );
    }
}
