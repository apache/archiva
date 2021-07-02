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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A validation response gives information about the validation status for certain attributes.
 *
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class ValidationResponse<R extends Repository> implements CheckedResult<R, Map<String, List<ValidationError>>>
{
    final boolean valid;
    final R repository;
    final Map<String, List<ValidationError>> validationErrors = new HashMap<>( );


    public ValidationResponse( R repo, Map<String, List<ValidationError>> errors)
    {
        if( errors==null || errors.size()==0 ) {
            this.valid = true;
        } else {
            this.valid = false;
            validationErrors.putAll( errors );
        }
        this.repository = repo;
    }

    public static <S extends Repository> ValidationResponse<S> getValid( S repository )
    {
        return new ValidationResponse<>( repository, null );
    }

    @Override
    public R getRepository( )
    {
        return repository;
    }

    /**
     * Returns true, if the validation was successful and there are not validation errors.
     * @return <code>true</code>, if the validation was successful, otherwise <code>false</code>
     */
    @Override
    public boolean isValid( )
    {
        return valid;
    }

    @Override
    public Map<String, List<ValidationError>> getResult( )
    {
        return validationErrors;
    }


    /**
     * Add the given validation error to the list for the given attribute.
     *
     * @param attribute the name of the attribute
     * @param error the error that is added to the list
     */
    public void addValidationError(String attribute, ValidationError error) {
        if (!validationErrors.containsKey( attribute )) {
            validationErrors.put( attribute, new ArrayList<>( ) );
        }
        validationErrors.get( attribute ).add( error );
    }

    /**
     * Returns a list of validation errors that are stored for the given attribute. If there are no
     * errors stored for this attribute, a empty list is returned.
     *
     * @param attribute the name of the attribute
     * @return the list of validation errors
     */
    public List<ValidationError> getValidationErrors(String attribute) {
        if (validationErrors.containsKey( attribute )) {
            return validationErrors.get( attribute );
        } else {
            return Collections.emptyList( );
        }
    }

}
