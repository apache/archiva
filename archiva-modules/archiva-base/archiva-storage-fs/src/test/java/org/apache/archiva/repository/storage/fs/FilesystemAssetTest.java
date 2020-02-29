package org.apache.archiva.repository.storage.fs;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.common.filelock.DefaultFileLockManager;
import org.apache.archiva.repository.storage.fs.FilesystemAsset;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

public class FilesystemAssetTest {

    Path assetPathFile;
    Path assetPathDir;
    FilesystemStorage filesystemStorage;

    @Before
    public void init() throws IOException {
        assetPathDir = Files.createTempDirectory("assetDir");
        assetPathFile = Files.createTempFile(assetPathDir,"assetFile", "dat");
        filesystemStorage = new FilesystemStorage(assetPathDir, new DefaultFileLockManager());
    }

    @After
    public void cleanup() {

        try {
            Files.deleteIfExists(assetPathFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileUtils.deleteQuietly(assetPathDir.toFile());
    }


    @Test
    public void getPath() {
        FilesystemAsset asset = new FilesystemAsset(filesystemStorage, assetPathFile.getFileName().toString(), assetPathFile);
        Assert.assertEquals("/"+assetPathFile.getFileName().toString(), asset.getPath());
    }

    @Test
    public void getName() {
        FilesystemAsset asset = new FilesystemAsset(filesystemStorage, "/"+assetPathFile.getFileName().toString(), assetPathFile);
        Assert.assertEquals(assetPathFile.getFileName().toString(), asset.getName());

    }

    @Test
    public void getModificationTime() throws IOException {
        Instant modTime = Files.getLastModifiedTime(assetPathFile).toInstant();
        FilesystemAsset asset = new FilesystemAsset(filesystemStorage, "/test123", assetPathFile);
        Assert.assertTrue(modTime.equals(asset.getModificationTime()));
    }

    @Test
    public void isContainer() {
        FilesystemAsset asset = new FilesystemAsset(filesystemStorage, "/test1323", assetPathFile);
        Assert.assertFalse(asset.isContainer());
        FilesystemAsset asset2 = new FilesystemAsset(filesystemStorage, "/test1234", assetPathDir);
        Assert.assertTrue(asset2.isContainer());
    }

    @Test
    public void list() throws IOException {
        FilesystemAsset asset = new FilesystemAsset(filesystemStorage, "/test1234", assetPathFile);
        Assert.assertEquals(0, asset.list().size());

        FilesystemAsset asset2 = new FilesystemAsset(filesystemStorage, "/test1235", assetPathDir);
        Assert.assertEquals(1, asset2.list().size());
        Path f1 = Files.createTempFile(assetPathDir, "testfile", "dat");
        Path f2 = Files.createTempFile(assetPathDir, "testfile", "dat");
        Path d1 = Files.createTempDirectory(assetPathDir, "testdir");
        Assert.assertEquals(4, asset2.list().size());
        Assert.assertTrue(asset2.list().stream().anyMatch(p -> p.getName().equals(f1.getFileName().toString())));
        Assert.assertTrue(asset2.list().stream().anyMatch(p -> p.getName().equals(f2.getFileName().toString())));
        Assert.assertTrue(asset2.list().stream().anyMatch(p -> p.getName().equals(d1.getFileName().toString())));
        Files.deleteIfExists(f1);
        Files.deleteIfExists(f2);
        Files.deleteIfExists(d1);


    }

    @Test
    public void getSize() throws IOException {
        FilesystemAsset asset = new FilesystemAsset(filesystemStorage, "/test1234", assetPathFile);
        Assert.assertEquals(0, asset.getSize());

        Files.write(assetPathFile, new String("abcdef").getBytes("ASCII"));
        Assert.assertTrue(asset.getSize()>=6);


    }

    @Test
    public void getData() throws IOException {
        FilesystemAsset asset = new FilesystemAsset(filesystemStorage, "/test1234", assetPathFile);
        Files.write(assetPathFile, "abcdef".getBytes("ASCII"));
        try(InputStream is = asset.getReadStream()) {
            Assert.assertEquals("abcdef", IOUtils.toString(is, "ASCII"));
        }

    }

    @Test
    public void getDataExceptionOnDir() throws IOException {
        FilesystemAsset asset = new FilesystemAsset(filesystemStorage, "/test1234", assetPathDir);
        Files.write(assetPathFile, "abcdef".getBytes("ASCII"));
        try {
            InputStream is = asset.getReadStream();
            Assert.assertFalse("Exception expected for data on dir", true);
        } catch (IOException e) {
            // fine
        }

    }

    @Test
    public void writeData() throws IOException {
        FilesystemAsset asset = new FilesystemAsset(filesystemStorage, "/test1234", assetPathFile);
        Files.write(assetPathFile, "abcdef".getBytes("ASCII"));
        try(OutputStream os  = asset.getWriteStream(true)) {
            IOUtils.write("test12345", os, "ASCII");
        }
        Assert.assertEquals("test12345", IOUtils.toString(assetPathFile.toUri().toURL(), "ASCII"));
    }

    @Test
    public void writeDataAppend() throws IOException {
        FilesystemAsset asset = new FilesystemAsset(filesystemStorage, "/test1234", assetPathFile);
        Files.write(assetPathFile, "abcdef".getBytes("ASCII"));
        try(OutputStream os  = asset.getWriteStream(false)) {
            IOUtils.write("test12345", os, "ASCII");
        }
        Assert.assertEquals("abcdeftest12345", IOUtils.toString(assetPathFile.toUri().toURL(), "ASCII"));
    }

    @Test
    public void writeDataExceptionOnDir() throws IOException {
        FilesystemAsset asset = new FilesystemAsset(filesystemStorage, "/test1234", assetPathDir);
        try {

            OutputStream os = asset.getWriteStream(true);
            Assert.assertTrue("Writing to a directory should throw a IOException", false);
        } catch (IOException e) {
            // Fine
        }
    }

    @Test
    public void storeDataFile() throws IOException {
        FilesystemAsset asset = new FilesystemAsset(filesystemStorage, "/test1234", assetPathFile);
        Path dataFile = Files.createTempFile("testdata", "dat");
        try(OutputStream os = Files.newOutputStream(dataFile)) {
            IOUtils.write("testkdkdkd", os, "ASCII");
        }
        asset.replaceDataFromFile(dataFile);
        Assert.assertEquals("testkdkdkd", IOUtils.toString(assetPathFile.toUri().toURL(), "ASCII"));
    }

    @Test
    public void exists() {
        FilesystemAsset asset = new FilesystemAsset(filesystemStorage, "/test1234", assetPathFile);
        Assert.assertTrue(asset.exists());
        FilesystemAsset asset2 = new FilesystemAsset(filesystemStorage, "/test1234", Paths.get("abcdefgkdkdk"));
        Assert.assertFalse(asset2.exists());

    }

    @Test
    public void getFilePath() {
        FilesystemAsset asset = new FilesystemAsset(filesystemStorage, "/test1234", assetPathFile);
        Assert.assertEquals(assetPathFile, asset.getFilePath());
    }
}