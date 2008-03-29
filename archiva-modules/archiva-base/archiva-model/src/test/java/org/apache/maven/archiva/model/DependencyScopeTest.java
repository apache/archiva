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

import junit.framework.TestCase;

/**
 * DependencyScopeTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DependencyScopeTest
    extends TestCase
{
    public void testIsWithinScope()
    {
        // Test on blank / empty desired scopes.
        assertFalse( DependencyScope.isWithinScope( "compile", null ) );
        assertFalse( DependencyScope.isWithinScope( "test", null ) );
        assertFalse( DependencyScope.isWithinScope( "runtime", null ) );
        assertFalse( DependencyScope.isWithinScope( "provided", null ) );
        assertFalse( DependencyScope.isWithinScope( "compile", "" ) );
        assertFalse( DependencyScope.isWithinScope( "test", "" ) );
        assertFalse( DependencyScope.isWithinScope( "runtime", "" ) );
        assertFalse( DependencyScope.isWithinScope( "provided", "" ) );

        // Tests on blank / empty actual scopes.
        assertTrue( DependencyScope.isWithinScope( "", DependencyScope.COMPILE ) );
        assertTrue( DependencyScope.isWithinScope( null, DependencyScope.COMPILE ) );
        assertTrue( DependencyScope.isWithinScope( "", DependencyScope.TEST ) );
        assertTrue( DependencyScope.isWithinScope( null, DependencyScope.TEST ) );
        assertFalse( DependencyScope.isWithinScope( "", DependencyScope.PROVIDED ) );
        assertFalse( DependencyScope.isWithinScope( null, DependencyScope.PROVIDED ) );
        assertFalse( DependencyScope.isWithinScope( "", DependencyScope.RUNTIME ) );
        assertFalse( DependencyScope.isWithinScope( null, DependencyScope.RUNTIME ) );

        // Tests on compile desired scopes.
        assertTrue( DependencyScope.isWithinScope( "compile", DependencyScope.COMPILE ) );
        assertFalse( DependencyScope.isWithinScope( "test", DependencyScope.COMPILE ) );

        // Tests on test desired scopes.
        assertTrue( DependencyScope.isWithinScope( "compile", DependencyScope.TEST ) );
        assertTrue( DependencyScope.isWithinScope( "test", DependencyScope.TEST ) );

        // Tests on oddball scopes.
        assertFalse( DependencyScope.isWithinScope( "compile", DependencyScope.PROVIDED ) );
        assertFalse( DependencyScope.isWithinScope( "test", DependencyScope.PROVIDED ) );
        assertTrue( DependencyScope.isWithinScope( "provided", DependencyScope.PROVIDED ) );

        assertFalse( DependencyScope.isWithinScope( "compile", DependencyScope.RUNTIME ) );
        assertFalse( DependencyScope.isWithinScope( "test", DependencyScope.RUNTIME ) );
        assertTrue( DependencyScope.isWithinScope( "provided", DependencyScope.RUNTIME ) );
        assertTrue( DependencyScope.isWithinScope( "runtime", DependencyScope.RUNTIME ) );
    }
}
