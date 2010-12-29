package org.apache.archiva.metadata.repository.file;

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
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;

import java.util.Map;

/**
 * @plexus.component role="org.apache.archiva.metadata.repository.RepositorySessionFactory" role-hint="file"
 */
public class FileRepositorySessionFactory
    implements RepositorySessionFactory
{
    /**
     * @plexus.requirement role="org.apache.archiva.metadata.model.MetadataFacetFactory"
     */
    private Map<String, MetadataFacetFactory> metadataFacetFactories;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration configuration;

    /**
     * @plexus.requirement
     */
    private MetadataResolver metadataResolver;

    public RepositorySession createSession()
    {
        MetadataRepository metadataRepository = new FileMetadataRepository( metadataFacetFactories, configuration );

        return new RepositorySession( metadataRepository, metadataResolver );
    }
}
