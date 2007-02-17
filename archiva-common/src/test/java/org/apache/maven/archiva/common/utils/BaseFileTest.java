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

import java.io.File;

import junit.framework.TestCase;

/**
 * BaseFileTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class BaseFileTest
    extends TestCase
{
    public void testFileString()
    {
        File repoDir = new File( "/home/user/foo/repository" );
        String pathFile = "path/to/resource.xml";
        BaseFile file = new BaseFile( repoDir, pathFile );

        assertEquals( "/home/user/foo/repository/path/to/resource.xml", file.getAbsolutePath() );
        assertEquals( "path/to/resource.xml", file.getRelativePath() );
        assertEquals( new File( "/home/user/foo/repository" ), file.getBaseDir() );
    }

    public void testFileFile()
    {
        File repoDir = new File( "/home/user/foo/repository" );
        File pathFile = new File( "/home/user/foo/repository/path/to/resource.xml" );
        BaseFile file = new BaseFile( repoDir, pathFile );

        assertEquals( "/home/user/foo/repository/path/to/resource.xml", file.getAbsolutePath() );
        assertEquals( "path/to/resource.xml", file.getRelativePath() );
        assertEquals( new File( "/home/user/foo/repository" ), file.getBaseDir() );
    }

    public void testStringFile()
    {
        String repoDir = "/home/user/foo/repository";
        File pathFile = new File( "/home/user/foo/repository/path/to/resource.xml" );
        BaseFile file = new BaseFile( repoDir, pathFile );

        assertEquals( "/home/user/foo/repository/path/to/resource.xml", file.getAbsolutePath() );
        assertEquals( "path/to/resource.xml", file.getRelativePath() );
        assertEquals( new File( "/home/user/foo/repository" ), file.getBaseDir() );
    }

    public void testFileThenSetBaseString()
    {
        String repoDir = "/home/user/foo/repository";
        File pathFile = new File( "/home/user/foo/repository/path/to/resource.xml" );
        BaseFile file = new BaseFile( pathFile );
        file.setBaseDir( repoDir );

        assertEquals( "/home/user/foo/repository/path/to/resource.xml", file.getAbsolutePath() );
        assertEquals( "path/to/resource.xml", file.getRelativePath() );
        assertEquals( new File( "/home/user/foo/repository" ), file.getBaseDir() );
    }

    public void testFileThenSetBaseFile()
    {
        File repoDir = new File( "/home/user/foo/repository" );
        File pathFile = new File( "/home/user/foo/repository/path/to/resource.xml" );
        BaseFile file = new BaseFile( pathFile );
        file.setBaseDir( repoDir );

        assertEquals( "/home/user/foo/repository/path/to/resource.xml", file.getAbsolutePath() );
        assertEquals( "path/to/resource.xml", file.getRelativePath() );
        assertEquals( new File( "/home/user/foo/repository" ), file.getBaseDir() );
    }
    
    public void testStringThenSetBaseString()
    {
        String repoDir = "/home/user/foo/repository";
        String pathFile = "/home/user/foo/repository/path/to/resource.xml";
        BaseFile file = new BaseFile( pathFile );
        file.setBaseDir( repoDir );

        assertEquals( "/home/user/foo/repository/path/to/resource.xml", file.getAbsolutePath() );
        assertEquals( "path/to/resource.xml", file.getRelativePath() );
        assertEquals( new File( "/home/user/foo/repository" ), file.getBaseDir() );
    }

    public void testStringThenSetBaseFile()
    {
        File repoDir = new File( "/home/user/foo/repository" );
        String pathFile = "/home/user/foo/repository/path/to/resource.xml";
        BaseFile file = new BaseFile( pathFile );
        file.setBaseDir( repoDir );

        assertEquals( "/home/user/foo/repository/path/to/resource.xml", file.getAbsolutePath() );
        assertEquals( "path/to/resource.xml", file.getRelativePath() );
        assertEquals( new File( "/home/user/foo/repository" ), file.getBaseDir() );
    }    
}
