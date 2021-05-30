package org.apache.archiva.repository.storage.util;

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


import org.apache.archiva.repository.storage.AssetType;
import org.apache.archiva.repository.storage.RepositoryStorage;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.archiva.repository.storage.mock.MockAsset;
import org.apache.archiva.repository.storage.mock.MockStorage;
import org.junit.jupiter.api.Assertions;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
class StorageUtilTest extends AbstractStorageUtilTest
{


    private MockStorage createStorage(MockAsset root) {
        return new MockStorage( root );
    }

    private MockAsset createAsset( MockAsset parent, String name, AssetType type ) {
        if (parent==null) {
            return new MockAsset( name );
        } else
        {
            return new MockAsset( parent, name );
        }
    }


    protected void activateException(MockAsset root) {
        root.list( ).get( 1 ).list( ).get( 2 ).setThrowException( true );
    }

    @Override
    protected StorageAsset createAsset( StorageAsset parent, String name, AssetType type )
    {
        return createAsset( (MockAsset) parent, name, type);
    }

    @Override
    protected StorageAsset createRootAsset( )
    {
        return new MockAsset( "" );
    }

    @Override
    protected void activateException( StorageAsset root )
    {
        activateException( (MockAsset)root );
    }

    @Override
    protected RepositoryStorage createStorage( StorageAsset root )
    {
        return new MockStorage( (MockAsset) root );
    }

    protected void testDeletionStatus( int expected, RepositoryStorage storage )
    {
        if ( storage instanceof MockStorage )
        {
            Assertions.assertEquals( expected, ( (MockStorage) storage ).getStatus( ).size( MockStorage.REMOVE ) );
        }
        else
        {
            Assertions.fail( "Deletion status not implemented for this storage " + storage.getClass( ).getName( ) );
        }
    }
}