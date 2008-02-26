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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.ClassUtils;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.springframework.beans.factory.ListableBeanFactory;

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
        if ( i >= 0 )
        {
            return Character.toLowerCase( string.charAt( i + 1 ) ) + string.substring( i + 2 );
        }
        return string;
    }

    public static String toCamelCase( String string )
    {
        StringBuilder camelCase = new StringBuilder();
        boolean first = true;

        StringTokenizer tokenizer = new StringTokenizer( string, "-" );
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
        return isDefaultHint( roleHint ) ? id : id + '#' + roleHint;
    }

    private static boolean isDefaultHint( String roleHint )
    {
        return roleHint == null || roleHint.length() == 0 || "default".equals( roleHint );
    }

    public static Map lookupMap( String role, ListableBeanFactory beanFactory )
    {
        Map map = new HashMap();
        String mask = role + '#';
        String[] beans = beanFactory.getBeanDefinitionNames();
        for ( int i = 0; i < beans.length; i++ )
        {
            String name = beans[i];
            if ( name.startsWith( mask ) )
            {
                map.put( name.substring( mask.length() ), beanFactory.getBean( name ) );
            }
        }
        if ( beanFactory.containsBean( role ) )
        {
            map.put( PlexusConstants.PLEXUS_DEFAULT_HINT, beanFactory.getBean( role ) );
        }
        return map;
    }

    public static List LookupList( String role, ListableBeanFactory beanFactory )
    {
        return new ArrayList( PlexusToSpringUtils.lookupMap( role, beanFactory ).values() );
    }
}
