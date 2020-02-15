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
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FilenameUtils;

/**
 * PathUtilTest 
 *
 *
 */
public class PathUtilTest
    extends TestCase
{

    public void testToRelativeWithoutSlash()
    {
        assertEquals( FilenameUtils.separatorsToSystem( "path/to/resource.xml" ), PathUtil.getRelative( "/home/user/foo/repository",
                                                                    "/home/user/foo/repository/path/to/resource.xml" ) );
    }
    
    public void testToRelativeWithSlash()
    {
        assertEquals( FilenameUtils.separatorsToSystem( "path/to/resource.xml" ), PathUtil.getRelative( "/home/user/foo/repository/",
                                                                    "/home/user/foo/repository/path/to/resource.xml" ) );
    }

    public void testToUrlRelativePath()
    {
        Path workingDir = Paths.get( "" );

        String workingDirname = StringUtils.replaceChars( workingDir.toAbsolutePath().toString(), '\\', '/' );

        // Some JVM's retain the "." at the end of the path.  Drop it.
        if ( workingDirname.endsWith( "/." ) )
        {
            workingDirname = workingDirname.substring( 0, workingDirname.length() - 2 );
        }

        if ( !workingDirname.startsWith( "/" ) )
        {
            workingDirname = "/" + workingDirname;
        }

        String path = "path/to/resource.xml";
        String expectedPath = "file:" + workingDirname + "/" + path;

        assertEquals( expectedPath, PathUtil.toUrl( path ) );
    }

    public void testToUrlUsingFileUrl()
    {
        Path workingDir = Paths.get( "." );

        String workingDirname = StringUtils.replaceChars( workingDir.toAbsolutePath().toString(), '\\', '/' );

        // Some JVM's retain the "." at the end of the path.  Drop it.
        if ( workingDirname.endsWith( "/." ) )
        {
            workingDirname = workingDirname.substring( 0, workingDirname.length() - 2 );
        }

        if ( !workingDirname.startsWith( "/" ) )
        {
            workingDirname = "/" + workingDirname;
        }

        String path = "path/to/resource.xml";
        String expectedPath = "file:" + workingDirname + "/" + path;
        
        assertEquals( expectedPath, PathUtil.toUrl( expectedPath ) );
    }
}
