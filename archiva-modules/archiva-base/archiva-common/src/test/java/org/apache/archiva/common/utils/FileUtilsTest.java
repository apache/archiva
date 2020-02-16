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

import org.junit.Assume;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

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


    @Test
    public void testDeleteWithStatus() throws IOException
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

        IOStatus status = FileUtils.deleteDirectoryWithStatus( td );
        assertFalse(Files.exists(f1));
        assertFalse(Files.exists(f2));
        assertFalse(Files.exists(f111));
        assertFalse(Files.exists(f112));
        assertFalse(Files.exists(d1));

        assertTrue( status.isOk( ) );
        assertFalse( status.hasErrors( ) );
        assertEquals( 7, status.getSuccessFiles( ).size( ) );
        assertEquals( 0, status.getErrorFiles( ).size() );
    }


    @Test
    public void testDeleteNonExist() throws IOException
    {
        Path tf = Paths.get("aaserijdmcjdjhdejeidmdjdlasrjerjnbmckdkdk");
        assertFalse(Files.exists(tf));
        FileUtils.deleteDirectory( tf );
    }

    @Test(expected = IOException.class)
    public void testDeleteWithException() throws IOException
    {
        Assume.assumeTrue( FileSystems.getDefault().supportedFileAttributeViews().contains("posix") );
        Path tmpDir = Files.createTempDirectory( "FileUtilsTest" );
        Path tmpDir2 = tmpDir.resolve("testdir1");
        Files.createDirectories( tmpDir2 );
        Path tmpFile = tmpDir2.resolve("testfile1.txt");
        OutputStream stream = null;
        try
        {
            stream = Files.newOutputStream( tmpFile, StandardOpenOption.APPEND, StandardOpenOption.CREATE );
            stream.write( 1 );
            stream.close( );
            assertTrue( Files.exists( tmpFile ) );
            stream = Files.newOutputStream( tmpFile, StandardOpenOption.APPEND, StandardOpenOption.CREATE );
            stream.write( 1 );
            Set<PosixFilePermission> perms = new HashSet<>( );
            Files.setPosixFilePermissions( tmpFile, perms );
            Files.setPosixFilePermissions( tmpDir2, perms );
            FileUtils.deleteDirectory( tmpDir );
            assertFalse( Files.exists( tmpDir ) );
            assertFalse( Files.exists( tmpFile ) );
        } finally {
            if (stream!=null) {
                stream.close();
            }
            Set<PosixFilePermission> perms = new HashSet<>( );
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions( tmpDir2, perms );
            Files.setPosixFilePermissions( tmpFile, perms );
            Files.deleteIfExists( tmpFile );
            Files.deleteIfExists( tmpDir2 );
            Files.deleteIfExists( tmpDir );
        }
    }

    @Test
    public void unzip() throws URISyntaxException, IOException {
        Path destPath = Paths.get("target/unzip");
        try {
            Path zipFile = Paths.get(Thread.currentThread().getContextClassLoader().getResource("test-repository.zip").toURI());
            if (Files.exists(destPath)) {
                org.apache.commons.io.FileUtils.deleteQuietly(destPath.toFile());
            }
            FileUtils.unzip(zipFile, destPath);
            assertTrue(Files.exists(destPath.resolve("org/apache/maven/A/1.0/A-1.0.pom")));
            assertTrue(Files.isRegularFile(destPath.resolve("org/apache/maven/A/1.0/A-1.0.pom")));
            assertTrue(Files.exists(destPath.resolve("org/apache/maven/A/1.0/A-1.0.war")));
            assertTrue(Files.isRegularFile(destPath.resolve("org/apache/maven/A/1.0/A-1.0.war")));
            assertTrue(Files.exists(destPath.resolve("org/apache/maven/test/1.0-SNAPSHOT/wrong-artifactId-1.0-20050611.112233-1.jar")));
            assertTrue(Files.isRegularFile(destPath.resolve("org/apache/maven/test/1.0-SNAPSHOT/wrong-artifactId-1.0-20050611.112233-1.jar")));
            assertTrue(Files.exists(destPath.resolve("KEYS")));
            assertTrue(Files.isRegularFile(destPath.resolve("KEYS")));
            assertTrue(Files.isDirectory(destPath.resolve("invalid")));
            assertTrue(Files.isDirectory(destPath.resolve("javax")));
            assertTrue(Files.isDirectory(destPath.resolve("org")));
        } finally {
            org.apache.commons.io.FileUtils.deleteQuietly(destPath.toFile());
        }
    }

}
