package org.apache.maven.archiva.discoverer.consumers;

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

import org.apache.maven.archiva.discoverer.PathUtil;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * MockRepositoryMetadataConsumer 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.discoverer.DiscovererConsumers"
 *     role-hint="mock-metadata"
 *     instantiation-strategy="per-lookup"
 */
public class MockRepositoryMetadataConsumer
    extends GenericRepositoryMetadataConsumer
{
    private Map repositoryMetadataMap = new HashMap();

    public void processRepositoryMetadata( RepositoryMetadata metadata, File file )
    {
        String relpath = PathUtil.getRelative( repository.getBasedir(), file );
        repositoryMetadataMap.put( relpath, metadata );
    }

    public Map getRepositoryMetadataMap()
    {
        return repositoryMetadataMap;
    }
}