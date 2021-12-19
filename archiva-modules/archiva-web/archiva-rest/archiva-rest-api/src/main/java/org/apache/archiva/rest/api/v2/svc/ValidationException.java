package org.apache.archiva.rest.api.v2.svc;
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
import org.apache.archiva.repository.validation.ValidationResponse;
import org.apache.archiva.rest.api.v2.model.ValidationError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Exception is thrown
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class ValidationException extends ArchivaRestServiceException
{
    public static final int DEFAULT_CODE = 422;

    public static final ErrorMessage DEFAULT_MESSAGE = new ErrorMessage( ErrorKeys.VALIDATION_ERROR );

    private List<ValidationError> validationErrors;

    public ValidationException( )
    {
        super( DEFAULT_MESSAGE, DEFAULT_CODE );
    }

    public ValidationException( int errorCode )
    {
        super( DEFAULT_MESSAGE, errorCode );
    }

    public ValidationException( List<ValidationError> errors )
    {
        super( DEFAULT_MESSAGE, DEFAULT_CODE );
        this.validationErrors = errors;
    }

    public static ValidationException of( List<org.apache.archiva.repository.validation.ValidationError> errorList )
    {
        return new ValidationException( errorList.stream( ).map( ValidationError::of ).collect( Collectors.toList( ) ) );
    }

    public static ValidationException of( Map<String, List<org.apache.archiva.repository.validation.ValidationError>> errorMap )
    {
        return new ValidationException( errorMap.entrySet( ).stream( )
            .flatMap( v -> v.getValue( ).stream( ).map( k -> ValidationError.of(v.getKey(), k)))
            .collect( Collectors.toList( ) ) );
    }

    public static <R extends Repository> ValidationException of( ValidationResponse<R> result )
    {
        if ( result.isValid( ) )
        {
            return new ValidationException( );
        }
        else
        {
            return new ValidationException( result.getResult( ).entrySet( ).stream( ).flatMap(
                v -> v.getValue( ).stream( ).map( e -> ValidationError.of( v.getKey( ), e ) ) ).collect( Collectors.toList( ) ) );
        }
    }

    public List<ValidationError> getValidationErrors( )
    {
        return validationErrors == null ? Collections.emptyList( ) : validationErrors;
    }

    public void setValidationErrors( List<ValidationError> validationErrors )
    {
        this.validationErrors = validationErrors;
    }

    public void addValidationError( ValidationError error )
    {
        if ( this.validationErrors == null )
        {
            this.validationErrors = new ArrayList<>( );
        }
        this.validationErrors.add( error );
    }
}
