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

import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;

/**
 * @author Olivier Lamy
 * @since 2.0.0
 */
public interface CassandraArchivaManager
{
    void start();

    void shutdown();

    boolean started();

    Keyspace getKeyspace();

    Cluster getCluster();

    String getRepositoryFamilyName();

    String getNamespaceFamilyName();

    String getProjectFamilyName();

    String getProjectVersionMetadataFamilyName();

    String getArtifactMetadataFamilyName();

    String getMetadataFacetFamilyName();

    String getMailingListFamilyName();

    String getLicenseFamilyName();

    String getDependencyFamilyName();

    String getChecksumFamilyName();

}
