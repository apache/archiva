package org.apache.archiva.repository.content;

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

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import static org.junit.Assert.*;

public class FilesystemAssetTest {

    Path assetPathFile;
    Path assetPathDir;

    @Before
    public void init() throws IOException {
        assetPathFile = Files.createTempFile("assetFile", "dat");
        assetPathDir = Files.createTempDirectory("assetDir");
    }

    @After
    public void cleanup() {

        try {
            Files.deleteIfExists(assetPathFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Files.deleteIfExists(assetPathDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void getPath() {
        FilesystemAsset asset = new FilesystemAsset("/"+assetPathFile.getFileName().toString(), assetPathFile);
        assertEquals("/"+assetPathFile.getFileName().toString(), asset.getPath());
    }

    @Test
    public void getName() {
        FilesystemAsset asset = new FilesystemAsset("/"+assetPathFile.getFileName().toString(), assetPathFile);
        assertEquals(assetPathFile.getFileName().toString(), asset.getName());

    }

    @Test
    public void getModificationTime() throws IOException {
        Instant modTime = Files.getLastModifiedTime(assetPathFile).toInstant();
        FilesystemAsset asset = new FilesystemAsset("/test123", assetPathFile);
        assertTrue(modTime.equals(asset.getModificationTime()));
    }

    @Test
    public void isContainer() {
        FilesystemAsset asset = new FilesystemAsset("/test1323", assetPathFile);
        assertFalse(asset.isContainer());
        FilesystemAsset asset2 = new FilesystemAsset("/test1234", assetPathDir);
        assertTrue(asset2.isContainer());
    }

    @Test
    public void list() throws IOException {
        FilesystemAsset asset = new FilesystemAsset("/test1234", assetPathFile);
        assertEquals(0, asset.list().size());

        FilesystemAsset asset2 = new FilesystemAsset("/test1235", assetPathDir);
        assertEquals(0, asset2.list().size());
        Path f1 = Files.createTempFile(assetPathDir, "testfile", "dat");
        Path f2 = Files.createTempFile(assetPathDir, "testfile", "dat");
        Path d1 = Files.createTempDirectory(assetPathDir, "testdir");
        assertEquals(3, asset2.list().size());
        assertTrue(asset2.list().stream().anyMatch(p -> p.getName().equals(f1.getFileName().toString())));
        assertTrue(asset2.list().stream().anyMatch(p -> p.getName().equals(f2.getFileName().toString())));
        assertTrue(asset2.list().stream().anyMatch(p -> p.getName().equals(d1.getFileName().toString())));
        Files.deleteIfExists(f1);
        Files.deleteIfExists(f2);
        Files.deleteIfExists(d1);


    }

    @Test
    public void getSize() throws IOException {
        FilesystemAsset asset = new FilesystemAsset("/test1234", assetPathFile);
        assertEquals(0, asset.getSize());

        Files.write(assetPathFile, new String("abcdef").getBytes("ASCII"));
        assertTrue(asset.getSize()>=6);


    }

    @Test
    public void getData() throws IOException {
        FilesystemAsset asset = new FilesystemAsset("/test1234", assetPathFile);
        Files.write(assetPathFile, "abcdef".getBytes("ASCII"));
        try(InputStream is = asset.getData()) {
            assertEquals("abcdef", IOUtils.toString(is, "ASCII"));
        }

    }

    @Test
    public void getDataExceptionOnDir() throws IOException {
        FilesystemAsset asset = new FilesystemAsset("/test1234", assetPathDir);
        Files.write(assetPathFile, "abcdef".getBytes("ASCII"));
        try {
            InputStream is = asset.getData();
            assertFalse("Exception expected for data on dir", true);
        } catch (IOException e) {
            // fine
        }

    }

    @Test
    public void writeData() throws IOException {
        FilesystemAsset asset = new FilesystemAsset("/test1234", assetPathFile);
        Files.write(assetPathFile, "abcdef".getBytes("ASCII"));
        try(OutputStream os  = asset.writeData(true)) {
            IOUtils.write("test12345", os, "ASCII");
        }
        assertEquals("test12345", IOUtils.toString(assetPathFile.toUri().toURL(), "ASCII"));
    }

    @Test
    public void writeDataAppend() throws IOException {
        FilesystemAsset asset = new FilesystemAsset("/test1234", assetPathFile);
        Files.write(assetPathFile, "abcdef".getBytes("ASCII"));
        try(OutputStream os  = asset.writeData(false)) {
            IOUtils.write("test12345", os, "ASCII");
        }
        assertEquals("abcdeftest12345", IOUtils.toString(assetPathFile.toUri().toURL(), "ASCII"));
    }

    @Test
    public void writeDataExceptionOnDir() throws IOException {
        FilesystemAsset asset = new FilesystemAsset("/test1234", assetPathDir);
        try {

            OutputStream os = asset.writeData(true);
            assertTrue("Writing to a directory should throw a IOException", false);
        } catch (IOException e) {
            // Fine
        }
    }

    @Test
    public void storeDataFile() throws IOException {
        FilesystemAsset asset = new FilesystemAsset("/test1234", assetPathFile);
        Path dataFile = Files.createTempFile("testdata", "dat");
        try(OutputStream os = Files.newOutputStream(dataFile)) {
            IOUtils.write("testkdkdkd", os, "ASCII");
        }
        asset.storeDataFile(dataFile);
        assertEquals("testkdkdkd", IOUtils.toString(assetPathFile.toUri().toURL(), "ASCII"));
    }

    @Test
    public void exists() {
        FilesystemAsset asset = new FilesystemAsset("/test1234", assetPathFile);
        assertTrue(asset.exists());
        FilesystemAsset asset2 = new FilesystemAsset("/test1234", Paths.get("abcdefgkdkdk"));
        assertFalse(asset2.exists());

    }

    @Test
    public void getFilePath() {
        FilesystemAsset asset = new FilesystemAsset("/test1234", assetPathFile);
        assertEquals(assetPathFile, asset.getFilePath());
    }
}