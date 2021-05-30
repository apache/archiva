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
import org.apache.archiva.repository.storage.AssetType;
import org.apache.archiva.repository.storage.RepositoryStorage;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.repository.storage.util.AbstractStorageUtilTest;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@TestInstance( TestInstance.Lifecycle.PER_CLASS)
public class FileSystemStorageUtilTest extends AbstractStorageUtilTest
{

    List<Path> tmpDirs = new ArrayList<>( );

    @Override
    protected StorageAsset createAsset( StorageAsset parent, String name, AssetType type )
    {
        if (parent instanceof FilesystemAsset) {
            return createAsset( (FilesystemAsset) parent, name, type );
        } else {
            fail( "Bad asset instance type" );
            return null;
        }
    }

    @AfterAll
    private void cleanup() {
        for( Path dir : tmpDirs) {
            if (Files.exists( dir )) {
                FileUtils.deleteQuietly( dir.toFile( ) );
            }
        }
    }

    private FilesystemAsset createAsset(FilesystemAsset parent, String name, AssetType type) {
        FilesystemAsset asset = (FilesystemAsset) parent.resolve( name );
        try
        {
            asset.create(type);
            return asset;
        }
        catch ( IOException e )
        {
            fail( "Could not create asset " + e.getMessage( ) );
            return null;
        }
    }

    @Override
    protected StorageAsset createRootAsset( )
    {
        try
        {
            Path tmpDir = Files.createTempDirectory( "testfs" );
            tmpDirs.add( tmpDir );
            FilesystemStorage storage = new FilesystemStorage( tmpDir, new DefaultFileLockManager( ) );
            return storage.getRoot( );
        }
        catch ( IOException e )
        {
            fail( "Could not create storage" );
            return null;
        }
    }

    @Override
    protected void activateException( StorageAsset root )
    {
        // Not done here
    }

    @Override
    protected RepositoryStorage createStorage( StorageAsset root )
    {
        if (root instanceof FilesystemAsset) {
            return root.getStorage( );
        } else {
            fail( "Wrong asset implementation " + root.getClass( ).getName( ) );
            return null;
        }
    }

    @Override
    protected void testDeletionStatus( int expected, RepositoryStorage storage )
    {
        assertFalse( Files.exists( storage.getRoot( ).getFilePath( ) ) );
    }
}
