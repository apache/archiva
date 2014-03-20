package org.apache.archiva.metadata.repository.cassandra;

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

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.entitystore.EntityManager;
import org.apache.archiva.metadata.repository.cassandra.model.ArtifactMetadataModel;
import org.apache.archiva.metadata.repository.cassandra.model.MetadataFacetModel;
import org.apache.archiva.metadata.repository.cassandra.model.Namespace;
import org.apache.archiva.metadata.repository.cassandra.model.Project;
import org.apache.archiva.metadata.repository.cassandra.model.ProjectVersionMetadataModel;
import org.apache.archiva.metadata.repository.cassandra.model.Repository;

/**
 * @author Olivier Lamy
 * @since 2.0.0
 */
public interface CassandraArchivaManager
{
    Keyspace getKeyspace();

    void start();

    void shutdown();

    boolean started();

    EntityManager<Repository, String> getRepositoryEntityManager();

    EntityManager<Namespace, String> getNamespaceEntityManager();

    EntityManager<Project, String> getProjectEntityManager();

    EntityManager<ArtifactMetadataModel, String> getArtifactMetadataModelEntityManager();

    EntityManager<MetadataFacetModel, String> getMetadataFacetModelEntityManager();

    EntityManager<ProjectVersionMetadataModel, String> getProjectVersionMetadataModelEntityManager();


}
