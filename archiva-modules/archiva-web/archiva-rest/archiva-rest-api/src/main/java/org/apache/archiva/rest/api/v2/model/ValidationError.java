package org.apache.archiva.rest.api.v2.model;
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


import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@XmlRootElement(name = "validationError")
@Schema(name = "ValidationError", description = "A validation error.")
public class ValidationError implements Serializable, RestModel
{
    private static final long serialVersionUID = 2079020598090660171L;

    String key;
    String field;
    String category;
    String type;
    List<String> parameter;


    public ValidationError( )
    {
    }

    public ValidationError( String key, String field, String category, String type, List<String> parameter) {
        this.key = key;
        this.field = field;
        this.category = category;
        this.type = type;
        if (parameter==null) {
            this.parameter = new ArrayList<>( );
        } else
        {
            this.parameter = parameter;
        }
    }

    /**
     * Creates a new instance based on the given error
     * @param error the error instance
     * @return
     */
    public static ValidationError of( org.apache.archiva.repository.validation.ValidationError error ) {
        return error != null ? new ValidationError( error.getErrorKey( ), error.getAttribute( ), error.getCategory( ),
            error.getType( ), error.getArguments( ).stream( ).map( Object::toString ).collect( Collectors.toList( ) ) )
            : new ValidationError( );
    }

    /**
     * Creates a new instance based on the field name and the error instance
     * @param fieldName the name of the field to which the error applies
     * @param error the error definition
     * @return a new validation error instance
     */
    public static ValidationError of( String fieldName, org.apache.archiva.repository.validation.ValidationError error ) {
        return error != null ? new ValidationError( error.getErrorKey( ), fieldName, error.getCategory( ),
            error.getType( ), error.getArguments( ).stream( ).map( Object::toString ).collect( Collectors.toList( ) ) )
            : new ValidationError( );
    }

    @Schema(name="key", description = "The full key of the validation error")
    public String getKey( )
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    @Schema(name="field", description = "The name of the field where the error was detected")
    public String getField( )
    {
        return field;
    }

    public void setField( String field )
    {
        this.field = field;
    }

    @Schema(name="category", description = "The name of the category this error is assigned to")
    public String getCategory( )
    {
        return category;
    }

    public void setCategory( String category )
    {
        this.category = category;
    }

    @Schema(name="type", description = "The type of the error. This is a unique string that defines the type of error, e.g. empty, bad_number_range, ... .")
    public String getType( )
    {
        return type;
    }

    public void setType( String type )
    {
        this.type = type;
    }

    @Schema(name="parameter", description = "The list of parameters, that can be used to create a translated error message")
    public List<String> getParameter( )
    {
        return parameter;
    }

    public void setParameter( List<String> parameter )
    {
        this.parameter = parameter;
    }
}
