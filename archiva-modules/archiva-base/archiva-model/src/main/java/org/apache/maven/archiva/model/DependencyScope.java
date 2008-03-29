package org.apache.maven.archiva.model;

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

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;

/**
 * DependencyScope - utility methods and constants for working with scopes.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DependencyScope
{
    public static final String SYSTEM = "system";

    public static final String COMPILE = "compile";

    public static final String PROVIDED = "provided";

    public static final String RUNTIME = "runtime";

    public static final String TEST = "test";

    private static final MultiValueMap scopeMap;

    static
    {
        // Store the map of scopes to what other scopes are 'within' that scope.
        scopeMap = new MultiValueMap();

        scopeMap.put( COMPILE, COMPILE );
        scopeMap.put( COMPILE, RUNTIME );
        scopeMap.put( COMPILE, PROVIDED );
        scopeMap.put( COMPILE, SYSTEM );

        scopeMap.put( TEST, COMPILE );
        scopeMap.put( TEST, RUNTIME );
        scopeMap.put( TEST, PROVIDED );
        scopeMap.put( TEST, SYSTEM );
        scopeMap.put( TEST, TEST );
        
        scopeMap.put( RUNTIME, RUNTIME );
        scopeMap.put( RUNTIME, PROVIDED );
        scopeMap.put( RUNTIME, SYSTEM );
        
        scopeMap.put( PROVIDED, RUNTIME );
        scopeMap.put( PROVIDED, PROVIDED );
        scopeMap.put( PROVIDED, SYSTEM );
        
        scopeMap.put( SYSTEM, SYSTEM );
    }

    public static boolean isSystemScoped( Dependency dep )
    {
        return StringUtils.equals( SYSTEM, dep.getScope() );
    }

    /**
     * Test the provided scope against the desired scope to see if it is
     * within that scope's pervue.
     * 
     * Examples: 
     * actual:compile,  desired:test = true
     * actual:compile,  desired:compile = true
     * actual:test,     desired:compile = false
     * actual:provided, desired:compile = false
     * 
     * @param actualScope
     * @param desiredScope
     * @return
     */
    public static boolean isWithinScope( String actualScope, String desiredScope )
    {
        if ( StringUtils.isBlank( desiredScope ) )
        {
            // nothing desired? everything should fail.
            return false;
        }

        String scope = StringUtils.defaultIfEmpty( actualScope, COMPILE );

        return scopeMap.containsValue( desiredScope, scope );
    }
}
