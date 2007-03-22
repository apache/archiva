package org.apache.maven.archiva.repository.version;

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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * VersionConstants 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class VersionUtil
{
    public static final String SNAPSHOT = "SNAPSHOT";

    public static final Pattern UNIQUE_SNAPSHOT_PATTERN = Pattern.compile( "^(.*)-([0-9]{8}\\.[0-9]{6})-([0-9]+)$" );

    public static boolean isSnapshot( String version )
    {
        Matcher m = UNIQUE_SNAPSHOT_PATTERN.matcher( version );
        if ( m.matches() )
        {
            return true;
        }
        else
        {
            return version.endsWith( SNAPSHOT );
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
}
