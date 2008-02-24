package org.codehaus.plexus.spring;


/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.StringTokenizer;

import org.apache.commons.lang.ClassUtils;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;

/**
 * Utility method to convert plexus descriptors to spring bean context.
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 * @since 1.1
 */
public class PlexusToSpringUtils
{

    public static String toSpringId( String string )
    {
        int i = string.lastIndexOf( '.' );
        if (i >= 0 )
        {
            return Character.toLowerCase( string.charAt( i + 1 ) ) + string.substring( i + 2 );
        }
        return string;
    }

    public static String toCamelCase( String string )
    {
        StringBuilder camelCase = new StringBuilder();
        boolean first = true;

        StringTokenizer tokenizer = new StringTokenizer( string.toLowerCase(), "-" );
        while ( tokenizer.hasMoreTokens() )
        {
            String token = tokenizer.nextToken();
            if ( first )
            {
                camelCase.append( token.charAt( 0 ) );
                first = false;
            }
            else
            {
                camelCase.append( Character.toUpperCase( token.charAt( 0 ) ) );
            }
            camelCase.append( token.substring( 1, token.length() ) );
        }
        return camelCase.toString();
    }

    public static boolean isInitializable( String className )
    {
        boolean initializable = false;
        try
        {
            initializable = Initializable.class.isAssignableFrom( ClassUtils.getClass( className ) );
        }
        catch ( ClassNotFoundException e )
        {
            // ignored
        }
        return initializable;
    }

    public static boolean isDisposable( String className )
    {
        boolean disposable = false;
        try
        {
            disposable = Disposable.class.isAssignableFrom( ClassUtils.getClass( className ) );
        }
        catch ( ClassNotFoundException e )
        {
            // ignored
        }
        return disposable;
    }

    public static String buildSpringId( String role, String roleHint )
    {
        int i = role.lastIndexOf( '.' ) + 1;
        if ( i <= 0 )
        {
            i = 0;
        }
        String id = Character.toLowerCase( role.charAt( i ) ) + role.substring( i + 1 );
        return roleHint.length() == 0 ? id : id + '#' + roleHint;
    }}
