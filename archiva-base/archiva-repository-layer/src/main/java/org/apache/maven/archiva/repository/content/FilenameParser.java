package org.apache.maven.archiva.repository.content;

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

import org.apache.maven.archiva.common.utils.VersionUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generic Filename Parser for use with layout routines.
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class FilenameParser
{
    private String name;

    private String extension;

    private int offset;

    private static final Pattern specialCases = Pattern.compile( "(maven-.*-plugin)|(maven-plugin)" );

    private static final Pattern extensionPattern = Pattern.compile( "(.tar.gz$)|(.tar.bz2$)|(.[a-z0-9]{1,4}$)",
                                                                     Pattern.CASE_INSENSITIVE );

    private static final Pattern section = Pattern.compile( "([^-]*)" );

    private Matcher matcher;

    protected FilenameParser( String filename )
    {
        this.name = filename;

        Matcher mat = extensionPattern.matcher( name );
        if ( mat.find() )
        {
            extension = filename.substring( mat.start() + 1 );
            name = name.substring( 0, name.length() - extension.length() - 1 );
        }

        matcher = section.matcher( name );

        reset();
    }

    protected void reset()
    {
        offset = 0;
    }

    protected String next()
    {
        // Past the end of the string.
        if ( offset > name.length() )
        {
            return null;
        }

        // Return the next section.
        if ( matcher.find( offset ) )
        {
            // Return found section.
            offset = matcher.end() + 1;
            return matcher.group();
        }

        // Nothing to return.
        return null;
    }

    protected String expect( String expected )
    {
        if ( name.startsWith( expected, offset ) )
        {
            // Potential hit. check for '.' or '-' at end of expected.
            int seperatorOffset = offset + expected.length();

            // Test for "out of bounds" first. 
            if ( seperatorOffset >= name.length() )
            {
                offset = name.length();
                return expected;
            }

            // Test for seperator char.
            char seperatorChar = name.charAt( seperatorOffset );
            if ( ( seperatorChar == '-' ) || ( seperatorChar == '.' ) )
            {
                offset = seperatorOffset + 1;
                return expected;
            }
        }

        return null;
    }

    protected String getName()
    {
        return name;
    }

    protected String getExtension()
    {
        return extension;
    }

    protected String remaining()
    {
        if ( offset >= name.length() )
        {
            return null;
        }

        String end = name.substring( offset );
        offset = name.length();
        return end;
    }

    protected String nextNonVersion()
    {
        boolean done = false;

        StringBuffer ver = new StringBuffer();

        // Any text upto the end of a special case is considered non-version. 
        Matcher specialMat = specialCases.matcher( name );
        if ( specialMat.find() )
        {
            ver.append( name.substring( offset, specialMat.end() ) );
            offset = specialMat.end() + 1;
        }

        while ( !done )
        {
            int initialOffset = offset;
            String section = next();
            if ( section == null )
            {
                done = true;
            }
            else if ( !VersionUtil.isVersion( section ) )
            {
                if ( ver.length() > 0 )
                {
                    ver.append( '-' );
                }
                ver.append( section );
            }
            else
            {
                offset = initialOffset;
                done = true;
            }
        }

        return ver.toString();
    }

    protected String nextVersion()
    {
        boolean done = false;

        StringBuffer ver = new StringBuffer();

        while ( !done )
        {
            int initialOffset = offset;
            String section = next();
            if ( section == null )
            {
                done = true;
            }
            else if ( VersionUtil.isVersion( section ) )
            {
                if ( ver.length() > 0 )
                {
                    ver.append( '-' );
                }
                ver.append( section );
            }
            else
            {
                offset = initialOffset;
                done = true;
            }
        }

        return ver.toString();
    }
}
