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
import org.apache.archiva.repository.storage.StorageAsset;
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
import java.nio.file.StandardCopyOption;

public class FilesystemStorageTest {

    private FilesystemStorage fsStorage;
    private FilesystemAsset file1Asset;
    private FilesystemAsset dir1Asset;
    private Path baseDir;
    private Path file1;
    private Path dir1;

    @Before
    public void init() throws IOException {
        baseDir = Files.createTempDirectory("FsStorageTest");
        DefaultFileLockManager fl = new DefaultFileLockManager();
        fsStorage = new FilesystemStorage(baseDir,fl);
        Files.createDirectories(baseDir.resolve("dir1"));
        Files.createDirectories(baseDir.resolve("dir2"));
        file1 = Files.createFile(baseDir.resolve("dir1/testfile1.dat"));
        dir1 = Files.createDirectories(baseDir.resolve("dir1/testdir"));
        file1Asset = new FilesystemAsset(fsStorage, "/dir1/testfile1.dat", file1);
        dir1Asset = new FilesystemAsset(fsStorage, "/dir1/testdir", dir1);
    }

    private class StringResult {
        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        String data;
    }


    @After
    public void cleanup() {
        FileUtils.deleteQuietly(file1.toFile());
        FileUtils.deleteQuietly(dir1.toFile());
        FileUtils.deleteQuietly(baseDir.resolve("dir1").toFile());
        FileUtils.deleteQuietly(baseDir.resolve("dir2").toFile());
        FileUtils.deleteQuietly(baseDir.toFile());
    }




    @Test
    public void consumeData() throws IOException {
        try(OutputStream os = Files.newOutputStream(file1)) {
            IOUtils.write("abcdefghijkl", os, "ASCII");
        }
        StringResult result = new StringResult();
        fsStorage.consumeData(file1Asset, is -> consume(is, result), false );
        Assert.assertEquals("abcdefghijkl" ,result.getData());
    }

    private void consume(InputStream is, StringResult result) {
        try {
            result.setData(IOUtils.toString(is, "ASCII"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void getAsset() {
        StorageAsset asset = fsStorage.getAsset("/dir1/testfile1.dat");
        Assert.assertEquals(file1, asset.getFilePath());
    }

    @Test
    public void addAsset() {
        StorageAsset newAsset = fsStorage.addAsset("dir2/test", false);
        Assert.assertNotNull(newAsset);
        Assert.assertFalse(newAsset.isContainer());
        Assert.assertFalse(newAsset.exists());

        StorageAsset newDirAsset = fsStorage.addAsset("/dir2/testdir2", true);
        Assert.assertNotNull(newDirAsset);
        Assert.assertTrue(newDirAsset.isContainer());
        Assert.assertFalse(newDirAsset.exists());
    }

    @Test
    public void removeAsset() throws IOException {
        Assert.assertTrue(Files.exists(file1));
        fsStorage.removeAsset(file1Asset);
        Assert.assertFalse(Files.exists(file1));

        Assert.assertTrue(Files.exists(dir1));
        fsStorage.removeAsset(dir1Asset);
        Assert.assertFalse(Files.exists(dir1));
    }

    @Test
    public void moveAsset() throws IOException {
        Path newFile=null;
        Path newDir=null;
        try {
            Assert.assertTrue(Files.exists(file1));
            try (OutputStream os = Files.newOutputStream(file1)) {
                IOUtils.write("testakdkkdkdkdk", os, "ASCII");
            }
            long fileSize = Files.size(file1);
            fsStorage.moveAsset(file1Asset, "/dir2/testfile2.dat");
            Assert.assertFalse(Files.exists(file1));
            newFile = baseDir.resolve("dir2/testfile2.dat");
            Assert.assertTrue(Files.exists(newFile));
            Assert.assertEquals(fileSize, Files.size(newFile));


            Assert.assertTrue(Files.exists(dir1));
            newDir = baseDir.resolve("dir2/testdir2");
            fsStorage.moveAsset(dir1Asset, "dir2/testdir2");
            Assert.assertFalse(Files.exists(dir1));
            Assert.assertTrue(Files.exists(newDir));
        } finally {
            if (newFile!=null) Files.deleteIfExists(newFile);
            if (newDir!=null) Files.deleteIfExists(newDir);
        }
    }

    @Test
    public void copyAsset() throws IOException {
        Path newFile=null;
        Path newDir=null;
        try {
            Assert.assertTrue(Files.exists(file1));
            try (OutputStream os = Files.newOutputStream(file1)) {
                IOUtils.write("testakdkkdkdkdk", os, "ASCII");
            }
            long fileSize = Files.size(file1);
            fsStorage.copyAsset(file1Asset, "/dir2/testfile2.dat", StandardCopyOption.REPLACE_EXISTING);
            Assert.assertTrue(Files.exists(file1));
            Assert.assertEquals(fileSize, Files.size(file1));
            newFile = baseDir.resolve("dir2/testfile2.dat");
            Assert.assertTrue(Files.exists(newFile));
            Assert.assertEquals(fileSize, Files.size(newFile));

            try {
                fsStorage.copyAsset(file1Asset, "/dir2/testfile2.dat");
                Assert.assertTrue("IOException should be thrown (File exists)", false);
            } catch (IOException ex) {
                Assert.assertTrue("Exception must contain 'file exists'", ex.getMessage().contains("file exists"));
            }

            Assert.assertTrue(Files.exists(dir1));
            newDir = baseDir.resolve("dir2/testdir2");
            fsStorage.copyAsset(dir1Asset, "dir2/testdir2");
            Assert.assertTrue(Files.exists(dir1));
            Assert.assertTrue(Files.exists(newDir));
        } finally {
            if (newFile!=null) Files.deleteIfExists(newFile);
            if (newDir!=null) FileUtils.deleteQuietly(newDir.toFile());
        }
    }
}