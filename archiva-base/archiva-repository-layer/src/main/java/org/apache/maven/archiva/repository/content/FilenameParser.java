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

    private static final Pattern mavenPluginPattern = Pattern.compile( "(maven-.*-plugin)|(.*-maven-plugin)" );

    private static final Pattern extensionPattern =
        Pattern.compile( "(\\.tar\\.gz$)|(\\.tar\\.bz2$)|(\\.[a-z0-9]*$)", Pattern.CASE_INSENSITIVE );

    private static final Pattern SNAPSHOT_PATTERN = Pattern.compile( "^([0-9]{8}\\.[0-9]{6}-[0-9]+)(.*)$" );

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
        String value = null;

        if ( name.startsWith( expected, offset ) )
        {
            value = expected;
        }
        else if ( VersionUtil.isGenericSnapshot( expected ) )
        {
            String version = name.substring( offset );

            // check it starts with the same version up to the snapshot part
            int leadingLength = expected.length() - 9;
            if ( version.startsWith( expected.substring( 0, leadingLength ) ) && version.length() > leadingLength )
            {
                // If we expect a non-generic snapshot - look for the timestamp
                Matcher m = SNAPSHOT_PATTERN.matcher( version.substring( leadingLength + 1 ) );
                if ( m.matches() )
                {
                    value = version.substring( 0, leadingLength + 1 ) + m.group( 1 );
                }
            }
        }

        if ( value != null )
        {
            // Potential hit. check for '.' or '-' at end of expected.
            int seperatorOffset = offset + value.length();

            // Test for "out of bounds" first. 
            if ( seperatorOffset >= name.length() )
            {
                offset = name.length();
                return value;
            }

            // Test for seperator char.
            char seperatorChar = name.charAt( seperatorOffset );
            if ( ( seperatorChar == '-' ) || ( seperatorChar == '.' ) )
            {
                offset = seperatorOffset + 1;
                return value;
            }
        }

        return null;
    }

    /**
     * Get the current seperator character.
     *
     * @return the seperator character (either '.' or '-'), or 0 if no seperator character available.
     */
    protected char seperator()
    {
        // Past the end of the string?
        if ( offset >= name.length() )
        {
            return 0;
        }

        // Before the start of the string?
        if ( offset <= 0 )
        {
            return 0;
        }

        return name.charAt( offset - 1 );
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
        Matcher specialMat = mavenPluginPattern.matcher( name );
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
