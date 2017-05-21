package org.apache.archiva.common.utils;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * VersionComparatorTest 
 *
 *
 */
public class VersionComparatorTest
    extends TestCase
{
    public void testComparator()
    {
        /* Sort order is oldest to newest */

        assertSort( new String[] { "1.0", "3.0", "2.0" }, new String[] { "1.0", "2.0", "3.0" } );
        assertSort( new String[] { "1.5", "1.2", "1.0" }, new String[] { "1.0", "1.2", "1.5" } );

        assertSort( new String[] { "1.5-SNAPSHOT", "1.2", "1.20" }, new String[] { "1.2", "1.5-SNAPSHOT", "1.20" } );

        assertSort( new String[] { "1.1", "1.0-SNAPSHOT", "1.1-m6", "1.1-rc1" }, new String[] {
            "1.0-SNAPSHOT",
            "1.1-rc1",
            "1.1-m6",
            "1.1" } );

        assertSort( new String[] { "1.1-m6", "1.0-SNAPSHOT", "1.1-rc1", "1.1" }, new String[] {
            "1.0-SNAPSHOT",
            "1.1-rc1",
            "1.1-m6",
            "1.1" } );

        assertSort( new String[] { "2.0.5", "2.0.4-SNAPSHOT", "2.0", "2.0-rc1" }, new String[] {
            "2.0-rc1",
            "2.0",
            "2.0.4-SNAPSHOT",
            "2.0.5" } );

        assertSort( new String[] { "1.0-alpha-1", "1.0-alpha-22", "1.0-alpha-10", "1.0-alpha-9" }, new String[] {
            "1.0-alpha-1",
            "1.0-alpha-9",
            "1.0-alpha-10",
            "1.0-alpha-22" } );

        assertSort( new String[] { "1.0-alpha1", "1.0-alpha22", "1.0-alpha10", "1.0-alpha9" }, new String[] {
            "1.0-alpha1",
            "1.0-alpha9",
            "1.0-alpha10",
            "1.0-alpha22" } );
        
        assertSort( new String[] { "1.0-1", "1.0-22", "1.0-10", "1.0-9" }, new String[] {
            "1.0-1",
            "1.0-9",
            "1.0-10",
            "1.0-22" } );
        
        assertSort( new String[] { "alpha-1", "alpha-22", "alpha-10", "alpha-9" }, new String[] {
            "alpha-1",
            "alpha-9",
            "alpha-10",
            "alpha-22" } );
        
        assertSort( new String[] { "1.0.1", "1.0.22", "1.0.10", "1.0.9" }, new String[] {
            "1.0.1",
            "1.0.9",
            "1.0.10",
            "1.0.22" } );
        
        
        // TODO: write more unit tests.
    }

    private void assertSort( String[] rawVersions, String[] expectedSort )
    {
        List<String> versions = new ArrayList<>();
        versions.addAll( Arrays.asList( rawVersions ) );

        Collections.sort( versions, VersionComparator.getInstance() );

        assertEquals( "Versions.size()", expectedSort.length, versions.size() );
        for ( int i = 0; i < expectedSort.length; i++ )
        {
            assertEquals( "Sorted Versions[" + i + "]", expectedSort[i], (String) versions.get( i ) );
        }
    }

    public void testToParts()
    {
        assertParts( "1.0", new String[] { "1", "0" } );
        assertParts( "1.0-alpha-1", new String[] { "1", "0", "alpha", "1" } );
        assertParts( "2.0-rc2", new String[] { "2", "0", "rc", "2" } );
        assertParts( "1.3-m6", new String[] { "1", "3", "m", "6" } );
    }

    private void assertParts( String version, String[] expectedParts )
    {
        String actualParts[] = VersionComparator.toParts( version );
        assertEquals( "Parts.length", expectedParts.length, actualParts.length );

        for ( int i = 0; i < expectedParts.length; i++ )
        {
            assertEquals( "parts[" + i + "]", expectedParts[i], actualParts[i] );
        }
    }
}
