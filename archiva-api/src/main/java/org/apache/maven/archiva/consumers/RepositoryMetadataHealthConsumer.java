package org.apache.maven.archiva.consumers;

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

import org.apache.maven.archiva.common.utils.BaseFile;
import org.apache.maven.archiva.reporting.database.MetadataResultsDatabase;
import org.apache.maven.archiva.reporting.group.ReportGroup;
import org.apache.maven.archiva.reporting.model.MetadataResults;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;

/**
 * RepositoryMetadataHealthConsumer 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.common.consumers.Consumer"
 *     role-hint="metadata-health"
 *     instantiation-strategy="per-lookup"
 */
public class RepositoryMetadataHealthConsumer
    extends GenericRepositoryMetadataConsumer
{
    /**
     * @plexus.requirement
     */
    private MetadataResultsDatabase database;

    /**
     * @plexus.requirement role-hint="health"
     */
    private ReportGroup health;

    public void processRepositoryMetadata( RepositoryMetadata metadata, BaseFile file )
    {
        MetadataResults results = database.getMetadataResults( metadata );
        database.clearResults( results );

        health.processMetadata( metadata, repository );
    }
    
    public void processFileProblem( BaseFile path, String message )
    {
        
    }
    
    public String getName()
    {
        return "RepositoryMetadata Health Consumer";
    }
}
