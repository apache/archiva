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

import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Version utility methods. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class VersionUtil
{
    /**
     * These are the version patterns found in the filenames of the various artifact's versions IDs.
     * These patterns are all tackling lowercase version IDs.
     */
    private static final String versionPatterns[] = new String[] {
        "([0-9][_.0-9a-z]*)",
        "(snapshot)",
        "(g?[_.0-9ab]*(pre|rc|g|m)[_.0-9]*)",
        "(dev[_.0-9]*)",
        "(alpha[_.0-9]*)",
        "(beta[_.0-9]*)",
        "(rc[_.0-9]*)",
//        "(test[_.0-9]*)", -- omitted for MRM-681, can be reinstated as part of MRM-712
        "(debug[_.0-9]*)",
        "(unofficial[_.0-9]*)",
        "(current)",
        "(latest)",
        "(fcs)",
        "(release[_.0-9]*)",
        "(nightly)",
        "(final)",
        "(incubating)",
        "(incubator)",
        "([ab][_.0-9]+)" };

    public static final String SNAPSHOT = "SNAPSHOT";

    public static final Pattern UNIQUE_SNAPSHOT_PATTERN = Pattern.compile( "^(.*)-([0-9]{8}\\.[0-9]{6})-([0-9]+)$" );

    public static final Pattern TIMESTAMP_PATTERN = Pattern.compile( "^([0-9]{8})\\.([0-9]{6})$" );

    public static final Pattern GENERIC_SNAPSHOT_PATTERN = Pattern.compile( "^(.*)-" + SNAPSHOT );

    private static final Pattern VERSION_MEGA_PATTERN = Pattern.compile( StringUtils.join( versionPatterns, '|' ), Pattern.CASE_INSENSITIVE );

    /**
     * <p>
     * Tests if the unknown string contains elements that identify it as a version string (or not).
     * </p>
     * 
     * <p>
     * The algorithm tests each part of the string that is delimited by a '-' (dash) character.
     * If 75% or more of the sections are identified as 'version' strings, the result is
     * determined to be of a high probability to be version identifier string.
     * </p>
     * 
     * @param unknown the unknown string to test.
     * @return true if the unknown string is likely a version string.
     */
    public static boolean isVersion( String unknown )
    {
        String versionParts[] = StringUtils.split( unknown, '-' );

        Matcher mat;

        int countValidParts = 0;

        for ( int i = 0; i < versionParts.length; i++ )
        {
            String part = versionParts[i];
            mat = VERSION_MEGA_PATTERN.matcher( part );

            if ( mat.matches() )
            {
                countValidParts++;
            }
        }

        /* Calculate version probability as true if 3/4's of the input string has pieces of
         * of known version identifier strings.
         */
        int threshold = (int) Math.floor( Math.max( (double) 1.0, (double) ( versionParts.length * 0.75 ) ) );

        return ( countValidParts >= threshold );
    }

    /**
     * <p>
     * Tests if the identifier is a known simple version keyword.
     * </p>
     * 
     * <p>
     * This method is different from {@link #isVersion(String)} in that it tests the whole input string in
     * one go as a simple identifier. (eg "alpha", "1.0", "beta", "debug", "latest", "rc#", etc...)
     * </p>
     * 
     * @param identifier the identifier to test.
     * @return true if the unknown string is likely a version string.
     */
    public static boolean isSimpleVersionKeyword( String identifier )
    {
        Matcher mat = VERSION_MEGA_PATTERN.matcher( identifier );

        return mat.matches();
    }

    public static boolean isSnapshot( String version )
    {
        Matcher m = UNIQUE_SNAPSHOT_PATTERN.matcher( version );
        if ( m.matches() )
        {
            return true;
        }
        else
        {
            return isGenericSnapshot(version);
        }
    }

    public static String getBaseVersion( String version )
    {
        Matcher m = UNIQUE_SNAPSHOT_PATTERN.matcher( version );
        if ( m.matches() )
        {
            return m.group( 1 ) + "-" + SNAPSHOT;
        }
        else
        {
            return version;
        }
    }
    
    /**
     * <p>
     * Get the release version of the snapshot version.
     * </p>
     * 
     * <p>
     * If snapshot version is 1.0-SNAPSHOT, then release version would be 1.0
     * And if snapshot version is 1.0-20070113.163208-1.jar, then release version would still be 1.0
     * </p>
     * 
     * @param snapshotVersion
     * @return
     */
    public static String getReleaseVersion( String snapshotVersion )
    {
        Matcher m = UNIQUE_SNAPSHOT_PATTERN.matcher( snapshotVersion );
        
        if( isGenericSnapshot( snapshotVersion ) )
        {
            m = GENERIC_SNAPSHOT_PATTERN.matcher( snapshotVersion );
        }
                
        if ( m.matches() )
        {   
            return m.group( 1 );
        }
        else
        {        
            return snapshotVersion;
        }
    }

    public static boolean isUniqueSnapshot( String version )
    {             
        Matcher m = UNIQUE_SNAPSHOT_PATTERN.matcher( version );
        if( m.matches() )
        {
            return true;
        }

        return false;
    }

    public static boolean isGenericSnapshot( String version )
    {
        return version.endsWith( SNAPSHOT );    
    }
}
