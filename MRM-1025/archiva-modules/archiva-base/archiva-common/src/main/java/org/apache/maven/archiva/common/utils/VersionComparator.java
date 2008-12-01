package org.apache.maven.archiva.common.utils;

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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * VersionComparator - compare the parts of two version strings.
 * <p/>
 * Technique.
 * <p/>
 * * Split the version strings into parts by splitting on <code>"-._"</code> first, then breaking apart words from numbers.
 * <p/>
 * <code>
 * "1.0"         = "1", "0"
 * "1.0-alpha-1" = "1", "0", "alpha", "1"
 * "2.0-rc2"     = "2", "0", "rc", "2"
 * "1.3-m2"      = "1", "3", "m", "3"
 * </code>
 * <p/>
 * compare each part individually, and when they do not match, perform the following test.
 * <p/>
 * Numbers are calculated per normal comparison rules.
 * Words that are part of the "special word list" will be treated as their index within that heirarchy.
 * Words that cannot be identified as special, are treated using normal case-insensitive comparison rules.
 *
 * @version $Id$
 */
public class VersionComparator
    implements Comparator<String>
{
    private static Comparator<String> INSTANCE = new VersionComparator();

    private List<String> specialWords;

    public VersionComparator()
    {
        specialWords = new ArrayList<String>();

        // ids that refer to LATEST
        specialWords.add( "final" );
        specialWords.add( "release" );
        specialWords.add( "current" );
        specialWords.add( "latest" );
        specialWords.add( "g" );
        specialWords.add( "gold" );
        specialWords.add( "fcs" );

        // ids that are for a release cycle.
        specialWords.add( "a" );
        specialWords.add( "alpha" );
        specialWords.add( "b" );
        specialWords.add( "beta" );
        specialWords.add( "pre" );
        specialWords.add( "rc" );
        specialWords.add( "m" );
        specialWords.add( "milestone" );

        // ids that are for dev / debug cycles.
        specialWords.add( "dev" );
        specialWords.add( "test" );
        specialWords.add( "debug" );
        specialWords.add( "unofficial" );
        specialWords.add( "nightly" );
        specialWords.add( "incubating" );
        specialWords.add( "incubator" );
        specialWords.add( "snapshot" );
    }

    public static Comparator<String> getInstance()
    {
        return INSTANCE;
    }

    public int compare( String o1, String o2 )
    {
        if ( o1 == null && o2 == null )
        {
            return 0;
        }

        if ( o1 == null )
        {
            return 1;
        }

        if ( o2 == null )
        {
            return -1;
        }

        String[] parts1 = toParts( o1 );
        String[] parts2 = toParts( o2 );

        int diff;
        int partLen = Math.max( parts1.length, parts2.length );
        for ( int i = 0; i < partLen; i++ )
        {
            diff = comparePart( safePart( parts1, i ), safePart( parts2, i ) );
            if ( diff != 0 )
            {
                return diff;
            }
        }

        diff = parts2.length - parts1.length;

        if ( diff != 0 )
        {
            return diff;
        }

        return o1.compareToIgnoreCase( o2 );
    }

    private String safePart( String[] parts, int idx )
    {
        if ( idx < parts.length )
        {
            return parts[idx];
        }

        return "0";
    }

    private int comparePart( String s1, String s2 )
    {
        boolean is1Num = NumberUtils.isNumber( s1 );
        boolean is2Num = NumberUtils.isNumber( s2 );

        // (Special Case) Test for numbers both first.
        if ( is1Num && is2Num )
        {
            int i1 = NumberUtils.toInt( s1 );
            int i2 = NumberUtils.toInt( s2 );

            return i1 - i2;
        }

        // Test for text both next.
        if ( !is1Num && !is2Num )
        {
            int idx1 = specialWords.indexOf( s1.toLowerCase() );
            int idx2 = specialWords.indexOf( s2.toLowerCase() );

            // Only operate perform index based operation, if both strings
            // are found in the specialWords index.
            if ( idx1 >= 0 && idx2 >= 0 )
            {
                return idx1 - idx2;
            }
        }

        // Comparing text to num
        if ( !is1Num && is2Num )
        {
            return -1;
        }

        // Comparing num to text
        if ( is1Num && !is2Num )
        {
            return 1;
        }

        // Return comparison of strings themselves.
        return s1.compareToIgnoreCase( s2 );
    }

    public static String[] toParts( String version )
    {
        if ( StringUtils.isBlank( version ) )
        {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }

        int modeOther = 0;
        int modeDigit = 1;
        int modeText = 2;

        List<String> parts = new ArrayList<String>();
        int len = version.length();
        int i = 0;
        int start = 0;
        int mode = modeOther;

        while ( i < len )
        {
            char c = version.charAt( i );

            if ( Character.isDigit( c ) )
            {
                if ( mode != modeDigit )
                {
                    if ( mode != modeOther )
                    {
                        parts.add( version.substring( start, i ) );
                    }
                    mode = modeDigit;
                    start = i;
                }
            }
            else if ( Character.isLetter( c ) )
            {
                if ( mode != modeText )
                {
                    if ( mode != modeOther )
                    {
                        parts.add( version.substring( start, i ) );
                    }
                    mode = modeText;
                    start = i;
                }
            }
            else
            {
                // Other.
                if ( mode != modeOther )
                {
                    parts.add( version.substring( start, i ) );
                    mode = modeOther;
                }
            }

            i++;
        }

        // Add remainder
        if ( mode != modeOther )
        {
            parts.add( version.substring( start, i ) );
        }

        return parts.toArray( new String[parts.size()] );
    }
}
