package org.apache.archiva.webdav;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.proxy.maven.MavenRepositoryProxyHandler;
import org.apache.archiva.proxy.model.ProxyFetchResult;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.io.FileUtils;

import java.io.IOException;

class OverridingRepositoryProxyHandler
    extends MavenRepositoryProxyHandler
{
    private ArchivaDavResourceFactoryTest archivaDavResourceFactoryTest;

    public OverridingRepositoryProxyHandler(ArchivaDavResourceFactoryTest archivaDavResourceFactoryTest) {
        this.archivaDavResourceFactoryTest = archivaDavResourceFactoryTest;
    }

    @Override
    public ProxyFetchResult fetchMetadataFromProxies( ManagedRepository repository, String logicalPath )
    {
        StorageAsset target = repository.getAsset( logicalPath );
        try
        {
            FileUtils.copyFile( archivaDavResourceFactoryTest.getProjectBase().resolve( "target/test-classes/maven-metadata.xml" ).toFile(), target.getFilePath().toFile() );
        }
        catch ( IOException e )
        {

        }

        return new ProxyFetchResult( target, true );
    }
}
