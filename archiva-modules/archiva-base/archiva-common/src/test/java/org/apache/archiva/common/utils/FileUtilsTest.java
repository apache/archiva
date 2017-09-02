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

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class FileUtilsTest
{
    @Test
    public void testDeleteQuietly() throws IOException
    {
        Path tf = Files.createTempFile( "FileUtilsTest", ".txt" );
        assertTrue(Files.exists(tf));
        FileUtils.deleteQuietly( tf );
        assertFalse(Files.exists(tf));

        Path td = Files.createTempDirectory( "FileUtilsTest" );
        Path f1 = td.resolve("file1.txt");
        Path f2 = td.resolve("file2.txt");
        Path d1 = td.resolve("dir1");
        Files.createDirectory( d1 );
        Path d11 = d1.resolve("dir11");
        Files.createDirectory( d11 );
        Path f111 = d11.resolve("file111.txt");
        Path f112 = d11.resolve("file112.txt");
        Files.write(f1,"file1".getBytes());
        Files.write(f2, "file2".getBytes());
        Files.write(f111, "file111".getBytes());
        Files.write(f112, "file112".getBytes());
        assertTrue(Files.exists(d1));
        assertTrue(Files.exists(f1));
        assertTrue(Files.exists(f2));
        assertTrue(Files.exists(f111));
        assertTrue(Files.exists(f112));

        FileUtils.deleteQuietly( td );
        assertFalse(Files.exists(f1));
        assertFalse(Files.exists(f2));
        assertFalse(Files.exists(f111));
        assertFalse(Files.exists(f112));
        assertFalse(Files.exists(d1));


    }

    @Test
    public void testDelete() throws IOException
    {
        Path td = Files.createTempDirectory( "FileUtilsTest" );
        Path f1 = td.resolve("file1.txt");
        Path f2 = td.resolve("file2.txt");
        Path d1 = td.resolve("dir1");
        Files.createDirectory( d1 );
        Path d11 = d1.resolve("dir11");
        Files.createDirectory( d11 );
        Path f111 = d11.resolve("file111.txt");
        Path f112 = d11.resolve("file112.txt");
        Files.write(f1,"file1".getBytes());
        Files.write(f2, "file2".getBytes());
        Files.write(f111, "file111".getBytes());
        Files.write(f112, "file112".getBytes());
        assertTrue(Files.exists(d1));
        assertTrue(Files.exists(f1));
        assertTrue(Files.exists(f2));
        assertTrue(Files.exists(f111));
        assertTrue(Files.exists(f112));

        FileUtils.deleteDirectory( td );
        assertFalse(Files.exists(f1));
        assertFalse(Files.exists(f2));
        assertFalse(Files.exists(f111));
        assertFalse(Files.exists(f112));
        assertFalse(Files.exists(d1));

    }

    @Test(expected = java.io.IOException.class)
    public void testDeleteException() throws IOException
    {
        Path tf = Paths.get("aaserijdmcjdjhdejeidmdjdlasrjerjnbmckdkdk");
        assertFalse(Files.exists(tf));
        FileUtils.deleteDirectory( tf );
    }

}
