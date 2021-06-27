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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a single validation error. A error is defined by a global unique key and has a optional number
 * of arguments.
 * <p>
 * The unique key should represent a category, the attribute and a generic type, separated by '.'
 * E.g. repository_group.id.empty
 * </p>
 * <p>
 * Categories normally separate errors for different domain types, like managed repository, repository group, maven repository.
 * <p>
 * Types define a certain type of error that can be handled similar independent of the attribute or category
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class ValidationError
{
    public static final String UNSPECIFIED = "unspecified";

    final String errorKey;
    final String attribute;
    final String category;
    final String type;
    final List<Object> arguments = new ArrayList<>();


    public static ValidationError ofKey( final String errorKey, Object... arguments )
    {
        return new ValidationError( errorKey, getCategoryFromKey( errorKey ), getTypeFromKey( errorKey ), getAttributeFromKey( errorKey ),
            Arrays.asList( arguments ) );
    }

    public static ValidationError ofKey( String errorKey, List<Object> arguments )
    {
        return new ValidationError( errorKey, getCategoryFromKey( errorKey ), getTypeFromKey( errorKey ), getAttributeFromKey( errorKey ),
            arguments );
    }

    public ValidationError( String errorKey, String category, String type, String attribute, List<Object> arguments )
    {
        if ( StringUtils.isEmpty( errorKey ) )
        {
            throw new IllegalArgumentException( "The key of a validation error cannot be empty" );
        }
        this.errorKey = errorKey;
        if ( arguments != null )
        {
            this.arguments.addAll( arguments );
        }
        this.type = type;
        this.category = category;
        this.attribute = attribute;
    }

    private static String getTypeFromKey( final String errorKey )
    {
        return errorKey.contains( "." ) ?
            StringUtils.substringAfterLast( errorKey, "." ) :
            UNSPECIFIED;
    }

    private static String getCategoryFromKey( final String errorKey )
    {
        return errorKey.contains( "." ) ?
            StringUtils.substringBefore( errorKey, "." ) :
            UNSPECIFIED;
    }

    private static String getAttributeFromKey( final String errorKey )
    {
        return StringUtils.countMatches( errorKey, "." ) >= 2 ?
            StringUtils.substringBetween( errorKey, "." ) : UNSPECIFIED;
    }

    /**
     * Returns the unique key of this validation error. It is best practice for keys to contain the
     * validation source, the attribute and a unique error definition.
     * E.g. repository_group.id.empty
     *
     * @return
     */
    public String getErrorKey( )
    {
        return errorKey;
    }

    /**
     * Returns the list of arguments stored for this error
     * @return the list of arguments
     */
    public List<Object> getArguments( )
    {
        return arguments;
    }

    /**
     * Adds the given argument to the list
     * @param argument the argument to add
     */
    public void addArgument( Object argument )
    {
        this.arguments.add( argument );
    }

    /**
     * Returns the generic error type, this error represents.
     *
     * @return the error type or {@link #UNSPECIFIED} if not explicitly set.
     */
    public String getType( )
    {
        return type;
    }

    /**
     * Returns the category of the error.
     *
     * @return the category or {@link #UNSPECIFIED} if not explicitly set
     */
    public String getCategory( )
    {
        return category;
    }

    /**
     * Returns the attribute name
     * @return the attribute name or {@link #UNSPECIFIED} if not explicitly set
     */
    public String getAttribute( )
    {
        return attribute;
    }

    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "ValidationError{" );
        sb.append( "errorKey='" ).append( errorKey ).append( '\'' );
        sb.append( '}' );
        return sb.toString( );
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;

        ValidationError that = (ValidationError) o;

        if ( !errorKey.equals( that.errorKey ) ) return false;
        return arguments.equals( that.arguments );
    }

    @Override
    public int hashCode( )
    {
        int result = errorKey.hashCode( );
        result = 31 * result + arguments.hashCode( );
        return result;
    }
}
