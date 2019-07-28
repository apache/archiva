package org.apache.archiva.proxy.model;

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


import org.apache.archiva.repository.storage.StorageAsset;

/**
 * A result from a proxy fetch operation.
 *
 * @since 2.1.2
 */
public class ProxyFetchResult
{

    //The file returned
    private StorageAsset file;

    //Was the local file modified by the fetch?
    private boolean modified;

    public ProxyFetchResult( StorageAsset file, boolean modified )
    {
        this.file = file;
        this.modified = modified;
    }

    public StorageAsset getFile()
    {
        return file;
    }

    public boolean isModified()
    {
        return modified;
    }
}
