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

package org.apache.archiva.repository.content.base;

import org.apache.archiva.common.filelock.DefaultFileLockManager;
import org.apache.archiva.repository.content.ContentItem;
import org.apache.archiva.repository.content.base.builder.OptBuilder;
import org.apache.archiva.repository.mock.ManagedRepositoryContentMock;
import org.apache.archiva.repository.storage.fs.FilesystemStorage;
import org.apache.archiva.repository.storage.StorageAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public abstract class ContentItemTest
{
    protected ManagedRepositoryContentMock repository;
    protected FilesystemStorage storage;
    protected StorageAsset asset;

    @BeforeEach
    public void setup() throws IOException
    {
        this.repository = new ManagedRepositoryContentMock( );

        this.storage = new FilesystemStorage( Paths.get( "target" ), new DefaultFileLockManager( ) );

        this.asset = storage.getAsset( "" );

    }

    public abstract OptBuilder getBuilder();

    @Test
    void testWithAttribute() {
        ContentItem test = getBuilder( ).withAttribute( "testkey1", "testvalue1" ).withAttribute( "testkey2", "testvalue2" ).build( );

        assertNotNull( test.getAttributes( ) );
        assertEquals( 2, test.getAttributes( ).size( ) );
        assertEquals( "testvalue1", test.getAttribute( "testkey1" ) );
        assertEquals( "testvalue2", test.getAttribute( "testkey2" ) );
        assertNull( test.getAttribute( "key123" ) );

    }

}
